/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.io;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.io.ConnectorService;

/**
 * IO Connector bundle Activator.
 * 
 * @version $Rev$ $Date$
 */
public class Activator implements BundleActivator
{

    private ServiceRegistration m_registration;
    private ConnectorServiceImpl m_connectorService;

    /**
     * Called when IO Connector bundle is started. Creates and registers IO Connector Service.
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception
    {
        m_connectorService = new ConnectorServiceImpl(context);
        context.registerService(ConnectorService.class.getName(), m_connectorService, null);
    }

    /**
     * Called when IO Connector bundle is stopped. Stops IO Connector Service and unregisters the service.
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception
    {
        if (m_connectorService != null)
        {
            m_connectorService.stop();
        }
        if (m_registration != null)
        {
            m_registration.unregister();
        }
    }

}