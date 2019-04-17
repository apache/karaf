/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.docker;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Component(
        property = { "alias=/servlet-example", "servlet-name=Example"}
)
public class ExampleServlet extends HttpServlet implements Servlet {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExampleServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        LOGGER.info("Client " + request.getRemoteAddr() + " request received on " + request.getRequestURL());

        try (PrintWriter writer = response.getWriter()) {
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<title>Example</title>");
            writer.println("</head>");
            writer.println("<body align='center'>");
            writer.println("<h1>Example Servlet</h1>");
            writer.println("</body>");
            writer.println("</html>");
        }
    }

}
