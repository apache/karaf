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
import jakarta.servlet.annotation.WebServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(
        service = Servlet.class,
        property = {"osgi.http.whiteboard.servlet.pattern=/example-websocket"}
)
@WebServlet(name = "Example WebSocket Servlet", urlPatterns = {"/example-websocket"})
public class WebsocketExampleServlet extends JettyWebSocketServlet {

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.register(WebSocketExample.class);
    }

    @Activate
    public void activate() {
        WebSocketExample.notification = true;
        Thread notification = new Thread(new WebSocketExample.NotificationThread(WebSocketExample.sessions));
        notification.start();
    }

    @Deactivate
    public void deactivate() {
        WebSocketExample.notification = false;
    }

}
