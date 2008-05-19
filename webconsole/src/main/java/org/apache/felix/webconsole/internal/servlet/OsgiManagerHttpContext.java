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

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;


final class OsgiManagerHttpContext implements HttpContext
{

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    /**
     * The encoding table which causes BaseFlex encoding/deconding to work like
     * Base64 encoding/deconding.
     */
    private static final String base64Table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * The pad character used in Base64 encoding/deconding.
     */
    private static final char base64Pad = '=';

    String realm;

    String userId;
    String user;

    private final HttpContext base;


    OsgiManagerHttpContext( HttpService httpService, String realm, String userId, String password )
    {
        this.base = httpService.createDefaultHttpContext();
        this.realm = realm;
        this.userId = userId;
        this.user = encode( userId, password );
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
        if ( this.user == null )
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
                if ( authType.equalsIgnoreCase( AUTHENTICATION_SCHEME_BASIC ) && this.user.equals( authInfo ) )
                {

                    // as per the spec, set attributes
                    request.setAttribute( HttpContext.AUTHENTICATION_TYPE, "" );
                    request.setAttribute( HttpContext.REMOTE_USER, this.userId );

                    // succeed
                    return true;
                }
            }
        }

        // request authentication
        response.setHeader( HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"" );
        try
        {
            response.sendError( HttpServletResponse.SC_UNAUTHORIZED );
        }
        catch ( IOException ioe )
        {
            // failed sending the error, fall back to setting the status
            response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        }

        // inform HttpService that authentication failed
        return false;
    }


    /**
     * Base64 encodes the user name and password for comparison to the value of
     * a Basic encoded HTTP header authentication.
     *
     * @param user The name of the user in the username/password pair
     * @param password The password in the username/password pair
     * @return The Base64 encoded username/password pair or <code>null</code>
     *         if <code>user</code> is <code>null</code> or empty.
     */
    public static String encode( String user, String password )
    {

        /* check arguments */
        if ( user == null || user.length() == 0 )
            return null;

        String srcString = user + ":";
        if ( password != null && password.length() > 0 )
        {
            srcString += password;
        }

        // need bytes
        byte[] src;
        try
        {
            src = srcString.getBytes( "ISO-8859-1" );
        }
        catch ( UnsupportedEncodingException uee )
        {
            // we do not expect this, the API presribes ISO-8859-1 to be present
            // anyway, fallback to platform default
            src = srcString.getBytes();
        }

        int srcsize = src.length;
        int tbllen = base64Table.length();

        StringBuffer result = new StringBuffer( srcsize );

        /* encode */
        int tblpos = 0;
        int bitpos = 0;
        int bitsread = -1;
        int inpos = 0;
        int pos = 0;

        while ( inpos <= srcsize )
        {

            if ( bitsread < 0 )
            {
                if ( inpos < srcsize )
                {
                    pos = src[inpos++];
                }
                else
                {
                    // inpos++;
                    // pos = 0;
                    break;
                }
                bitsread = 7;
            }

            tblpos = 0;
            bitpos = tbllen / 2;
            while ( bitpos > 0 )
            {
                if ( bitsread < 0 )
                {
                    pos = ( inpos < srcsize ) ? src[inpos] : '\0';
                    inpos++;
                    bitsread = 7;
                }

                /* test if bit at pos <bitpos> in <pos> is set.. */
                if ( ( ( 1 << bitsread ) & pos ) != 0 )
                    tblpos += bitpos;

                bitpos /= 2;
                bitsread--;
            }

            // got one
            result.append( base64Table.charAt( tblpos ) );
        }

        /* add the padding bytes */
        while ( bitsread != -1 )
        {
            bitpos = tbllen / 2;
            while ( bitpos > 0 )
            {
                if ( bitsread < 0 )
                    bitsread = 7;
                bitpos /= 2;
                bitsread--;
            }

            result.append( base64Pad );
        }

        return result.toString();
    }
}