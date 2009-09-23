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


public abstract class AbstractConfigurationPrinter implements ConfigurationPrinter, OsgiManagerPlugin
{

    private BundleContext bundleContext;

    private ServiceRegistration registration;


    public void activate( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
        this.registration = bundleContext.registerService( SERVICE, this, null );
    }


    public void deactivate()
    {
        this.registration.unregister();
        this.bundleContext = null;
    }


    protected BundleContext getBundleContext()
    {
        return bundleContext;
    }
}
