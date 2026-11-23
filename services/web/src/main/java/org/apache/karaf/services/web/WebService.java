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
package org.apache.karaf.services.web;

import java.util.List;

/**
 * Describes what the Karaf web container is serving.
 * <p>
 * The web container embeds Apache Tomcat. It deploys the servlets, filters, listeners, resources and servlet
 * context helpers registered following the OSGi Servlet Whiteboard specification, and the Web Application
 * Bundles (bundles with a {@code Web-ContextPath} header).
 */
public interface WebService {

    /**
     * @return the URLs of the enabled connectors, for instance {@code http://0.0.0.0:8181}.
     */
    List<String> getEndpoints();

    /**
     * @return the servlet contexts, whiteboard contexts and web application bundles.
     */
    List<WebElement> getContexts();

    /**
     * @return the servlets, including the whiteboard resources and the servlets of the web application bundles.
     */
    List<WebElement> getServlets();

    /**
     * @return the filters, of the whiteboard contexts and of the web application bundles.
     */
    List<WebElement> getFilters();

}
