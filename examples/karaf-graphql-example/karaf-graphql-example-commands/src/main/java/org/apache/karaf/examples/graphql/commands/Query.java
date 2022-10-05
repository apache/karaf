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
package org.apache.karaf.examples.graphql.commands;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.apache.karaf.examples.graphql.api.GraphQLSchemaProvider;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 * Sample calls:
 * graphql:query "{books { name }}"
 * graphql:query "{bookById(id:\"1\") { name id pageCount }}"
 * graphql:query "mutation { addBook(name:\"Lord of the Rings\" pageCount:100) { id name }}
 */
@Service
@Command(scope = "graphql", name = "query", description = "Execute GraphQL query")
public class Query implements Action {

    @Argument(index = 0, name = "query", required = true, multiValued = false)
    String query;

    @Reference
    private GraphQLSchemaProvider schemaProvider;

    @Override
    public Object execute() throws Exception {
        GraphQLSchema schema = schemaProvider.createSchema();
        ExecutionResult result = GraphQL.newGraphQL(schema).build().execute(query);
        if (result.getData() != null) {
            System.out.println(result.getData().toString());
        } else {
            result.getErrors().forEach(System.out::println);
        }
        return null;
    }
}
