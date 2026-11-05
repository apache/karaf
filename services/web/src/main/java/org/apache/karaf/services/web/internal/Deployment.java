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
package org.apache.karaf.services.web.internal;

import java.util.List;

import org.apache.karaf.services.web.WebElement;

/**
 * A context deployed in Tomcat, a whiteboard context or a web application bundle.
 * <p>
 * The deployments are only used from the thread of the web container.
 */
public interface Deployment {

    /**
     * @return the context path, {@code /} for the root context.
     */
    String getPath();

    /**
     * Adds the context, its servlets and its filters to the given lists.
     */
    void describe(List<WebElement> contexts, List<WebElement> servlets, List<WebElement> filters);

    void undeploy();

}
