/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.felix.shell.remote;


/**
 * A latch is a boolean condition that is set at most once, ever.
 * Once a single release is issued, all acquires will pass.
 * <p/>
 * <b>Sample usage.</b> Here are a set of classes that use
 * a latch as a start signal for a group of worker threads that
 * are created and started beforehand, and then later enabled.
 * <pre>
 * class Worker implements Runnable {
 *   private final Latch startSignal;
 *   Worker(Latch l) { startSignal = l; }
 *    public void run() {
 *      startSignal.acquire();
 *      doWork();
 *   }
 *   void doWork() { ... }
 * }
 * <p/>
 * class Driver { // ...
 *   void main() {
 *     Latch go = new Latch();
 *     for (int i = 0; i < N; ++i) // make threads
 *       new Thread(new Worker(go)).start();
 *     doSomethingElse();         // don't let run yet
 *     go.release();              // let all threads proceed
 *   }
 * }
 * </pre>
 */
class Latch
{

    protected boolean latched_ = false;


    /*
      This could use double-check, but doesn't.
      If the latch is being used as an indicator of
      the presence or state of an object, the user would
      not necessarily get the memory barrier that comes with synch
      that would be needed to correctly use that object. This
      would lead to errors that users would be very hard to track down. So, to
      be conservative, we always use synch.
    */

    public void acquire() throws InterruptedException
    {
        if ( Thread.interrupted() )
            throw new InterruptedException();
        synchronized ( this )
        {
            while ( !latched_ )
                wait();
        }
    }//acquire


    public boolean attempt( long msecs ) throws InterruptedException
    {
        if ( Thread.interrupted() )
            throw new InterruptedException();
        synchronized ( this )
        {
            if ( latched_ )
                return true;
            else if ( msecs <= 0 )
                return false;
            else
            {
                long waitTime = msecs;
                long start = System.currentTimeMillis();
                for ( ;; )
                {
                    wait( waitTime );
                    if ( latched_ )
                        return true;
                    else
                    {
                        waitTime = msecs - ( System.currentTimeMillis() - start );
                        if ( waitTime <= 0 )
                            return false;
                    }
                }
            }
        }
    }//attempt


    /**
     * Enable all current and future acquires to pass *
     */
    public synchronized void release()
    {
        latched_ = true;
        notifyAll();
    }//release

}//class Latch

