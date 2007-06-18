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

import org.apache.felix.eventadmin.impl.dispatch.TaskQueue;
import org.apache.felix.eventadmin.impl.dispatch.ThreadPool;

/**
 * This class does the actual work of the asynchronous event dispatch. 
 * 
 * <p>It serves two purposes: first, it will append tasks to its queue hence, 
 * asynchronous event delivery is executed - second, it will set up a given dispatch 
 * task with its <tt>ThreadPool</tt> in a way that it is associated with a 
 * <tt>DeliverTask</tt> that will block in case the thread hits the 
 * <tt>SyncDeliverTasks</tt>.
 * </p>
 * In other words, if the asynchronous event dispatching thread is used to send a 
 * synchronous event then it will spin-off a new asynchronous dispatching thread
 * while the former waits for the synchronous event to be delivered and then return
 * to its <tt>ThreadPool</tt>.
 *  
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AsyncDeliverTasks implements DeliverTasks, HandoverTask, DeliverTask
{
    // The asynchronous event delivery queue
    private final TaskQueue m_queue;

    // The synchronous event delivery queue needed in case that the asynchronous 
    // event dispatching thread is used to send a synchronous event. This is a
    // private member and only default because it is used in an inner class (for
    // performance reasons)
    final TaskQueue m_handoverQueue; 
    
    // The thread pool to use to spin-off new threads
    private final ThreadPool m_pool;
    
    /**
     * The constructor of the class that will use the asynchronous queue to append
     * event dispatch handlers. Furthermore, a second queue is used to append
     * the events in case that the asynchronous event dispatching thread is used to
     * send a synchronous event - in this case the given <tt>ThreadPool</tt> is used
     * to spin-off a new asynchronous event dispatching thread while the former waits
     * for the synchronous event to be delivered.
     * 
     * @param queue The asynchronous event queue
     * @param handoverQueue The synchronous event queue, to be used in case that the
     *      asynchronous event dispatching thread is used to send a synchronous event
     * @param pool The thread pool used to spin-off new asynchronous event 
     *      dispatching threads in case of timeout or that the asynchronous event 
     *      dispatching thread is used to send a synchronous event
     */
    public AsyncDeliverTasks(final TaskQueue queue, final TaskQueue handoverQueue, 
        final ThreadPool pool)
    {
        m_queue = queue;
     
        m_handoverQueue = handoverQueue;
        
        m_pool = pool;
    }
    
    /**
     * Return a <tt>DeliverTask</tt> that can be used to execute asynchronous event
     * dispatch.
     * 
     * @return A task that can be used to execute asynchronous event dispatch
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.DeliverTasks#createTask()
     */
    public DeliverTask createTask()
    {
        return this;
    }
    
    /**
     * Execute asynchronous event dispatch.
     * 
     * @param tasks The event dispatch tasks to execute
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.DeliverTask#execute(org.apache.felix.eventadmin.impl.tasks.HandlerTask[])
     */
    public void execute(final HandlerTask[] tasks)
    {
        m_queue.append(tasks);
    }
    
    /**
     * Execute the handover in case of timeout or that the asynchronous event
     * dispatching thread is used to send a synchronous event.
     * 
     * @param task The task to set-up in a new thread
     *  
     * @see org.apache.felix.eventadmin.impl.tasks.HandoverTask#execute(org.apache.felix.eventadmin.impl.tasks.DispatchTask)
     */
    public void execute(final DispatchTask task)
    {
        // This will spin-off a new thread using the thread pool and set it up with
        // the given task. Additionally, the thread is associated with a callback
        // that will handover (i.e., yet again call this method) and append the 
        // tasks given to to the m_handoverQueue (i.e., the synchronous queue). This
        // will happen in case that the current asynchronous thread is used to 
        // send a synchronous event. 
        m_pool.execute(task, new DeliverTask()
        {
            public void execute(final HandlerTask[] managers)
            {
                final BlockTask waitManager = new BlockTask();

                final HandlerTask[] newmanagers = new HandlerTask[managers.length + 1];

                System.arraycopy(managers, 0, newmanagers, 0,
                    managers.length);

                newmanagers[managers.length] = waitManager;

                m_handoverQueue.append(newmanagers);

                task.handover();

                waitManager.block();
            }
        });
    }
}
