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

import org.apache.karaf.web.WebContainerService;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.info.WebApplicationInfo;
import org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the WebContainer service.
 */
public class WebContainerServiceImpl implements WebContainerService {
    
    private BundleContext bundleContext;
    private WebContainer webContainer;

    private static final Logger LOGGER = LoggerFactory.getLogger(WebContainerServiceImpl.class);
    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setWebContainer(WebContainer webContainer) {
        this.webContainer = webContainer;
    }

    @Override
    public List<WebApplicationInfo> list() throws Exception {
        if (webContainer == null) {
            return Collections.emptyList();
        }
        ReportWebContainerView view = webContainer.adapt(ReportWebContainerView.class);
        if (view == null) {
            return Collections.emptyList();
        }

        Set<WebApplicationInfo> webBundles = view.listWebApplications();
        return new ArrayList<>(webBundles);
    }

    @Override
    public void install(String location, String contextPath) throws Exception {
        String completeLocation = "webbundle:" + location + "?Web-ContextPath=" + contextPath;
        Bundle bundle = bundleContext.installBundle(completeLocation);
        bundle.start();
    }

    @Override
    public void uninstall(List<Long> bundleIds) throws Exception {
        List<WebApplicationInfo> apps = list();
        Map<Long, Bundle> mapping = new HashMap<>();
        for (WebApplicationInfo app : apps) {
            mapping.put(app.getBundle().getBundleId(), app.getBundle());
        }

        if (bundleIds != null && !bundleIds.isEmpty()) {
            for (long bundleId : bundleIds) {
                Bundle bundle = mapping.get(bundleId);
                if (bundle != null) {
                    bundle.uninstall();
                } else {
                    System.out.println("Bundle ID " + bundleId + " is invalid");
                    LOGGER.warn("Bundle ID {} is invalid", bundleId);
                }
            }
        }
    }

    @Override
    public void start(List<Long> bundleIds) throws Exception {
        if (bundleIds != null && !bundleIds.isEmpty()) {
            List<WebApplicationInfo> apps = list();
            Map<Long, Bundle> mapping = new HashMap<>();
            for (WebApplicationInfo app : apps) {
                mapping.put(app.getBundle().getBundleId(), app.getBundle());
            }
            for (long bundleId : bundleIds) {
                Bundle bundle = mapping.get(bundleId);
                if (bundle != null) {
                    // deploy
                    // TOCHECK: Pax Web has no "War Manager", so WAB == Bundle and we can't have started Bundle without
                    //  started WAB
                    bundle.start();
                } else {
                    System.out.println("Bundle ID " + bundleId + " is invalid");
                    LOGGER.warn("Bundle ID {} is invalid", bundleId);
                }
            }
        }
    }

    @Override
    public void stop(List<Long> bundleIds) throws Exception {
        if (bundleIds != null && !bundleIds.isEmpty()) {
            List<WebApplicationInfo> apps = list();
            Map<Long, Bundle> mapping = new HashMap<>();
            for (WebApplicationInfo app : apps) {
                mapping.put(app.getBundle().getBundleId(), app.getBundle());
            }
            for (long bundleId : bundleIds) {
                Bundle bundle = mapping.get(bundleId);
                if (bundle != null) {
                    // undeploy
                    // TOCHECK: Pax Web has no "War Manager", so WAB == Bundle and we can't have started Bundle without
                    //  started WAB
                    bundle.stop();
                } else {
                    System.out.println("Bundle ID " + bundleId + " is invalid");
                    LOGGER.warn("Bundle ID {} is invalid", bundleId);
                }
            }
        }
    }

    @Override
    public String state(long bundleId) throws Exception {
        List<WebApplicationInfo> apps = list();
        Map<Long, Bundle> mapping = new HashMap<>();
        StringBuilder topic = new StringBuilder("Unknown    ");
        for (WebApplicationInfo app : apps) {
            if (bundleId == app.getBundle().getBundleId()) {
                topic = new StringBuilder(app.getDeploymentState());
            }
        }

        while (topic.length() < 11) {
            topic.append(" ");
        }

        return topic.toString();
    }

	@Override
	public String getWebContextPath(Long id) throws Exception {
        List<WebApplicationInfo> apps = list();
        Map<Long, Bundle> mapping = new HashMap<>();
        for (WebApplicationInfo app : apps) {
            if (id == app.getBundle().getBundleId()) {
                return app.getContextPath();
            }
        }
        return "";
	}

}
