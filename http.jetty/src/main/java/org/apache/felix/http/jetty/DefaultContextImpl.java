/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.jetty;


import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;


/**
 * Implementation of default HttpContext as per OSGi specification.
 *
 * Notes
 *
 *      - no current inclusion/support for permissions
 *      - security allows all request. Spec leaves security handling to be
 *        implementation specific, but does outline some suggested handling.
 *        Deeper than my understanding of HTTP at this stage, so left for now.
 */
public class DefaultContextImpl implements HttpContext
{
    private Bundle m_bundle;


    public DefaultContextImpl( Bundle bundle )
    {
        m_bundle = bundle;
    }


    public String getMimeType( String name )
    {
        return null;
    }


    public URL getResource( String name )
    {
        //TODO: need to grant "org.osgi.framework.AdminPermission" when
        //      permissions are included.
        Activator.debug( "getResource for:" + name );

        //TODO: temp measure for name. Bundle classloading doesn't seem to find
        // resources which have a leading "/". This code should be removed
        // if the bundle classloader is changed to allow a leading "/"
        if ( name.startsWith( "/" ) )
        {
            name = name.substring( 1 );
        }

        return m_bundle.getResource( name );
    }


    public boolean handleSecurity( HttpServletRequest request, HttpServletResponse response )
    {
        //TODO: need to look into what's appropriate for default security
        //      handling. Default to all requests to be serviced for now.
        return true;
    }
}