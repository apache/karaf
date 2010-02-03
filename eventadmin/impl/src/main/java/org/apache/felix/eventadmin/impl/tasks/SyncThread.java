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
 * This thread class is used for sending the events
 * synchronously.
 * It handles cascaded synchronous events.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SyncThread extends Thread
{

    /** Counter to track the nesting level. */
    private volatile int counter;

    /** The barriers for synchronizing. */
    private volatile Rendezvous timerBarrier;
    private volatile Rendezvous cascadingBarrier;

    /**
     * Constructor used by the thread pool.
     */
    public SyncThread(Runnable target)
    {
        super(target);
    }

    public void init(final Rendezvous timerBarrier, final Rendezvous cascadingBarrier)
    {
        this.timerBarrier = timerBarrier;
        this.cascadingBarrier = cascadingBarrier;
    }

    public void cleanup()
    {
        this.timerBarrier = null;
        this.cascadingBarrier = null;
    }

    public Rendezvous getTimerBarrier()
    {
        return timerBarrier;
    }

    public Rendezvous getCascadingBarrier()
    {
        return cascadingBarrier;
    }

    public boolean isTopMostHandler()
    {
        return counter == 0;
    }

    public void innerEventHandlingStart()
    {
        counter++;
    }

    public void innerEventHandlingStopped()
    {
        counter--;
    }
}
