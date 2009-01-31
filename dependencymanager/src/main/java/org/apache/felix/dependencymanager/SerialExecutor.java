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
package org.apache.felix.dependencymanager;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Allows you to enqueue tasks from multiple threads and then execute
 * them on one thread sequentially. It assumes more than one thread will
 * try to execute the tasks and it will make an effort to pick the first
 * task that comes along whilst making sure subsequent tasks return
 * without waiting.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class SerialExecutor {
    private final LinkedList m_workQueue = new LinkedList();
    private Runnable m_active;
    
    /**
     * Enqueue a new task for later execution. This method is
     * thread-safe, so multiple threads can contribute tasks.
     * 
     * @param runnable the runnable containing the actual task
     */
    public synchronized void enqueue(final Runnable runnable) {
    	m_workQueue.addLast(new Runnable() {
			public void run() {
				try {
					runnable.run();
				}
				finally {
					scheduleNext();
				}
			}
		});
    }
    
    /**
     * Execute any pending tasks. This method is thread safe,
     * so multiple threads can try to execute the pending
     * tasks, but only the first will be used to actually do
     * so. Other threads will return immediately.
     */
    public void execute() {
    	Runnable active;
    	synchronized (this) {
    		active = m_active;
    	}
    	if (active == null) {
    		scheduleNext();
    	}
    }

    private void scheduleNext() {
    	Runnable active;
    	synchronized (this) {
    		try {
    			m_active = (Runnable) m_workQueue.removeFirst();
    		}
    		catch (NoSuchElementException e) {
    			m_active = null;
    		}
    		active = m_active;
    	}
    	if (active != null) {
            active.run();
        }
    }
}
