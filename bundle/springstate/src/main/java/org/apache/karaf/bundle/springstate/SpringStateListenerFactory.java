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
package org.apache.karaf.bundle.springstate;


import org.apache.karaf.bundle.core.BundleStateService;
import org.osgi.framework.BundleContext;


public class SpringStateListenerFactory {

    private BundleContext bundleContext;
    private BundleStateService listener;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() {
        getListener();
    }

    public void destroy() throws Exception {
        if (listener instanceof Destroyable) {
            ((Destroyable) listener).destroy();
        }
    }

    public synchronized BundleStateService getListener() {
        if (listener == null) {
            listener = createListener();
        }
        return listener;
    }

    private BundleStateService createListener() {
        try {
            // Use dynamic class loading to make sure we actually try to reload the class for
            // dynamic imports to kick in   if possible
            Class<?> cl = getClass().getClassLoader().loadClass("org.apache.karaf.shell.bundles.SpringStateListenerFactory$SpringApplicationListener");
            return (BundleStateService) cl.getConstructor(BundleContext.class).newInstance(bundleContext);
//            return new SpringApplicationListener(bundleContext);
        } catch (Throwable t) {
            return null;
        }
    }

    public static interface Destroyable {

        public void destroy() throws Exception;

    }

}
