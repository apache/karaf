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

import org.apache.karaf.web.WebBundle;
import org.apache.karaf.web.WebContainerService;
import org.apache.karaf.web.management.WebMBean;

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

    public TabularData getWebBundles() throws MBeanException {
        try {
            CompositeType webType = new CompositeType("Web Bundle", "An OSGi Web bundle",
                    new String[]{"ID", "State", "Web-State", "Level", "Web-ContextPath", "Name"},
                    new String[]{"ID of the bundle",
                            "OSGi state of the bundle",
                            "Web state of the bundle",
                            "Start level of the bundle",
                            "Web context path",
                            "Name of the bundle"},
                    new OpenType[]{SimpleType.LONG, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING});
            TabularType tableType = new TabularType("Web Bundles", "Table of web bundles", webType,
                    new String[]{"ID"});
            TabularData table = new TabularDataSupport(tableType);
            for (WebBundle webBundle : webContainerService.list()) {
                try {
                    CompositeData data = new CompositeDataSupport(webType,
                            new String[]{"ID", "State", "Web-State", "Level", "Web-ContextPath", "Name"},
                            new Object[]{webBundle.getBundleId(),
                                    webBundle.getState(),
                                    webBundle.getWebState(),
                                    webBundle.getLevel(),
                                    webBundle.getContextPath(),
                                    webBundle.getName()});
                    table.put(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return table;
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    public void start(Long bundleId) throws MBeanException {
        try {
            List<Long> list = new ArrayList<Long>();
            list.add(bundleId);
            webContainerService.start(list);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    public void start(List<Long> bundleIds) throws MBeanException {
        try {
            webContainerService.start(bundleIds);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    public void stop(Long bundleId) throws MBeanException {
        try {
            List<Long> list = new ArrayList<Long>();
            list.add(bundleId);
            webContainerService.stop(list);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    public void stop(List<Long> bundleIds) throws MBeanException {
        try {
            webContainerService.stop(bundleIds);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

}
