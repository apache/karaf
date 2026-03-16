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

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@WebSocket
public class WebSocketExample {

    static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    static volatile boolean notification = false;

    @OnWebSocketOpen
    public void onOpen(Session session) {
        session.setIdleTimeout(Duration.ZERO);
        sessions.add(session);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    static class NotificationThread implements Runnable {

        private final Set<Session> sessions;

        public NotificationThread(Set<Session> sessions) {
            this.sessions = sessions;
        }

        @Override
        public void run() {
            try {
                while (notification) {
                    for (Session session : sessions) {
                        session.sendText("Hello World", Callback.NOOP);
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
