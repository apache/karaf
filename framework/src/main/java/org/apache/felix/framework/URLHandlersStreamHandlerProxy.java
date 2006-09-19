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
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.url.*;

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
    implements URLStreamHandlerSetter
{
    private Map m_trackerMap = new HashMap();
    private String m_protocol = null;

    public URLHandlersStreamHandlerProxy(String protocol)
    {
        m_protocol = protocol;
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
        Felix framework = URLHandlers.getFrameworkFromContext();

        // If the framework has disabled the URL Handlers service,
        // then it will not be found so just return null.
        if (framework == null)
        {
            return null;
        }

        // Get the service tracker for the framework instance or create one.
        URLHandlersServiceTracker tracker =
            (URLHandlersServiceTracker) m_trackerMap.get(framework);
        if (tracker == null)
        {
            // Get the framework's system bundle context.
            BundleContext context =
                ((SystemBundleActivator)
                    ((SystemBundle) framework.getBundle(0)).getActivator())
                        .getBundleContext();
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
            tracker = new URLHandlersServiceTracker(context, filter);
            // Cache the simple service tracker.
            m_trackerMap.put(framework, tracker);
        }
        return (URLStreamHandlerService) tracker.getService();
    }
}