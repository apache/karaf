/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.webconsole.plugins.memoryusage.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator
{

    private MemoryUsageSupport support;

    public void start(final BundleContext bundleContext)
    {
        support = new MemoryUsageSupport(bundleContext);

        // install thread handler shell command
        new AbstractServiceFactory(bundleContext, null, "org.apache.felix.shell.Command")
        {
            @Override
            protected Object createObject()
            {
                return new MemoryUsageCommand(support);
            }
        };

        // install Web Console plugin
        Dictionary<String, Object> pluginProps = new Hashtable<String, Object>();
        pluginProps.put("felix.webconsole.label", MemoryUsageConstants.LABEL);
        new AbstractServiceFactory(bundleContext, pluginProps, "javax.servlet.Servlet",
            "org.apache.felix.webconsole.ConfigurationPrinter")
        {
            @Override
            public Object createObject()
            {
                return new MemoryUsagePanel(support);
            }
        };

        // register for configuration
        Dictionary<String, Object> cmProps = new Hashtable<String, Object>();
        cmProps.put(Constants.SERVICE_PID, MemoryUsageConstants.PID);
        new AbstractServiceFactory(bundleContext, cmProps, "org.osgi.service.cm.ManagedService")
        {
            @Override
            public Object createObject()
            {
                return new MemoryUsageConfigurator(support);
            }
        };
    }

    public void stop(BundleContext bundleContext)
    {
        if (support != null)
        {
            support.dispose();
            support = null;
        }
    }

    private static abstract class AbstractServiceFactory implements ServiceFactory
    {
        private int counter;
        private Object service;

        public AbstractServiceFactory(BundleContext context, Dictionary<String, Object> properties,
            String... serviceNames)
        {
            // ensure properties
            if (properties == null)
            {
                properties = new Hashtable<String, Object>();
            }

            // default settings
            properties.put(Constants.SERVICE_DESCRIPTION, "Memory Usage (" + serviceNames[0] + ")");
            properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");

            context.registerService(serviceNames, this, properties);
        }

        public synchronized void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
        {
            counter--;
            if (counter <= 0)
            {
                service = null;
            }
        }

        public synchronized Object getService(Bundle bundle, ServiceRegistration registration)
        {
            counter++;
            if (service == null)
            {
                service = createObject();
            }
            return service;
        }

        protected abstract Object createObject();
    }
}
