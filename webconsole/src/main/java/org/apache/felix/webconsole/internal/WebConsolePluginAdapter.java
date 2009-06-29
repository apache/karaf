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
package org.apache.felix.webconsole.internal;


import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;


/**
 * The <code>WebConsolePluginAdapter</code> is an adapter to the
 * {@link AbstractWebConsolePlugin} for regular servlets registered with the
 * {@link org.apache.felix.webconsole.WebConsoleConstants#PLUGIN_TITLE}
 * service attribute.
 */
public class WebConsolePluginAdapter extends AbstractWebConsolePlugin
{

    /** serial UID */
    private static final long serialVersionUID = 1L;

    // the plugin label (aka address)
    private final String label;

    // the plugin title rendered in the top bar
    private final String title;

    // the actual plugin to forward rendering requests to
    private final Servlet plugin;


    public WebConsolePluginAdapter( String label, String title, Servlet plugin )
    {
        this.label = label;
        this.title = title;
        this.plugin = plugin;
    }


    //---------- AbstractWebConsolePlugin API

    /**
     * Returns the label of this plugin page as defined in the constructor.
     */
    public String getLabel()
    {
        return label;
    }


    /**
     * Returns the title of this plugin page as defined in the constructor.
     */
    public String getTitle()
    {
        return title;
    }


    /**
     * Call the plugin servlet's service method to render the content of this
     * page.
     */
    protected void renderContent( HttpServletRequest req, HttpServletResponse res ) throws ServletException,
        IOException
    {
        plugin.service( req, res );
    }


    //---------- Servlet API overwrite

    /**
     * Initializes this servlet as well as the plugin servlet.
     */
    public void init( ServletConfig config ) throws ServletException
    {
        // base classe initialization
        super.init( config );

        // plugin initialization
        plugin.init( config );
    }


    /**
     * Destroys this servlet as well as the plugin servlet.
     */
    public void destroy()
    {
        plugin.destroy();
        super.destroy();
    }
}
