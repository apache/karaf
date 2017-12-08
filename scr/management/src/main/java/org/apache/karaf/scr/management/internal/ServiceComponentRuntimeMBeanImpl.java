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
package org.apache.karaf.scr.management.internal;

import org.apache.karaf.scr.management.ServiceComponentRuntimeMBean;
import org.apache.karaf.scr.management.codec.JmxComponentConfiguration;
import org.apache.karaf.scr.management.codec.JmxComponentDescription;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.TabularData;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component(
        name = ServiceComponentRuntimeMBeanImpl.COMPONENT_NAME,
        enabled = true,
        immediate = true,
        properties = {"org/apache/karaf/scr/management/internal/ServiceComponentRuntimeMBeanImpl.properties"})
public class ServiceComponentRuntimeMBeanImpl extends StandardMBean implements ServiceComponentRuntimeMBean {

    public static final String OBJECT_NAME = "org.apache.karaf:type=scr,name=" + System.getProperty("karaf.name", "root");

    public static final String COMPONENT_NAME = "ServiceComponentRuntimeMBean";

    public static final String COMPONENT_LABEL = "Apache Karaf ServiceComponentRuntime MBean";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceComponentRuntimeMBeanImpl.class);

    private MBeanServer mBeanServer;

    private BundleContext context;

    private ServiceComponentRuntime scrService;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates new Declarative Services MBean.
     *
     * @throws NotCompliantMBeanException If the MBean is not a valid MBean.
     */
    public ServiceComponentRuntimeMBeanImpl() throws NotCompliantMBeanException {
        super(ServiceComponentRuntimeMBean.class);
    }

    /**
     * Service component activation call back.  Called when all dependencies are satisfied.
     *
     * @throws Exception If the activation fails.
     */
    @Activate
    public void activate(BundleContext context) throws Exception {
        LOGGER.info("Activating the " + COMPONENT_LABEL);
        Map<Object, String> mbeans = new HashMap<>();
        mbeans.put(this, "org.apache.karaf:type=scr,name=${karaf.name}");
        try {
            lock.writeLock().lock();
            this.context = context;
            if (mBeanServer != null) {
                mBeanServer.registerMBean(this, new ObjectName(OBJECT_NAME));
            }
        } catch (Exception e) {
            LOGGER.error("Exception registering the SCR Management MBean: " + e.getLocalizedMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Service component deactivation call back.  Called after the component is in an active
     * state when any dependencies become unsatisfied.
     *
     * @throws Exception If the deactivation fails.
     */
    @Deactivate
    public void deactivate() throws Exception {
        LOGGER.info("Deactivating the " + COMPONENT_LABEL);
        try {
            lock.writeLock().lock();
            if (mBeanServer != null) {
                mBeanServer.unregisterMBean(new ObjectName(OBJECT_NAME));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public TabularData getComponents() {
        return JmxComponentDescription.tableFrom(scrService.getComponentDescriptionDTOs());
    }

    @Override
    public TabularData getComponentConfigs() {
        return JmxComponentConfiguration.tableFrom(
                scrService.getComponentDescriptionDTOs().stream()
                    .map(c -> scrService.getComponentConfigurationDTOs(c))
                    .flatMap(Collection::stream));
    }

    @Override
    public TabularData getComponentConfigs(long bundleId, String componentName) {
        return JmxComponentConfiguration.tableFrom(
                scrService.getComponentConfigurationDTOs(
                        findComponent(bundleId, componentName)));
    }

    public boolean isComponentEnabled(long bundleId, String componentName) {
        return scrService.isComponentEnabled(findComponent(bundleId, componentName));
    }

    public void enableComponent(long bundleId, String componentName) {
        scrService.enableComponent(findComponent(bundleId, componentName));
    }

    public void disableComponent(long bundleId, String componentName) {
        scrService.disableComponent(findComponent(bundleId, componentName));
    }

    private ComponentDescriptionDTO findComponent(long bundleId, String componentName) {
        Bundle bundle = context.getBundle(bundleId);
        if (bundle != null) {
            return scrService.getComponentDescriptionDTO(bundle, componentName);
        } else {
            throw new IllegalArgumentException("No component found for name: " + componentName);
        }
    }

    @Reference
    public void setmBeanServer(MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
    }

    public void unsetmBeanServer(MBeanServer mBeanServer) {
        this.mBeanServer = null;
    }

    @Reference
    public void setScrService(ServiceComponentRuntime scrService) {
        this.scrService = scrService;
    }

    public void unsetScrService(ServiceComponentRuntime scrService) {
        this.scrService = null;
    }

}
