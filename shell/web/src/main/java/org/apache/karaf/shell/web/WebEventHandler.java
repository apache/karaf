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
package org.apache.karaf.shell.web;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Class implementing {@link EventHandler} service to retrieve "org/osgi/service/web/*" specific Events
 */
public class WebEventHandler implements EventHandler {
	
	private final Map<Long, String> bundleEvents = new HashMap<Long, String>();

	/* (non-Javadoc)
	 * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
	 */
	public void handleEvent(Event event) {
		String topic = event.getTopic();
		Long bundleID = (Long) event.getProperty("bundle.id");
		getBundleEvents().put(bundleID, topic);
	}

	/**
	 * @return the bundleEvents
	 */
	public Map<Long, String> getBundleEvents() {
		return bundleEvents;
	}

}
