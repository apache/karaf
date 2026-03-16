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

import graphql.GraphQL;
import graphql.execution.SubscriptionExecutionStrategy;
import org.apache.karaf.examples.graphql.api.GraphQLSchemaProvider;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import jakarta.servlet.Servlet;
import jakarta.servlet.annotation.WebServlet;

@WebServlet(name = "Example GraphQL WebSocket Servlet", urlPatterns = {"/graphql-websocket"})
@Component(service = Servlet.class, property = {"osgi.http.whiteboard.servlet.pattern=/graphql-websocket"})
public class GraphQLWebSocketServlet extends JettyWebSocketServlet implements JettyWebSocketCreator {

    @Reference(service = GraphQLSchemaProvider.class)
    private GraphQLSchemaProvider schemaProvider;

    @Override
    public Object createWebSocket(JettyServerUpgradeRequest request, JettyServerUpgradeResponse response) {
        GraphQL graphQL = GraphQL.newGraphQL(schemaProvider.createSchema())
                .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
                .build();
        return new GraphQLWebSocketExample(graphQL);
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setCreator(this);
    }
}
