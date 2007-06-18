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

/**
 * This task will can be used to block a thread that subsequently will be unblocked 
 * once the task is executed.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BlockTask implements HandlerTask
{
    // The internal lock for this object used instead synchronized(this)
    private final Object m_lock = new Object();

    // Has this task not been executed?
    private boolean m_blocking = true;

    /**
     * Unblock possibly blocking threads.
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.HandlerTask#execute()
     */
    public void execute()
    {
        synchronized (m_lock)
        {
            m_blocking = false;
            m_lock.notifyAll();
        }
    }

    /**
     * This methods does nothing since we only need this task to block and unblock
     * threads.
     * 
     * @see org.apache.felix.eventadmin.impl.tasks.HandlerTask#blackListHandler()
     */
    public void blackListHandler()
    {
        // This method does nothing since we only need this task to block and 
        // unblock threads
    }

    /**
     * Block the calling thread until this task is executed.
     */
    public void block()
    {
        synchronized (m_lock)
        {
            while (m_blocking)
            {
                try
                {
                    m_lock.wait();
                } catch (InterruptedException e)
                {

                }
            }
        }
    }

}
