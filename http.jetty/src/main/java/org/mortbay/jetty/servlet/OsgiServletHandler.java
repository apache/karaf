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
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.jetty.Activator;
import org.mortbay.util.LazyList;
import org.osgi.service.log.LogService;


public class OsgiServletHandler extends ServletHandler
{
    // allow external adding of osgi servlet holder
    public void addOsgiServletHolder( String pathSpec, ServletHolder holder )
    {
        super.addServletWithMapping( holder, pathSpec );
    }


    public ServletHolder removeOsgiServletHolder( String pathSpec )
    {
        ServletMapping oldMapping = null;
        ServletMapping[] mappings = getServletMappings();
        for ( int i = 0; i < mappings.length && oldMapping == null; i++ )
        {
            String[] pathSpecs = mappings[i].getPathSpecs();
            for ( int j = 0; j < pathSpecs.length && oldMapping == null; j++ )
            {
                if ( pathSpec.equals( pathSpecs[j] ) )
                {
                    oldMapping = mappings[i];
                }
            }
        }

        if ( oldMapping == null )
        {
            return null;
        }

        ServletHolder[] holders = getServlets();
        if ( holders != null )
        {
            holders = ( ServletHolder[] ) holders.clone();
        }

        ServletHolder oldHolder = null;
        for ( int i = 0; i < holders.length; i++ )
        {
            if ( oldMapping.getServletName().equals( holders[i].getName() ) )
            {
                oldHolder = holders[i];
            }
        }
        if ( oldHolder == null )
        {
            return null;
        }

        try
        {
            setServlets( ( ServletHolder[] ) LazyList.removeFromArray( holders, oldHolder ) );
            setServletMappings( ( ServletMapping[] ) LazyList.removeFromArray( mappings, oldMapping ) );

            if (oldHolder.isStarted() && isStopped()) {
                oldHolder.stop();
            }

            return ( ServletHolder ) oldHolder;
        }
        catch ( Exception e )
        {
            setServlets( holders );
            if ( e instanceof RuntimeException )
                throw ( RuntimeException ) e;
            throw new RuntimeException( e );
        }
    }


    // override standard handler behaviour to return resource from OSGi
    // HttpContext
    public URL getResource( String uriInContext )
    {
        Activator.debug( "OSGI ServletHandler getResource:" + uriInContext );
        return null;
    }


    public void handle( String target, HttpServletRequest request, HttpServletResponse response, int type )
        throws IOException, ServletException
    {
        Activator.debug( "dispatch path = " + target );
        super.handle( target, request, response, type );
    }

}
