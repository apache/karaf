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
package org.apache.karaf.examples.websocket;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(
        service = Servlet.class,
        property = {
                "osgi.http.whiteboard.servlet.pattern=/example-websocket-init",
                "osgi.http.whiteboard.servlet.init-order=1"
        }
)
public class WebsocketExampleServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        ServerContainer container = (ServerContainer) getServletContext()
                .getAttribute(ServerContainer.class.getName());
        if (container != null) {
            try {
                container.addEndpoint(WebSocketExampleEndpoint.class);
            } catch (DeploymentException e) {
                throw new ServletException("Failed to register WebSocket endpoint", e);
            }
        }
        WebSocketExampleEndpoint.notification = true;
        Thread notification = new Thread(new WebSocketExampleEndpoint.NotificationThread(WebSocketExampleEndpoint.sessions));
        notification.start();
    }

    @Deactivate
    public void deactivate() {
        WebSocketExampleEndpoint.notification = false;
    }
}
