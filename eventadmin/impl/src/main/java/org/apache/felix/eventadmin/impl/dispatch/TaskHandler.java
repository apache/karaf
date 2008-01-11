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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.eventadmin.impl.tasks.HandlerTask;

/**
 * This class implements the <tt>TaskQueue</tt> and the <tt>TaskProducer</tt>
 * interface. It makes the tasks added via the queue interface available via the
 * producer interface until the queue is closed and the producer returns
 * <tt>null</tt>.
 *
 * @see org.apache.felix.eventadmin.impl.dispatch.TaskQueue
 * @see org.apache.felix.eventadmin.impl.dispatch.TaskProducer
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TaskHandler implements TaskQueue, TaskProducer
{
    // The queue that is used as a lock as well
    private final List m_queue = new ArrayList();

    // Are we closed?
    private boolean m_closed = false;

    /**
     * Append the tasks to this queue in one atomic operation while preserving their
     * order.
     *
     * @param tasks The tasks to append to this queue
     *
     * @throws IllegalStateException in case that this queue is already closed
     *
     * @see org.apache.felix.eventadmin.impl.dispatch.TaskQueue#append(HandlerTask[])
     */
    public void append(final HandlerTask[] tasks)
    {
        synchronized (m_queue)
        {
            if(m_closed)
            {
                throw new IllegalArgumentException("Queue is closed");
            }

            for (int i = 0; i < tasks.length; i++)
            {
                m_queue.add(tasks[i]);
            }

            if(!m_queue.isEmpty())
            {
                m_queue.notifyAll();
            }
        }
    }

    /**
     * Push the tasks to this queue in one atomic operation while preserving their
     * order.
     *
     * @param tasks The tasks to push to the front of this queue.
     *
     * @throws IllegalStateException in case that this queue is already closed
     *
     * @see org.apache.felix.eventadmin.impl.dispatch.TaskQueue#push(HandlerTask[])
     */
    public void push(final HandlerTask[] tasks)
    {
        synchronized (m_queue)
        {
            if(m_closed)
            {
                throw new IllegalArgumentException("Queue is closed");
            }

            for (int i = tasks.length -1; i >= 0; i--)
            {
                m_queue.add(0, tasks[i]);
            }

            if(!m_queue.isEmpty())
            {
                m_queue.notifyAll();
            }
        }
    }

    /**
     * Close the queue. The given shutdown task will be executed once the queue is
     * empty.
     *
     * @param shutdownTask The task to execute once the queue is empty
     *
     * @see org.apache.felix.eventadmin.impl.dispatch.TaskQueue#close(HandlerTask)
     */
    public void close(final HandlerTask shutdownTask)
    {
        synchronized(m_queue)
        {
            m_closed = true;

            m_queue.add(shutdownTask);

            m_queue.notifyAll();
        }
    }

    /**
     * Block until a new task is ready and is returned or no more tasks will be
     * returned.
     *
     * @return The next task or <tt>null</tt> if no more tasks will be produced
     *
     * @see org.apache.felix.eventadmin.impl.dispatch.TaskProducer#next()
     */
    public HandlerTask next()
    {
        synchronized (m_queue)
        {
            while(!m_closed && m_queue.isEmpty())
            {
                try
                {
                    m_queue.wait();
                } catch (InterruptedException e)
                {
                    // Not needed
                }
            }

            if(!m_queue.isEmpty())
            {
                return (HandlerTask) m_queue.remove(0);
            }

            return null;
        }
    }
}
