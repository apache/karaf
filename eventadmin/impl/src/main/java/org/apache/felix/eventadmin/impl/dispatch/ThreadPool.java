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
     */
    public void executeTask(final Runnable task);

    /**
     * Close the pool i.e, stop pooling threads. Note that subsequently, task will
     * still be executed but no pooling is taking place anymore.
     */
    public void close();
}
