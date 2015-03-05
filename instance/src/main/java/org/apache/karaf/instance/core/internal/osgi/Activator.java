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
package org.apache.karaf.instance.core.internal.osgi;

import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.internal.InstanceServiceImpl;
import org.apache.karaf.instance.core.internal.InstancesMBeanImpl;
import org.apache.karaf.shell.api.console.CommandLoggingFilter;
import org.apache.karaf.shell.support.RegexCommandLoggingFilter;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;

@Services(provides = @ProvideService(InstanceService.class))
public class Activator extends BaseActivator {

    @Override
    protected void doStart() throws Exception {
        InstanceService instanceService = new InstanceServiceImpl();
        register(InstanceService.class, instanceService);

        RegexCommandLoggingFilter filter = new RegexCommandLoggingFilter();
        filter.addCommandOption("--password", "connect");
        filter.addCommandOption("-p", "connect");
        register(CommandLoggingFilter.class, filter);

        InstancesMBeanImpl mbean = new InstancesMBeanImpl(instanceService);
        registerMBean(mbean, "type=instance");
    }

}
