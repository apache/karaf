/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.console.loader;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class JarInJarURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private ClassLoader classLoader;
    private URLStreamHandlerFactory chainFac;

    public JarInJarURLStreamHandlerFactory(ClassLoader cl) {
        this.classLoader = cl;
    }

    @Override
    public URLStreamHandler createURLStreamHandler( String protocol) {
        if (JarInJarConstants.INTERNAL_URL_PROTOCOL.equals(protocol))
            return new JarInJarStreamHandler( classLoader);
        if (chainFac != null)
            return chainFac.createURLStreamHandler(protocol);
        return null;
    }
}
