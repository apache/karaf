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
package org.apache.felix.eventadmin.impl.dispatch;

import org.apache.felix.eventadmin.impl.tasks.HandlerTask;

/**
 * Instances of this interface will deliver new tasks as soon as they are available
 * while blocking in the <tt>next()</tt> call until then. Unless there won't be any
 * more tasks in which case <tt>null</tt> is returned.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface TaskProducer
{
    /**
     * Block until a new task is ready and is returned or no more tasks will be 
     * returned.
     * 
     * @return The next task or <tt>null</tt> if no more tasks will be produced
     */
    public HandlerTask next();
}
