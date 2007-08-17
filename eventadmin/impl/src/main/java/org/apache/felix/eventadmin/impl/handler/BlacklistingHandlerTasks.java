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
package org.apache.felix.eventadmin.impl.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.eventadmin.impl.security.TopicPermissions;
import org.apache.felix.eventadmin.impl.tasks.HandlerTask;
import org.apache.felix.eventadmin.impl.tasks.HandlerTaskImpl;
import org.apache.felix.eventadmin.impl.util.LogWrapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This class is an implementation of the HandlerTasks interface that does provide
 * blacklisting of event handlers. Furthermore, handlers are determined from the
 * framework on any call to <tt>createHandlerTasks()</tt> hence, there is no
 * book-keeping of <tt>EventHandler</tt> services while they come and go but a
 * query for each sent event. In order to do this, an ldap-filter is created that
 * will match applicable <tt>EventHandler</tt> references. In order to ease some of
 * the overhead pains of this approach some light caching is going on.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BlacklistingHandlerTasks implements HandlerTasks
{
    // The blacklist that holds blacklisted event handler service references
    private final BlackList m_blackList;

    // The context of the bundle used to get the actual event handler services
    private final BundleContext m_context;

    // Used to create the filters that can determine applicable event handlers for
    // a given event
    private final TopicHandlerFilters m_topicHandlerFilters;

    // Used to create the filters that are used to determine whether an applicable
    // event handler is interested in a particular event
    private final Filters m_filters;

    // Used to create and possibly cache topic permissions
    private final TopicPermissions m_topicPermissions;

    /**
     * The constructor of the factory.
     *
     * @param context The context of the bundle
     * @param blackList The set to use for keeping track of blacklisted references
     * @param topicHandlerFilters The factory for topic handler filters
     * @param filters The factory for <tt>Filter</tt> objects
     * @param topicPermissions The factory for permission objects of type PUBLISH
     */
    public BlacklistingHandlerTasks(final BundleContext context,
        final BlackList blackList,
        final TopicHandlerFilters topicHandlerFilters, final Filters filters,
        final TopicPermissions topicPermissions)
    {
        checkNull(context, "Context");
        checkNull(blackList, "BlackList");
        checkNull(topicHandlerFilters, "TopicHandlerFilters");
        checkNull(filters, "Filters");
        checkNull(topicPermissions, "TopicPermissions");

        m_context = context;

        m_blackList = blackList;

        m_topicHandlerFilters = topicHandlerFilters;

        m_filters = filters;

        m_topicPermissions = topicPermissions;
    }

    /**
     * Create the handler tasks for the event. All matching event handlers are
     * determined and delivery tasks for them returned.
     *
     * @param event The event for which' handlers delivery tasks must be created
     *
     * @return A delivery task for each handler that matches the given event
     *
     * @see org.apache.felix.eventadmin.impl.handler.HandlerTasks#createHandlerTasks(org.osgi.service.event.Event)
     */
    public HandlerTask[] createHandlerTasks(final Event event)
    {
        final List result = new ArrayList();

        ServiceReference[] handlerRefs = null;

        try
        {
            handlerRefs = m_context.getServiceReferences(EventHandler.class
                .getName(), m_topicHandlerFilters.createFilterForTopic(event
                .getTopic()));
        } catch (InvalidSyntaxException e)
        {
            LogWrapper.getLogger().log(LogWrapper.LOG_WARNING,
                "Invalid EVENT_TOPIC [" + event.getTopic() + "]", e);
        }

        if(null == handlerRefs)
        {
            handlerRefs = new ServiceReference[0];
        }

        for (int i = 0; i < handlerRefs.length; i++)
        {
            if (!m_blackList.contains(handlerRefs[i])
                && handlerRefs[i].getBundle().hasPermission(
                    m_topicPermissions.createTopicPermission(event.getTopic())))
            {
                try
                {
                    if (event.matches(m_filters.createFilter(
                        (String) handlerRefs[i]
                            .getProperty(EventConstants.EVENT_FILTER),
                        Filters.TRUE_FILTER)))
                    {
                        result.add(new HandlerTaskImpl(handlerRefs[i],
                            event, this));
                    }
                } catch (InvalidSyntaxException e)
                {
                    LogWrapper.getLogger().log(
                        handlerRefs[i],
                        LogWrapper.LOG_WARNING,
                        "Invalid EVENT_FILTER - Blacklisting ServiceReference ["
                            + handlerRefs[i] + " | Bundle("
                            + handlerRefs[i].getBundle() + ")]", e);

                    m_blackList.add(handlerRefs[i]);
                }
            }
        }

        return (HandlerTaskImpl[]) result
            .toArray(new HandlerTaskImpl[result.size()]);
    }

    /**
     * Blacklist the given service reference. This is a private method and only
     * public due to its usage in a friend class.
     *
     * @param handlerRef The service reference to blacklist
     */
    public void blackList(final ServiceReference handlerRef)
    {
        m_blackList.add(handlerRef);

        LogWrapper.getLogger().log(
            LogWrapper.LOG_WARNING,
            "Blacklisting ServiceReference [" + handlerRef + " | Bundle("
                + handlerRef.getBundle() + ")] due to timeout!");
    }

    /**
     * Get the real EventHandler service for the handlerRef from the context in case
     * the ref is not blacklisted and the service is not unregistered. The
     * NullEventHandler object is returned otherwise. This is a private method and
     * only public due to its usage in a friend class.
     *
     * @param handlerRef The service reference for which to get its service
     * @return The service of the reference or a null object if the service is
     *      unregistered
     */
    public EventHandler getEventHandler(final ServiceReference handlerRef)
    {
        final Object result = (m_blackList.contains(handlerRef)) ? null
            : m_context.getService(handlerRef);

        return (EventHandler) ((null != result) ? result : m_nullEventHandler);
    }

    /**
     * Unget the service reference for the given event handler unless it is the
     * NullEventHandler. This is a private method and only public due to
     * its usage in a friend class.
     *
     * @param handler The event handler service to unget
     * @param handlerRef The service reference to unget
     */
    public void ungetEventHandler(final EventHandler handler,
            final ServiceReference handlerRef)
    {
            if(m_nullEventHandler != handler)
            {
                // Is the handler not unregistered or blacklisted?
                if(!m_blackList.contains(handlerRef) && (null !=
                    handlerRef.getBundle()))
                {
                    m_context.ungetService(handlerRef);
                }
            }
    }

    /*
     * This is a null object that is supposed to do nothing. This is used once an
     * EventHandler is requested for a service reference that is either stale
     * (i.e., unregistered) or blacklisted
     */
    private final EventHandler m_nullEventHandler = new EventHandler()
    {
        /**
         * This is a null object that is supposed to do nothing at this point.
         *
         * @param event an event that is not used
         */
        public void handleEvent(final Event event)
        {
            // This is a null object that is supposed to do nothing at this
            // point. This is used once a EventHandler is requested for a
            // servicereference that is either stale (i.e., unregistered) or
            // blacklisted.
        }
    };

    /*
     * This is a utility method that will throw a <tt>NullPointerException</tt>
     * in case that the given object is null. The message will be of the form name +
     * may not be null.
     */
    private void checkNull(final Object object, final String name)
    {
        if(null == object)
        {
            throw new NullPointerException(name + " may not be null");
        }
    }
}
