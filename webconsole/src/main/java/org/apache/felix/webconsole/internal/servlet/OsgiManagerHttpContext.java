/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.internal.servlet;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;


final class OsgiManagerHttpContext implements HttpContext
{

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    String realm;

    WebConsoleSecurityProvider securityProvider;

    private final HttpContext base;


    OsgiManagerHttpContext( HttpService httpService, String realm, WebConsoleSecurityProvider securityProvider)
    {
        this.base = httpService.createDefaultHttpContext();
        this.realm = realm;
        this.securityProvider = securityProvider;
    }


    public String getMimeType( String name )
    {
        return this.base.getMimeType( name );
    }


    public URL getResource( String name )
    {
        URL url = this.base.getResource( name );
        if ( url == null && name.endsWith( "/" ) )
        {
            return this.base.getResource( name.substring( 0, name.length() - 1 ) );
        }
        return url;
    }


    /**
     * Checks the <code>Authorization</code> header of the request for Basic
     * authentication user name and password. If contained, the credentials are
     * compared to the user name and password set for the OSGi Console.
     * <p>
     * If no user name is set, the <code>Authorization</code> header is
     * ignored and the client is assumed to be authenticated.
     *
     * @param request The HTTP request used to get the
     *            <code>Authorization</code> header.
     * @param response The HTTP response used to send the authentication request
     *            if authentication is required but not satisfied.
     * @return <code>true</code> if authentication is required and not
     *         satisfied by the request.
     */
    public boolean handleSecurity( HttpServletRequest request, HttpServletResponse response )
    {

        // don't care for authentication if no user name is configured
        if ( this.securityProvider == null )
        {
            return true;
        }

        // Return immediately if the header is missing
        String authHeader = request.getHeader( HEADER_AUTHORIZATION );
        if ( authHeader != null && authHeader.length() > 0 )
        {

            // Get the authType (Basic, Digest) and authInfo (user/password)
            // from
            // the header
            authHeader = authHeader.trim();
            int blank = authHeader.indexOf( ' ' );
            if ( blank > 0 )
            {
                String authType = authHeader.substring( 0, blank );
                String authInfo = authHeader.substring( blank ).trim();

                // Check whether authorization type matches
                if ( authType.equalsIgnoreCase( AUTHENTICATION_SCHEME_BASIC ))
                {
                    try
                    {
                        String srcString = base64Decode( authInfo );
                        int i = srcString.indexOf(':');
                        String username = srcString.substring(0, i);
                        String password = srcString.substring(i + 1);

                        // authenticate
                        Object id = securityProvider.authenticate( username, password );
                        if (id != null) {
                            // as per the spec, set attributes
                            request.setAttribute( HttpContext.AUTHENTICATION_TYPE, "" );
                            request.setAttribute( HttpContext.REMOTE_USER, username );

                            // succeed
                            return true;
                        }
                    }
                    catch (Exception e)
                    {
                        // Ignore
                    }
                }
            }
        }

        // request authentication
        try
        {
            response.setHeader( HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"" );
            response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
            response.setContentLength(0);
            response.flushBuffer();
        }
        catch ( IOException ioe )
        {
            // failed sending the response ... cannot do anything about it
        }

        // inform HttpService that authentication failed
        return false;
    }

    public static String base64Decode( String srcString )
    {
        byte[] transformed = Base64.decodeBase64( srcString );
        try
        {
            return new String( transformed, "ISO-8859-1" );
        }
        catch ( UnsupportedEncodingException uee )
        {
            return new String( transformed );
        }
    }

}