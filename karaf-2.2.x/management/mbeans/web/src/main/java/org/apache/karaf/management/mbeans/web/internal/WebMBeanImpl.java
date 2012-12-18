/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *
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
package org.apache.karaf.management.mbeans.web.internal;

import org.apache.karaf.management.mbeans.web.WebMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;

/**
 * Web MBean implementation.
 */
public class WebMBeanImpl extends StandardMBean implements WebMBean {

    private BundleContext bundleContext;

    public WebMBeanImpl() throws NotCompliantMBeanException {
        super(WebMBean.class);
    }

    public TabularData list() throws Exception {
        CompositeType webType = new CompositeType("Web Bundle", "An OSGi Web bundle",
                new String[]{"ID", "Name", "Context"},
                new String[]{"ID of the bundle", "Name of the bundle", "Web Context"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
        TabularType tableType = new TabularType("Web Bundles", "Table of web bundles", webType,
                new String[]{"ID"});
        TabularData table = new TabularDataSupport(tableType);
        for (Bundle bundle : bundleContext.getBundles()) {
            try {
                String webContext = (String) bundle.getHeaders().get("Web-ContextPath");
                if (webContext == null)
                    webContext = (String) bundle.getHeaders().get("Webapp-Context");
                if (webContext == null)
                    continue;

                webContext.trim();
                if (!webContext.startsWith("/")) {
                    webContext = "/" + webContext;
                }

                String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
                name = (name == null) ? bundle.getSymbolicName() : name;
                // If there is no symbolic name, resort to location.
                name = (name == null) ? bundle.getLocation() : name;

                CompositeData data = new CompositeDataSupport(webType,
                        new String[]{"ID", "Name", "Context"},
                        new Object[]{bundle.getBundleId(), name, webContext});
                table.put(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return table;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
