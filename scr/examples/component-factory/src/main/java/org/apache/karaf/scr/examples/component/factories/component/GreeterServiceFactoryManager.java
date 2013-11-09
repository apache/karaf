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
package org.apache.karaf.scr.examples.component.factories.component;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import org.apache.karaf.scr.examples.component.factories.GreeterServiceComponentFactory;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component(name = GreeterServiceFactoryManager.COMPONENT_NAME)
public class GreeterServiceFactoryManager {

    public static final String COMPONENT_NAME = "GreeterServiceFactoryManager";

    public static final String COMPONENT_LABEL = "Greeter Service Factory Manager";

    private static final Logger LOG = LoggerFactory.getLogger(GreeterServiceFactoryManager.class);

    private ComponentFactory factory;
    private ComponentInstance instance;
    private GreeterServiceComponentFactory greeterService;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Called when all of the SCR Components required dependencies have been satisfied.
     */
    @Activate
    public void activate() {
        LOG.info("Activating the {}", COMPONENT_LABEL);
        try {
            lock.readLock().lock();
            if (factory != null) {
                final Properties props = new Properties();
                props.setProperty("salutation", "Hello");
                props.setProperty("name", "User");
                instance = factory.newInstance((Dictionary) props);
                greeterService = (GreeterServiceComponentFactory) instance.getInstance();
                greeterService.startGreeter();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Callend when any of the SCR Components required dependencies become unsatisfied.
     */
    @Deactivate
    public void deactivate() {
        LOG.info("Deactivating the {}", COMPONENT_LABEL);
        try {
            lock.readLock().lock();
            greeterService.stopGreeter();
            instance.dispose();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Reference(target = "(component.factory=greeter.factory.provider)")
    public void setFactory(final ComponentFactory factory) {
        try {
            lock.writeLock().lock();
            this.factory = factory;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unsetFactory() {
        try {
            lock.writeLock().lock();
            this.factory = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

}
