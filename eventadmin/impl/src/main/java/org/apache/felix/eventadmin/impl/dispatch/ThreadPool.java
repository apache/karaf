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

import org.apache.felix.eventadmin.impl.tasks.DeliverTask;
import org.apache.felix.eventadmin.impl.tasks.DispatchTask;

/**
 * A ThreadPool interface that allows to execute tasks using pooled threads in order
 * to ease the thread creation overhead and additionally, to associate a callback
 * with the thread that executes the task. Subsequently, the callback for a given 
 * thread can be asked from instances of this class. Finally, the currently executed
 * task of a thread created by this pool can be retrieved as well. The look-up
 * methods accept plain thread objects and will return given default values in case
 * that the specific threads have not been created by this pool. Note that a closed
 * pool should still execute new tasks but stop pooling threads. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ThreadPool
{
    /**
     * Execute the task in a free thread or create a new one. The given callback 
     * will be associated with the executing thread as long as it is executed. 
     * 
     * @param task The task to execute
     * @param callback The callback that will be associated with the executing thread
     *      or <tt>null</tt> if none.
     */
    public void execute(final DispatchTask task, final DeliverTask callback);
    
    /**
     * Look-up the callback associated with the task that the given thread is 
     * currently executing or return the default value that may be <tt>null</tt>.
     * 
     * @param thread The thread that is currently executing the task for which to 
     *      return the callback. In case the thread is not created by an instance of
     *      this class the default value will be returned.
     * @param defaultCallback The value to return in case that the thread was not
     *      created by an instance of this class. May be <tt>null</tt> 
     * @return The callback associated with the given thread or the default value.
     */
    public DeliverTask getCallback(final Thread thread, 
        final DeliverTask defaultCallback);
    
    /**
     * Look-up the task that the given thread is currently executing or return the
     * default value that may be <tt>null</tt> in case that the thread has not been
     * created by an instance of this class.
     * 
     * @param thread The thread whose currently executed task should be returned.
     * @param defaultTask The default value to be returned in case that the thread
     *      was not created by this instance or doesn't currently has a task. May be
     *      <tt>null</tt>
     * @return The task the given thread is currently executing or the defaultTask
     */
    public DispatchTask getTask(final Thread thread, final DispatchTask defaultTask);

    /**
     * Close the pool i.e, stop pooling threads. Note that subsequently, task will
     * still be executed but no pooling is taking place anymore.
     */
    public void close();
}
