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

import EDU.oswego.cs.dl.util.concurrent.*;

/**
 * This is a simplified version of the CyclicBarrier implementation.
 * It provides the same methods but internally ignores the exceptions.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Rendezvous extends CyclicBarrier
{
    /** Flag for timedout handling. */
    private volatile boolean timedout = false;

    /**
     * Create a Barrier for the indicated number of parties, and the default
     * Rotator function to run at each barrier point.
     */
    public Rendezvous()
    {
        super(2);
    }

    /**
     * see {@link CyclicBarrier#barrier()}
     */
    public void waitForRendezvous()
    {
        if ( timedout )
        {
            // if we have timed out, we return immediately
            return;
        }
        try
        {
            this.barrier();
        }
        catch (BrokenBarrierException ignore1)
        {
        }
        catch (InterruptedException ignore2)
        {
        }
    }

    /**
     * see {@link CyclicBarrier#attemptBarrier(long)}
     */
    public void waitAttemptForRendezvous(final long timeout)
    throws TimeoutException
    {
        try
        {
            this.attemptBarrier(timeout);
            this.restart();
        }
        catch (BrokenBarrierException ignore1)
        {
        }
        catch (TimeoutException te)
        {
            timedout = true;
            throw te;
        }
        catch (InterruptedException ignore2)
        {
        }
    }

    public boolean isTimedOut()
    {
        return timedout;
    }
}
