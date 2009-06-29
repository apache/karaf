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
     * Only {@link #SERVICE_NAME} services with this service registration
     * property set to a non-empty String values are accepted by the OSGi
     * Manager as a plugin.
     */
    public static final String PLUGIN_LABEL = "felix.webconsole.label";

    /**
     * The title under which the OSGi Manager plugin is called by
     * the OSGi Manager (value is "felix.webconsole.label").
     * <p>
     * Only {@link #SERVICE_NAME} services with this service registration
     * property set to a non-empty String values are accepted by the OSGi
     * Manager as a plugin.
     *
     * @since 1.2.12
     */
    public static final String PLUGIN_TITLE = "felix.webconsole.title";

    /**
     * The name of the request attribute providing the absolute path of the
     * Web Console root (value is "felix.webconsole.appRoot"). This consists of
     * the servlet context path (from <code>ServletRequest.getContextPath()</code>)
     * and the configured path of the web console root (<code>/system/console</code>
     * by default).
     * <p>
     * The type of this request attribute is <code>String</code>.
     *
     * @since 1.2.12
     */
    public static final String ATTR_APP_ROOT = "felix.webconsole.appRoot";

    /**
     * The name of the request attribute providing a mapping of labels to page
     * titles of registered console plugins (value is "felix.webconsole.labelMap").
     * This map may be used to render a navigation of the console plugins as the
     * {@link AbstractWebConsolePlugin#renderTopNavigation(javax.servlet.http.HttpServletRequest, java.io.PrintWriter)}
     * method does.
     * <p>
     * The type of this request attribute is <code>Map<String, String></code>.
     *
     * @since 1.2.12
     */
    public static final String ATTR_LABEL_MAP = "felix.webconsole.labelMap";

}
