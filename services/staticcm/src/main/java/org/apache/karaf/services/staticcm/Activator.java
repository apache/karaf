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
package org.apache.karaf.services.staticcm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class Activator implements BundleActivator {

    public static final String CONFIG_DIRS = "org.apache.karaf.services.staticcm.ConfigDirs";

    ServiceRegistration<ConfigurationAdmin> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        List<Configuration> configs = new ArrayList<>();

        String cfgDirs = context.getProperty(CONFIG_DIRS);
        if (cfgDirs == null) {
            cfgDirs = System.getProperty("karaf.etc");
        }
        for (String dir : cfgDirs.split(",")) {
            List<Configuration> cfgs = Configurations.loadConfigurations(context, new File(dir.trim()));
            configs.addAll(cfgs);
        }

        StaticConfigAdminImpl cm = new StaticConfigAdminImpl(context, configs);
        registration = context.registerService(ConfigurationAdmin.class, cm, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registration != null) {
            registration.unregister();
        }
    }
}
