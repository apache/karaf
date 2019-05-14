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

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component(
        name = "example-websocket",
        immediate = true
)
@WebSocket
public class WebSocketExample {

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    private boolean notification = false;

    @Reference
    private HttpService httpService;

    @OnWebSocketConnect
    public void onOpen(Session session) {
        session.setIdleTimeout(-1);
        sessions.add(session);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    @Activate
    public void activate() throws Exception {
        httpService.registerServlet("/example-websocket", new WebsocketExampleServlet(), null, null);

        notification = true;
        Thread notification = new  Thread(new NotificationThread(sessions));
        notification.start();
    }

    @Deactivate
    public void deactivate() throws Exception {
        httpService.unregister("/example-websocket");
        notification = false;
    }

    class NotificationThread implements Runnable {

        private Set<Session> sessions;

        public NotificationThread(Set<Session> sessions) {
            this.sessions = sessions;
        }

        @Override
        public void run() {
            try {
                while (notification) {
                    for (Session session : sessions) {
                        session.getRemote().sendString("Hello World");
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
