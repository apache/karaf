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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.framework.util.SecureAction;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;

/**
 * <p>
 * This class implements a stream handler proxy. When the stream handler
 * proxy instance is created, it is associated with a particular protocol
 * and will answer all future requests for handling of that stream type. It
 * does not directly handle the stream handler requests, but delegates the
 * requests to an underlying stream handler service.
 * </p>
 * <p>
 * The proxy instance for a particular protocol is used for all framework
 * instances that may contain their own stream handler services. When
 * performing a stream handler operation, the proxy retrieves the handler
 * service from the framework instance associated with the current call
 * stack and delegates the call to the handler service.
 * </p>
 * <p>
 * The proxy will create simple stream handler service trackers for each
 * framework instance. The trackers will listen to service events in its
 * respective framework instance to maintain a reference to the "best"
 * stream handler service at any given time.
 * </p>
**/
public class URLHandlersStreamHandlerProxy extends URLStreamHandler
    implements URLStreamHandlerSetter, InvocationHandler
{
    private final Map m_trackerMap = new HashMap();
    private final String m_protocol;
    private final Object m_service;
    private final SecureAction m_action;

    public URLHandlersStreamHandlerProxy(String protocol, SecureAction action)
    {
        m_protocol = protocol;
        m_service = null;
        m_action = action;
    }
    
    private URLHandlersStreamHandlerProxy(Object service, SecureAction action)
    {
        m_protocol = null;
        m_service = service;
        m_action = action;
    }

    //
    // URLStreamHandler interface methods.
    //

    protected synchronized boolean equals(URL url1, URL url2)
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url1.getProtocol());
        }
        return svc.equals(url1, url2);
    }

    protected synchronized int getDefaultPort()
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException("Stream handler unavailable.");
        }
        return svc.getDefaultPort();
    }

    protected synchronized InetAddress getHostAddress(URL url)
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url.getProtocol());
        }
        return svc.getHostAddress(url);
    }

    protected synchronized int hashCode(URL url)
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url.getProtocol());
        }
        return svc.hashCode(url);
    }

    protected synchronized boolean hostsEqual(URL url1, URL url2)
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url1.getProtocol());
        }
        return svc.hostsEqual(url1, url2);
    }

    protected synchronized URLConnection openConnection(URL url) throws IOException
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new MalformedURLException("Unknown protocol: " + url.toString());
        }
        return svc.openConnection(url);
    }

    protected synchronized void parseURL(URL url, String spec, int start, int limit)
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url.getProtocol());
        }
        svc.parseURL(this, url, spec, start, limit);
    }

    protected synchronized boolean sameFile(URL url1, URL url2)
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url1.getProtocol());
        }
        return svc.sameFile(url1, url2);
    }

    public void setURL(
        URL url, String protocol, String host, int port, String authority,
        String userInfo, String path, String query, String ref)
    {
        super.setURL(url, protocol, host, port, authority, userInfo, path, query, ref);
    }

    public void setURL(
        URL url, String protocol, String host, int port, String file, String ref)
    {
        super.setURL(url, protocol, host, port, null, null, file, null, ref);
    }

    protected synchronized String toExternalForm(URL url)
    {
        URLStreamHandlerService svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url.getProtocol());
        }
        return svc.toExternalForm(url);
    }

    /**
     * <p>
     * Private method to retrieve the stream handler service from the
     * framework instance associated with the current call stack. A
     * simple service tracker is created and cached for the associated
     * framework instance when this method is called.
     * </p>
     * @return the stream handler service from the framework instance
     *         associated with the current call stack or <tt>null</tt>
     *         is no service is available.
    **/
    private URLStreamHandlerService getStreamHandlerService()
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
                // Create a filter for the protocol.
                String filter = 
                    "(&(objectClass="
                    + URLStreamHandlerService.class.getName()
                    + ")("
                    + URLConstants.URL_HANDLER_PROTOCOL
                    + "="
                    + m_protocol
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
            Object service = m_action.invoke(m_action.getMethod(
                tracker.getClass(), "getService", null), tracker, null);
            if (service == null)
            {
                return null;
            }
            if (service instanceof URLStreamHandlerService)
            {
                return (URLStreamHandlerService) service;
            }
            return (URLStreamHandlerService) Proxy.newProxyInstance(
                URLStreamHandlerService.class.getClassLoader(), 
                new Class[]{URLStreamHandlerService.class}, 
                new URLHandlersStreamHandlerProxy(service, m_action));
        }
        catch (Exception ex)
        {
            // TODO: log this or something
            ex.printStackTrace();
            return null;
        }
    }

    public Object invoke(Object obj, Method method, Object[] params)
        throws Throwable
    {
        try
        {
            Class[] types = method.getParameterTypes();
            if ("parseURL".equals(method.getName()))
            {
                types[0] = m_service.getClass().getClassLoader().loadClass(
                    URLStreamHandlerSetter.class.getName());
                params[0] = Proxy.newProxyInstance(
                    m_service.getClass().getClassLoader(), new Class[]{types[0]}, 
                    (URLHandlersStreamHandlerProxy) params[0]);
            }
            return m_action.invoke(m_action.getMethod(m_service.getClass(), 
                method.getName(), types), m_service, params);
        } 
        catch (Exception ex)
        {
            throw ex;
        }
    }
}