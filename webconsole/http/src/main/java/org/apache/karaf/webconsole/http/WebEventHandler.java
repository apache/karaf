/* Copyright 2011 Achim.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.webconsole.http;

import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class WebEventHandler implements WebListener, BundleListener {

    BundleContext bundleContext;
    private final Map<Long, WebEvent> bundleEvents = new HashMap<>();

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

    @Override
    public synchronized void webEvent(WebEvent event) {
        bundleEvents.put(event.getBundle().getBundleId(), event);
    }

    public synchronized Map<Long, WebEvent> getBundleEvents() {
        return new HashMap<>(bundleEvents);
    }

    public synchronized void removeEventsForBundle(Bundle bundle) {
        bundleEvents.remove(bundle.getBundleId());
    }

}
