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
package org.apache.felix.webconsole.internal;


import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * AbstractConfigurationPrinter is an utility class, that provides a basic implementation
 * of {@link ConfigurationPrinter} and {@link OsgiManagerPlugin} interfaces.
 */
public abstract class AbstractConfigurationPrinter implements ConfigurationPrinter, OsgiManagerPlugin
{

    private BundleContext bundleContext;

    private ServiceRegistration registration;


    /**
     * @see org.apache.felix.webconsole.internal.OsgiManagerPlugin#activate(org.osgi.framework.BundleContext)
     */
    public void activate( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
        this.registration = bundleContext.registerService( SERVICE, this, null );
    }


    /**
     * @see org.apache.felix.webconsole.internal.OsgiManagerPlugin#deactivate()
     */
    public void deactivate()
    {
        this.registration.unregister();
        this.bundleContext = null;
    }


    /**
     * Returns the <code>BundleContext</code> with which this plugin has been
     * activated. If the plugin has not be activated by calling the
     * {@link #activate(BundleContext)} method, this method returns
     * <code>null</code>.
     *
     * @return the bundle context or <code>null</code> if the bundle is not activated.
     */
    protected BundleContext getBundleContext()
    {
        return bundleContext;
    }
}
