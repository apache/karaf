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

import java.util.List;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.ops4j.pax.web.service.spi.WarManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;

public abstract class WebManagerCommand extends OsgiCommandSupport {

	WebEventHandler eventHandler;
	private WarManager warManager;

    @Argument(index = 0, name = "ids", description = "The list of bundle IDs separated by whitespaces", required = true, multiValued = true)
    List<Long> ids;

	@Override
	protected Object doExecute() throws Exception {
		if (ids != null && !ids.isEmpty()) {
            for (long id : ids) {
            	if (eventHandler.getBundleEvents().containsKey(id)) {
            		WebEvent webEvent = eventHandler.getBundleEvents().get(id);
            		Bundle bundle = webEvent.getBundle();
            		if (bundle == null) {
	                    System.err.println("Bundle ID" + id + " is invalid");
	                } 
            	} else {
            		ids.remove(id);
            	}
            }
        }
        doExecute(ids);
        return null;
	}

	
	/**
	 * @param eventHandler the eventHandler to set
	 */
	public void setEventHandler(WebEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	/**
	 * @return the eventHandler
	 */
	public WebEventHandler getEventHandler() {
		return eventHandler;
	}

	abstract void doExecute(List<Long> bundles) throws Exception;

	/**
	 * @param warManager the warManager to set
	 */
	public void setWarManager(WarManager warManager) {
		this.warManager = warManager;
	}

	/**
	 * @return the warManager
	 */
	public WarManager getWarManager() {
		return warManager;
	}
}