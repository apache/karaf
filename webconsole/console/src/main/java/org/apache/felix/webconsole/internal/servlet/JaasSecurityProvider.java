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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AccountException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.felix.webconsole.WebConsoleSecurityProvider2;
import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaasSecurityProvider implements WebConsoleSecurityProvider2, ManagedService {

	private static final Logger LOG = LoggerFactory.getLogger(JaasSecurityProvider.class);

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private String realm;
    private String role;
    private int sessionTimeout;

    public JaasSecurityProvider() {
        updated(null);
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public Object authenticate(final String username, final String password) {
        return doAuthenticate( "?", username, password );
    }

    @Override
    public void updated(Dictionary<String, ?> properties) {
        if (properties == null) {
            properties = new Hashtable<>();
        }
        realm = getString(properties, "realm", "karaf");
        role = getString(properties, "role", "admin");
        sessionTimeout = Integer.parseInt(getString(properties, "sessionTimeout", "0"));
    }

    private String getString(Dictionary<String, ?> properties, String key, String def) {
        if (properties != null) {
            Object val = properties.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return def;
    }

    public Subject doAuthenticate(final String address, final String username, final String password) {
        try {
            Subject subject = new Subject();
            subject.getPrincipals().add(new ClientPrincipal("webconsole", address));
            LoginContext loginContext = new LoginContext(realm, subject, callbacks -> {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        ((NameCallback) callback).setName(username);
                    } else if (callback instanceof PasswordCallback) {
                        ((PasswordCallback) callback).setPassword(password.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(callback);
                    }
                }
            });
            loginContext.login();
            if (role != null && role.length() > 0) {
                String clazz = "org.apache.karaf.jaas.boot.principal.RolePrincipal";
                String name = role;
                int idx = role.indexOf(':');
                if (idx > 0) {
                    clazz = role.substring(0, idx);
                    name = role.substring(idx + 1);
                }
                boolean found = false;
                for (Principal p : subject.getPrincipals()) {
                    if (p.getClass().getName().equals(clazz)
                            && p.getName().equals(name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new FailedLoginException("User does not have the required role " + role);
                }
            }
            return subject;
        } catch (FailedLoginException e) {
            LOG.debug("Login failed", e);
            return null;
        } catch (AccountException e) {
            LOG.warn("Account failure", e);
            return null;
        } catch (GeneralSecurityException e) {
            LOG.error("General Security Exception", e);
            return null;
        }
    }

    public boolean authorize(Object o, String s) {
        return true;
    }

    @Override
    public boolean authenticate( HttpServletRequest request, HttpServletResponse response )
    {
        // Return immediately if the header is missing
        String authHeader = request.getHeader( HEADER_AUTHORIZATION );
        if ( authHeader != null && authHeader.length() > 0 )
        {

            // Get the authType (Basic, Digest) and authInfo (user/password)
            // from the header
            authHeader = authHeader.trim();
            int blank = authHeader.indexOf( ' ' );
            if ( blank > 0 )
            {
                String authType = authHeader.substring( 0, blank );
                String authInfo = authHeader.substring( blank ).trim();

                // Check whether authorization type matches
                if ( authType.equalsIgnoreCase( AUTHENTICATION_SCHEME_BASIC ) )
                {
                    try
                    {
                        String srcString = base64Decode( authInfo );
                        int i = srcString.indexOf( ':' );
                        String username = srcString.substring( 0, i );
                        String password = srcString.substring( i + 1 );

                        // authenticate
                        Subject subject = null;
                        try
                        {
                            HttpSession session = request.getSession(false);
                            if ( session != null )
                            {
                                subject = (Subject) session.getAttribute( KarafOsgiManager.SUBJECT_RUN_AS );
                            }
                        }
                        catch ( Throwable t )
                        {
                            // ignore
                        }
                        if ( subject == null )
                        {
                            String addr = request.getRemoteHost() + ":" + request.getRemotePort();
                            subject = doAuthenticate( addr, username, password );
                        }
                        if ( subject != null )
                        {
                            // as per the spec, set attributes
                            request.setAttribute( HttpContext.AUTHENTICATION_TYPE, HttpServletRequest.BASIC_AUTH );
                            request.setAttribute( HttpContext.REMOTE_USER, username );

                            // set web console user attribute
                            request.setAttribute( WebConsoleSecurityProvider2.USER_ATTRIBUTE, username );

                            // set the JAAS subject
                            request.setAttribute( KarafOsgiManager.SUBJECT_RUN_AS, subject );

                            // create a session and store the information
                            try
                            {
                                HttpSession session = request.getSession(true);
                                if (sessionTimeout != 0)
                                {
                                    session.setMaxInactiveInterval(sessionTimeout);
                                }
                                session.setAttribute( KarafOsgiManager.SUBJECT_RUN_AS, subject );
                            }
                            catch ( Throwable t )
                            {
                                // ignore
                            }

                            // succeed
                            return true;
                        }
                    }
                    catch ( Exception e )
                    {
                        LOG.warn("Error during authentication", e);
                    }
                }
            }
        }

        requireAuthentication(response);

        // inform HttpService that authentication failed
        return false;
    }

    private void requireAuthentication(HttpServletResponse response) {
        response.setHeader( HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"" );
        response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        response.setContentLength( 0 );
        try {
            response.flushBuffer();
        } catch (IOException e) {
            LOG.debug("Error flushing after sending auth required", e);
        }
    }

    private static String base64Decode( String srcString )
    {
        byte[] transformed = Base64.decodeBase64(srcString);
        return new String( transformed, StandardCharsets.ISO_8859_1);
    }


}
