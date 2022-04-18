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
package org.apache.karaf.web.management.internal;

import org.apache.karaf.web.WebContainerService;
import org.apache.karaf.web.management.WebMBean;
import org.ops4j.pax.web.service.spi.model.info.WebApplicationInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Web MBean.
 */
public class WebMBeanImpl extends StandardMBean implements WebMBean {

    private WebContainerService webContainerService;

    public WebMBeanImpl() throws NotCompliantMBeanException {
        super(WebMBean.class);
    }

    public void setWebContainerService(WebContainerService webContainerService) {
        this.webContainerService = webContainerService;
    }

    @Override
    public TabularData getWebBundles() throws MBeanException {
        try {
            CompositeType webType = new CompositeType("Web Bundle", "An OSGi Web bundle",
                    new String[]{"ID", "Context Name", "State", "Web-State", "Level", "Web-ContextPath", "Name"},
                    new String[]{"ID of the bundle",
                            "Name of the context",
                            "OSGi state of the bundle",
                            "Web state of the bundle",
                            "Start level of the bundle",
                            "Web context path",
                            "Name of the bundle"},
                    new OpenType[]{SimpleType.LONG, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING});
            TabularType tableType = new TabularType("Web Bundles", "Table of web bundles", webType,
                    new String[]{"ID", "Context Name"});
            TabularData table = new TabularDataSupport(tableType);
            for (WebApplicationInfo webBundle : webContainerService.list()) {
                String contextPath = webBundle.getContextPath();
                String contextName = webBundle.getName();

                // get the bundle name
                String name = webBundle.getBundle().getHeaders().get(Constants.BUNDLE_NAME);
                // if there is no name, then default to symbolic name
                name = (name == null) ? webBundle.getBundle().getSymbolicName() : name;
                // if there is no symbolic name, resort to location
                name = (name == null) ? webBundle.getBundle().getLocation() : name;
                // get the bundle version
                String version = webBundle.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
                name = ((version != null)) ? name + " (" + version + ")" : name;
                long bundleId = webBundle.getBundle().getBundleId();
                int level = webBundle.getBundle().adapt(BundleStartLevel.class).getStartLevel();
                if (!contextPath.startsWith("/")) {
                    contextPath = "/" + contextPath;
                }

                try {
                    CompositeData data = new CompositeDataSupport(webType,
                            new String[]{"ID", "Context Name", "State", "Web-State", "Level", "Web-ContextPath", "Name"},
                            new Object[]{webBundle.getBundle().getBundleId(),
                                    contextName,
                                    getStateString(webBundle.getBundle()),
                                    webBundle.getDeploymentState(),
                                    level,
                                    contextPath,
                                    name});
                    table.put(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return table;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void install(String location, String contextPath) throws MBeanException {
        try {
            webContainerService.install(location, contextPath);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void uninstall(Long bundleId) throws MBeanException {
        try {
            List<Long> list = new ArrayList<>();
            list.add(bundleId);
            webContainerService.uninstall(list);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void uninstall(List<Long> bundleIds) throws MBeanException {
        try {
            webContainerService.uninstall(bundleIds);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void start(Long bundleId) throws MBeanException {
        try {
            List<Long> list = new ArrayList<>();
            list.add(bundleId);
            webContainerService.start(list);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void start(List<Long> bundleIds) throws MBeanException {
        try {
            webContainerService.start(bundleIds);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void stop(Long bundleId) throws MBeanException {
        try {
            List<Long> list = new ArrayList<>();
            list.add(bundleId);
            webContainerService.stop(list);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void stop(List<Long> bundleIds) throws MBeanException {
        try {
            webContainerService.stop(bundleIds);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
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
