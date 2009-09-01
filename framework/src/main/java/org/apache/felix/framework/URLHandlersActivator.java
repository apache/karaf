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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

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
    private final Map m_configMap;
    private final Felix m_framework;

    public URLHandlersActivator(Map configMap, Felix framework)
    {
        m_configMap = configMap;
        m_framework = framework;
    }

    //
    // Bundle activator methods.
    //

    public void start(BundleContext context)
    {
        // Only register the framework with the URL Handlers service
        // if the service is enabled.
        boolean enable = (m_configMap.get(
                FelixConstants.SERVICE_URLHANDLERS_PROP) == null)
                ? true
                : !m_configMap.get(FelixConstants.SERVICE_URLHANDLERS_PROP).equals("false");

        if (enable)
        {
            m_streamTracker = new ServiceTracker(context, "org.osgi.service.url.URLStreamHandlerService", null);
            m_contentTracker= new ServiceTracker(context, "java.net.ContentHandler", null);
            m_streamTracker.open();
            m_contentTracker.open();
            m_framework.setURLHandlersActivator(this);
        }
        URLHandlers.registerFrameworkInstance(m_framework, enable);
    }

    public void stop(BundleContext context)
    {
        URLHandlers.unregisterFrameworkInstance(m_framework);
        m_framework.setURLHandlersActivator(null);
        if (m_streamTracker != null)
        {
            m_streamTracker.close();
        }
        if (m_contentTracker != null)
        {
            m_contentTracker.close();
        }
        m_streamTracker = null;
        m_contentTracker = null;
    }

    private volatile ServiceTracker m_streamTracker;
    private volatile ServiceTracker m_contentTracker;
    
    protected Object getStreamHandlerService(String protocol)
    {
        return get(m_streamTracker, "url.handler.protocol", protocol);
    }

    protected Object getContentHandlerService(String mimeType)
    {
        return get(m_contentTracker, "url.content.mimetype", mimeType);
    }

    private Object get(ServiceTracker tracker, String key, String value)
    {
    	Object service = null;
        if (tracker != null)
        {
            ServiceReference[] refs = tracker.getServiceReferences();

            if (refs != null)
            {
                if (refs.length > 1)
                {
                    Arrays.sort(refs, Collections.reverseOrder());
                }

                for (int i = 0;(i < refs.length) && (service == null);i++)
                {
                    Object values = refs[i].getProperty(key);
                    if (values instanceof String[])
                    {
                        for (int j = 0;(j < ((String[]) values).length) && (service == null);j++)
                        {
                            if (value.equals(((String[]) values)[j]))
                            {
                                service = tracker.getService(refs[i]);
                            }
                        }
                    }
                    else if (value.equals(values))
                    {
                        service = tracker.getService(refs[i]);
                    }
                }
            }
        }

        return service;
    }
}
