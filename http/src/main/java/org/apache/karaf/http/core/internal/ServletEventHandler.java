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
package org.apache.karaf.http.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.Bundle;

public class ServletEventHandler implements ServletListener {

	Map<String, ServletEvent> servletEvents = new HashMap<>();
	
	public synchronized void servletEvent(ServletEvent event) {
		servletEvents.put(event.getServletName(), event);
	}

	/**
	 * @return the servletEvents
	 */
	public synchronized List<ServletEvent> getServletEvents() {
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
