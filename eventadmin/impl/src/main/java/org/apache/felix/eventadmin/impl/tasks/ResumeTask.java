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

import org.apache.felix.eventadmin.impl.dispatch.ThreadPool;

/**
 * A task that wakes-up a disabled <tt>DispatchTask</tt>. Additionally, it will
 * stop the currently running task.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResumeTask implements HandlerTask
{
    // The task to wake-up on execution
    private final DispatchTask m_target;
    
    // The pool used to get the task to stop on execution
    private final ThreadPool m_pool;
    
    /**
     * @param target The task to wake-up on execution
     * @param pool The pool used to get the task to stop on execution
     */
    public ResumeTask(final DispatchTask target, final ThreadPool pool)
    {
        m_target = target;
        
        m_pool = pool;
    }
    
    /**
     * Stop the current task and wake-up the target.
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.HandlerTask#execute()
     */
    public void execute()
    {
        m_pool.getTask(Thread.currentThread(), null).stop();
        
        m_target.resume();
    }

    /**
     * This does nothing since this task is only used to wake-up disabled tasks.
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.HandlerTask#blackListHandler()
     */
    public void blackListHandler()
    {
    }
}
