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
package org.apache.felix.webconsole;

/**
 * WebConsoleConstants provides some common constants that are used by plugin
 * developers.
 */
public interface WebConsoleConstants
{

    /**
     * The name of the service to register as to be used as a "plugin" for
     * the OSGi Manager (value is "javax.servlet.Servlet").
     */
    public static final String SERVICE_NAME = "javax.servlet.Servlet";

    /**
     * The URI address label under which the OSGi Manager plugin is called by
     * the OSGi Manager (value is "felix.webconsole.label").
     * <p>
     * This service registration property must be set to a single non-empty
     * String value. Otherwise the {@link #SERVICE_NAME Servlet} services will
     * be ignored by the Felix Web Console and not be used as a plugin.
     */
    public static final String PLUGIN_LABEL = "felix.webconsole.label";

    /**
     * The title under which the OSGi Manager plugin is called by
     * the OSGi Manager (value is "felix.webconsole.title").
     * <p>
     * For {@link #SERVICE_NAME Servlet} services not extending the
     * {@link AbstractWebConsolePlugin} this property is required for the
     * service to be used as a plugin. Otherwise the service is just ignored
     * by the Felix Web Console.
     * <p>
     * For {@link #SERVICE_NAME Servlet} services extending from the
     * {@link AbstractWebConsolePlugin} abstract class this property is not
     * technically required. To support lazy service access, e.g. for plugins
     * implemented using the OSGi <i>Service Factory</i> pattern, the use
     * of this service registration property is encouraged.
     *
     * @since 2.0.0
     */
    public static final String PLUGIN_TITLE = "felix.webconsole.title";

    /**
     * The name of the service registration properties providing references
     * to addition CSS files that should be loaded when rendering the header
     * for a registered plugin.
     * <p>
     * This property is expected to be a single string value, array of string
     * values or a Collection (or Vector) of string values.
     * <p>
     * This service registration property is only used for plugins registered
     * as {@link #SERVICE_NAME} services which do not extend the
     * {@link AbstractWebConsolePlugin}. Extensions of the
     * {@link AbstractWebConsolePlugin} should overwrite the
     * {@link AbstractWebConsolePlugin#getCssReferences()} method to provide
     * additional CSS resources.
     *
     * @since 2.0.0
     */
    public static final String PLUGIN_CSS_REFERENCES = "felix.webconsole.css";

    /**
     * The name of the request attribute providing the absolute path of the
     * Web Console root (value is "felix.webconsole.appRoot"). This consists of
     * the servlet context path (from <code>HttpServletRequest.getContextPath()</code>)
     * and the Web Console servlet path (from
     * <code>HttpServletRequest.getServletPath()</code>,
     * <code>/system/console</code> by default).
     * <p>
     * The type of this request attribute is <code>String</code>.
     *
     * @since 2.0.0
     */
    public static final String ATTR_APP_ROOT = "felix.webconsole.appRoot";

    /**
     * The name of the request attribute providing the absolute path of the
     * current plugin (value is "felix.webconsole.pluginRoot"). This consists of
     * the servlet context path (from <code>ServletRequest.getContextPath()</code>),
     * the configured path of the web console root (<code>/system/console</code>
     * by default) and the plugin label {@link #PLUGIN_LABEL}.
     * <p>
     * The type of this request attribute is <code>String</code>.
     *
     * @since 1.2.12
     */
    public static final String ATTR_PLUGIN_ROOT = "felix.webconsole.pluginRoot";

    /**
     * The name of the request attribute providing a mapping of labels to page
     * titles of registered console plugins (value is "felix.webconsole.labelMap").
     * This map may be used to render a navigation of the console plugins as the
     * {@link AbstractWebConsolePlugin#renderTopNavigation(javax.servlet.http.HttpServletRequest, java.io.PrintWriter)}
     * method does.
     * <p>
     * The type of this request attribute is <code>Map<String, String></code>.
     *
     * @since 2.0.0
     */
    public static final String ATTR_LABEL_MAP = "felix.webconsole.labelMap";

    /**
     * The name of the request attribute holding the {@link VariableResolver}
     * for the request (value is "felix.webconsole.variable.resolver").
     *
     * @see VariableResolver
     * @see WebConsoleUtil#getVariableResolver(javax.servlet.ServletRequest)
     * @see WebConsoleUtil#setVariableResolver(javax.servlet.ServletRequest, VariableResolver)
     * @since 3.0
     */
    static final String ATTR_CONSOLE_VARIABLE_RESOLVER = "felix.webconsole.variable.resolver";
}
