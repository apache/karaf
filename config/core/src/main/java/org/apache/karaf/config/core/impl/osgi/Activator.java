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

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.config.core.impl.ConfigMBeanImpl;
import org.apache.karaf.config.core.impl.ConfigRepositoryImpl;
import org.apache.karaf.config.core.impl.JsonConfigInstaller;
import org.apache.karaf.config.core.impl.KarafConfigurationPlugin;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPlugin;

import java.util.Dictionary;
import java.util.Hashtable;

@Services(
        requires = @RequireService(ConfigurationAdmin.class),
        provides = {
                @ProvideService(ConfigRepository.class),
                @ProvideService(ConfigurationPlugin.class),
                @ProvideService(ArtifactInstaller.class),
                @ProvideService(ConfigurationListener.class)
        }
)
public class Activator extends BaseActivator {

    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null) {
            return;
        }

        ConfigRepository configRepository = new ConfigRepositoryImpl(configurationAdmin);
        register(ConfigRepository.class, configRepository);

        KarafConfigurationPlugin karafConfigurationPlugin = new KarafConfigurationPlugin();
        Dictionary<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(ConfigurationPlugin.CM_RANKING, KarafConfigurationPlugin.PLUGIN_RANKING);
        serviceProps.put("config.plugin.id", KarafConfigurationPlugin.PLUGIN_ID);
        register(ConfigurationPlugin.class, karafConfigurationPlugin, serviceProps);

        JsonConfigInstaller jsonConfigInstaller = new JsonConfigInstaller(configurationAdmin);
        register(new Class[]{ ArtifactInstaller.class, ConfigurationListener.class }, jsonConfigInstaller);

        ConfigMBeanImpl configMBean = new ConfigMBeanImpl();
        configMBean.setConfigRepo(configRepository);
        registerMBean(configMBean, "type=config");
    }

}
