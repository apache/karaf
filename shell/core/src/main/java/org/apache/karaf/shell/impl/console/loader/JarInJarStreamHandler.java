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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class JarInJarStreamHandler extends URLStreamHandler {
    private ClassLoader classLoader;

    public JarInJarStreamHandler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected URLConnection openConnection( URL u) throws IOException {
        return new JarInJarURLConnection(u, classLoader);
    }

    @Override
    protected void parseURL(URL url, String spec, int start, int limit) {
        String file;
        if (spec.startsWith(JarInJarConstants.INTERNAL_URL_PROTOCOL_WITH_COLON))
            file = spec.substring(5);
        else if (url.getFile().equals(JarInJarConstants.CURRENT_DIR))
            file = spec;
        else if (url.getFile().endsWith(JarInJarConstants.PATH_SEPARATOR))
            file = url.getFile() + spec;
        else
            file = spec;
        setURL(url, JarInJarConstants.INTERNAL_URL_PROTOCOL, "", -1, null, null, file, null, null);	 //$NON-NLS-1$
    }
}
