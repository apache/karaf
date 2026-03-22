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

package org.apache.karaf.examples.graphql.websocket;

import graphql.ExecutionResult;
import graphql.GraphQL;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@WebSocket
public class GraphQLWebSocketExample {

    private static final List<Session> sessions = new ArrayList<>();
    private final GraphQL graphQL;

    public GraphQLWebSocketExample(GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    @OnWebSocketOpen
    public void onOpen(Session session) {
        session.setIdleTimeout(Duration.ZERO);
        sessions.add(session);

        String query = "" +
                "    subscription BookFeed {\n" +
                "        bookCreated {\n" +
                "            id\n" +
                "            name\n" +
                "        }\n" +
                "    }\n";

        ExecutionResult executionResult = graphQL.execute(query);
        Publisher<ExecutionResult> bookStream = executionResult.getData();
        AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
        bookStream.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscriptionRef.set(subscription);
                subscription.request(1);
            }

            @Override
            public void onNext(ExecutionResult executionResult) {
                session.sendText(executionResult.getData().toString(), Callback.NOOP);
                subscriptionRef.get().request(1);
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }
}