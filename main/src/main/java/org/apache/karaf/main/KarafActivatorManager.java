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
package org.apache.karaf.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.karaf.main.util.BootstrapLogManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.launch.Framework;

public class KarafActivatorManager {

    public static final String KARAF_ACTIVATOR = "Karaf-Activator";

    Logger LOG = Logger.getLogger(this.getClass().getName());

    private List<BundleActivator> karafActivators = new ArrayList<>();
    private final ClassLoader classLoader;
    private final Framework framework;
    
    public KarafActivatorManager(ClassLoader classLoader, Framework framework) {
        this.classLoader = classLoader;
        this.framework = framework;
        BootstrapLogManager.configureLogger(LOG);
    }

    void startKarafActivators() throws IOException {
        Enumeration<URL> urls = classLoader.getResources("META-INF/MANIFEST.MF");
        while (urls != null && urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String className = null;
            try (InputStream is = url.openStream()) {
                Manifest mf = new Manifest(is);
                className = mf.getMainAttributes().getValue(KARAF_ACTIVATOR);
                if (className != null) {
                    BundleActivator activator = (BundleActivator) classLoader.loadClass(className).newInstance();
                    activator.start(framework.getBundleContext());
                    karafActivators.add(activator);
                }
            } catch (Throwable e) {
                if (className != null) {
                    System.err.println("Error starting karaf activator " + className + ": " + e.getMessage());
                    LOG.log(Level.WARNING, "Error starting karaf activator " + className + " from url " + url, e);
                }
            }
        }
    }

    void stopKarafActivators() {
        for (BundleActivator activator : karafActivators) {
            try {
                activator.stop(framework.getBundleContext());
            } catch (Throwable e) {
                LOG.log(Level.WARNING, "Error stopping karaf activator " + activator.getClass().getName(), e);
            }
        }
    }

}
