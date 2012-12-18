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
package org.apache.karaf.management.mbeans.services.internal;

import org.apache.karaf.management.mbeans.services.ServicesMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Services MBean.
 */
public class ServicesMBeanImpl extends StandardMBean implements ServicesMBean {

    private BundleContext bundleContext;

    public ServicesMBeanImpl() throws NotCompliantMBeanException {
        super(ServicesMBean.class);
    }

    public TabularData list() throws Exception {
        return list(-1, false);
    }

    public TabularData list(boolean inUse) throws Exception {
        return list(-1, inUse);
    }

    public TabularData list(long bundleId) throws Exception {
        return list(bundleId, false);
    }

    public TabularData list(long bundleId, boolean inUse) throws Exception {
        CompositeType serviceType = new CompositeType("Service", "OSGi Service",
                new String[]{"Interfaces", "Properties"},
                new String[]{"Interfaces class name of the service", "Properties of the service"},
                new OpenType[]{new ArrayType(1, SimpleType.STRING), new ArrayType(1, SimpleType.STRING)});
        TabularType tableType = new TabularType("Services", "Table of OSGi Services", serviceType,
                new String[]{"Interfaces"});
        TabularData table = new TabularDataSupport(tableType);

        Bundle[] bundles;
        if (bundleId >= 0) {
            bundles = new Bundle[]{bundleContext.getBundle(bundleId)};
        } else {
            bundles = bundleContext.getBundles();
        }
        for (Bundle bundle : bundles) {
            try {
                ServiceReference[] serviceReferences;
                if (inUse) {
                    serviceReferences = bundle.getServicesInUse();
                } else {
                    serviceReferences = bundle.getRegisteredServices();
                }
                if (serviceReferences != null) {
                    for (ServiceReference reference : serviceReferences) {
                        String[] interfaces = (String[]) reference.getProperty("objectClass");
                        List<String> properties = new ArrayList<String>();
                        for (String key : reference.getPropertyKeys()) {
                            properties.add(key + " = " + reference.getProperty(key));
                        }
                        CompositeData data = new CompositeDataSupport(serviceType,
                                new String[]{"Interfaces", "Properties"},
                                new Object[]{interfaces, properties.toArray(new String[0])});
                        table.put(data);
                    }
                }
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
