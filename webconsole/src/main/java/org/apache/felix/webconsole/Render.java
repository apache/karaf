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
package org.apache.felix.webconsole;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>Render</code> interface defines a service interface for
 * information renderings to be plugged into the web console.
 * <p>
 * <strong>NOTE: This interface is just an intermediate solution for making the
 * web console extensible. Future releases of the web console will remove this
 * and the {@link Action} interfaces and use the
 * <code>javax.servlet.Servlet</code> interface with predefined service
 * registration properties instead.</strong>
 * 
 * @deprecated This interface will be removed when <a
 *             href="https://issues.apache.org/jira/browse/FELIX-574">FELIX-574</a>
 *             will be implemented.
 */
public interface Render {

    static final String SERVICE = Render.class.getName();

    String getName();

    String getLabel();

    void render(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;

}
