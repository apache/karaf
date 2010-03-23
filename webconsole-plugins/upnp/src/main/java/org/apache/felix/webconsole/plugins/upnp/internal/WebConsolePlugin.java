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
package org.apache.felix.webconsole.plugins.upnp.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Provides Web Console interface to UPnP Devices.
 */
public class WebConsolePlugin extends SimpleWebConsolePlugin
{

    private static final String LABEL = "upnp";
    private static final String TITLE = "%pluginTitle";
    private static final String CSS[] = { "/" + LABEL + "/res/upnp.css", //
            "/" + LABEL + "/res/jquery-treeview-1.4/jquery.treeview.css", //
    };

    private final ServiceTracker tracker;
    ControlServlet controller;

    //templates
    private final String TEMPLATE;

    /**
     * Creates new plugin
     * 
     * @param tracker the UPnP Device tracker
     */
    public WebConsolePlugin(ServiceTracker tracker)
    {
        super(LABEL, TITLE, CSS);
        this.tracker = tracker;

        // load templates
        TEMPLATE = readTemplateFile("/res/upnp.html");
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#activate(org.osgi.framework.BundleContext)
     */
    public void activate(BundleContext bundleContext)
    {
        super.activate(bundleContext);
        controller = new ControlServlet(bundleContext, tracker);
    }

    /**
     * @see org.apache.felix.webconsole.SimpleWebConsolePlugin#deactivate()
     */
    public void deactivate()
    {
        if (controller != null)
        {
            controller.close();
            controller = null;
        }
        super.deactivate();
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        res.getWriter().print(TEMPLATE);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        if (request.getParameter("action") != null)
        {
            controller.doPost(request, response);
        }
        else
        {
            super.doPost(request, response);
        }
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        if (request.getParameter("icon") != null)
        {
            controller.doGet(request, response);
        }
        else
        {
            super.doGet(request, response);
        }
    }

}
