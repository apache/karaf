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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.felix.eventadmin.impl.adapter.AbstractAdapter;
import org.apache.felix.eventadmin.impl.adapter.BundleEventAdapter;
import org.apache.felix.eventadmin.impl.adapter.FrameworkEventAdapter;
import org.apache.felix.eventadmin.impl.adapter.ServiceEventAdapter;
import org.apache.felix.eventadmin.impl.handler.EventAdminImpl;
import org.apache.felix.eventadmin.impl.security.SecureEventAdminFactory;
import org.apache.felix.eventadmin.impl.tasks.DefaultThreadPool;
import org.apache.felix.eventadmin.impl.util.LogWrapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.MetaTypeProvider;

/**
 * <p>The <code>Configuration</code> class encapsules the
 * configuration for the event admin.</p>
 *
 * <p>The service knows about the following properties which are read at bundle startup:</p>
 * <p>
 *      <code>org.apache.felix.eventadmin.ThreadPoolSize</code> - The size of the thread
 *          pool.
 * </p>
 *
 * <p>The default value is 10. Increase in case of a large amount of synchronous events
 * where the <code>EventHandler</code> services in turn send new synchronous events in
 * the event dispatching thread or a lot of timeouts are to be expected. A value of
 * less then 2 triggers the default value. A value of 2 effectively disables thread
 * pooling.</p>
 *
 * <p>
 *      <code>org.apache.felix.eventadmin.Timeout</code> - The black-listing timeout in
 *          milliseconds
 * </p>
 *
 * <p>The default value is 5000. Increase or decrease at own discretion. A value of less
 * then 100 turns timeouts off. Any other value is the time in milliseconds granted
 * to each <code>EventHandler</code> before it gets blacklisted.</p>
 *
 * <p>
 *      <code>org.apache.felix.eventadmin.RequireTopic</code> - Are <code>EventHandler</code>
 *          required to be registered with a topic?
 * </p>
 *
 * <p>The default is <code>true</code>. The specification says that <code>EventHandler</code>
 * must register with a list of topics they are interested in. Setting this value to
 * <code>false</code> will enable that handlers without a topic are receiving all events
 * (i.e., they are treated the same as with a topic=*).</p>
 *
 * <p>
 *      <code>org.apache.felix.eventadmin.IgnoreTimeout</code> - Configure
 *         <code>EventHandler</code>s to be called without a timeout.
 * </p>
 *
 * <p>If a timeout is configured by default all event handlers are called using the timeout.
 * For performance optimization it is possible to configure event handlers where the
 * timeout handling is not used - this reduces the thread usage from the thread pools
 * as the timout handling requires an additional thread to call the event handler.
 * However, the application should work without this configuration property. It is a
 * pure optimization.
 * The value is a list of string (separated by comma). If the string ends with a dot,
 * all handlers in exactly this package are ignored. If the string ends with a star,
 * all handlers in this package and all subpackages are ignored. If the string neither
 * ends with a dot nor with a start, this is assumed to define an exact class name.</p>
 *
 * <p>These properties are read at startup and serve as a default configuration.
 * If a configuration admin is configured, the event admin can be configured
 * through the config admin.</p>
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Configuration
{
    /** The PID for the event admin. */
    static final String PID = "org.apache.felix.eventadmin.impl.EventAdmin";

    static final String PROP_THREAD_POOL_SIZE = "org.apache.felix.eventadmin.ThreadPoolSize";
    static final String PROP_ASYNC_TO_SYNC_THREAD_RATIO = "org.apache.felix.eventadmin.AsyncToSyncThreadRatio";
    static final String PROP_TIMEOUT = "org.apache.felix.eventadmin.Timeout";
    static final String PROP_REQUIRE_TOPIC = "org.apache.felix.eventadmin.RequireTopic";
    static final String PROP_IGNORE_TIMEOUT = "org.apache.felix.eventadmin.IgnoreTimeout";
    static final String PROP_IGNORE_TOPIC = "org.apache.felix.eventadmin.IgnoreTopic";
    static final String PROP_LOG_LEVEL = "org.apache.felix.eventadmin.LogLevel";
    static final String PROP_ADD_TIMESTAMP = "org.apache.felix.eventadmin.AddTimestamp";
    static final String PROP_ADD_SUBJECT = "org.apache.felix.eventadmin.AddSubject";

    /** The bundle context. */
    private final BundleContext m_bundleContext;

    private int m_threadPoolSize;

    private double m_asyncToSyncThreadRatio;

    private int m_asyncThreadPoolSize;

    private int m_timeout;

    private boolean m_requireTopic;

    private String[] m_ignoreTimeout;

    private String[] m_ignoreTopics;

    private int m_logLevel;

    private boolean m_addTimestamp;

    private boolean m_addSubject;

    // The thread pool used - this is a member because we need to close it on stop
    private volatile DefaultThreadPool m_sync_pool;

    private volatile DefaultThreadPool m_async_pool;

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
        startOrUpdate();

        // check for Configuration Admin configuration
        try
        {
            Object service = tryToCreateManagedService();
            if ( service != null )
            {
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
                Dictionary<String, Object> props = new Hashtable<>();
                props.put( Constants.SERVICE_PID, PID );
                m_managedServiceReg = m_bundleContext.registerService( interfaceNames, service, props );
            }
        }
        catch ( Throwable t )
        {
            // don't care
        }
    }

    void updateFromConfigAdmin(final Dictionary<String, ?> config)
    {
        // do this in the background as we don't want to stop
        // the config admin
        new Thread(() -> {
            synchronized ( Configuration.this )
            {
                Configuration.this.configure( config );
                Configuration.this.startOrUpdate();
            }
        }).start();

    }

    /**
     * Configures this instance.
     */
    void configure( Dictionary<String, ?> config )
    {
        if ( config == null )
        {
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

            // The ratio of asynchronous to synchronous threads in the internal thread
            // pool.  Ratio must be positive and may be adjusted to represent the
            // distribution of post to send operations.  Applications with higher number
            // of post operations should have a higher ratio.
            m_asyncToSyncThreadRatio = getDoubleProperty(
                    PROP_ASYNC_TO_SYNC_THREAD_RATIO, m_bundleContext.getProperty(PROP_ASYNC_TO_SYNC_THREAD_RATIO), 0.5, 0.0);

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
                for(int i=0; i<m_ignoreTimeout.length; i++)
                {
                    m_ignoreTimeout[i] = st.nextToken();
                }
            }

            final String valueIgnoreTopic = m_bundleContext.getProperty(PROP_IGNORE_TOPIC);
            if ( valueIgnoreTopic == null )
            {
                m_ignoreTopics = null;
            }
            else
            {
                final StringTokenizer st = new StringTokenizer(valueIgnoreTopic, ",");
                m_ignoreTopics = new String[st.countTokens()];
                for(int i=0; i<m_ignoreTopics.length; i++)
                {
                    m_ignoreTopics[i] = st.nextToken();
                }
            }
            m_logLevel = getIntProperty(PROP_LOG_LEVEL,
                    m_bundleContext.getProperty(PROP_LOG_LEVEL),
                    LogWrapper.LOG_WARNING, // default log level is WARNING
                    LogWrapper.LOG_ERROR);
            m_addTimestamp = getBooleanProperty(
                    m_bundleContext.getProperty(PROP_ADD_TIMESTAMP), false);
            m_addSubject = getBooleanProperty(
                    m_bundleContext.getProperty(PROP_ADD_SUBJECT), false);
        }
        else
        {
            m_threadPoolSize = getIntProperty(PROP_THREAD_POOL_SIZE, config.get(PROP_THREAD_POOL_SIZE), 20, 2);
            m_asyncToSyncThreadRatio = getDoubleProperty(
                    PROP_ASYNC_TO_SYNC_THREAD_RATIO, m_bundleContext.getProperty(PROP_ASYNC_TO_SYNC_THREAD_RATIO), 0.5, 0.0);
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
            else if ( value != null )
            {
                LogWrapper.getLogger().log(LogWrapper.LOG_WARNING,
                        "Value for property: " + PROP_IGNORE_TIMEOUT + " is neither a string nor a string array - Using default");
            }
            m_ignoreTopics = null;
            final Object valueIT = config.get(PROP_IGNORE_TOPIC);
            if ( valueIT instanceof String )
            {
                m_ignoreTopics = new String[] {(String)valueIT};
            }
            else if ( valueIT instanceof String[] )
            {
                m_ignoreTopics = (String[])valueIT;
            }
            else if ( valueIT != null )
            {
                LogWrapper.getLogger().log(LogWrapper.LOG_WARNING,
                        "Value for property: " + PROP_IGNORE_TOPIC + " is neither a string nor a string array - Using default");
            }
            m_logLevel = getIntProperty(PROP_LOG_LEVEL,
                    config.get(PROP_LOG_LEVEL),
                    LogWrapper.LOG_WARNING, // default log level is WARNING
                    LogWrapper.LOG_ERROR);
            m_addTimestamp = getBooleanProperty(
                    config.get(PROP_ADD_TIMESTAMP), false);
            m_addSubject = getBooleanProperty(
                    config.get(PROP_ADD_SUBJECT), false);
        }
        // a timeout less or equals to 100 means : disable timeout
        if ( m_timeout <= 100 )
        {
            m_timeout = 0;
        }
        m_asyncThreadPoolSize = m_threadPoolSize > 5 ? (int)Math.floor(m_threadPoolSize * m_asyncToSyncThreadRatio)  : 2;
    }

    private void startOrUpdate()
    {
        LogWrapper.getLogger().setLogLevel(m_logLevel);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
                PROP_LOG_LEVEL + "=" + m_logLevel);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
                PROP_THREAD_POOL_SIZE + "=" + m_threadPoolSize);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
                PROP_ASYNC_TO_SYNC_THREAD_RATIO + "=" + m_asyncToSyncThreadRatio);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
                "Async Pool Size=" + m_asyncThreadPoolSize);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
                PROP_TIMEOUT + "=" + m_timeout);
        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
                PROP_REQUIRE_TOPIC + "=" + m_requireTopic);

        // Note that this uses a lazy thread pool that will create new threads on
        // demand - in case none of its cached threads is free - until threadPoolSize
        // is reached. Subsequently, a threadPoolSize of 2 effectively disables
        // caching of threads.
        if ( m_sync_pool == null )
        {
            m_sync_pool = new DefaultThreadPool(m_threadPoolSize, true);
        }
        else
        {
            m_sync_pool.configure(m_threadPoolSize);
        }
        final int asyncThreadPoolSize = m_asyncThreadPoolSize;
        if ( m_async_pool == null )
        {
            m_async_pool = new DefaultThreadPool(asyncThreadPoolSize, false);
        }
        else
        {
            m_async_pool.configure(asyncThreadPoolSize);
        }

        if ( m_admin == null )
        {
            m_admin = new EventAdminImpl(m_bundleContext,
                    m_sync_pool,
                    m_async_pool,
                    m_timeout,
                    m_ignoreTimeout,
                    m_requireTopic,
                    m_ignoreTopics,
                    m_addTimestamp,
                    m_addSubject);

            // Finally, adapt the outside events to our kind of events as per spec
            adaptEvents(m_admin);

            // register the admin wrapped in a service factory (SecureEventAdminFactory)
            // that hands-out the m_admin object wrapped in a decorator that checks
            // appropriated permissions of each calling bundle
            m_registration = m_bundleContext.registerService(EventAdmin.class.getName(),
                    new SecureEventAdminFactory(m_admin), null);
        }
        else
        {
            m_admin.update(m_timeout, m_ignoreTimeout, m_requireTopic, m_ignoreTopics, m_addTimestamp, m_addSubject);
        }

    }

    /**
     * Called upon stopping the bundle. This will block until all pending events are
     * delivered. An IllegalStateException will be thrown on new events starting with
     * the begin of this method. However, it might take some time until we settle
     * down which is somewhat cumbersome given that the spec asks for return in
     * a timely manner.
     */
    public void destroy()
    {
        synchronized ( this )
        {
            if ( m_adapters != null )
            {
                for (AbstractAdapter adapter : m_adapters) {
                    adapter.destroy(m_bundleContext);
                }
                m_adapters = null;
            }
            if ( m_managedServiceReg != null )
            {
                m_managedServiceReg.unregister();
                m_managedServiceReg = null;
            }
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
            if (m_async_pool != null )
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
    }

    /**
     * Init the adapters in org.apache.felix.eventadmin.impl.adapter
     */
    private void adaptEvents(final EventAdmin admin)
    {
        m_adapters = new AbstractAdapter[3];
        m_adapters[0] = new FrameworkEventAdapter(m_bundleContext, admin);
        m_adapters[1] = new BundleEventAdapter(m_bundleContext, admin);
        m_adapters[2] = new ServiceEventAdapter(m_bundleContext, admin);
        // KARAF: disable log events as they are published by PaxLogging
        //m_adapters[3] = new LogEventAdapter(m_bundleContext, admin);
    }

    private Object tryToCreateMetaTypeProvider(final Object managedService)
    {
        try
        {
            return new MetaTypeProviderImpl((ManagedService)managedService,
                    m_threadPoolSize, m_timeout, m_requireTopic,
                    m_ignoreTimeout, m_ignoreTopics, m_asyncToSyncThreadRatio);
        }
        catch (final Throwable t)
        {
            // we simply ignore this
        }
        return null;
    }

    private Object tryToCreateManagedService()
    {
        try
        {
            return (ManagedService) this::updateFromConfigAdmin;
        }
        catch (Throwable t)
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
     * Returns either the parsed double from the value of the property if it is set and
     * not less then the min value or the default. Additionally, a warning is
     * generated in case the value is erroneous (i.e., can not be parsed as an double or
     * is less then the min value).
     */
    private double getDoubleProperty(final String key, final Object value,
                                     final double defaultValue, final double min)
    {
        if(null != value)
        {
            final double result;
            if ( value instanceof Double )
            {
                result = ((Double)value).doubleValue();
            }
            else
            {
                try
                {
                    result = Double.parseDouble(value.toString());
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
