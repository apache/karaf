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

import java.util.Map;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebEvent.WebTopic;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.startlevel.StartLevel;

@Command(scope = "web", name = "list", description = "Lists details for war bundles.")
public class WebListCommand extends OsgiCommandSupport {
	
	private StartLevel startLevelService;
	
	private WebEventHandler eventHandler;

	/* (non-Javadoc)
	 * @see org.apache.karaf.shell.war.WarCommandSupport#doExecute(org.osgi.service.packageadmin.PackageAdmin)
	 */
	@Override
	protected Object doExecute() {
		Bundle[] bundles = getBundleContext().getBundles();
		Map<Long, WebEvent> bundleEvents = eventHandler.getBundleEvents();
		if (bundles != null) {
			String level = (startLevelService == null) ? "" : "  Level ";
			String webState = (bundleEvents == null || bundleEvents.isEmpty()) ? "" : "  Web-State     ";
			String headers = "   ID   State       ";
			headers += webState + level + " Web-ContextPath           Name";
            System.out.println(headers);
            for (int i = 0; i < bundles.length; i++) {
            	//First check if this bundle contains  a webapp ctxt
            	String webappctxt = (String) bundles[i].getHeaders().get("Web-ContextPath");
            	if (webappctxt == null)
            		webappctxt = (String) bundles[i].getHeaders().get("Webapp-Context");//this one is used by pax-web but is deprecated.
            	
            	if (webappctxt == null)
            		continue; //only list war archives. 
            	
            	webappctxt.trim();
            	
            	// Get the bundle name or location.
                String name = (String) bundles[i].getHeaders().get(Constants.BUNDLE_NAME);
                // If there is no name, then default to symbolic name.
                name = (name == null) ? bundles[i].getSymbolicName() : name;
                // If there is no symbolic name, resort to location.
                name = (name == null) ? bundles[i].getLocation() : name;
                // Show bundle version if not showing location.
                String version = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
                name = ((version != null)) ? name + " (" + version + ")" : name;
                long l = bundles[i].getBundleId();
                String id = String.valueOf(l);
                if (startLevelService == null) {
                    level = "";
                }
                else {
                    level = String.valueOf(startLevelService.getBundleStartLevel(bundles[i]));
                }
                while (level.length() < 5) {
                    level = " " + level;
                }
                while (id.length() < 4) {
                    id = " " + id;
                }
                
                //prepend ctxt with slash (looks better)
                if (!webappctxt.startsWith("/"))
                	webappctxt = "/" + webappctxt;
                
                while (webappctxt.length() < 24) {
                	webappctxt += " ";
                }
                
                String line = "[" + id + "] [" + getStateString(bundles[i]) + "]";
                if (bundleEvents != null && !bundleEvents.isEmpty())
                	line += " ["+ getWebStateString(bundles[i]) +"] ";
                line += " [" + level + "] [" + webappctxt + "] " + name;
                System.out.println(line);
            }
		}
		return null;

	}

    public String getStateString(Bundle bundle)
    {
        int state = bundle.getState();
        if (state == Bundle.ACTIVE) {
            return "Active     ";
        } else if (state == Bundle.INSTALLED) {
            return "Installed  ";
        } else if (state == Bundle.RESOLVED) {
            return "Resolved   ";
        } else if (state == Bundle.STARTING) {
            return "Starting   ";
        } else if (state == Bundle.STOPPING) {
            return "Stopping   ";
        } else {
            return "Unknown    ";
        }
    }
    
    public String getWebStateString(Bundle bundle) {
    	
    	long bundleId = bundle.getBundleId();
    	
    	Map<Long, WebEvent> bundleEvents = eventHandler.getBundleEvents();
    	String topic = "Unknown    ";
    	
		if (bundleEvents.containsKey(bundleId)) {
    		WebEvent webEvent = bundleEvents.get(bundleId);

    		switch(webEvent.getType()) {
    		case WebEvent.DEPLOYING:
    			topic = "Deploying  ";
    			break;
    		case WebEvent.DEPLOYED:
    			topic = "Deployed   ";
    			break;
    		case WebEvent.UNDEPLOYING:
    			topic = "Undeploying";
    			break;
    		case WebEvent.UNDEPLOYED:
    			topic = "Undeployed ";
    			break;
    		case WebEvent.FAILED:
    			topic = "Unknown    ";
    			topic = "Failed     ";
    			break;
    		default:
    			topic = "Failed     ";
    		}
		}
		
		while (topic.length() < 11) {
        	topic += " ";
        }
    	
    	return topic;
    }

	/**
	 * @param startLevelService the startLevelService to set
	 */
	public void setStartLevelService(StartLevel startLevelService) {
		this.startLevelService = startLevelService;
	}

	/**
	 * @return the startLevelService
	 */
	public StartLevel getStartLevelService() {
		return startLevelService;
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

}
