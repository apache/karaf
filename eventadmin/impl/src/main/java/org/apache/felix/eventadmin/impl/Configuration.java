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
package org.apache.felix.eventadmin.impl;


import java.util.*;

import org.apache.felix.eventadmin.impl.adapter.*;
import org.apache.felix.eventadmin.impl.dispatch.DefaultThreadPool;
import org.apache.felix.eventadmin.impl.dispatch.ThreadPool;
import org.apache.felix.eventadmin.impl.handler.*;
import org.apache.felix.eventadmin.impl.security.*;
import org.apache.felix.eventadmin.impl.tasks.*;
import org.apache.felix.eventadmin.impl.util.LeastRecentlyUsedCacheMap;
import org.apache.felix.eventadmin.impl.util.LogWrapper;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.TopicPermission;
import org.osgi.service.metatype.MetaTypeProvider;


/**
 * The <code>Configuration</code> class encapsules the
 * configuration for the event admin.
 *
 * The service knows about the following properties which are read at bundle startup:
 * <p>
 * <p>
 *      <tt>org.apache.felix.eventadmin.CacheSize</tt> - The size of various internal
 *          caches.
 * </p>
 * The default value is 30. Increase in case of a large number (more then 100) of
 * <tt>EventHandler</tt> services. A value less then 10 triggers the default value.
 * </p>
 * <p>
 * <p>
 *      <tt>org.apache.felix.eventadmin.ThreadPoolSize</tt> - The size of the thread
 *          pool.
 * </p>
 * The default value is 10. Increase in case of a large amount of synchronous events
 * where the <tt>EventHandler</tt> services in turn send new synchronous events in
 * the event dispatching thread or a lot of timeouts are to be expected. A value of
 * less then 2 triggers the default value. A value of 2 effectively disables thread
 * pooling.
 * </p>
 * <p>
 * <p>
 *      <tt>org.apache.felix.eventadmin.Timeout</tt> - The black-listing timeout in
 *          milliseconds
 * </p>
 * The default value is 5000. Increase or decrease at own discretion. A value of less
 * then 100 turns timeouts off. Any other value is the time in milliseconds granted
 * to each <tt>EventHandler</tt> before it gets blacklisted.
 * </p>
 * <p>
 * <p>
 *      <tt>org.apache.felix.eventadmin.RequireTopic</tt> - Are <tt>EventHandler</tt>
 *          required to be registered with a topic?
 * </p>
 * The default is <tt>true</tt>. The specification says that <tt>EventHandler</tt>
 * must register with a list of topics they are interested in. Setting this value to
 * <tt>false</tt> will enable that handlers without a topic are receiving all events
 * (i.e., they are treated the same as with a topic=*).
 * </p>
 * <p>
 * <p>
 *      <tt>org.apache.felix.eventadmin.IgnoreTimeout</tt> - Configure
 *         <tt>EventHandler</tt>s to be called without a timeout.
 * </p>
 * If a timeout is configured by default all event handlers are called using the timeout.
 * For performance optimization it is possible to configure event handlers where the
 * timeout handling is not used - this reduces the thread usage from the thread pools
 * as the timout handling requires an additional thread to call the event handler.
 * However, the application should work without this configuration property. It is a
 * pure optimization!
 * The value is a list of string (separated by comma). If the string ends with a dot,
 * all handlers in exactly this package are ignored. If the string ends with a star,
 * all handlers in this package and all subpackages are ignored. If the string neither
 * ends with a dot nor with a start, this is assumed to define an exact class name.
 *
 * These properties are read at startup and serve as a default configuration.
 * If a configuration admin is configured, the event admin can be configured
 * through the config admin.
 */
public class Configuration
{
    /** The PID for the event admin. */
    static final String PID = "org.apache.felix.eventadmin.impl.EventAdmin";

    static final String PROP_CACHE_SIZE = "org.apache.felix.eventadmin.CacheSize";
    static final String PROP_THREAD_POOL_SIZE = "org.apache.felix.eventadmin.ThreadPoolSize";
    static final String PROP_TIMEOUT = "org.apache.felix.eventadmin.Timeout";
    static final String PROP_REQUIRE_TOPIC = "org.apache.felix.eventadmin.RequireTopic";
    static final String PROP_IGNORE_TIMEOUT = "org.apache.felix.eventadmin.IgnoreTimeout";

    /** The bundle context. */
    private final BundleContext m_bundleContext;

    private int m_cacheSize;

    private int m_threadPoolSize;

    private int m_timeout;

    private boolean m_requireTopic;

    private String[] m_ignoreTimeout;

    // The thread pool used - this is a member because we need to close it on stop
    private volatile ThreadPool m_sync_pool;

    private volatile ThreadPool m_async_pool;

    // The actual implementation of the service - this is a member because we need to
    // close it on stop. Note, security is not part of this implementation but is
    // added via a decorator in the start method (this is the wrapped object without
    // the wrapper).
    private volatile EventAdminImpl m_admin;

    // The registration of the security decorator factory (i.e., the service)
    private volatile ServiceRegistration m_registration;

    // all adapters
    private AbstractAdapter[] m_adapters;

    private ServiceRegistration m_managedServiceReg;

    public Configuration( BundleContext bundleContext )
    {
        m_bundleContext = bundleContext;

        // default configuration
        configure( null );
        start();

        // check for Configuration Admin configuration
        try
        {
            Object service = new ManagedService()
            {
                public synchronized void updated( Dictionary properties ) throws ConfigurationException
                {
                    configure( properties );
                    stop();
                    start();
                }
            };
            // add meta type provider if interfaces are available
            Object enhancedService = tryToCreateMetaTypeProvider(service);
            final String[] interfaceNames;
            if ( enhancedService == null )
            {
                interfaceNames = new String[] {ManagedService.class.getName()};
            }
            else
            {
                interfaceNames = new String[] {ManagedService.class.getName(), MetaTypeProvider.class.getName()};
                service = enhancedService;
            }
            Dictionary props = new Hashtable();
            props.put( Constants.SERVICE_PID, PID );
            m_managedServiceReg = m_bundleContext.registerService( interfaceNames, service, props );
        }
        catch ( Throwable t )
        {
            // don't care
        }
    }

    /**
     * Configures this instance.
     */
    void configure( Dictionary config )
    {
        if ( config == null )
        {
            // The size of various internal caches. At the moment there are 4
            // internal caches affected. Each will cache the determined amount of
            // small but frequently used objects (i.e., in case of the default value
            // we end-up with a total of 120 small objects being cached). A value of less
            // then 10 triggers the default value.
            m_cacheSize = getIntProperty(PROP_CACHE_SIZE,
                m_bundleContext.getProperty(PROP_CACHE_SIZE), 30, 10);

            // The size of the internal thread pool. Note that we must execute
            // each synchronous event dispatch that happens in the synchronous event
            // dispatching thread in a new thread, hence a small thread pool is o.k.
            // A value of less then 2 triggers the default value. A value of 2
            // effectively disables thread pooling. Furthermore, this will be used by
            // a lazy thread pool (i.e., new threads are created when needed). Ones the
            // the size is reached and no cached thread is available new threads will
            // be created.
            m_threadPoolSize = getIntProperty(
                PROP_THREAD_POOL_SIZE, m_bundleContext.getProperty(PROP_THREAD_POOL_SIZE), 20, 2);

            // The timeout in milliseconds - A value of less then 100 turns timeouts off.
            // Any other value is the time in milliseconds granted to each EventHandler
            // before it gets blacklisted.
            m_timeout = getIntProperty(PROP_TIMEOUT,
                    m_bundleContext.getProperty(PROP_TIMEOUT), 5000, Integer.MIN_VALUE);

            // Are EventHandler required to be registered with a topic? - The default is
            // true. The specification says that EventHandler must register with a list
            // of topics they are interested in. Setting this value to false will enable
            // that handlers without a topic are receiving all events
            // (i.e., they are treated the same as with a topic=*).
            m_requireTopic = getBooleanProperty(
                m_bundleContext.getProperty(PROP_REQUIRE_TOPIC), true);
            final String value = m_bundleContext.getProperty(PROP_IGNORE_TIMEOUT);
            if ( value == null )
            {
                m_ignoreTimeout = null;
            }
            else
            {
                final StringTokenizer st = new StringTokenizer(value, ",");
                m_ignoreTimeout = new String[st.countTokens()];
                for(int i=0; i<m_ignoreTimeout.length; i++) {
                    m_ignoreTimeout[i] = st.nextToken();
                }
            }
        }
        else
        {
            m_cacheSize = getIntProperty(PROP_CACHE_SIZE, config.get(PROP_CACHE_SIZE), 30, 10);
            m_threadPoolSize = getIntProperty(PROP_THREAD_POOL_SIZE, config.get(PROP_THREAD_POOL_SIZE), 20, 2);
            m_timeout = getIntProperty(PROP_TIMEOUT, config.get(PROP_TIMEOUT), 5000, Integer.MIN_VALUE);
            m_requireTopic = getBooleanProperty(config.get(PROP_REQUIRE_TOPIC), true);
            m_ignoreTimeout = null;
            final Object value = config.get(PROP_IGNORE_TIMEOUT);
            if ( value instanceof String )
            {
                m_ignoreTimeout = new String[] {(String)value};
            }
            else if ( value instanceof String[] )
            {
                m_ignoreTimeout = (String[])value;
            }
            else
            {
                LogWrapper.getLogger().log(LogWrapper.LOG_WARNING,
                        "Value for property: " + PROP_IGNORE_TIMEOUT + " is neither a string nor a string array - Using default");
            }
        }
    }

    public synchronized void start()
    {
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
                PROP_CACHE_SIZE + "=" + m_cacheSize);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
            PROP_THREAD_POOL_SIZE + "=" + m_threadPoolSize);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
            PROP_TIMEOUT + "=" + m_timeout);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
            PROP_REQUIRE_TOPIC + "=" + m_requireTopic);

        final TopicPermissions publishPermissions = new CacheTopicPermissions(
            new LeastRecentlyUsedCacheMap(m_cacheSize), TopicPermission.PUBLISH);

        final TopicPermissions subscribePermissions = new CacheTopicPermissions(
            new LeastRecentlyUsedCacheMap(m_cacheSize), TopicPermission.SUBSCRIBE);

        final TopicHandlerFilters topicHandlerFilters =
            new CacheTopicHandlerFilters(new LeastRecentlyUsedCacheMap(m_cacheSize),
            m_requireTopic);

        final Filters filters = new CacheFilters(
            new LeastRecentlyUsedCacheMap(m_cacheSize), m_bundleContext);

        // The handlerTasks object is responsible to determine concerned EventHandler
        // for a given event. Additionally, it keeps a list of blacklisted handlers.
        // Note that blacklisting is deactivated by selecting a different scheduler
        // below (and not in this HandlerTasks object!)
        final HandlerTasks handlerTasks = new BlacklistingHandlerTasks(m_bundleContext,
            new CleanBlackList(), topicHandlerFilters, filters,
            subscribePermissions);

        // Note that this uses a lazy thread pool that will create new threads on
        // demand - in case none of its cached threads is free - until threadPoolSize
        // is reached. Subsequently, a threadPoolSize of 2 effectively disables
        // caching of threads.
        m_sync_pool = new DefaultThreadPool(m_threadPoolSize, true);
        m_async_pool = new DefaultThreadPool(m_threadPoolSize > 5 ? m_threadPoolSize / 2 : 2, false);

        final DeliverTask syncExecuter = new SyncDeliverTasks(m_sync_pool,
                (m_timeout > 100 ? m_timeout : 0),
                m_ignoreTimeout);
        m_admin = createEventAdmin(m_bundleContext,
                handlerTasks,
                new AsyncDeliverTasks(m_async_pool, syncExecuter),
                syncExecuter);

        // register the admin wrapped in a service factory (SecureEventAdminFactory)
        // that hands-out the m_admin object wrapped in a decorator that checks
        // appropriated permissions of each calling bundle
        m_registration = m_bundleContext.registerService(EventAdmin.class.getName(),
            new SecureEventAdminFactory(m_admin, publishPermissions), null);

        // Finally, adapt the outside events to our kind of events as per spec
        adaptEvents(m_bundleContext, m_admin);
    }

    /**
     * Called to stop the event admin and restart it.
     */
    public synchronized void stop()
    {
        // We need to unregister manually
        if ( m_registration != null )
        {
            m_registration.unregister();
            m_registration = null;
        }
        if ( m_admin != null )
        {
            m_admin.stop();
            m_admin = null;
        }
        if ( m_async_pool != null )
        {
            m_async_pool.close();
            m_async_pool = null;
        }
        if ( m_sync_pool != null )
        {
            m_sync_pool.close();
            m_sync_pool = null;
        }
    }

    /**
     * Called upon stopping the bundle. This will block until all pending events are
     * delivered. An IllegalStateException will be thrown on new events starting with
     * the begin of this method. However, it might take some time until we settle
     * down which is somewhat cumbersome given that the spec asks for return in
     * a timely manner.
     */
    public synchronized void destroy()
    {
        if ( m_adapters != null )
        {
            for(int i=0;i<m_adapters.length;i++)
            {
                m_adapters[i].destroy(m_bundleContext);
            }
            m_adapters = null;
        }
        if ( m_managedServiceReg != null )
        {
            m_managedServiceReg.unregister();
            m_managedServiceReg = null;
        }
    }

    /**
     * Create a event admin implementation.
     * @param context      The bundle context
     * @param handlerTasks
     * @param asyncExecuters
     * @param syncExecuters
     * @return
     */
    protected EventAdminImpl createEventAdmin(BundleContext context,
                                              HandlerTasks handlerTasks,
                                              DeliverTask asyncExecuters,
                                              DeliverTask syncExecuters)
    {
        return new EventAdminImpl(handlerTasks, asyncExecuters, syncExecuters);
    }

    /**
     * Init the adapters in org.apache.felix.eventadmin.impl.adapter
     */
    private void adaptEvents(final BundleContext context, final EventAdmin admin)
    {
        if ( m_adapters == null )
        {
            m_adapters = new AbstractAdapter[4];
            m_adapters[0] = new FrameworkEventAdapter(context, admin);
            m_adapters[1] = new BundleEventAdapter(context, admin);
            m_adapters[2] = new ServiceEventAdapter(context, admin);
            m_adapters[3] = new LogEventAdapter(context, admin);
        }
        else
        {
            for(int i=0; i<m_adapters.length; i++)
            {
                m_adapters[i].update(admin);
            }
        }
    }

    private Object tryToCreateMetaTypeProvider(final Object managedService)
    {
        try
        {
            return new MetaTypeProviderImpl((ManagedService)managedService,
                    m_cacheSize, m_threadPoolSize, m_timeout, m_requireTopic,
                    m_ignoreTimeout);
        } catch (Throwable t)
        {
            // we simply ignore this
        }
        return null;
    }

    /**
     * Returns either the parsed int from the value of the property if it is set and
     * not less then the min value or the default. Additionally, a warning is
     * generated in case the value is erroneous (i.e., can not be parsed as an int or
     * is less then the min value).
     */
    private int getIntProperty(final String key, final BundleContext context,
        final int defaultValue, final int min)
    {
        final String value = context.getProperty(key);

        if(null != value)
        {
            try {
                final int result = Integer.parseInt(value);

                if(result >= min)
                {
                    return result;
                }

                LogWrapper.getLogger().log(LogWrapper.LOG_WARNING,
                        "Value for property: " + key + " is to low - Using default");
            } catch (NumberFormatException e) {
                LogWrapper.getLogger().log(LogWrapper.LOG_WARNING,
                    "Unable to parse property: " + key + " - Using default", e);
            }
        }

        return defaultValue;
    }

    /**
     * Returns either the parsed int from the value of the property if it is set and
     * not less then the min value or the default. Additionally, a warning is
     * generated in case the value is erroneous (i.e., can not be parsed as an int or
     * is less then the min value).
     */
    private int getIntProperty(final String key, final Object value,
        final int defaultValue, final int min)
    {
        if(null != value)
        {
            final int result;
            if ( value instanceof Integer )
            {
                result = ((Integer)value).intValue();
            }
            else
            {
                try
                {
                    result = Integer.parseInt(value.toString());
                }
                catch (NumberFormatException e)
                {
                    LogWrapper.getLogger().log(LogWrapper.LOG_WARNING,
                        "Unable to parse property: " + key + " - Using default", e);
                    return defaultValue;
                }
            }
            if(result >= min)
            {
                return result;
            }

            LogWrapper.getLogger().log(LogWrapper.LOG_WARNING,
                    "Value for property: " + key + " is to low - Using default");
        }

        return defaultValue;
    }

    /**
     * Returns true if the value of the property is set and is either 1, true, or yes
     * Returns false if the value of the property is set and is either 0, false, or no
     * Returns the defaultValue otherwise
     */
    private boolean getBooleanProperty(final Object obj,
        final boolean defaultValue)
    {
        if(null != obj)
        {
            if ( obj instanceof Boolean )
            {
                return ((Boolean)obj).booleanValue();
            }
            String value = obj.toString().trim().toLowerCase();

            if(0 < value.length() && ("0".equals(value) || "false".equals(value)
                || "no".equals(value)))
            {
                return false;
            }

            if(0 < value.length() && ("1".equals(value) || "true".equals(value)
                || "yes".equals(value)))
            {
                return true;
            }
        }

        return defaultValue;
    }
}
