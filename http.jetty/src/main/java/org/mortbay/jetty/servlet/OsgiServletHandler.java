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
package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.http.PathMap;
import org.mortbay.util.Code;


public class OsgiServletHandler
        extends ServletHandler
{
    protected org.osgi.service.http.HttpContext     m_osgiHttpContext;


    public OsgiServletHandler(
            org.osgi.service.http.HttpContext osgiHttpContext)
    {
        m_osgiHttpContext = osgiHttpContext;
    }


    // allow external adding of osgi servlet holder
    public void addOsgiServletHolder(String pathSpec, ServletHolder holder)
    {
        super.addServletHolder(pathSpec, holder);
    }


    public OsgiServletHolder removeOsgiServletHolder(String pathSpec)
    {
        OsgiServletHolder holder = (OsgiServletHolder)
                super.getServletHolder(pathSpec);
        PathMap map = super.getServletMap();
        map.remove(pathSpec);

        // Remove holder from handler name map to allow re-registration.
        super._nameMap.remove(holder.getName());

        return holder;
    }


    // override standard handler behaviour to return resource from OSGi
    // HttpContext
    public URL getResource(String uriInContext)
                         throws MalformedURLException
    {
        Code.debug("OSGI ServletHandler getResource:" + uriInContext);
        return m_osgiHttpContext.getResource(uriInContext);
    }

    // override standard behaviour to check context first
    protected void dispatch(String pathInContext,
                  HttpServletRequest request,
                  HttpServletResponse response,
                  ServletHolder servletHolder)
        throws ServletException,
               UnavailableException,
               IOException
    {
        Code.debug("dispatch path = " + pathInContext);
        if (m_osgiHttpContext.handleSecurity(request, response))
        {
            // service request
            servletHolder.handle(request,response);
        }
        else
        {
            //TODO: any other error/auth handling we should do in here?
            response.flushBuffer();
        }
    }
}


