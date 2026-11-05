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
package org.apache.karaf.services.web.internal.wab;

import java.io.IOException;
import java.net.URL;

import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.InputSource;

/**
 * Configures a web application bundle as Tomcat configures a web application: {@code WEB-INF/web.xml}, the
 * {@code web-fragment.xml} of the libraries, the {@code @WebServlet}, {@code @WebFilter} and {@code @WebListener}
 * annotations, the {@code ServletContainerInitializer}s and the authenticator matching the login configuration.
 * <p>
 * The resources come from the bundle, so there is no document base to fix, and there is no {@code context.xml}.
 * The global {@code web.xml} defines the default servlet and the welcome files.
 */
class WabContextConfig extends ContextConfig {

    private static final String DEFAULT_WEB_XML = "default-web.xml";

    @Override
    protected void webConfig() {
        // the digester parsing web.xml loads the Tomcat descriptor classes with the thread context class loader,
        // which is the bundle class loader of the web application while the context starts
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(WabContextConfig.class.getClassLoader());
        try {
            super.webConfig();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Override
    protected void contextConfig(Digester digester) {
        // no context.xml
    }

    @Override
    protected void fixDocBase() {
        // the resources are the entries of the bundle
    }

    @Override
    protected void antiLocking() {
        // the resources are the entries of the bundle
    }

    @Override
    protected InputSource getGlobalWebXmlSource() {
        URL url = WabContextConfig.class.getResource(DEFAULT_WEB_XML);
        if (url == null) {
            return null;
        }
        try {
            InputSource source = new InputSource(url.toExternalForm());
            source.setByteStream(url.openStream());
            return source;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + url, e);
        }
    }

    @Override
    protected InputSource getHostWebXmlSource() {
        return null;
    }

}
