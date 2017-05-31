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
package org.apache.karaf.webconsole.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class ServletEventHandler implements ServletListener, BundleListener {

    BundleContext bundleContext;
    Map<String, ServletEvent> servletEvents = new HashMap<>();

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() {
        bundleContext.addBundleListener(this);
    }

    public void destroy() {
        bundleContext.removeBundleListener(this);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED
                || event.getType() == BundleEvent.UNRESOLVED
                || event.getType() == BundleEvent.STOPPED) {
            removeEventsForBundle(event.getBundle());
        }
    }

    public synchronized void servletEvent(ServletEvent event) {
        servletEvents.put(event.getServletName(), event);
    }

    /**
     * @return the servletEvents
     */
    public synchronized Collection<ServletEvent> getServletEvents() {
        return new ArrayList<>(servletEvents.values());
    }

    public synchronized void removeEventsForBundle(Bundle bundle) {
        Iterator<Map.Entry<String,ServletEvent>> iterator = servletEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String,ServletEvent> entry = iterator.next();
            if (entry.getValue().getBundle() == bundle) {
                iterator.remove();
            }
        }
    }

}
