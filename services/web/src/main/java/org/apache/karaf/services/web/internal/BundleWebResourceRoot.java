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

import java.net.URL;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.EmptyResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.karaf.services.web.internal.wab.BundleResourceSet;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resources of a context, read from the entries of a bundle rather than from a directory or a WAR file.
 */
public class BundleWebResourceRoot extends StandardRoot {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleWebResourceRoot.class);

    private final Bundle bundle;

    /**
     * @param bundle the bundle providing the resources, or {@code null} for a context without resources.
     */
    public BundleWebResourceRoot(Context context, Bundle bundle) {
        super(context);
        this.bundle = bundle;
    }

    @Override
    protected WebResourceSet createMainResourceSet() {
        return bundle == null ? new EmptyResourceSet(this) : new BundleResourceSet(this, bundle);
    }

    @Override
    protected void processWebInfLib() {
        // the libraries in WEB-INF/lib are loaded by the bundle class loader, from the Bundle-ClassPath
    }

    @Override
    protected void registerURLStreamHandlerFactory() {
        // the OSGi framework owns the JVM wide URL stream handler factory
    }

    @Override
    public void createWebResourceSet(ResourceSetType type, String webAppMount, URL url, String internalPath) {
        String protocol = url.getProtocol();
        if ("file".equals(protocol) || url.toString().startsWith("jar:file:")) {
            super.createWebResourceSet(type, webAppMount, url, internalPath);
        } else {
            // for instance the META-INF/resources folder of a jar in WEB-INF/lib
            LOGGER.warn("Resources of {}{} are not supported, they are ignored", url, internalPath);
        }
    }

}
