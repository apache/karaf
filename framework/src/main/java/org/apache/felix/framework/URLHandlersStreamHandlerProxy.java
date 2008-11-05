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
import java.util.StringTokenizer;

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
public final class URLHandlersStreamHandlerProxy extends URLStreamHandler
    implements URLStreamHandlerSetter, InvocationHandler
{
    private static final String STREAM_HANDLER_PACKAGE_PROP = "java.protocol.handler.pkgs";
    private static final String DEFAULT_STREAM_HANDLER_PACKAGE = "sun.net.www.protocol|com.ibm.oti.net.www.protocol|gnu.java.net.protocol|wonka.net|com.acunia.wonka.net|org.apache.harmony.luni.internal.net.www.protocol|weblogic.utils|weblogic.net|javax.net.ssl|COM.newmonics.www.protocols";
    
    private static final Method EQUALS;
    private static final Method GET_DEFAULT_PORT;
    private static final Method GET_HOST_ADDRESS;
    private static final Method HASH_CODE;
    private static final Method HOSTS_EQUAL;
    private static final Method OPEN_CONNECTION;
    private static final Method PARSE_URL;
    private static final Method SAME_FILE;
    private static final Method TO_EXTERNAL_FORM;
    
    static {
        try
        {
            EQUALS = URLStreamHandler.class.getDeclaredMethod("equals", 
                new Class[]{URL.class, URL.class});
            GET_DEFAULT_PORT = URLStreamHandler.class.getDeclaredMethod("getDefaultPort", null);
            GET_HOST_ADDRESS = URLStreamHandler.class.getDeclaredMethod(
                    "getHostAddress", new Class[]{URL.class});
            HASH_CODE = URLStreamHandler.class.getDeclaredMethod( 
                    "hashCode", new Class[]{URL.class});
            HOSTS_EQUAL = URLStreamHandler.class.getDeclaredMethod(
                    "hostsEqual", new Class[]{URL.class, URL.class});
            OPEN_CONNECTION = URLStreamHandler.class.getDeclaredMethod(
                    "openConnection", new Class[]{URL.class});
            PARSE_URL = URLStreamHandler.class.getDeclaredMethod( 
                    "parseURL", new Class[]{URL.class, String.class, Integer.TYPE, Integer.TYPE});
            SAME_FILE = URLStreamHandler.class.getDeclaredMethod(
                    "sameFile", new Class[]{URL.class, URL.class});
            TO_EXTERNAL_FORM = URLStreamHandler.class.getDeclaredMethod( 
                   "toExternalForm", new Class[]{URL.class});
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }

    private static final Map m_builtIn = new HashMap();
    private final URLStreamHandlerFactory m_factory;
    
    private final Map m_trackerMap = new HashMap();
    private final String m_protocol;
    private final Object m_service;
    private final SecureAction m_action;

    public URLHandlersStreamHandlerProxy(String protocol, SecureAction action, 
        URLStreamHandlerFactory factory)
    {
        m_protocol = protocol;
        m_service = null;
        m_action = action;
        m_factory = factory;
    }
    
    private URLHandlersStreamHandlerProxy(Object service, SecureAction action)
    {
        m_protocol = null;
        m_service = service;
        m_action = action;
        m_factory = null;
    }

    //
    // URLStreamHandler interface methods.
    //
    protected boolean equals(URL url1, URL url2)
    {
        Object svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url1.getProtocol());
        }
        if (svc instanceof URLStreamHandlerService)
        {
            return ((URLStreamHandlerService) svc).equals(url1, url2);
        }
        try 
        {
            return ((Boolean) m_action.invoke(EQUALS, svc, new Object[]{url1, url2})).booleanValue();
        } 
        catch (Exception ex)  
        {
            ex.printStackTrace();
            throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
        }
    }

    protected int getDefaultPort()
    {
        Object svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException("Stream handler unavailable.");
        }
        if (svc instanceof URLStreamHandlerService)
        {
            return ((URLStreamHandlerService) svc).getDefaultPort();
        }
        try 
        {
            return ((Integer) m_action.invoke(GET_DEFAULT_PORT, svc, null)).intValue();
        } 
        catch (Exception ex)  
        {
            ex.printStackTrace();
            throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
        }
    }

    protected InetAddress getHostAddress(URL url)
    {
        Object svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url.getProtocol());
        }
        if (svc instanceof URLStreamHandlerService)
        {
            return ((URLStreamHandlerService) svc).getHostAddress(url);
        }
        try 
        {
            return (InetAddress) m_action.invoke(GET_HOST_ADDRESS, svc, new Object[]{url});
        } 
        catch (Exception ex)  
        {
            ex.printStackTrace();
            throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
        }
    }

    protected int hashCode(URL url)
    {
        Object svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url.getProtocol());
        }
        if (svc instanceof URLStreamHandlerService)
        {
            return ((URLStreamHandlerService) svc).hashCode(url);
        }
        try 
        {
            return ((Integer) m_action.invoke(HASH_CODE, svc, new Object[]{url})).intValue();
        } 
        catch (Exception ex)  
        {
            ex.printStackTrace();
            throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
        }
    }

    protected boolean hostsEqual(URL url1, URL url2)
    {
        Object svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url1.getProtocol());
        }
        if (svc instanceof URLStreamHandlerService)
        {
            return ((URLStreamHandlerService) svc).hostsEqual(url1, url2);
        }
        try 
        {
            return ((Boolean) m_action.invoke(HOSTS_EQUAL, svc, new Object[]{url1, url2})).booleanValue();
        } 
        catch (Exception ex)  
        {
            ex.printStackTrace();
            throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
        }
    }

    protected URLConnection openConnection(URL url) throws IOException
    {
        Object svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new MalformedURLException("Unknown protocol: " + url.toString());
        }
        if (svc instanceof URLStreamHandlerService)
        {
            return ((URLStreamHandlerService) svc).openConnection(url);
        }
        try 
        {
            return (URLConnection) m_action.invoke(OPEN_CONNECTION, svc, new Object[]{url});
        } 
        catch (Exception ex)  
        {
            ex.printStackTrace();
            throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
        }
    }

    protected void parseURL(URL url, String spec, int start, int limit)
    {
        Object svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url.getProtocol());
        }
        if (svc instanceof URLStreamHandlerService)
        {
            ((URLStreamHandlerService) svc).parseURL(this, url, spec, start, limit);
        }
        else
        {
            try 
            {
                URL test = new URL(url, spec, (URLStreamHandler) svc);
                
                super.setURL(url, test.getProtocol(), test.getHost(), test.getPort(),test.getAuthority(), 
                        test.getUserInfo(), test.getPath(), test.getQuery(), test.getRef());
            } 
            catch (Exception ex)  
            {
                ex.printStackTrace();
                throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
            }
        }
    }

    protected boolean sameFile(URL url1, URL url2)
    {
        Object svc = getStreamHandlerService();
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url1.getProtocol());
        }
        if (svc instanceof URLStreamHandlerService)
        {
            return ((URLStreamHandlerService) svc).sameFile(url1, url2);
        }
        try 
        {
            return ((Boolean) m_action.invoke(SAME_FILE, 
                svc, new Object[]{url1, url2})).booleanValue();
        } 
        catch (Exception ex)  
        {
            ex.printStackTrace();
            throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
        }
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

    protected String toExternalForm(URL url)
    {
        return toExternalForm(url, getStreamHandlerService());
    }
    
    private String toExternalForm(URL url, Object svc)
    {
        if (svc == null)
        {
            throw new IllegalStateException(
                "Unknown protocol: " + url.getProtocol());
        }
        if (svc instanceof URLStreamHandlerService)
        {
            return ((URLStreamHandlerService) svc).toExternalForm(url);
        }
        try 
        {
            return (String) m_action.invoke(TO_EXTERNAL_FORM, 
                svc, new Object[]{url});
        } 
        catch (Exception ex)  
        {
            ex.printStackTrace();
            throw new IllegalStateException("Stream handler unavailable due to: " + ex.getMessage());
        }
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
    private Object getStreamHandlerService()
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
            Object service;
            if (tracker instanceof URLHandlersServiceTracker)
            {
                service = ((URLHandlersServiceTracker) tracker).getService();
            }
            else
            {
                service = m_action.invoke(m_action.getMethod(
                    tracker.getClass(), "getService", null), tracker, null);
            }
            if (service == null) 
            {
                return getBuiltIn();
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
    
    private URLStreamHandler getBuiltIn()
    {
        synchronized (m_builtIn)
        {
            if (m_builtIn.containsKey(m_protocol))
            {
                return (URLStreamHandler) m_builtIn.get(m_protocol);
            }
        }
        if (m_factory != null)
        {
            URLStreamHandler result = m_factory.createURLStreamHandler(m_protocol);
            if (result != null)
            {
                return addToCache(m_protocol, result);
            }
        }
        // Check for built-in handlers for the mime type.
        String pkgs = m_action.getSystemProperty(STREAM_HANDLER_PACKAGE_PROP, "");
        pkgs = (pkgs.equals(""))
            ? DEFAULT_STREAM_HANDLER_PACKAGE
            : pkgs + "|" + DEFAULT_STREAM_HANDLER_PACKAGE;

        // Iterate over built-in packages.
        StringTokenizer pkgTok = new StringTokenizer(pkgs, "| ");
        while (pkgTok.hasMoreTokens())
        {
            String pkg = pkgTok.nextToken().trim();
            String className = pkg + "." + m_protocol + ".Handler";
            try
            {
                // If a built-in handler is found then cache and return it
                Class handler = m_action.forName(className); 
                if (handler != null)
                {
                    return addToCache(m_protocol, 
                        (URLStreamHandler) handler.newInstance());
                }
            }
            catch (Exception ex)
            {
                // This could be a class not found exception or an
                // instantiation exception, not much we can do in either
                // case other than ignore it.
            }
        }
        return addToCache(m_protocol, null);
    }

    private synchronized URLStreamHandler addToCache(String protocol, URLStreamHandler result)
    {
        if (!m_builtIn.containsKey(protocol))
        {
            m_builtIn.put(protocol, result);
            return result;
        }
        return (URLStreamHandler) m_builtIn.get(protocol);
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
