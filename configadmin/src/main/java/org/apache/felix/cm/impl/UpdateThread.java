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
package org.apache.felix.cm.impl;


import java.util.LinkedList;

import org.osgi.service.log.LogService;


/**
 * The <code>UpdateThread</code> is the thread used to update managed services
 * and managed service factories as well as to send configuration events.
 *
 * @author fmeschbe
 */
public class UpdateThread extends Thread
{

    // the configuration manager on whose behalf this thread is started
    // (this is mainly used for logging)
    private ConfigurationManager configurationManager;
    
    // the queue of Runnable instances  to be run
    private LinkedList updateTasks;


    public UpdateThread( ConfigurationManager configurationManager )
    {
        super( "Configuration Updater" );

        this.configurationManager = configurationManager;
        this.updateTasks = new LinkedList();
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
            synchronized ( updateTasks )
            {
                while ( updateTasks.isEmpty() )
                {
                    try
                    {
                        updateTasks.wait();
                    }
                    catch ( InterruptedException ie )
                    {
                        // don't care
                    }
                }

                task = ( Runnable ) updateTasks.removeFirst();
            }

            // return if the task is this thread itself
            if ( task == this )
            {
                return;
            }

            // otherwise execute the task, log any issues
            try
            {
                task.run();
            }
            catch ( Throwable t )
            {
                configurationManager.log( LogService.LOG_ERROR, "Unexpected problem executing task", t );
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
    void schedule( Runnable update )
    {
        synchronized ( updateTasks )
        {
            // append to the task queue
            updateTasks.add( update );
            
            // notify the waiting thread
            updateTasks.notifyAll();
        }
    }
}
