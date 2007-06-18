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
package org.apache.felix.eventadmin.impl.tasks;

import org.apache.felix.eventadmin.impl.handler.BlacklistingHandlerTasks;
import org.apache.felix.eventadmin.impl.util.LogWrapper;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * An implementation of the <tt>HandlerTask</tt> interface.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class HandlerTaskImpl implements HandlerTask
{
    // The service reference of the handler
    private final ServiceReference m_eventHandlerRef;

    // The event to deliver to the handler
    private final Event m_event;

    // Used to blacklist the service or get the service object for the reference
    private final BlacklistingHandlerTasks m_handlerTasks;

    /**
     * Construct a delivery task for the given service and event.
     * 
     * @param eventHandlerRef The servicereference of the handler
     * @param event The event to deliver
     * @param handlerTasks Used to blacklist the service or get the service object
     *      for the reference 
     */
    public HandlerTaskImpl(final ServiceReference eventHandlerRef,
        final Event event, final BlacklistingHandlerTasks handlerTasks)
    {
        m_eventHandlerRef = eventHandlerRef;

        m_event = event;

        m_handlerTasks = handlerTasks;
    }

    /**
     * @see org.apache.felix.eventadmin.impl.tasks.HandlerTask#execute()
     */
    public void execute()
    {
        // Get the service object 
        final EventHandler handler = m_handlerTasks
            .getEventHandler(m_eventHandlerRef);

        try
        {
            handler.handleEvent(m_event);
        } catch (Exception e)
        {
            // The spec says that we must catch exceptions and log them:
            LogWrapper.getLogger().log(
                m_eventHandlerRef,
                LogWrapper.LOG_WARNING,
                "Exception during event dispatch [" + m_event + " | "
                    + m_eventHandlerRef + " | Bundle("
                    + m_eventHandlerRef.getBundle() + ")]", e);
        }
        
        m_handlerTasks.ungetEventHandler(handler, m_eventHandlerRef);
    }

    /**
     * @see org.apache.felix.eventadmin.impl.tasks.HandlerTask#blackListHandler()
     */
    public void blackListHandler()
    {
        m_handlerTasks.blackList(m_eventHandlerRef);
    }
}
