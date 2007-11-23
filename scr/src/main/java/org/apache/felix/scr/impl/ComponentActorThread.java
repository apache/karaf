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
package org.apache.felix.scr.impl;


import java.util.LinkedList;

import org.osgi.service.log.LogService;


/**
 * The <code>ComponentActorThread</code> is the thread used to act upon registered
 * components of the service component runtime.
 *
 * @author fmeschbe
 */
class ComponentActorThread extends Thread
{

    // the queue of Runnable instances  to be run
    private LinkedList tasks;


    ComponentActorThread()
    {
        super( "SCR Component Actor" );
        tasks = new LinkedList();
    }


    // waits on Runnable instances coming into the queue. As instances come
    // in, this method calls the Runnable.run method, logs any exception
    // happening and keeps on waiting for the next Runnable. If the Runnable
    // taken from the queue is this thread instance itself, the thread
    // terminates.
    public void run()
    {
        for ( ;; )
        {
            Runnable task;
            synchronized ( tasks )
            {
                while ( tasks.isEmpty() )
                {
                    try
                    {
                        tasks.wait();
                    }
                    catch ( InterruptedException ie )
                    {
                        // don't care
                    }
                }

                task = ( Runnable ) tasks.removeFirst();
            }

            // return if the task is this thread itself
            if ( task == this )
            {
                Activator.log( LogService.LOG_DEBUG, null, "Shutting down ComponentActorThread", null );
                return;
            }

            // otherwise execute the task, log any issues
            try
            {
                Activator.log( LogService.LOG_DEBUG, null, "Running task [" + task + "]", null );
                task.run();
            }
            catch ( Throwable t )
            {
                Activator.log( LogService.LOG_ERROR, null, "Unexpected problem executing task", t );
            }
        }
    }


    // cause this thread to terminate by adding this thread to the end
    // of the queue
    void terminate()
    {
        schedule( this );
    }


    // queue the given runnable to be run as soon as possible
    void schedule( Runnable task )
    {
        synchronized ( tasks )
        {
            // append to the task queue
            tasks.add( task );

            Activator.log( LogService.LOG_DEBUG, null, "Adding task [" + task + "] as #" + tasks.size()
                + " in the queue", null );
            
            // notify the waiting thread
            tasks.notifyAll();
        }
    }
}
