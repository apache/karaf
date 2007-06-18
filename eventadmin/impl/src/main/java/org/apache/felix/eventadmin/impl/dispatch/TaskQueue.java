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
 * This is the interface for a simple queue that allows to append or push arrays
 * of tasks to it. The elements of such an array are added atomically (i.e, they
 * are in the same order one after the other in the queue) either at the end or the
 * front of the queue. Additionally, the queue can be closed. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface TaskQueue
{
    /**
     * Append the tasks to this queue in one atomic operation while preserving their
     * order.
     * 
     * @param tasks The tasks to append to this queue
     * 
     * @throws IllegalStateException in case that this queue is already closed
     */
    public void append(HandlerTask[] tasks);
    
    /**
     * Push the tasks to this queue in one atomic operation while preserving their
     * order.  
     * 
     * @param tasks The tasks to push to the front of this queue.
     * 
     * @throws IllegalStateException in case that this queue is already closed
     */
    public void push(HandlerTask[] tasks);
    
    /**
     * Close the queue. The given callback will be executed once the queue is empty.
     * 
     * @param shutdownTask The task to execute once the queue is empty
     */
    public void close(final HandlerTask shutdownTask);
}
