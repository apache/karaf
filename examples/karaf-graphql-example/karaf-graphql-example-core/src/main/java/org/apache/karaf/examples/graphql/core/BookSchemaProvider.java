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
package org.apache.karaf.examples.graphql.core;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.apache.karaf.examples.graphql.api.Book;
import org.apache.karaf.examples.graphql.api.BookRepository;
import org.apache.karaf.examples.graphql.api.GraphQLSchemaProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Publisher;

import java.util.Collection;

@Component(service = GraphQLSchemaProvider.class)
public class BookSchemaProvider implements GraphQLSchemaProvider {

    @Reference(service = BookRepository.class)
    private BookRepository bookRepository;

    private PublishSubject<Book> subject;

    public void setBookRepository(InMemoryBookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Activate
    public void activate() {
        subject = PublishSubject.create();
    }

    @Override
    public GraphQLSchema createSchema() {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(
                this.getClass().getResourceAsStream("/schema.graphql"));

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Mutation", builder -> builder.dataFetcher("addBook", addBookFetcher()))
                .type("Query", builder -> builder.dataFetcher("bookById", bookByIdFetcher()))
                .type("Query", builder -> builder.dataFetcher("books", booksFetcher()))
                .type("Subscription", builder -> builder.dataFetcher("bookCreated", bookCreatedFetcher()))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    }

    private DataFetcher<Book> addBookFetcher() {
        return environment -> {
            String name = environment.getArgument("name");
            int pageCount = environment.getArgument("pageCount");
            Book book = bookRepository.storeBook(new Book(name, pageCount));
            subject.onNext(book);
            return book;
        };
    }

    private DataFetcher<Book> bookByIdFetcher() {
        return environment -> {
            String id = environment.getArgument("id");
            return bookRepository.getBookById(id);
        };
    }

    private DataFetcher<Collection<Book>> booksFetcher() {
        return environment -> bookRepository.getBooks();
    }

    private DataFetcher<Publisher<Book>> bookCreatedFetcher() {
        return environment -> getPublisher();
    }

    private Publisher<Book> getPublisher() {
        subject = PublishSubject.create();
        ConnectableObservable<Book> connectableObservable = subject.share().publish();
        connectableObservable.connect();
        return connectableObservable.toFlowable(BackpressureStrategy.BUFFER);
    }
}
