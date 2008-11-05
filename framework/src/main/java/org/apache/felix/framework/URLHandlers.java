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
import java.util.*;

import org.apache.felix.framework.searchpolicy.ContentClassLoader;
import org.apache.felix.framework.util.*;

/**
 * <p>
 * This class is a singleton and implements the stream and content handler
 * factories for all framework instances executing within the JVM. Any
 * calls to retrieve stream or content handlers is routed through this class
 * and it acts as a multiplexer for all framework instances. To achieve this,
 * all framework instances register with this class when they are created so
 * that it can maintain a centralized registry of instances.
 * </p>
 * <p>
 * When this class receives a request for a stream or content handler, it
 * always returns a proxy handler instead of only returning a proxy if a
 * handler currently exists. This approach is used for three reasons:
 * </p>
 * <ol>
 *   <li>Potential caching behavior by the JVM of stream handlers does not give
 *       you a second chance to provide a handler.
 *   </li>
 *   <li>Due to the dynamic nature of OSGi services, handlers may appear at
 *       any time, so always creating a proxy makes sense.
 *   </li>
 *   <li>Since these handler factories service all framework instances,
 *       some instances may have handlers and others may not, so returning
 *       a proxy is the only answer that makes sense.
 *   </li>
 * </ol>
 * <p>
 * It is possible to disable the URL Handlers service by setting the
 * <tt>framework.service.urlhandlers</tt> configuration property to <tt>false</tt>.
 * When multiple framework instances are in use, if no framework instances enable
 * the URL Handlers service, then the singleton stream and content factories will
 * never be set (i.e., <tt>URL.setURLStreamHandlerFactory()</tt> and
 * <tt>URLConnection.setContentHandlerFactory()</tt>). However, if one instance
 * enables URL Handlers service, then the factory methods will be invoked. In
 * that case, framework instances that disable the URL Handlers service will
 * simply not provide that services to their contained bundles, while framework
 * instances with the service enabled will.
 * </p>
**/
class URLHandlers implements URLStreamHandlerFactory, ContentHandlerFactory
{
    private static final SecureAction m_secureAction = new SecureAction();

    private static volatile SecurityManagerEx m_sm = null;
    private static volatile URLHandlers m_handler = null;

    // This maps classloaders of URLHandlers in other classloaders to lists of 
    // their frameworks.
    private static Map m_classloaderToFrameworkLists = new HashMap();

    // The list to hold all enabled frameworks registered with this handlers 
    private static final List m_frameworks = new ArrayList();
    private static int m_counter = 0;

    private static Map m_contentHandlerCache = null;
    private static Map m_streamHandlerCache = null;
    private static URLStreamHandlerFactory m_streamHandlerFactory;
    private static ContentHandlerFactory m_contentHandlerFactory;

    /**
     * <p>
     * Only one instance of this class is created per classloader 
     * and that one instance is registered as the stream and content handler
     * factories for the JVM. Unless, we already register one from a different
     * classloader. In this case we attach to this root.
     * </p> 
    **/
    private URLHandlers()
    {
        synchronized (URL.class)
        {
            try
            {
                URL.setURLStreamHandlerFactory(this);
                m_streamHandlerFactory = this;
            }
            catch (Error err)
            {
                try
                {
                    // there already is a factory set so try to swap it with ours.
                    m_streamHandlerFactory = (URLStreamHandlerFactory)
                        m_secureAction.swapStaticFieldIfNotClass(URL.class, 
                        URLStreamHandlerFactory.class, URLHandlers.class, "streamHandlerLock");
                    
                    if (m_streamHandlerFactory == null)
                    {
                        throw err;
                    }
                    if (!m_streamHandlerFactory.getClass().getName().equals(URLHandlers.class.getName()))
                    {
                        URL.setURLStreamHandlerFactory(this);
                    }
                    else if (URLHandlers.class != m_streamHandlerFactory.getClass())
                    {
                        try
                        {
                            m_secureAction.invoke(
                                m_secureAction.getDeclaredMethod(m_streamHandlerFactory.getClass(), 
                                "registerFrameworkListsForContextSearch", 
                                new Class[]{ClassLoader.class, List.class}), 
                                m_streamHandlerFactory, new Object[]{ URLHandlers.class.getClassLoader(), 
                                    m_frameworks });
                        }
                        catch (Exception ex)
                        {
                            new RuntimeException(ex.getMessage());
                        }
                    }
                }
                catch (Exception e)
                {
                    throw err;
                }
            }
            
            try
            {
                URLConnection.setContentHandlerFactory(this);
                m_contentHandlerFactory = this;
            }
            catch (Error err)
            {
                // there already is a factory set so try to swap it with ours.
                try
                {   
                    m_contentHandlerFactory = (ContentHandlerFactory) 
                        m_secureAction.swapStaticFieldIfNotClass(
                            URLConnection.class, ContentHandlerFactory.class, 
                            URLHandlers.class, null);
                    if (m_contentHandlerFactory == null)
                    {
                        throw err;
                    }
                    if (!m_contentHandlerFactory.getClass().getName().equals(
                        URLHandlers.class.getName()))
                    {
                        URLConnection.setContentHandlerFactory(this);
                    }
                }
                catch (Exception ex)
                {
                    throw err;
                }
            }
        }
        // are we not the new root?
        if ((m_streamHandlerFactory == this) || !URLHandlers.class.getName().equals(
            m_streamHandlerFactory.getClass().getName()))
        {
            // we only need a security manager in the root
            m_sm = new SecurityManagerEx();
        }
    }

    static void registerFrameworkListsForContextSearch(ClassLoader index, 
        List frameworkLists)
    {
        synchronized (URL.class)
        {
            synchronized (m_classloaderToFrameworkLists)
            {
                m_classloaderToFrameworkLists.put(index, frameworkLists);
            }
        }
    }

    static void unregisterFrameworkListsForContextSearch(ClassLoader index)
    {
        synchronized (URL.class)
        {
            synchronized (m_classloaderToFrameworkLists)
            {
                m_classloaderToFrameworkLists.remove(index);
                if (m_classloaderToFrameworkLists.isEmpty() )
                {
                    synchronized (m_frameworks)
                    {
                        if (m_frameworks.isEmpty())
                        {
                            try
                            {
                                m_secureAction.swapStaticFieldIfNotClass(URL.class, 
                                    URLStreamHandlerFactory.class, null, "streamHandlerLock");
                            }
                            catch (Exception ex)
                            {
                                // TODO log this
                                ex.printStackTrace();
                            }
                            
                            if (m_streamHandlerFactory.getClass() != URLHandlers.class)
                            {
                                URL.setURLStreamHandlerFactory(m_streamHandlerFactory);
                            }
                            try
                            {
                                m_secureAction.swapStaticFieldIfNotClass(
                                    URLConnection.class, ContentHandlerFactory.class, 
                                    null, null);
                            }
                            catch (Exception ex)
                            {
                                // TODO log this
                                ex.printStackTrace();
                            }
                            
                            if (m_contentHandlerFactory.getClass() != URLHandlers.class)
                            {
                                URLConnection.setContentHandlerFactory(m_contentHandlerFactory);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * This is a method implementation for the <tt>URLStreamHandlerFactory</tt>
     * interface. It simply creates a stream handler proxy object for the
     * specified protocol. It caches the returned proxy; therefore, subsequent
     * requests for the same protocol will receive the same handler proxy.
     * </p>
     * @param protocol the protocol for which a stream handler should be returned.
     * @return a stream handler proxy for the specified protocol.
    **/
    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        // See if there is a cached stream handler.
        // IMPLEMENTATION NOTE: Caching is not strictly necessary for
        // stream handlers since the Java runtime caches them. Caching is
        // performed for code consistency between stream and content
        // handlers and also because caching behavior may not be guaranteed
        // across different JRE implementations.
        URLStreamHandler handler = getFromStreamCache(protocol);
        
        if (handler != null)
        {
            return handler;
        }
        // If this is the framework's "bundle:" protocol, then return
        // a handler for that immediately, since no one else can be
        // allowed to deal with it.
        if (protocol.equals(FelixConstants.BUNDLE_URL_PROTOCOL))
        {
            return addToStreamCache(protocol, 
                new URLHandlersBundleStreamHandler(m_secureAction));
        }
    
        // If this is the framework's "felix:" extension protocol, then
        // return the ExtensionManager.m_extensionManager handler for 
        // that immediately - this is a workaround for certain jvms that
        // do a toString() on the extension url we add to the global
        // URLClassloader.
        if (protocol.equals("felix"))
        {
            return addToStreamCache(protocol, new URLStreamHandler()
            {
                protected URLConnection openConnection(URL url)
                    throws IOException
                {
                    Object framework = getFrameworkFromContext();
                    
                    try
                    {
                        Object handler =  m_secureAction.getDeclaredField(
                            framework.getClass(),"m_extensionManager", framework);

                        return (URLConnection) m_secureAction.invoke(
                            m_secureAction.getMethod(handler.getClass(), 
                            "openConnection", new Class[]{URL.class}), handler, 
                            new Object[]{url});
                    }
                    catch (Exception ex)
                    {
                        throw new IOException(ex.getMessage());
                    }
                }
            });
        }

        // If built-in content handler, then create a proxy handler.
        return addToStreamCache(protocol, new URLHandlersStreamHandlerProxy(protocol, m_secureAction, 
            (m_streamHandlerFactory != this) ? m_streamHandlerFactory : null));
    }

    /**
     * <p>
     * This is a method implementation for the <tt>ContentHandlerFactory</tt>
     * interface. It simply creates a content handler proxy object for the
     * specified mime type. It caches the returned proxy; therefore, subsequent
     * requests for the same content type will receive the same handler proxy.
     * </p>
     * @param mimeType the mime type for which a content handler should be returned.
     * @return a content handler proxy for the specified mime type.
    **/
    public ContentHandler createContentHandler(String mimeType)
    {
        // See if there is a cached stream handler.
        // IMPLEMENTATION NOTE: Caching is not strictly necessary for
        // stream handlers since the Java runtime caches them. Caching is
        // performed for code consistency between stream and content
        // handlers and also because caching behavior may not be guaranteed
        // across different JRE implementations.
        ContentHandler handler = getFromContentCache(mimeType);
        
        if (handler != null)
        {
            return handler;
        }

        return addToContentCache(mimeType, 
            new URLHandlersContentHandlerProxy(mimeType, m_secureAction, 
            (m_contentHandlerFactory != this) ? m_contentHandlerFactory : null));
    }

    private synchronized ContentHandler addToContentCache(String mimeType, ContentHandler handler)
    {
        if (m_contentHandlerCache == null)
        {
            m_contentHandlerCache = new HashMap();
        }
        return (ContentHandler) addToCache(m_contentHandlerCache, mimeType, handler);
    }
    
    private synchronized ContentHandler getFromContentCache(String mimeType)
    {
        return (ContentHandler) ((m_contentHandlerCache != null) ? 
            m_contentHandlerCache.get(mimeType) : null);
    }

    private synchronized URLStreamHandler addToStreamCache(String protocol, URLStreamHandler handler)
    {
        if (m_streamHandlerCache == null)
        {
            m_streamHandlerCache = new HashMap();
        }
        return (URLStreamHandler) addToCache(m_streamHandlerCache, protocol, handler);
    }
    
    private synchronized URLStreamHandler getFromStreamCache(String protocol)
    {
        return (URLStreamHandler) ((m_streamHandlerCache != null) ? 
            m_streamHandlerCache.get(protocol) : null);
    }
    
    private Object addToCache(Map cache, String key, Object value)
    {
        if (value == null)
        {
            return null;
        }
        
        Object result = cache.get(key);
            
        if (result == null)
        {
            cache.put(key, value);
            result = value;
        }
        return result;
    }

    /**
     * <p>
     * Static method that adds a framework instance to the centralized
     * instance registry.
     * </p>
     * @param framework the framework instance to be added to the instance
     *        registry.
     * @param enable a flag indicating whether or not the framework wants to
     *        enable the URL Handlers service.
    **/
    public static void registerFrameworkInstance(Object framework, boolean enable)
    {
        synchronized (m_frameworks)
        {
            // If the URL Handlers service is not going to be enabled,
            // then return immediately.
            if (enable)
            {
                // We need to create an instance if this is the first
                // time this method is called, which will set the handler
                // factories.
                if (m_handler == null)
                {
                    m_handler = new URLHandlers();
                }
                m_frameworks.add(framework);
            }
            m_counter++;
        }
    }

    /**
     * <p>
     * Static method that removes a framework instance from the centralized
     * instance registry.
     * </p>
     * @param framework the framework instance to be removed from the instance
     *        registry.
    **/
    public static void unregisterFrameworkInstance(Object framework)
    {
        synchronized (m_frameworks)
        {
            m_counter--;
            if (m_frameworks.remove(framework) && m_frameworks.isEmpty())
            {
                if (m_handler.m_streamHandlerFactory.getClass().getName().equals(
                    URLHandlers.class.getName()))
                {
                    try
                    {
                        m_secureAction.invoke(m_secureAction.getDeclaredMethod(
                            m_handler.m_streamHandlerFactory.getClass(), 
                            "unregisterFrameworkListsForContextSearch", 
                            new Class[]{ ClassLoader.class}), 
                            m_handler.m_streamHandlerFactory, 
                            new Object[] {URLHandlers.class.getClassLoader()});
                    }
                    catch (Exception e)
                    {
                        // TODO this should not happen
                        e.printStackTrace();
                    }
                }
                m_handler = null;
            }
        }
    }

    /**
     * <p>
     * This method returns the system bundle context for the caller.
     * It determines the appropriate system bundle by retrieving the
     * class call stack and find the first class that is loaded from
     * a bundle. It then checks to see which of the registered framework
     * instances owns the class and returns its system bundle context.
     * </p>
     * @return the system bundle context associated with the caller or
     *         <tt>null</tt> if no associated framework was found.
    **/
    public static Object getFrameworkFromContext()
    {
        // This is a hack. The idea is to return the only registered framework
        synchronized (m_classloaderToFrameworkLists)
        {
            if (m_classloaderToFrameworkLists.isEmpty())
            {
                synchronized (m_frameworks)
                {
                    if ((m_counter == 1) && (m_frameworks.size() == 1))
                    {
                        return m_frameworks.get(0);
                    }
                }
            }
        }
        // get the current class call stack.
        Class[] stack = m_sm.getClassContext();
        // Find the first class that is loaded from a bundle.
        Class targetClass = null;
        for (int i = 0; i < stack.length; i++)
        {
            if ((stack[i].getClassLoader() != null) && 
                ContentClassLoader.class.getName().equals(
                stack[i].getClassLoader().getClass().getName()))
            {
                targetClass = stack[i];
                break;
            }
        }
        
        // If we found a class loaded from a bundle, then iterate
        // over the framework instances and see which framework owns
        // the bundle that loaded the class.
        if (targetClass != null)
        {
            synchronized (m_classloaderToFrameworkLists)
            {
                ClassLoader index = targetClass.getClassLoader().getClass().getClassLoader();
                
                List frameworks = (List) m_classloaderToFrameworkLists.get(
                    index);
                
                if ((frameworks == null) && (index == URLHandlers.class.getClassLoader())) 
                {
                    frameworks = m_frameworks;
                }
                if (frameworks != null)
                {
                    synchronized (frameworks)
                    {
                        // Check the registry of framework instances
                        for (int i = 0; i < frameworks.size(); i++)
                        {
                            Object framework = frameworks.get(i);
                            try
                            {
                                if (m_secureAction.invoke(
                                    m_secureAction.getDeclaredMethod(framework.getClass(), 
                                    "getBundle", new Class[]{Class.class}),
                                    framework, new Object[]{targetClass}) != null)
                                {
                                    return framework;
                                }
                            }
                            catch (Exception ex)
                            {
                                // This should not happen but if it does there is 
                                // not much we can do other then ignore it.
                                // Maybe log this or something.
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}