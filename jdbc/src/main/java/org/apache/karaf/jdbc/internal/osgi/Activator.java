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
package org.apache.karaf.jdbc.internal.osgi;

import org.apache.karaf.jdbc.JdbcService;
import org.apache.karaf.jdbc.internal.JdbcMBeanImpl;
import org.apache.karaf.jdbc.internal.JdbcServiceImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.ProvideService;
import org.apache.karaf.util.tracker.RequireService;
import org.apache.karaf.util.tracker.Services;
import org.osgi.service.cm.ConfigurationAdmin;

@Services(
    provides = @ProvideService(JdbcService.class),
    requires = @RequireService(ConfigurationAdmin.class)
)
public class Activator extends BaseActivator {
    @Override
    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        JdbcServiceImpl service = new JdbcServiceImpl();
        service.setBundleContext(bundleContext);
        service.setConfigAdmin(configurationAdmin);
        register(JdbcService.class, service);

        JdbcMBeanImpl mbean = new JdbcMBeanImpl();
        mbean.setJdbcService(service);
        registerMBean(mbean, "type=jdbc");
    }
}
