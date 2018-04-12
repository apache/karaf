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
package org.apache.karaf.service.core.internal;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.karaf.service.core.ServicesMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Implementation of the Services MBean.
 */
public class ServicesMBeanImpl extends StandardMBean implements ServicesMBean {

    private BundleContext bundleContext;

    public ServicesMBeanImpl() throws NotCompliantMBeanException {
        super(ServicesMBean.class);
    }

    public TabularData getServices() throws MBeanException {
        try {
            return getServices(-1, false);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public TabularData getServices(boolean inUse) throws MBeanException {
        try {
            return getServices(-1, inUse);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public TabularData getServices(long bundleId) throws MBeanException {
        try {
            return getServices(bundleId, false);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public TabularData getServices(long bundleId, boolean inUse) throws MBeanException {
        try {
            CompositeType serviceType = new CompositeType("Service", "OSGi Service",
                    new String[]{"Interfaces", "Properties"},
                    new String[]{"Interfaces class name of the service", "Properties of the service"},
                    new OpenType[]{new ArrayType(1, SimpleType.STRING), new ArrayType(1, SimpleType.STRING)});
            TabularType tableType = new TabularType("Services", "Table of OSGi Services", serviceType,
                    new String[]{"Interfaces", "Properties"});
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
                            List<String> properties = new ArrayList<>();
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
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
