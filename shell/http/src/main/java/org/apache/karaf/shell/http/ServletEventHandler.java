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

package org.apache.karaf.shell.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.Bundle;

/**
 * @author Achim
 *
 */
public class ServletEventHandler implements ServletListener {

	Map<Bundle, ServletEvent> servletEvents =  new HashMap<Bundle, ServletEvent>();
	
	public void servletEvent(ServletEvent event) {
		servletEvents.put(event.getBundle(), event);
	}

	/**
	 * @return the servletEvents
	 */
	public Collection<ServletEvent> getServletEvents() {
		return servletEvents.values();
	}

}
