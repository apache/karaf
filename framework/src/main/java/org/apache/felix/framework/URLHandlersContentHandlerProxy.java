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
import java.net.ContentHandlerFactory;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

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
    private static final String CONTENT_HANDLER_PACKAGE_PROP = "java.content.handler.pkgs";
    private static final String DEFAULT_CONTENT_HANDLER_PACKAGE = "sun.net.www.content|com.ibm.oti.net.www.content|gnu.java.net.content|org.apache.harmony.luni.internal.net.www.content|COM.newmonics.www.content";
    
    private static final Map m_builtIn = new HashMap();
    private static final String m_pkgs;

    static 
    {
        String pkgs = new SecureAction().getSystemProperty(CONTENT_HANDLER_PACKAGE_PROP, "");
        m_pkgs = (pkgs.equals(""))
            ? DEFAULT_CONTENT_HANDLER_PACKAGE
            : pkgs + "|" + DEFAULT_CONTENT_HANDLER_PACKAGE;
    }

    private final ContentHandlerFactory m_factory;

    private final Map m_trackerMap = new HashMap();
    private final String m_mimeType;
    private final SecureAction m_action;

    public URLHandlersContentHandlerProxy(String mimeType, SecureAction action, 
        ContentHandlerFactory factory)
    {
        m_mimeType = mimeType;
        m_action = action;
        m_factory = factory;
    }

    //
    // ContentHandler interface method.
    //

    public Object getContent(URLConnection urlc) throws IOException
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

        if (framework == null) 
        {
            return getBuiltIn();
        }

        // Get the service tracker for the framework instance or create one.
        Object tracker;
        synchronized (m_trackerMap)
        {
            tracker = m_trackerMap.get(framework);
        }
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
                    new Class[]{framework.getClass().getClassLoader().loadClass(
                    Felix.class.getName()), String.class}), 
                    new Object[]{framework, filter});
                // Cache the simple service tracker.
                synchronized (m_trackerMap) 
                {
                    if (!m_trackerMap.containsKey(framework))
                    {
                        m_trackerMap.put(framework, tracker);
                    }
                    else
                    {
                        tracker = m_trackerMap.get(framework);
                    }
                }
            }
            ContentHandler result;
            if (tracker instanceof URLHandlersServiceTracker)
            {
                result = (ContentHandler) 
                    ((URLHandlersServiceTracker) tracker).getService();
            }
            else
            {
                result = (ContentHandler) m_action.invokeDirect(
                    m_action.getMethod(tracker.getClass(), "getService", null), 
                    tracker, null);
            }
            if (result == null)
            {
                result = getBuiltIn();
            }
            return result;
        }
        catch (Exception ex)
        {
            // TODO: log this or something
            ex.printStackTrace();
            return null;
        }
    }

    void flush()
    {
        synchronized (m_trackerMap)
        {
            for (Iterator iter = m_trackerMap.values().iterator(); iter.hasNext();)
            {
                unregister(iter.next());
            }
            m_trackerMap.clear();
        }
    }

    private void unregister(Object tracker)
    {
        if (tracker instanceof URLHandlersServiceTracker)
        {
            ((URLHandlersServiceTracker) tracker).unregister();
        }
        else
        {
            try
            {
                m_action.invokeDirect(m_action.getMethod(tracker.getClass(), 
                    "unregister", null), tracker, null);
            }
            catch (Exception e)
            {
                // Not much we can do - log this or something
            }
        }
    }

    private ContentHandler getBuiltIn()
    {
        synchronized (m_builtIn)
        {
            if (m_builtIn.containsKey(m_mimeType))
            {
                return (ContentHandler) m_builtIn.get(m_mimeType);
            }
        }
        if (m_factory != null)
        {
            ContentHandler result = m_factory.createContentHandler(m_mimeType);
            if (result != null)
            {
                return addToCache(m_mimeType, result);
            }
        }
        // Check for built-in handlers for the mime type.
        // Remove periods, slashes, and dashes from mime type.
        String fixedType = m_mimeType.replace('.', '_').replace('/', '.').replace('-', '_');

        // Iterate over built-in packages.
        StringTokenizer pkgTok = new StringTokenizer(m_pkgs, "| ");
        while (pkgTok.hasMoreTokens())
        {
            String pkg = pkgTok.nextToken().trim();
            String className = pkg + "." + fixedType;
            try
            {
                // If a built-in handler is found then cache and return it
                Class handler = m_action.forName(className); 
                if (handler != null)
                {
                    return addToCache(m_mimeType, 
                        (ContentHandler) handler.newInstance());
                }
            }
            catch (Exception ex)
            {
                // This could be a class not found exception or an
                // instantiation exception, not much we can do in either
                // case other than ignore it.
            }
        }
        return addToCache(m_mimeType, null);
    }

    private synchronized ContentHandler addToCache(String mimeType, ContentHandler handler)
    {
        if (!m_builtIn.containsKey(mimeType))
        {
            m_builtIn.put(mimeType, handler);
            return handler;
        }
        return (ContentHandler) m_builtIn.get(mimeType);
    }
}