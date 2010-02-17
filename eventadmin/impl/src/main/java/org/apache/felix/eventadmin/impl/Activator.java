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
package org.apache.felix.eventadmin.impl;

import org.apache.felix.eventadmin.impl.util.LogWrapper;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator of the EventAdmin bundle. This class registers an implementation of
 * the OSGi R4 <tt>EventAdmin</tt> service (see the Compendium 113) with the
 * framework. It features timeout-based blacklisting of event-handlers for both,
 * asynchronous and synchronous event-dispatching (as a spec conform optional
 * extension).
 *
 * @see Configuration For configuration features of the event admin.
 *
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
// TODO: Security is in place but untested due to not being implemented by the
// framework. However, it needs to be revisited once security is implemented.
// Two places are affected by this namely, security/* and handler/*
public class Activator implements BundleActivator
{
    private volatile Configuration m_config;

    /**
     * Called upon starting of the bundle. Constructs and registers the EventAdmin
     * service with the framework. Note that the properties of the service are
     * requested from the context in this method hence, the bundle has to be
     * restarted in order to take changed properties into account.
     *
     * @param context The bundle context passed by the framework
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context)
    {
        // init the LogWrapper. Subsequently, the static methods of the LogWrapper
        // can be used to log messages similar to the LogService. The effect of a
        // call to any of this methods is either a print to standard out (in case
        // no LogService is present) or a call to the respective method of
        // available LogServices (the reason is that this way the bundle is
        // independent of the org.osgi.service.log package)
        LogWrapper.setContext(context);

        // this creates the event admin and starts it
        m_config = new Configuration(context);
    }

    /**
     * Called upon stopping the bundle. This will block until all pending events are
     * delivered. An IllegalStateException will be thrown on new events starting with
     * the begin of this method. However, it might take some time until we settle
     * down which is somewhat cumbersome given that the spec asks for return in
     * a timely manner.
     *
     * @param context The bundle context passed by the framework
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context)
    {
        if ( m_config != null )
        {
            m_config.destroy();
        }
        m_config = null;

        // FELIX-2089: "unset" the bundle context on stop
        LogWrapper.setContext(null);
    }
}
