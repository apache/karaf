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

import org.apache.felix.eventadmin.impl.tasks.HandlerTask;
import org.osgi.service.event.Event;

/**
 * The factory for event handler tasks. Implementations of this interface can be
 * used to create tasks that handle the delivery of events to event handlers.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface HandlerTasks
{
    /**
     * Create the handler tasks for the event. All matching event handlers must
     * be determined and delivery tasks for them returned.
     *
     * @param event The event for which' handlers delivery tasks must be created
     *
     * @return A delivery task for each handler that matches the given event
     */
    HandlerTask[] createHandlerTasks(final Event event);
}
