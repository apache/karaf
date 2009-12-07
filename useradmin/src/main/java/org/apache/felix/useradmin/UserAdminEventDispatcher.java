/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin;

import org.osgi.service.useradmin.UserAdminEvent;

/**
 * <p>The <tt>UserAdminEventDispatcher</tt> represents event dispatcher
 * used for dispatching UserAdminEvent to the UserAdminListeners and
 * EventAdmin topic.
 * </p>
 *
 * @see org.osgi.serice.useradmin.UserAdminListener
 * @version $Rev$ $Date$
 */
public interface UserAdminEventDispatcher
{
    /**
     * Starting dispatcher thread.
     */
    void start();

    /**
     * Dispatching UserAdminEvent asynchronously.
     * 
     * @param event UserAdmin event.
     */
    void dispatchEventAsynchronusly(UserAdminEvent userAdminEvent);

    /**
     * Closing dispatcher thread.
     */
    void close();
}
