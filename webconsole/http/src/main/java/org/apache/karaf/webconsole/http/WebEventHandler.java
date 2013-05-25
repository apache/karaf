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

public class WebEventHandler implements WebListener {

    private final Map<Long, WebEvent> bundleEvents = new HashMap<Long, WebEvent>();

    @Override
    public void webEvent(WebEvent event) {
        bundleEvents.put(event.getBundle().getBundleId(), event);
    }

    public Map<Long, WebEvent> getBundleEvents() {
        return bundleEvents;
    }

}
