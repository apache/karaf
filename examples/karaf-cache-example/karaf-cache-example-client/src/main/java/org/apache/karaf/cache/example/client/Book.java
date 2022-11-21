/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.cache.example.client;

/**
 * This is to showcase caching of a more complex object, rather than something like String or Long.
 */
public class Book {
    private final String title;
    private final int numOfPages;

    public Book(String title, int numOfPages) {
        this.title = title;
        this.numOfPages = numOfPages;
    }

    public String getTitle() {
        return title;
    }

    public int getNumOfPages() {
        return numOfPages;
    }

    @Override
    public String toString() {
        return "Book{" + "title='" + title + '\'' + ", numOfPages=" + numOfPages + '}';
    }
}
