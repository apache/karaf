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
import org.osgi.framework.*;

/**
 * <p>
 * This class implements a simple service tracker that maintains a
 * service object reference to the "best" service available at any
 * given time that matches the filter associated with the tracker.
 * The best service is the one with the one with the highest ranking
 * and lowest service identifier.
 *</p>
**/
public class URLHandlersServiceTracker implements ServiceListener
{
    private final BundleContext m_context;
    private final String m_filter;
    private ServiceReference m_ref = null;
    private volatile Object m_svcObj = null;
    private long m_id = -1;
    private int m_rank = -1;

    /**
     * <p>
     * Creates a simple service tracker associated with the specified bundle
     * context for services matching the specified filter.
     * </p>
     * @param context the bundle context used for tracking services.
     * @param filter the filter used for matching services.
    **/
    public URLHandlersServiceTracker(Felix framework, String filter)
    {
        m_context = ((BundleImpl) framework).getBundleContext();
        m_filter = filter;

        synchronized (this)
        {
            // Add a service listener to track service changes
            // for services matching the specified filter.
            try
            {
                m_context.addServiceListener(this, m_filter);
            }
            catch (InvalidSyntaxException ex)
            {
                System.out.println("Cannot add service listener." + ex);
            }

            // Select the best service object.
            selectBestService();

        } // End of synchronized block.
    }

    public void unregister()
    {
        m_context.removeServiceListener(this);
    }

    public Object getService()
    {
        return m_svcObj;
    }
    
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference eventRef = event.getServiceReference();
        if ((event.getType() == ServiceEvent.REGISTERED) ||
            (event.getType() == ServiceEvent.MODIFIED))
        {
            synchronized (URLHandlersServiceTracker.this)
            {
                Long idObj = (Long) eventRef.getProperty(FelixConstants.SERVICE_ID);
                Integer rankObj = (Integer) eventRef.getProperty(FelixConstants.SERVICE_RANKING);
                int rank = (rankObj == null) ? 0 : rankObj.intValue();
                if ((rank > m_rank) ||
                    ((rank == m_rank) && (idObj.longValue() < m_id)))
                {
                    if (m_ref != null)
                    {
                        m_context.ungetService(m_ref);
                    }
                    m_ref = eventRef;
                    m_rank = rank;
                    m_id = idObj.longValue();
                    m_svcObj = m_context.getService(m_ref);
                }
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            synchronized (URLHandlersServiceTracker.this)
            {
                if (eventRef == m_ref)
                {
                    selectBestService();
                }
            }
        }
    }

    /**
     * <p>
     * This method selects the highest ranking service object with the
     * lowest service identifier out of the services selected by the
     * service filter associated with this proxy. This method is called
     * to initialize the proxy and any time when the service object
     * being used is unregistered. If there is an existing service
     * selected when this method is called, it will unget the existing
     * service before selecting the best available service.
     * </p>
    **/
    private synchronized void selectBestService()
    {
        // If there is an existing service, then unget it.
        if (m_ref != null)
        {
            m_context.ungetService(m_ref);
            m_ref = null;
            m_svcObj = null;
            m_id = -1;
            m_rank = -1;
        }

        try
        {
            // Get all service references matching the service filter
            // associated with this proxy.
            ServiceReference[] refs = m_context.getServiceReferences(null, m_filter);
            // Loop through all service references and select the reference
            // with the highest ranking and lower service identifier.
            for (int i = 0; (refs != null) && (i < refs.length); i++)
            {
                Long idObj = (Long) refs[i].getProperty(FelixConstants.SERVICE_ID);
                Integer rankObj = (Integer) refs[i].getProperty(FelixConstants.SERVICE_RANKING);
                // Ranking value defaults to zero.
                int rank = (rankObj == null) ? 0 : rankObj.intValue();
                if ((rank > m_rank) ||
                    ((rank == m_rank) && (idObj.longValue() < m_id)))
                {
                    m_ref = refs[i];
                    m_rank = rank;
                    m_id = idObj.longValue();
                }
            }

            // If a service reference was selected, then
            // get its service object.
            if (m_ref != null)
            {
                m_svcObj = m_context.getService(m_ref);
            }
        }
        catch (InvalidSyntaxException ex)
        {
//TODO: LOGGER.
            System.err.println("URLHandlersServiceTracker: " + ex);
        }
    }
}