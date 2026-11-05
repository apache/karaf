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

import java.beans.PropertyChangeListener;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.osgi.framework.Bundle;

/**
 * Creates the Tomcat contexts of the whiteboard and of the web application bundles.
 */
public final class Contexts {

    private Contexts() {
    }

    /**
     * @param path the context path, {@code /} for the root context.
     * @return the context path as used by Tomcat, the empty string for the root context.
     */
    public static String tomcatPath(String path) {
        return "/".equals(path) ? "" : path;
    }

    /**
     * Normalizes a context path: a leading slash, no trailing slash, {@code /} for the root context.
     *
     * @return the normalized path, or {@code null} if the path is not valid.
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        String result = path.trim();
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        while (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.contains("*") || result.contains("//") || result.contains("..") || result.contains("?")) {
            return null;
        }
        return result;
    }

    /**
     * Creates a context, not started yet.
     *
     * @param path        the context path, {@code /} for the root context.
     * @param classLoader the class loader of the context, used as thread context class loader.
     * @param bundle      the bundle serving the resources of the context, or {@code null} if the context has none.
     */
    public static StandardContext create(String path, ClassLoader classLoader, Bundle bundle, WebConfig config) {
        String tomcatPath = tomcatPath(path);
        StandardContext context = new StandardContext();
        context.setName(tomcatPath);
        context.setPath(tomcatPath);
        context.setUseNaming(false);
        context.setParentClassLoader(Contexts.class.getClassLoader());
        context.setLoader(new SimpleLoader(classLoader));
        context.setResources(new BundleWebResourceRoot(context, bundle));
        StandardJarScanner jarScanner = new StandardJarScanner();
        jarScanner.setScanClassPath(false);
        context.setJarScanner(jarScanner);
        context.setIgnoreAnnotations(true);
        context.setSessionTimeout(config.sessionTimeout);
        StandardManager manager = new StandardManager();
        // sessions are not persisted when the context stops
        manager.setPathname(null);
        context.setManager(manager);
        Tomcat.addDefaultMimeTypeMappings(context);
        return context;
    }

    /**
     * Marks a context as configured when it starts, for the contexts without {@code ContextConfig}.
     */
    public static void markConfigured(StandardContext context) {
        context.addLifecycleListener(event -> {
            if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
                context.setConfigured(true);
            }
        });
    }

    /**
     * A loader providing an existing class loader: a bundle class loader, rather than a web application class
     * loader built from {@code WEB-INF/classes} and {@code WEB-INF/lib}.
     */
    static final class SimpleLoader implements Loader {

        private final ClassLoader classLoader;
        private Context context;

        SimpleLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public void backgroundProcess() {
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public Context getContext() {
            return context;
        }

        @Override
        public void setContext(Context context) {
            this.context = context;
        }

        @Override
        public boolean getDelegate() {
            return false;
        }

        @Override
        public void setDelegate(boolean delegate) {
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }

        @Override
        public boolean modified() {
            return false;
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }

    }

}
