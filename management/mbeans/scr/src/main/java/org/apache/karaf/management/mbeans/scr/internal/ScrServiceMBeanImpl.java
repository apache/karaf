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
package org.apache.karaf.management.mbeans.scr.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.management.mbeans.scr.ScrServiceMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@aQute.bnd.annotation.component.Component(name = ScrServiceMBeanImpl.COMPONENT_NAME, enabled = true, immediate = true,
properties = {"hidden.component=true"})
public class ScrServiceMBeanImpl extends StandardMBean implements ScrServiceMBean {

    public static final String OBJECT_NAME = "org.apache.karaf:type=scr,name=" + System.getProperty("karaf.name", "root");

    public static final String COMPONENT_NAME = "ScrServiceMBean";

    public static final String COMPONENT_LABEL = "Apache Karaf SCR Service MBean";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrServiceMBeanImpl.class);

    private MBeanServer mBeanServer;

    private ScrService scrService;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates new Declarative Services mbean.
     * 
     * @throws NotCompliantMBeanException
     */
    public ScrServiceMBeanImpl() throws NotCompliantMBeanException {
        super(ScrServiceMBean.class);
    }


    /**
     * Service component activation call back. Called when all dependencies are
     * satisfied.
     * 
     * @throws Exception
     */
    @Activate
    public void activate() throws Exception {
        LOGGER.info("Activating the " + COMPONENT_LABEL);
        Map<Object, String> mbeans = new HashMap<Object, String>();
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
     * Service component deactivation call back. Called after the component is
     * in an active state when any dependencies become unsatisfied.
     * 
     * @throws Exception
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

    /*
     * @see org.apache.karaf.management.mbeans.scr.ScrServiceMBean#listComponents()
     *
     * @return
     * @throws Exception
     */
    public String[] listComponents() throws Exception {
        Component[] components = safe(scrService.getComponents());
        String[] componentNames = new String[components.length];
        for (int i = 0; i < componentNames.length; i++) {
            componentNames[i] = components[i].getName();
        }
        return componentNames;
    }

    /*
     * @see org.apache.karaf.management.mbeans.scr.ScrServiceMBean#isComponentActive(java.lang.String)
     *
     * @param componentName
     * @return
     * @throws Exception
     */
    public boolean isComponentActive(String componentName) throws Exception {
        return (componentState(componentName) == Component.STATE_ACTIVE) ? true : false;
    }

    /*
     * @see org.apache.karaf.management.mbeans.scr.ScrServiceMBean#componentState(java.lang.String)
     *
     * @param componentName
     * @return
     * @throws Exception
     */
    public int componentState(String componentName) throws Exception {
        int state = -1;
        final Component component = findComponent(componentName);
        if (component != null)
            state = component.getState();
        else
            LOGGER.warn("No component found for name: " + componentName);
        return state;
    }

    /*
     * @see org.apache.karaf.management.mbeans.scr.ScrServiceMBean#activateComponent(java.lang.String)
     *
     * @param componentName
     * @throws Exception
     */
    public void activateComponent(String componentName) throws Exception {
        final Component component = findComponent(componentName);
        if (component != null)
            component.enable();
        else
            LOGGER.warn("No component found for name: " + componentName);
    }

    /*
     * @see org.apache.karaf.management.mbeans.scr.ScrServiceMBean#deactiveateComponent(java.lang.String)
     *
     * @param componentName
     * @throws Exception
     */
    public void deactiveateComponent(String componentName) throws Exception {
        final Component component = findComponent(componentName);
        if (component != null)
            component.disable();
        else
            LOGGER.warn("No component found for name: " + componentName);
    }

    private Component findComponent(String componentName) {
        Component answer = null;
        if (scrService.getComponents(componentName) != null) {
            Component[] components = scrService.getComponents(componentName);
            for (Component component : safe(components)) {
                answer = component;
            }
        }
        return answer;
    }

    private Component[] safe(Component[] components) {
        return components == null ? new Component[0] : components;
    }

    /**
     * @param mBeanServer the mBeanServer to set
     */
    @Reference
    public void setmBeanServer(MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
    }

    public void unsetmBeanServer(MBeanServer mBeanServer) {
        this.mBeanServer = null;
    }

    /**
     * @param scrService the scrService to set
     */
    @Reference
    public void setScrService(ScrService scrService) {
        this.scrService = scrService;
    }

    public void unsetScrService(ScrService scrService) {
        this.scrService = null;
    }

}
