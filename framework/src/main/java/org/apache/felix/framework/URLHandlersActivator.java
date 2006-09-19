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
package org.apache.felix.framework;

import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * Simple activator class used by the system bundle to enable the
 * URL Handlers service. The only purpose of this class is to call
 * <tt>URLHandlers.registerInstance()</tt> when the framework is
 * started and <tt>URLHandlers.unregisterInstance()</tt> when the
 * framework is stopped.
 *</p>
**/
class URLHandlersActivator implements BundleActivator
{
    private Felix m_framework = null;
    private BundleContext m_context = null;

    public URLHandlersActivator(Felix framework)
    {
        m_framework = framework;
    }

    //
    // Bundle activator methods.
    //

    public void start(BundleContext context)
    {
        m_context = context;
        // Only register the framework with the URL Handlers service
        // if the service is enabled.
        boolean enable = (m_framework.getConfig().get(
                FelixConstants.SERVICE_URLHANDLERS_PROP) == null)
                ? true
                : !m_framework.getConfig().get(FelixConstants.SERVICE_URLHANDLERS_PROP).equals("false");
        URLHandlers.registerInstance(m_framework, m_context, enable);
    }

    public void stop(BundleContext context)
    {
        URLHandlers.unregisterInstance(m_framework);
    }
}