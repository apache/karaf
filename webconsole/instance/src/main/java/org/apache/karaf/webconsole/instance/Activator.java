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
package org.apache.karaf.webconsole.instance;

import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;

@Services(
        requires = @RequireService(InstanceService.class)
)
public class Activator extends BaseActivator {

    private InstancePlugin instancePlugin;

    @Override
    protected void doStart() throws Exception {
        instancePlugin = new InstancePlugin();
        instancePlugin.setBundleContext(bundleContext);
        instancePlugin.setInstanceService(getTrackedService(InstanceService.class));
        instancePlugin.start();

        Dictionary<String, String> props = new Hashtable<>();
        props.put("felix.webconsole.label", "instance");
        register(Servlet.class, instancePlugin, props);
    }

    @Override
    protected void doStop() {
        super.doStop();
        if (instancePlugin != null) {
            instancePlugin.stop();
            instancePlugin = null;
        }
    }

}
