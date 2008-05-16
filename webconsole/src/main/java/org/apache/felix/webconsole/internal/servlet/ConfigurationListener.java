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
package org.apache.felix.webconsole.internal.servlet;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

class ConfigurationListener implements ManagedService {

    private final OsgiManager slingManager;

    static ServiceRegistration create(OsgiManager slingManager) {
        ConfigurationListener cl = new ConfigurationListener(slingManager);

        Dictionary props = new Hashtable();
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_DESCRIPTION,
            "Sling Management Console Configuration Receiver");
        props.put(Constants.SERVICE_PID, slingManager.getClass().getName());

        return slingManager.getBundleContext().registerService(
            ManagedService.class.getName(), cl, props);
    }

    private ConfigurationListener(OsgiManager slingManager) {
        this.slingManager = slingManager;
    }

    public void updated(Dictionary config) {
        slingManager.updateConfiguration(config);
    }
}