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

/**
 * A simple scheduler that accepts a task and schedules its for execution at 
 * its own discretion (i.e., the behavior of the actual implementor). The only 
 * possible hint is a nice value that should be subtracted from any fixed scheduling 
 * interval. Additionally, a null object is provided that can be used to disable 
 * scheduled execution. 
 *  
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Scheduler
{
    /**
     * This is a null object that can be used in case no scheduling is needed. In 
     * other words tasks given to this scheduler are never executed.
     */
    public final Scheduler NULL_SCHEDULER = new Scheduler(){
        /**
         * This is a null object hence, this method does nothing.
         * 
         * @param task A task that will never be run.
         */
        public void schedule(final Runnable task)
        {
            // This is a null object hence we don't do nothing.
        }
        
        /**
         * This is a null object hence, this method does nothing.
         * 
         * @param task A task that will never be run.
         * @param nice A nice value that will never be used.
         */
        public void schedule(final Runnable task, final int nice)
        {
            // This is a null object hence we don't do nothing.
        }
    };
    
    /**
     * Schedule the given task for execution at a later time based on the behavior
     * of the actual implementor of this interface. Note that this may mean that
     * the task is never executed.
     * 
     * @param task The task to schedule for execution.
     */
    public void schedule(final Runnable task);
    
    /**
     * Schedule the given task for execution at a later time based on the behavior
     * of the actual implementor of this interface. Note that this may mean that
     * the task is never executed. The nice value should be subtracted from any fixed 
     * scheduling interval.
     * 
     * @param task The task to schedule for execution.
     * @param nice A value to subtract from any fixed scheduling interval.
     */
    public void schedule(final Runnable task, final int nice);
}
