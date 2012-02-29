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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.management.MBeanRegistrer;
import org.apache.karaf.scr.management.ScrServiceMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Deactivate;

@aQute.bnd.annotation.component.Component(name = ScrServiceMBeanImpl.COMPONENT_NAME, enabled = true, immediate = true)
public class ScrServiceMBeanImpl extends StandardMBean implements ScrServiceMBean {

    public static final String COMPONENT_NAME = "ScrServiceMBean";

    public static final String COMPONENT_LABEL = "Apache Karaf SCR Service MBean";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ScrServiceMBeanImpl.class);

    private MBeanServer mBeanServer;

    private ScrService scrService;

    private MBeanRegistrer mBeanRegistrer;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates new Declarative Services mbean.
     * 
     * @throws NotCompliantMBeanException
     */
    public ScrServiceMBeanImpl() throws NotCompliantMBeanException {
        super(ScrServiceMBean.class);
    }

    @Activate
    public void activate() throws Exception {
        LOGGER.info("Activating the " + COMPONENT_LABEL);
        Map<Object, String> mbeans = new HashMap<Object, String>();
        String karafName = System.getProperty("karaf.name", "root");
        mbeans.put(this, "org.apache.karaf:type=scr,name=" + karafName);
        try {
            lock.writeLock().lock();
            mBeanRegistrer = new MBeanRegistrer();
            mBeanRegistrer.setMbeans(mbeans);
            mBeanRegistrer.registerMBeanServer(mBeanServer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Deactivate
    public void deactivate() throws Exception {
        LOGGER.info("Deactivating the " + COMPONENT_LABEL);
        try {
            lock.writeLock().lock();
            mBeanRegistrer.unregisterMBeanServer(mBeanServer);
            mBeanRegistrer = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String[] listComponents() throws Exception {
        Component[] components = safe(scrService.getComponents());
        String[] componentNames = new String[components.length];
        for (int i = 0; i < componentNames.length; i++) {
            componentNames[i] = components[i].getName();
        }
        return componentNames;
    }

    public boolean isComponentActive(String componentName) throws Exception {
        boolean state = false;
        Component[] components = scrService.getComponents(componentName);
        for (Component component : safe(components)) {
            state = (component.getState() == Component.STATE_ACTIVE)?true:false;
        }
        return state;
    }

    public void activateComponent(String componentName) throws Exception {
        if (scrService.getComponents(componentName) != null) {
            Component[] components = scrService.getComponents(componentName);
            for (Component component : safe(components)) {
                component.enable();
            }
        }
    }

    public void deactiveateComponent(String componentName) throws Exception {
        if (scrService.getComponents(componentName) != null) {
            Component[] components = scrService.getComponents(componentName);
            for (Component component : safe(components)) {
                component.disable();
            }
        }
    }
    
    private Component[] safe( Component[] components ) {
        return components == null ? new Component[0] : components;
    }

	/**
	 * @param mBeanServer the mBeanServer to set
	 */
	public void setmBeanServer(MBeanServer mBeanServer) {
		this.mBeanServer = mBeanServer;
	}

	/**
	 * @param scrService the scrService to set
	 */
	public void setScrService(ScrService scrService) {
		this.scrService = scrService;
	}

}
