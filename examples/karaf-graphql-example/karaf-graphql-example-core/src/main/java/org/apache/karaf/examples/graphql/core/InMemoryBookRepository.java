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

import org.apache.karaf.examples.graphql.api.Book;
import org.apache.karaf.examples.graphql.api.BookRepository;
import org.osgi.service.component.annotations.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component(service = BookRepository.class)
public class InMemoryBookRepository implements BookRepository {

    private final AtomicInteger idCounter = new AtomicInteger(0);
    private final Map<String, Book> booksById = new HashMap<>();

    public void activate() {
        createInitialBooks();
    }

    @Override
    public Book storeBook(Book book) {
        String id = nextId();
        book.setId(id);
        booksById.put(id, book);
        return book;
    }

    public Collection<Book> getBooks() {
        return booksById.values();
    }

    public Book getBookById(String id) {
        return booksById.get(id);
    }

    private void createInitialBooks() {
        storeBook(new Book("Apache Karaf Cookbook", 260));
        storeBook(new Book("Effective Java", 416));
        storeBook(new Book("OSGi in Action", 375));
    }

    private String nextId() {
        return Integer.toString(idCounter.incrementAndGet());
    }
}
