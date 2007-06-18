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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple delay scheduler that schedules tasks based on a fixed delay. Possible
 * nice values are subtracted from this delay where appropriate. Note that this
 * class uses a <tt>java.util.Timer</tt> internally that is set to be a daemon hence,
 * allows to shutdown the vm regardless but can not be stopped. The spec says that 
 * a <tt>java.util.Timer</tt> without a reference to itself should go away eventually
 * but no guaranties are given. It follows that once the bundle is stopped all 
 * references to instances of this class should be released and this in turn will 
 * allow that the timer thread goes away eventually, but this may take an arbitrary 
 * amount of time.
 * 
 * @see org.apache.felix.eventadmin.impl.dispatch.Scheduler
 * @see java.util.Timer
 * 
 *  @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DelayScheduler implements Scheduler
{
    // The timer used for scheduling. Note that it will not be stopped by but 
    // by the vm once all references to this instance are gone (at least eventually).
    private final Timer m_timer = new Timer(true);

    private final int m_delay;

    /**
     * The constructor of the scheduler. The scheduler will use the given delay to 
     * schedule tasks accordingly.
     * 
     * @param delay The delay in milliseconds before a task is executed
     */
    public DelayScheduler(final int delay)
    {
        m_delay = delay;
    }
    
    /**
     * Schedule the task to execute after the given delay.
     *
     * @param task The task to schedule for execution.
     * 
     * @see org.apache.felix.eventadmin.impl.dispatch.Scheduler#schedule(java.lang.Runnable)
     */
    public void schedule(final Runnable task)
    {
        scheduleTaskWithDelay(task, m_delay);
    }

    /**
     * Schedule the task to execute after the given delay minus the nice.
     * 
     * @param task The task to schedule for execution after delay minus nice
     * @param nice The time to subtract from the delay.
     * 
     * @see org.apache.felix.eventadmin.impl.dispatch.Scheduler#schedule(java.lang.Runnable, int)
     */
    public void schedule(final Runnable task, int nice)
    {
        scheduleTaskWithDelay(task, m_delay - nice);
    }
    
    /*
     * This method creates a new TimerTask as a wrapper around the given task
     * and calls the m_timer.schedule method with it and the current time plus the 
     * delay.
     */
    private void scheduleTaskWithDelay(final Runnable task, final int delay)
    {
        m_timer.schedule(new TimerTask()
        {
            public void run()
            {
                task.run();
            }
        }, new Date(System.currentTimeMillis() + delay));
    }
}
