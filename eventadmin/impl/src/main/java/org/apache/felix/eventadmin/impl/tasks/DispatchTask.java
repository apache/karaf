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

import org.apache.felix.eventadmin.impl.dispatch.Scheduler;
import org.apache.felix.eventadmin.impl.dispatch.TaskProducer;

/**
 * This class is the core of the event dispatching (for both, synchronous and 
 * asynchronous). It implements handover and timeout capabilities.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DispatchTask implements Runnable
{   
    // A null scheduler object that does not schedule given tasks
    private static final Scheduler NULL_SCHEDULER = new Scheduler()
    {
        /**
         * This is a null object and will do nothing with the given task
         * 
         * @param task A task that is not used
         * 
         * @see org.apache.felix.eventadmin.impl.dispatch.Scheduler#schedule(java.lang.Runnable)
         */
        public void schedule(final Runnable task)
        {
            // This is a null object and will do nothing with the given task
        }

        /**
         * This is a null object and will do nothing with the given task
         * 
         * @param task A task that is not used
         * @parma nice A value that is not used
         * 
         * @see org.apache.felix.eventadmin.impl.dispatch.Scheduler#schedule(java.lang.Runnable, int)
         */
        public void schedule(final Runnable task, final int nice)
        {
            // This is a null object and will do nothing with the given task
        }
    };

    // A null producer object that will return null on any call to next()
    private static final TaskProducer NULL_PRODUCER = new TaskProducer()
    {
        /**
         * This is a null object and will return <tt>null</tt>
         * 
         * @return <tt>null</tt>
         * 
         * @see org.apache.felix.eventadmin.impl.dispatch.TaskProducer#next()
         */
        public HandlerTask next()
        {
            return null;
        }
    };

    // A null handover task that will do nothing on execute
    private static final HandoverTask NULL_HANDOVER = new HandoverTask()
    {
        /**
         * This is a null object that will do nothing.
         * 
         * @parma task A task that is not used
         * 
         * @see org.apache.felix.eventadmin.impl.tasks.HandoverTask#execute(org.apache.felix.eventadmin.impl.tasks.DispatchTask)
         */
        public void execute(final DispatchTask task)
        {
            // This is a null object that will do nothing.
        } 
    };
    
    //  The internal lock for this object used instead synchronized(this)
    final Object m_lock = new Object();

    // The task producer (i.e., the event queue) that will be a null object if not
    // needed anymore
    private volatile TaskProducer m_producer;

    // The scheduler to use that will be a null object if not needed anymore
    private Scheduler m_scheduler;

    // The handover callback that is called on timeouts and handovers and that will
    // be a null object if not needed anymore
    private HandoverTask m_handover;

    // Used to blacklist on timeout
    private BlackListTask m_blackListTask = null;

    // Are we currently blocked (i.e., do not tick the timeout clock down)?
    private boolean m_isHolding = false;

    /**
     * The constructor of the object. 
     * 
     * @param producer The producer (i.e., the event queue) that provides the next 
     *      tasks
     * @param scheduler The scheduler to use for timeout actions
     * @param handover The callback to use on timeouts and handovers
     */
    public DispatchTask(final TaskProducer producer, final Scheduler scheduler,
        final HandoverTask handover)
    {
        m_producer = producer;

        m_scheduler = scheduler;

        m_handover = handover;
    }
    
    /*
     * Construct a new object from a old one.
     */
    private DispatchTask(final DispatchTask old)
    {
        this(old.m_producer, old.m_scheduler, old.m_handover);
    }

    /**
     * This will loop until the producer returns <tt>null</tt>. Until then the 
     * returned tasks are executed.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        for (HandlerTask manager = m_producer.next(); null != manager; manager = m_producer
            .next())
        {
            synchronized (m_lock)
            {
                // Set-up the timeout 
                m_blackListTask = new BlackListTask(manager);

                m_scheduler.schedule(m_blackListTask);
            }

            // HandlerTask does catch exceptions hence, we don't need to do it.
            manager.execute();

            synchronized (m_lock)
            {
                // release the timeout 
                m_blackListTask.cancel();
            }
        }
    }

    /**
     * This method will trigger a callback to the handover callback and stop this 
     * task.
     */
    public void handover()
    {
        synchronized (m_lock)
        {
            // release the timeout
            m_blackListTask.cancel();

            // spin-off a new thread
            m_handover.execute(new DispatchTask(this));

            stop();
        }
    }
    
    /**
     * This method stops the tasks without a handover
     */
    public void stop()
    {
        synchronized (m_lock)
        {
            // release the timeout
            m_blackListTask.cancel();

            m_handover = NULL_HANDOVER;

            m_producer = NULL_PRODUCER;

            m_scheduler = NULL_SCHEDULER;
        }
    }

    /**
     * This will pause the task (including its timeout clock) until a call to 
     * <tt>resume()</tt>
     */
    public void hold()
    {
        synchronized (m_lock)
        {
            // release the timeout
            m_blackListTask.cancel();

            // record the time that we already used
            int pastTime = (int) (System.currentTimeMillis() - m_blackListTask
                .getTime());

            // spin-off a new thread
            m_handover.execute(new DispatchTask(this));

            // block until a call to resume()
            m_isHolding = true;

            while (m_isHolding)
            {
                try
                {
                    m_lock.wait();
                } catch (InterruptedException e)
                {
                }
            }

            // restore the timeout
            m_blackListTask = new BlackListTask(m_blackListTask, 
                System.currentTimeMillis() - pastTime);

            m_scheduler.schedule(m_blackListTask, pastTime);
        }
    }

    /**
     * This will let the previously hold task resume.
     */
    public void resume()
    {
        synchronized (m_lock)
        {
            m_isHolding = false;

            m_lock.notifyAll();
        }
    }

    /*
     * This is the implementation of the timeout.
     */
    private class BlackListTask implements Runnable
    {
        // Are we canceled?
        private boolean m_canceled = false;

        // The time we have been started
        private final long m_time;

        // The task we will blacklist if we are triggered
        private final HandlerTask m_manager;

        BlackListTask(final HandlerTask manager)
        {
            this(manager, System.currentTimeMillis());
        }
        
        BlackListTask(final HandlerTask manager, final long time)
        {
            m_manager = manager;
            
            m_time = time;
        }

        BlackListTask(final BlackListTask old, final long time)
        {
            this(old.m_manager, time);
        }

        /**
         * @return The time we have been created.
         */
        public long getTime()
        {
            return m_time;
        }

        /**
         * We have been triggered hence, blacklist the handler except if we are 
         * already canceled
         * 
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            synchronized (m_lock)
            {
                if (!m_canceled)
                {
                    m_manager.blackListHandler();

                    handover();
                }
            }
        }

        /**
         * Cancel the timeout
         */
        public void cancel()
        {
            synchronized (m_lock)
            {
                m_canceled = true;
            }
        }
    }
}
