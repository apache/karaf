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
 * This class does the actual work of the synchronous event delivery.
 * <p><tt>
 * It serves two purposes, first it is used to select the appropriate action
 * depending on whether the sending thread is the asynchronous, the synchronous, or
 * an unrelated thread. Second, it will set up a given dispatch 
 * task with its <tt>ThreadPool</tt> in a way that it is associated with a 
 * <tt>DeliverTask</tt> that will push given handler tasks to the queue and
 * then wait for the tasks to be completed.
 * </tt></p>
 * In other words if an unrelated thread is used to send a synchronous event it is
 * blocked until the event is send (or a timeout occurs), if an asynchronous thread
 * is used its handover callback is called in order to spin-off a new asynchronous
 * delivery thread and the former is blocked until the events are delivered and then 
 * released (or returned to its thread pool), if a synchronous thread is used its 
 * task is disabled, the events are pushed to the queue and the threads continuous 
 * with the delivery of the new events (as per spec). Once the new events are done
 * the thread wakes-up the disabled task and resumes to execute it. 
 * <p><tt>
 * Note that in case of a timeout while a task is disabled the thread is released and
 * we spin-off a new thread that resumes the disabled task hence, this is the only
 * place were we break the semantics of the synchronous delivery. While the only one
 * to notice this is the timed-out handler - it is the fault of this handler too 
 * (i.e., it blocked the dispatch for to long) but since it will not receive events 
 * anymore it will not notice this semantic difference except that it might not see 
 * events it already sent before.
 * </tt></pre>
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SyncDeliverTasks implements DeliverTasks, HandoverTask, DeliverTask
{
    // The synchronous event queue
    final TaskQueue m_queue;

    // The thread pool used to spin-off new threads and associate callbacks with
    // tasks
    final ThreadPool m_pool;

    /**
     * @param queue The synchronous event queue
     * @param pool The thread pool used to spin-off new threads and associate 
     *      callbacks with tasks
     */
    public SyncDeliverTasks(final TaskQueue queue, final ThreadPool pool)
    {
        m_queue = queue;

        m_pool = pool;
    }

    /**
     * This will select the appropriate action depending on whether the sending
     * thread is the asynchronous, the synchronous, or an unrelated thread.
     * 
     * @return The appropriate action
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.DeliverTasks#createTask()
     */
    public DeliverTask createTask()
    {
        return m_pool.getCallback(Thread.currentThread(), this);
    }
    
    /**
     * This blocks an unrelated thread used to send a synchronous event until the 
     * event is send (or a timeout occurs).
     * 
     * @param tasks The event handler dispatch tasks to execute
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.DeliverTask#execute(org.apache.felix.eventadmin.impl.tasks.HandlerTask[])
     */
    public void execute(final HandlerTask[] tasks)
    {
        final BlockTask waitManager = new BlockTask();

        final HandlerTask[] newtasks = new HandlerTask[tasks.length + 1];
        
        System.arraycopy(tasks, 0, newtasks, 0, tasks.length);
        
        newtasks[tasks.length] = waitManager;

        m_queue.append(newtasks);

        waitManager.block();
    }
    
    /**
     * Set up a given dispatch task with its <tt>ThreadPool</tt> in a way that it is 
     * associated with a <tt>DeliverTask</tt> that will push given handler tasks to 
     * the queue and then wait for the tasks to be completed.
     * 
     * @param task The task to set-up
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.HandoverTask#execute(org.apache.felix.eventadmin.impl.tasks.DispatchTask)
     */
    public void execute(final DispatchTask task)
    {
        m_pool.execute(task, new DeliverTask()
        {
            public void execute(final HandlerTask[] managers)
            {
                final ResumeTask resumeManager = new ResumeTask(
                    task, m_pool);

                final HandlerTask[] newmanagers = new HandlerTask[managers.length + 1];

                System.arraycopy(managers, 0, newmanagers, 0,
                    managers.length);

                newmanagers[managers.length] = resumeManager;

                m_queue.push(newmanagers);

                task.hold();
            }
        });
    }
}
