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
package org.apache.karaf.scr.examples.managed.service.component;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import org.apache.karaf.scr.examples.managed.service.ManagedGreeterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component(name = ManagedGreeterComponent.COMPONENT_NAME)
public class ManagedGreeterComponent {

    public static final String COMPONENT_NAME = "ManagedGreeterComponent";

    public static final String COMPONENT_LABEL = "Managed Greeter Component";

    private static final Logger LOG = LoggerFactory.getLogger(ManagedGreeterComponent.class);

    private ManagedGreeterService greeterService;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Called when all of the SCR Components required dependencies have been satisfied
     */
    @Activate
    public void activate() {
        LOG.info("Activating the {}", COMPONENT_LABEL);
        try {
            lock.readLock().lock();
            if (greeterService != null) {
                greeterService.startGreeter();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Called when any of the SCR Components required dependencies become unsatisfied.
     */
    @Deactivate
    public void deactivate() {
        LOG.info("Deactivating the {}", COMPONENT_LABEL);
        try {
            lock.readLock().lock();
            if (greeterService != null) {
                greeterService.stopGreeter();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Reference
    public void setGreeterService(final ManagedGreeterService greeterService) {
        try {
            lock.writeLock().lock();
            this.greeterService = greeterService;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unsetGreeterService() {
        try {
            lock.writeLock().lock();
            this.greeterService = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

}
