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

import org.apache.felix.eventadmin.impl.tasks.DeliverTask;
import org.apache.felix.eventadmin.impl.tasks.DispatchTask;

/**
 * An implementation of a thread pool that uses a fixed number of cached threads
 * but will spin-off new threads as needed. The underlying assumption is that
 * threads that have been created more recently will be available sooner then older
 * threads hence, once the pool size is reached older threads will be decoupled from
 * the pool and the newly created are added to it.
 *  
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
// TODO: The least recently used method deployed is rather a hack in this case
//      it really should be refactored into a plugable strategy. However, I believe 
//      it to be the best strategy in this case.
public class CacheThreadPool implements ThreadPool
{
    // The internal lock for this object used instead synchronized(this)
    // Note that it is used by the pooled threads created by this pool too. This is
    // the reason why it is not private. Don't use it from the outside.
    final Object m_lock = new Object();
    
    // The pooled threads
    private final PooledThread[] m_pool;
    
    // The least recently used index
    private final List m_index;
    
    // Is this pool closed i.e., do we not pool thread anymore?
    private boolean m_closed = false;
    
    /**
     * The constructor of the pool. The given size will be used as the max number of
     * pooled threads. 
     * 
     * @param size The max number of threads pooled at a given time.
     */
    public CacheThreadPool(final int size)
    {
        synchronized (m_lock)
        {
            m_pool = new PooledThread[size];
            
            // We assume that a list is expanded once it reaches half of its capacity
            // and it doesn't harm if the assumption is wrong.
            m_index = new ArrayList(size + 1 + (size / 2));
        }
    }
    
    /**
     * Executes the task in a thread out of the pool or a new thread if no pooled 
     * thread is available. In case that the max size is reached the least recently
     * used (i.e., the longest executing) thread in the pool is decoupled and a new 
     * one added to the pool that is used to execute the task.
     * 
     * @param task The task to execute
     * @param callback The callback associated with the task
     * 
     * @see org.apache.felix.eventadmin.impl.dispatch.ThreadPool#execute(DispatchTask, DeliverTask)
     */
    public void execute(final DispatchTask task, final DeliverTask callback)
    {
        // Note that we associate a callback with a task via the thread used to 
        // execute the task. In general, a free slot in the pool (i.e., m_pool[i] is 
        // null) can be used to set-up a new thread. Also note that we need to 
        // update the LRU index if we change the pool.
        synchronized(m_lock)
        {
            if(m_closed)
            {
                // We are closed hence, spin-of a new thread for the new task.
                final PooledThread result = new PooledThread();
                
                // Set-up the thread and associate the task with the callback.
                result.reset(task, callback);
                
                // release the thread immediately since we don't pool anymore.
                result.release();
                
                return;
            }
            
            // Search in the pool for a free thread.
            for (int i = 0; i < m_pool.length; i++)
            {
                // o.k. we found a free slot now set-up a new thread for it.
                if (null == m_pool[i])
                {
                    m_pool[i] = new PooledThread();

                    m_pool[i].reset(task, callback);
                    
                    m_index.add(new Integer(i));
                    
                    return;
                }
                else if (m_pool[i].available())
                {
                    // we found a free thread now set it up.
                    m_pool[i].reset(task, callback);
                    
                    final Integer idx = new Integer(i);
                    
                    m_index.remove(idx);
                    
                    m_index.add(idx);
                    
                    return;
                }
            }

            // The pool is full and no threads are available hence, spin-off a new
            // thread and add it to the pool while decoupling the least recently used
            // one. This assumes that older threads are likely to take longer to 
            // become available again then younger ones.
            final int pos = ((Integer) m_index.remove(0)).intValue();
            
            m_index.add(new Integer(pos));
            
            m_pool[pos].release();

            m_pool[pos] = new PooledThread();

            m_pool[pos].reset(task, callback);
        }
    }

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
     * 
     * @see org.apache.felix.eventadmin.impl.dispatch.ThreadPool#getCallback(Thread, DeliverTask)
     */
    public DeliverTask getCallback(final Thread thread, final DeliverTask defaultCallback)
    {
        synchronized (m_lock)
        {
            if (thread instanceof PooledThread)
            {
                return ((PooledThread) thread).getCallback();
            }

            return defaultCallback;
        }
    }

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
     * 
     * @see org.apache.felix.eventadmin.impl.dispatch.ThreadPool#getTask(Thread, DispatchTask)
     */
    public DispatchTask getTask(Thread thread, DispatchTask defaultTask)
    {
        synchronized (m_lock)
        {
            if (thread instanceof PooledThread)
            {
                return ((PooledThread) thread).getTask();
            }

            return defaultTask;
        }
    }

    /**
     * Close the pool i.e, stop pooling threads. Note that subsequently, task will
     * still be executed but no pooling is taking place anymore.
     * 
     * @see org.apache.felix.eventadmin.impl.dispatch.ThreadPool#close()
     */
    public void close()
    {
        synchronized (m_lock)
        {
            // We are closed hence, decouple all threads from the pool
            for (int i = 0; i < m_pool.length; i++)
            {
                if (null != m_pool[i])
                {
                    m_pool[i].release();
                    
                    m_pool[i] = null;
                }
            }
            
            m_closed = true;
        }
    }
    
    /*
     * The threads created by this pool. A PooledThread blocks until it gets a new
     * task from the pool or is released. Additionally, it is used to associate 
     * the task it currently runs with its callback.
     */
    private class PooledThread extends Thread
    {
        // The current task or null if none
        private DispatchTask m_runnable = null;

        // The callback associated with the current task
        private DeliverTask m_callback = null;

        // Is this thread decoupled from the pool (i.e, may cease to exists once its
        // current task is finished)?
        private boolean m_released = false;

        /*
         * This will set-up the thread as a daemon and start it too. No need to call
         * its start method explicitly
         */
        PooledThread()
        {
            setDaemon(true);

            start();
        }

        
        /**
         * Call next() in a loop until next() returns null indicating that we are
         * done (i.e., decoupled from the pool) and may cease to exist.
         */
        public void run()
        {
            for (Runnable next = next(); null != next; next = next())
            {
                next.run();

                synchronized (m_lock)
                {
                    m_runnable = null;
                }
            }
        }

        /*
         * Block until a new task is available or we are decoupled from the pool.
         * This will return the next task or null if we are decoupled from the pool
         */
        private DispatchTask next()
        {
            synchronized (m_lock)
            {
                while (null == m_runnable)
                {
                    if (m_released)
                    {
                        return null;
                    }

                    try
                    {
                        m_lock.wait();
                    } catch (InterruptedException e)
                    {
                        // Not needed
                    }
                }

                return m_runnable;
            }
        }

        /*
         * Set-up the thread for the next task
         */
        void reset(final DispatchTask next, final DeliverTask callback)
        {
            synchronized (m_lock)
            {
                m_runnable = next;
                m_callback = callback;
                m_lock.notifyAll();
            }
        }

        /*
         * Return the callback associated with the current task
         */
        DeliverTask getCallback()
        {
            synchronized (m_lock)
            {
                return m_callback;
            }
        }

        /*
         * Return whether this thread is available (i.e., has no task and has not 
         * been released) or not.
         */
        boolean available()
        {
            synchronized (m_lock)
            {
                return (null == m_runnable) && (!m_released);
            }
        }

        /*
         * Return the current task or null if none
         */
        DispatchTask getTask()
        {
            synchronized (m_lock)
            {
                return m_runnable;
            }
        }

        /*
         * Decouple this thread from the pool
         */
        void release()
        {
            synchronized (m_lock)
            {
                m_released = true;

                m_lock.notifyAll();
            }
        }
    }
}
