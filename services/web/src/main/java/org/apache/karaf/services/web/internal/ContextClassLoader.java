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
package org.apache.karaf.services.web.internal;

/**
 * Sets the thread context class loader until it is closed:
 * {@code try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) { ... }}.
 */
public final class ContextClassLoader implements AutoCloseable {

    private final Thread thread;
    private final ClassLoader previous;

    private ContextClassLoader(ClassLoader classLoader) {
        thread = Thread.currentThread();
        previous = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
    }

    public static ContextClassLoader use(ClassLoader classLoader) {
        return new ContextClassLoader(classLoader);
    }

    @Override
    public void close() {
        thread.setContextClassLoader(previous);
    }

}
