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
package org.apache.karaf.config.core.impl.osgi;

import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.config.core.impl.ConfigMBeanImpl;
import org.apache.karaf.config.core.impl.ConfigRepositoryImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;

@Services(
        requires = @RequireService(ConfigurationAdmin.class),
        provides = @ProvideService(ConfigRepository.class)
)
public class Activator extends BaseActivator {

    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null) {
            return;
        }

        ConfigRepository configRepository = new ConfigRepositoryImpl(configurationAdmin, new File(System.getProperty("karaf.etc")));
        register(ConfigRepository.class, configRepository);

        ConfigMBeanImpl configMBean = new ConfigMBeanImpl();
        configMBean.setConfigRepo(configRepository);
        registerMBean(configMBean, "type=config");
    }

}
