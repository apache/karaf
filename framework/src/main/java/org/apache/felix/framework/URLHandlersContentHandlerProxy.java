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

import java.io.IOException;
import java.net.ContentHandler;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.framework.util.SecureAction;
import org.osgi.service.url.URLConstants;

/**
 * <p>
 * This class implements a content handler proxy. When the content handler
 * proxy instance is created, it is associated with a particular mime type
 * and will answer all future requests for content of that type. It does
 * not directly handle the content requests, but delegates the requests to
 * an underlying content handler service.
 * </p>
 * <p>
 * The proxy for a particular mime type is used for all framework instances
 * that may contain their own content handler services. When performing a
 * content handler operation, the proxy retrieves the handler service from
 * the framework instance associated with the current call stack and delegates
 * the call to the handler service.
 * </p>
 * <p>
 * The proxy will create simple content handler service trackers for each
 * framework instance. The trackers will listen to service events in its
 * respective framework instance to maintain a reference to the "best"
 * content handler service at any given time.
 * </p>
**/
class URLHandlersContentHandlerProxy extends ContentHandler
{
    private final Map m_trackerMap = new HashMap();
    private final String m_mimeType;
    private final SecureAction m_action;

    public URLHandlersContentHandlerProxy(String mimeType, SecureAction action)
    {
        m_mimeType = mimeType;
        m_action = action;
    }

    //
    // ContentHandler interface method.
    //

    public synchronized Object getContent(URLConnection urlc) throws IOException
    {
        ContentHandler svc = getContentHandlerService();
        if (svc == null)
        {
            return urlc.getInputStream();
        }
        return svc.getContent(urlc);
    }

    /**
     * <p>
     * Private method to retrieve the content handler service from the
     * framework instance associated with the current call stack. A
     * simple service tracker is created and cached for the associated
     * framework instance when this method is called.
     * </p>
     * @return the content handler service from the framework instance
     *         associated with the current call stack or <tt>null</tt>
     *         is no service is available.
    **/
    private ContentHandler getContentHandlerService()
    {
        // Get the framework instance associated with call stack.
        Object framework = URLHandlers.getFrameworkFromContext();

        // If the framework has disabled the URL Handlers service,
        // then it will not be found so just return null.
        if (framework == null)
        {
            return null;
        }

        // Get the service tracker for the framework instance or create one.
        Object tracker = m_trackerMap.get(framework);
        try
        {
            if (tracker == null)
            {
                // Create a filter for the mime type.
                String filter = 
                    "(&(objectClass="
                    + ContentHandler.class.getName()
                    + ")("
                    + URLConstants.URL_CONTENT_MIMETYPE
                    + "="
                    + m_mimeType
                    + "))";
                // Create a simple service tracker for the framework.
                tracker = m_action.invoke(m_action.getConstructor(
                    framework.getClass().getClassLoader().loadClass(
                    URLHandlersServiceTracker.class.getName()),
                    new Class[]{framework.getClass(), String.class}), 
                    new Object[]{framework, filter});
                // Cache the simple service tracker.
                m_trackerMap.put(framework, tracker);
            }
            return (ContentHandler) m_action.invoke(
                m_action.getMethod(tracker.getClass(), "getService", null), 
                tracker, null);
        }
        catch (Exception ex)
        {
            // TODO: log this or something
            ex.printStackTrace();
            return null;
        }
    }
}