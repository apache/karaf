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

import org.apache.felix.eventadmin.impl.handler.HandlerTasks;
import org.apache.felix.eventadmin.impl.tasks.DeliverTasks;
import org.apache.felix.eventadmin.impl.tasks.HandlerTask;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This is the actual implementation of the OSGi R4 Event Admin Service (see the 
 * Compendium 113 for details). The implementation uses a <tt>HandlerTasks</tt>
 * in order to determine applicable <tt>EventHandler</tt> for a specific event and
 * subsequently dispatches the event to the handlers via <tt>DeliverTasks</tt>.
 * To do this, it uses two different <tt>DeliverTasks</tt> one for asynchronous and
 * one for synchronous event delivery depending on whether its <tt>post()</tt> or
 * its <tt>send()</tt> method is called. Note that the actual work is done in the 
 * implementations of the <tt>DeliverTasks</tt>. Additionally, a stop method is
 * provided that prevents subsequent events to be delivered.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminImpl implements EventAdmin
{
    // The factory used to determine applicable EventHandlers - this will be replaced
    // by a null object in stop() that subsequently throws an IllegalStateException
    private volatile HandlerTasks m_managers;

    // The asynchronous event dispatcher
    private final DeliverTasks m_postManager;

    // The synchronous event dispatcher
    private final DeliverTasks m_sendManager;

    /**
     * The constructor of the <tt>EventAdmin</tt> implementation. The 
     * <tt>HandlerTasks</tt> factory is used to determine applicable 
     * <tt>EventHandler</tt> for a given event. Additionally, the two 
     * <tt>DeliverTasks</tt> are used to dispatch the event.
     *  
     * @param managers The factory used to determine applicable <tt>EventHandler</tt>
     * @param postManager The asynchronous event dispatcher
     * @param sendManager The synchronous event dispatcher
     */
    public EventAdminImpl(final HandlerTasks managers,
        final DeliverTasks postManager, final DeliverTasks sendManager)
    {
        checkNull(managers, "Managers");
        checkNull(postManager, "PostManager");
        checkNull(sendManager, "SendManager");
        
        m_managers = managers;

        m_postManager = postManager;

        m_sendManager = sendManager;
    }
    
    /**
     * Post an asynchronous event.
     * 
     * @param event The event to be posted by this service
     * 
     * @throws IllegalStateException - In case we are stopped
     * 
     * @see org.osgi.service.event.EventAdmin#postEvent(org.osgi.service.event.Event)
     */
    public void postEvent(final Event event)
    {
        handleEvent(m_managers.createHandlerTasks(event), m_postManager);
    }

    /**
     * Send a synchronous event.
     * 
     * @param event The event to be send by this service
     *
     * @throws IllegalStateException - In case we are stopped
     * 
     * @see org.osgi.service.event.EventAdmin#sendEvent(org.osgi.service.event.Event)
     */
    public void sendEvent(final Event event)
    {
        handleEvent(m_managers.createHandlerTasks(event), m_sendManager);
    }

    /**
     * This method can be used to stop the delivery of events. The m_managers is 
     * replaced with a null object that throws an IllegalStateException on a call
     * to <tt>createHandlerTasks()</tt>.
     */
    public void stop()
    {
        // replace the HandlerTasks with a null object that will throw an 
        // IllegalStateException on a call to createHandlerTasks
        m_managers = new HandlerTasks()
        {
            /**
             * This is a null object and this method will throw an 
             * IllegalStateException due to the bundle being stopped.
             * 
             * @param event An event that is not used.
             * 
             * @return This method does not return normally
             * 
             * @throws IllegalStateException - This is a null object and this method
             *          will always throw an IllegalStateException
             */
            public HandlerTask[] createHandlerTasks(final Event event)
            {
                throw new IllegalStateException("The EventAdmin is stopped");
            }
        };
    }
    
    /*
     * This is a utility method that uses the given DeliverTasks to create a 
     * dispatch tasks that subsequently is used to dispatch the given HandlerTasks.
     */
    private void handleEvent(final HandlerTask[] managers,
        final DeliverTasks manager)
    {
        if (0 < managers.length)
        {
            // This might throw an IllegalStateException in case that we are stopped
            // and the null object for m_managers was not fast enough established
            // This is needed in the adapter/* classes due to them sending
            // events whenever they receive an event from their source.
            // Service importers that call us regardless of the fact that we are
            // stopped deserve an exception anyways
            manager.createTask().execute(managers);
        }
    }
    
    /*
     * This is a utility method that will throw a <tt>NullPointerException</tt>
     * in case that the given object is null. The message will be of the form 
     * "${name} + may not be null".
     */
    private void checkNull(final Object object, final String name)
    {
        if(null == object)
        {
            throw new NullPointerException(name + " may not be null");
        }
    }
}
