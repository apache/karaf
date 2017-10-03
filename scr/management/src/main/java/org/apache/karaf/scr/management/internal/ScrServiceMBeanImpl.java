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

import org.apache.karaf.scr.management.ScrServiceMBean;
import org.apache.karaf.scr.management.codec.JmxComponent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.TabularData;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component(
        name = ScrServiceMBeanImpl.COMPONENT_NAME,
        enabled = true,
        immediate = true,
        properties = {"org/apache/karaf/scr/management/internal/ScrServiceMBeanImpl.properties"})
public class ScrServiceMBeanImpl extends StandardMBean implements ScrServiceMBean {

    public static final String OBJECT_NAME = "org.apache.karaf:type=scr,name=" + System.getProperty("karaf.name", "root");

    public static final String COMPONENT_NAME = "ScrServiceMBean";

    public static final String COMPONENT_LABEL = "Apache Karaf SCR Service MBean";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrServiceMBeanImpl.class);

    private MBeanServer mBeanServer;

    private ScrService scrService;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates new Declarative Services MBean.
     *
     * @throws NotCompliantMBeanException If the MBean is not a valid MBean.
     */
    public ScrServiceMBeanImpl() throws NotCompliantMBeanException {
        super(ScrServiceMBean.class);
    }

    /**
     * Service component activation call back.  Called when all dependencies are satisfied.
     *
     * @throws Exception If the activation fails.
     */
    @Activate
    public void activate() throws Exception {
        LOGGER.info("Activating the " + COMPONENT_LABEL);
        Map<Object, String> mbeans = new HashMap<>();
        mbeans.put(this, "org.apache.karaf:type=scr,name=${karaf.name}");
        try {
            lock.writeLock().lock();
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
        try {
            return JmxComponent.tableFrom(scrService.getComponents());
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }
    }

    public String[] listComponents() {
        ScrService.Component[] components = scrService.getComponents();
        String[] componentNames = new String[components.length];
        for (int i = 0; i < componentNames.length; i++) {
            componentNames[i] = components[i].getName();
        }
        return componentNames;
    }

    public boolean isComponentActive(String componentName) throws MBeanException {
        try {
            return componentState(componentName) == ScrService.Component.STATE_ACTIVE;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public int componentState(String componentName) {
        int state = -1;
        final ScrService.Component component = findComponent(componentName);
        if (component != null)
            state = component.getState();
        else
            LOGGER.warn("No component found for name: " + componentName);
        return state;
    }

    public void activateComponent(String componentName) {
        final ScrService.Component component = findComponent(componentName);
        if (component != null)
            component.enable();
        else
            LOGGER.warn("No component found for name: " + componentName);
    }

    public void deactivateComponent(String componentName) {
        final ScrService.Component component = findComponent(componentName);
        if (component != null)
            component.disable();
        else
            LOGGER.warn("No component found for name: " + componentName);
    }

    private ScrService.Component findComponent(String componentName) {
        ScrService.Component answer = null;
        if (scrService.getComponents(componentName) != null) {
            ScrService.Component[] components = scrService.getComponents(componentName);
            for (ScrService.Component component : components) {
                answer = component;
            }
        }
        return answer;
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
        this.scrService = new ScrService(FrameworkUtil.getBundle(getClass()).getBundleContext(), scrService);
    }

    public void unsetScrService(ServiceComponentRuntime scrService) {
        this.scrService = null;
    }

}
