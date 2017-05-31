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
package org.apache.karaf.jndi.internal.osgi;

import java.util.Hashtable;

import javax.naming.spi.InitialContextFactory;

import org.apache.aries.proxy.ProxyManager;
import org.apache.karaf.jndi.JndiService;
import org.apache.karaf.jndi.KarafInitialContextFactory;
import org.apache.karaf.jndi.internal.JndiMBeanImpl;
import org.apache.karaf.jndi.internal.JndiServiceImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;

@Services(
        requires = @RequireService(ProxyManager.class),
        provides = @ProvideService(JndiService.class)
)
public class Activator extends BaseActivator {

    @Override
    protected void doStart() throws Exception {
        ProxyManager proxyManager = getTrackedService(ProxyManager.class);

        register(InitialContextFactory.class, new KarafInitialContextFactory());

        JndiServiceImpl service = new JndiServiceImpl();
        service.setBundleContext(bundleContext);
        service.setProxyManager(proxyManager);
        Hashtable<String, String> props = new Hashtable<>();
        // bind the JNDI service itself in the JNDI context
        props.put("osgi.jndi.service.name", "jndi");
        register(JndiService.class, service, props);
        JndiMBeanImpl mbean = new JndiMBeanImpl();
        mbean.setJndiService(service);
        registerMBean(mbean, "type=jndi");
    }
}
