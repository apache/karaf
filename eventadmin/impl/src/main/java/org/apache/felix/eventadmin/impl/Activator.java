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

import org.apache.felix.eventadmin.impl.adapter.*;
import org.apache.felix.eventadmin.impl.dispatch.DefaultThreadPool;
import org.apache.felix.eventadmin.impl.dispatch.ThreadPool;
import org.apache.felix.eventadmin.impl.handler.*;
import org.apache.felix.eventadmin.impl.security.*;
import org.apache.felix.eventadmin.impl.tasks.*;
import org.apache.felix.eventadmin.impl.util.LeastRecentlyUsedCacheMap;
import org.apache.felix.eventadmin.impl.util.LogWrapper;
import org.osgi.framework.*;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.TopicPermission;

/**
 * The activator of the EventAdmin bundle. This class registers an implementation of
 * the OSGi R4 <tt>EventAdmin</tt> service (see the Compendium 113) with the
 * framework. It features timeout-based blacklisting of event-handlers for both,
 * asynchronous and synchronous event-dispatching (as a spec conform optional
 * extension).
 *
 * The service knows about the following properties which are read at bundle startup:
 *
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
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
// TODO: Security is in place but untested due to not being implemented by the
// framework. However, it needs to be revisited once security is implemented.
// Two places are affected by this namely, security/* and handler/*
public class Activator implements BundleActivator
{
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

    /**
     * Called upon starting of the bundle. Constructs and registers the EventAdmin
     * service with the framework. Note that the properties of the service are
     * requested from the context in this method hence, the bundle has to be
     * restarted in order to take changed properties into account.
     *
     * @param context The bundle context passed by the framework
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context)
    {
        // init the LogWrapper. Subsequently, the static methods of the LogWrapper
        // can be used to log messages similar to the LogService. The effect of a
        // call to any of this methods is either a print to standard out (in case
        // no LogService is present) or a call to the respective method of
        // available LogServices (the reason is that this way the bundle is
        // independent of the org.osgi.service.log package)
        LogWrapper.setContext(context);

        // The size of various internal caches. At the moment there are 4
        // internal caches affected. Each will cache the determined amount of
        // small but frequently used objects (i.e., in case of the default value
        // we end-up with a total of 120 small objects being cached). A value of less
        // then 10 triggers the default value.
        final int cacheSize = getIntProperty("org.apache.felix.eventadmin.CacheSize",
            context, 30, 10);

        // The size of the internal thread pool. Note that we must execute
        // each synchronous event dispatch that happens in the synchronous event
        // dispatching thread in a new thread, hence a small thread pool is o.k.
        // A value of less then 2 triggers the default value. A value of 2
        // effectively disables thread pooling. Furthermore, this will be used by
        // a lazy thread pool (i.e., new threads are created when needed). Ones the
        // the size is reached and no cached thread is available new threads will
        // be created.
        final int threadPoolSize = getIntProperty(
            "org.apache.felix.eventadmin.ThreadPoolSize", context, 20, 2);

        // The timeout in milliseconds - A value of less then 100 turns timeouts off.
        // Any other value is the time in milliseconds granted to each EventHandler
        // before it gets blacklisted.
        final int timeout = getIntProperty("org.apache.felix.eventadmin.Timeout",
            context, 5000, Integer.MIN_VALUE);

        // Are EventHandler required to be registered with a topic? - The default is
        // true. The specification says that EventHandler must register with a list
        // of topics they are interested in. Setting this value to false will enable
        // that handlers without a topic are receiving all events
        // (i.e., they are treated the same as with a topic=*).
        final boolean requireTopic = getBooleanProperty(
            "org.apache.felix.eventadmin.RequireTopic", context, true);

        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
            "org.apache.felix.eventadmin.CacheSize=" + cacheSize);

        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
            "org.apache.felix.eventadmin.ThreadPoolSize=" + threadPoolSize);

        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
            "org.apache.felix.eventadmin.Timeout=" + timeout);

        LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG,
            "org.apache.felix.eventadmin.RequireTopic=" + requireTopic);

        final TopicPermissions publishPermissions = new CacheTopicPermissions(
            new LeastRecentlyUsedCacheMap(cacheSize), TopicPermission.PUBLISH);

        final TopicPermissions subscribePermissions = new CacheTopicPermissions(
            new LeastRecentlyUsedCacheMap(cacheSize), TopicPermission.SUBSCRIBE);

        final TopicHandlerFilters topicHandlerFilters =
            new CacheTopicHandlerFilters(new LeastRecentlyUsedCacheMap(cacheSize),
            requireTopic);

        final Filters filters = new CacheFilters(
            new LeastRecentlyUsedCacheMap(cacheSize), context);

        // The handlerTasks object is responsible to determine concerned EventHandler
        // for a given event. Additionally, it keeps a list of blacklisted handlers.
        // Note that blacklisting is deactivated by selecting a different scheduler
        // below (and not in this HandlerTasks object!)
        final HandlerTasks handlerTasks = new BlacklistingHandlerTasks(context,
            new CleanBlackList(), topicHandlerFilters, filters,
            subscribePermissions);

        // Note that this uses a lazy thread pool that will create new threads on
        // demand - in case none of its cached threads is free - until threadPoolSize
        // is reached. Subsequently, a threadPoolSize of 2 effectively disables
        // caching of threads.
        m_sync_pool = new DefaultThreadPool(threadPoolSize, true);
        m_async_pool = new DefaultThreadPool(threadPoolSize > 5 ? threadPoolSize / 2 : 2, false);

        final DeliverTask syncExecuter = createSyncExecuters( m_sync_pool, timeout);
        m_admin = createEventAdmin(context,
            handlerTasks,
            createAsyncExecuters(m_async_pool, syncExecuter),
            syncExecuter);

        // register the admin wrapped in a service factory (SecureEventAdminFactory)
        // that hands-out the m_admin object wrapped in a decorator that checks
        // appropriated permissions of each calling bundle
        m_registration = context.registerService(EventAdmin.class.getName(),
            new SecureEventAdminFactory(m_admin, publishPermissions), null);

        // Finally, adapt the outside events to our kind of events as per spec
        adaptEvents(context, m_admin);
    }

    /**
     * Called upon stopping the bundle. This will block until all pending events are
     * delivered. An IllegalStateException will be thrown on new events starting with
     * the begin of this method. However, it might take some time until we settle
     * down which is somewhat cumbersome given that the spec asks for return in
     * a timely manner.
     *
     * @param context The bundle context passed by the framework
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context)
    {
        // We need to unregister manually
        m_registration.unregister();

        m_admin.stop();

        m_admin = null;

        m_registration = null;

        m_async_pool.close();

        m_sync_pool.close();

        m_async_pool = null;

        m_sync_pool = null;
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

    /*
     * Create an AsyncDeliverTasks object that is used to dispatch asynchronous
     * events. Additionally, the asynchronous dispatch queue is initialized and
     * activated (i.e., a thread is started via the given ThreadPool).
     */
    private DeliverTask createAsyncExecuters(final ThreadPool pool, final DeliverTask deliverTask)
    {
        // init the queue
        final AsyncDeliverTasks result = new AsyncDeliverTasks(pool, deliverTask);

        return result;
    }

    /*
     * Create a SyncDeliverTasks object that is used to dispatch synchronous events.
     * Additionally, the synchronous dispatch queue is initialized and activated
     * (i.e., a thread is started via the given ThreadPool).
     */
    private DeliverTask createSyncExecuters(final ThreadPool pool, final long timeout)
    {
        // init the queue
        final SyncDeliverTasks result = new SyncDeliverTasks(pool, (timeout > 100 ? timeout : 0));

        return result;
    }

    /*
     * Init the adapters in org.apache.felix.eventadmin.impl.adapter
     */
    private void adaptEvents(final BundleContext context, final EventAdmin admin)
    {
        new FrameworkEventAdapter(context, admin);

        new BundleEventAdapter(context, admin);

        new ServiceEventAdapter(context, admin);

        new LogEventAdapter(context, admin);
    }

    /*
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


    /*
     * Returns true if the value of the property is set and is either 1, true, or yes
     * Returns false if the value of the property is set and is either 0, false, or no
     * Returns the defaultValue otherwise
     */
    private boolean getBooleanProperty(final String key, final BundleContext context,
        final boolean defaultValue)
    {
        String value = context.getProperty(key);

        if(null != value)
        {
            value = value.trim().toLowerCase();

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
