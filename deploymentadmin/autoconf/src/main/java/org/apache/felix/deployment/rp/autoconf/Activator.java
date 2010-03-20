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
package org.apache.felix.deployment.rp.autoconf;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeService;

/**
 * Bundle activator for the AutoConf Resource Processor Customizer bundle
 */
public class Activator extends DependencyActivatorBase {

    public void init(BundleContext context, DependencyManager manager) throws Exception {
    	Dictionary properties = new Properties();
        properties.put(Constants.SERVICE_PID, "org.osgi.deployment.rp.autoconf");
        
        manager.add(createService()
            .setInterface(ResourceProcessor.class.getName(), properties)
            .setImplementation(AutoConfResourceProcessor.class)
            .add(createServiceDependency()
                .setService(ConfigurationAdmin.class)
                .setRequired(true))
            .add(createServiceDependency()
                .setService(MetaTypeService.class)
                .setRequired(false))
            .add(createServiceDependency()
                .setService(LogService.class)
                .setRequired(false)));
    }

    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        // do nothing
    }
}