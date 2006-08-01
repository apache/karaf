/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.mortbay.jetty.servlet;


import java.util.Dictionary;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.UnavailableException;

public class OsgiServletHolder
        extends
                ServletHolder
{
    private Servlet m_servlet;
    private ServletConfig m_config;


    public OsgiServletHolder(ServletHandler handler, Servlet servlet,
            String name, Dictionary params)
    {
        super(handler,name,servlet.getClass().getName());
        m_servlet = servlet;

        // Seemed safer to copy params into parent holder, rather than override
        // the getInitxxx methods.
        if (params != null)
        {
            Enumeration e = params.keys();
            while (e.hasMoreElements())
            {
                Object key = e.nextElement();
                super.put(key, params.get(key));
            }
        }
    }

    public synchronized Servlet getServlet()
        throws UnavailableException
    {
        return m_servlet;        
    }

    public Servlet getOsgiServlet()
    {
        return m_servlet;
    }


    // override "Holder" method to prevent instantiation
    public synchronized Object newInstance()
        throws InstantiationException,
               IllegalAccessException
    {
        return getOsgiServlet();
    }

    // override "Holder" method to prevent attempt to load
    // the servlet class.
    public void start()
        throws Exception
    {
        _class=m_servlet.getClass();
        
        m_config=new Config();
        m_servlet.init(m_config);        
    }

    // override "Holder" method to prevent destroy, which is only called
    // when a bundle manually unregisters
    public void stop()
    {
    }
}

