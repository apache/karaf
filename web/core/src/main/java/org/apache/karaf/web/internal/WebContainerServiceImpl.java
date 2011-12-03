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
package org.apache.karaf.web.internal;

import org.apache.karaf.web.WebBundle;
import org.apache.karaf.web.WebContainerService;
import org.ops4j.pax.web.service.spi.WarManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the WebContainer service.
 */
public class WebContainerServiceImpl implements WebContainerService {
    
    private BundleContext bundleContext;
    private StartLevel startLevelService;
    private WebEventHandler webEventHandler;
    private WarManager warManager;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebContainerServiceImpl.class);
    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    public void setStartLevelService(StartLevel startLevelService) {
        this.startLevelService = startLevelService;
    }
    
    public void setWebEventHandler(WebEventHandler webEventHandler) {
        this.webEventHandler = webEventHandler;
    }

    public void setWarManager(WarManager warManager) {
        this.warManager = warManager;
    }
    
    public List<WebBundle> list() throws Exception {
        Bundle[] bundles = bundleContext.getBundles();
        Map<Long, WebEvent> bundleEvents = webEventHandler.getBundleEvents();
        List<WebBundle> webBundles = new ArrayList<WebBundle>();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                // first check if the bundle is a web bundle
                String contextPath = (String) bundle.getHeaders().get("Web-ContextPath");
                if (contextPath == null) {
                    contextPath = (String) bundle.getHeaders().get("Webapp-Context"); // this one used by pax-web but is deprecated
                }
                if (contextPath == null) {
                    // the bundle is not a web bundle
                    continue;
                }
                
                WebBundle webBundle = new WebBundle();
                contextPath.trim();
                
                // get the bundle name
                String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
                // if there is no name, then default to symbolic name
                name = (name == null) ? bundle.getSymbolicName() : name;
                // if there is no symbolic name, resort to location
                name = (name == null) ? bundle.getLocation() : name;
                // get the bundle version
                String version = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
                name = ((version != null)) ? name + " (" + version + ")" : name;
                long bundleId = bundle.getBundleId();
                int level = -1;
                if (startLevelService != null) {
                    level = startLevelService.getBundleStartLevel(bundle);
                }
                if (!contextPath.startsWith("/")) {
                    contextPath = "/" + contextPath;
                }
                
                webBundle.setBundleId(bundleId);
                webBundle.setName(name);
                webBundle.setContextPath(contextPath);
                webBundle.setLevel(level);
                webBundle.setState(getStateString(bundle));
                webBundle.setWebState(state(bundle.getBundleId()));
                
                webBundles.add(webBundle);
            }
        }
        
        return webBundles;
    }
    
    public void start(List<Long> bundleIds) throws Exception {
        if (bundleIds != null && !bundleIds.isEmpty()) {
            for (long bundleId : bundleIds) {
                if (webEventHandler.getBundleEvents().containsKey(bundleId)) {
                    WebEvent webEvent = webEventHandler.getBundleEvents().get(bundleId);
                    Bundle bundle = webEvent.getBundle();
                    if (bundle != null) {
                        // deploy
                        warManager.start(bundleId, null);
                    } else {
                        System.out.println("Bundle ID " + bundleId + " is invalid");
                        LOGGER.warn("Bundle ID {} is invalid", bundleId);
                    }
                }
            }
        }
    }

    public void stop(List<Long> bundleIds) throws Exception {
        if (bundleIds != null && !bundleIds.isEmpty()) {
            for (long bundleId : bundleIds) {
                if (webEventHandler.getBundleEvents().containsKey(bundleId)) {
                    WebEvent webEvent = webEventHandler.getBundleEvents().get(bundleId);
                    Bundle bundle = webEvent.getBundle();
                    if (bundle != null) {
                        // deploy
                        warManager.stop(bundleId);
                    } else {
                        System.out.println("Bundle ID " + bundleId + " is invalid");
                        LOGGER.warn("Bundle ID {} is invalid", bundleId);
                    }
                }
            }
        }
    }

    public String state(long bundleId) {

        Map<Long, WebEvent> bundleEvents = webEventHandler.getBundleEvents();
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
                    topic = "Failed     ";
                    break;
                case WebEvent.WAITING:
                    topic = "Waiting    ";
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
     * Return a string representation of the bundle state.
     * 
     * TODO use an util method provided by bundle core
     * 
     * @param bundle the target bundle.
     * @return the string representation of the state
     */
    private String getStateString(Bundle bundle) {
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

}
