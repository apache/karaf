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

import java.net.*;
import java.util.*;

import org.apache.felix.framework.searchpolicy.ContentClassLoader;
import org.apache.felix.framework.util.*;
import org.osgi.framework.BundleContext;

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
    private static final String STREAM_HANDLER_PACKAGE_PROP = "java.protocol.handler.pkgs";
    private static final String CONTENT_HANDLER_PACKAGE_PROP = "java.content.handler.pkgs";
    private static final String DEFAULT_STREAM_HANDLER_PACKAGE = "sun.net.www.protocol";
    
    private static final String DEFAULT_CONTENT_HANDLER_PACKAGE = "sun.net.www.content";
    private static String m_lock = new String("string-lock");
    private static SecurityManagerEx m_sm = null;
    private static URLHandlers m_handler = null;
    private static int m_frameworkCount = 0;
    private static List m_frameworkList = null;
    private static Map m_streamHandlerCache = null;
    private static Map m_contentHandlerCache = null;

    private final static SecureAction m_secureAction = new SecureAction();

    /**
     * <p>
     * Only one instance of this class is created in a static initializer
     * and that one instance is registered as the stream and content handler
     * factories for the JVM.
     * </p> 
    **/
    private URLHandlers()
    {
        // No one can create an instance, but we need an instance
        // so we can set this as the stream and content handler factory.
        URL.setURLStreamHandlerFactory(this);
        URLConnection.setContentHandlerFactory(this);
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
        synchronized (this)
        {
            // See if there is a cached stream handler.
            // IMPLEMENTATION NOTE: Caching is not strictly necessary for
            // stream handlers since the Java runtime caches them. Caching is
            // performed for code consistency between stream and content
            // handlers and also because caching behavior may not be guaranteed
            // across different JRE implementations.
            URLStreamHandler handler = (m_streamHandlerCache == null)
                ? null
                : (URLStreamHandler) m_streamHandlerCache.get(protocol);

            // If this is the framework's "bundle:" protocol, then return
            // a handler for that immediately, since no one else can be
            // allowed to deal with it.
            if (protocol.equals(FelixConstants.BUNDLE_URL_PROTOCOL))
            {
                handler = new URLHandlersBundleStreamHandler(null);
                if (m_streamHandlerCache == null)
                {
                    m_streamHandlerCache = new HashMap();
                }
                m_streamHandlerCache.put(protocol, handler);
                return handler;
            }

            // If this is the framework's "felix:" extension protocol, then
            // return the ExtensionManager.m_extensionManager handler for 
            // that immediately - this is a workaround for certain jvms that
            // do a toString() on the extension url we add to the global
            // URLClassloader.
            if (protocol.equals("felix"))
            {
                handler = ExtensionManager.m_extensionManager;
                if (m_streamHandlerCache == null)
                {
                    m_streamHandlerCache = new HashMap();
                }
                m_streamHandlerCache.put(protocol, handler);
                return handler;
            }

            // If there is not cached handler, then search for built-in
            // handler or create a new handler proxy.
            if (handler == null)
            {
                // Check for built-in handlers for the protocol.
                String pkgs = m_secureAction.getSystemProperty(STREAM_HANDLER_PACKAGE_PROP, "");
                pkgs = (pkgs.equals(""))
                    ? DEFAULT_STREAM_HANDLER_PACKAGE
                    : pkgs + "|" + DEFAULT_STREAM_HANDLER_PACKAGE;
    
                // Iterate over built-in packages.
                StringTokenizer pkgTok = new StringTokenizer(pkgs, "| ");
                while (pkgTok.hasMoreTokens())
                {
                    String pkg = pkgTok.nextToken().trim();
                    String className = pkg + "." + protocol + ".Handler";
                    try
                    {
                        // If a built-in handler is found then let the
                        // JRE handle it.
                        if (m_secureAction.forName(className) != null)
                        {
                            return null;
                        }
                    }
                    catch (Exception ex)
                    {
                        // This could be a class not found exception or an
                        // instantiation exception, not much we can do in either
                        // case other than ignore it.
                    }
                }
    
                // If no cached or built-in content handler, then create a
                // proxy handler and cache it.
                handler = new URLHandlersStreamHandlerProxy(protocol);
                if (m_streamHandlerCache == null)
                {
                    m_streamHandlerCache = new HashMap();
                }
                m_streamHandlerCache.put(protocol, handler);
            }

            return handler;
        }
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
        synchronized (this)
        {
            // See if there is a cached content handler.
            ContentHandler handler = (m_contentHandlerCache == null)
                ? null
                : (ContentHandler) m_contentHandlerCache.get(mimeType);

            // If there is not cached handler, then search for built-in
            // handler or create a new handler proxy.
            if (handler == null)
            {
                // Check for built-in handlers for the mime type.
                String pkgs = m_secureAction.getSystemProperty(CONTENT_HANDLER_PACKAGE_PROP, "");
                pkgs = (pkgs.equals(""))
                    ? DEFAULT_CONTENT_HANDLER_PACKAGE
                    : pkgs + "|" + DEFAULT_CONTENT_HANDLER_PACKAGE;
    
                // Remove periods, slashes, and dashes from mime type.
                String fixedType = mimeType.replace('.', '_').replace('/', '.').replace('-', '_');
    
                // Iterate over built-in packages.
                StringTokenizer pkgTok = new StringTokenizer(pkgs, "| ");
                while (pkgTok.hasMoreTokens())
                {
                    String pkg = pkgTok.nextToken().trim();
                    String className = pkg + "." + fixedType;
                    try
                    {
                        // If a built-in handler is found then let the
                        // JRE handle it.
                        if (m_secureAction.forName(className) != null)
                        {
                            return null;
                        }
                    }
                    catch (Exception ex)
                    {
                        // This could be a class not found exception or an
                        // instantiation exception, not much we can do in either
                        // case other than ignore it.
                    }
                }

                // If no cached or built-in content handler, then create a
                // proxy handler and cache it.
                handler = new URLHandlersContentHandlerProxy(mimeType);
                if (m_contentHandlerCache == null)
                {
                    m_contentHandlerCache = new HashMap();
                }
                m_contentHandlerCache.put(mimeType, handler);
            }

            return handler;
        }
    }

    /**
     * <p>
     * Static method that adds a framework instance to the centralized
     * instance registry.
     * </p>
     * @param framework the framework instance to be added to the instance
     *        registry.
     * @param context the system bundle context associated with the framework
     *        instance.
     * @param enable a flag indicating whether or not the framework wants to
     *        enable the URL Handlers service.
    **/
    public static void registerInstance(
        Felix framework, BundleContext context, boolean enable)
    {
        synchronized (m_lock)
        {
            // Increment framework instance count.
            m_frameworkCount++;

            // If the URL Handlers service is not going to be enabled,
            // then return immediately.
            if (enable)
            {
                // We need to create an instance if this is the first
                // time this method is called, which will set the handler
                // factories.
                if (m_handler == null)
                {
                    m_sm = new SecurityManagerEx();
                    m_handler = new URLHandlers();
                }
    
                // Create the framework list, if necessary, and add the
                // new framework instance to it.
                if (m_frameworkList == null)
                {
                    m_frameworkList = new ArrayList();
                }
                m_frameworkList.add(framework);
            }
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
    public static void unregisterInstance(Felix framework)
    {
        synchronized (m_lock)
        {
            m_frameworkCount--;
            if (m_frameworkList != null)
            {
                m_frameworkList.remove(framework);
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
    public static Felix getFrameworkFromContext()
    {
        synchronized (m_lock)
        {
            if (m_frameworkList != null)
            {
                // First, perform a simple short cut, if there is only
                // one framework instance registered, assume that this
                // is the bundle context to be returned and just return
                // it immediately.
                if ((m_frameworkList.size() == 1) && (m_frameworkCount == 1))
                {
                    return (Felix) m_frameworkList.get(0);
                }
    
                // If there is more than one registered framework instance,
                // then get the current class call stack.
                Class[] stack = m_sm.getClassContext();
                // Find the first class that is loaded from a bundle.
                Class targetClass = null;
                for (int i = 0; i < stack.length; i++)
                {
                    if (stack[i].getClassLoader() instanceof ContentClassLoader)
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
                    // Check the registry of framework instances
                    for (int i = 0; i < m_frameworkList.size(); i++)
                    {
                        if (((Felix) m_frameworkList.get(i)).getBundle(targetClass) != null)
                        {
                            return (Felix) m_frameworkList.get(i);
                        }
                    }
                }
            }
            return null;
        }
    }
}