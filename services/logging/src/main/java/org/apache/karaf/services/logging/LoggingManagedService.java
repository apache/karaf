/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.services.logging;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

/**
 * Applies the <code>org.apache.karaf.logging</code> configuration updates. It's kept apart from the {@link Activator}
 * as <code>org.osgi.service.cm</code> is an optional import: this class can only be loaded when it's wired.
 */
class LoggingManagedService implements ManagedService {

    private final JulConfig julConfig;

    private LoggingManagedService(JulConfig julConfig) {
        this.julConfig = julConfig;
    }

    static ServiceRegistration<?> register(BundleContext context, JulConfig julConfig) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, Activator.CONFIGURATION_PID);
        return context.registerService(ManagedService.class.getName(), new LoggingManagedService(julConfig), props);
    }

    @Override
    public void updated(Dictionary<String, ?> properties) {
        julConfig.update(properties);
    }
}
