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
package org.apache.felix.cm.integration.helper;


import junit.framework.TestCase;


/**
 * The <code>UpdateThreadSignalTask</code> class is a special task used by the
 * {@link org.apache.felix.cm.integration.ConfigurationTestBase#delay} method.
 * <p>
 * This task is intended to be added to the update thread schedule and signals
 * to the tests that all current tasks on the queue have terminated and tests
 * may continue checking results.
 */
public class UpdateThreadSignalTask implements Runnable
{

    private final Object trigger = new Object();

    private volatile boolean signal;


    public void run()
    {
        synchronized ( trigger )
        {
            signal = true;
            trigger.notifyAll();
        }
    }


    public void waitSignal()
    {
        synchronized ( trigger )
        {
            if ( !signal )
            {
                try
                {
                    trigger.wait( 10 * 1000L ); // seconds
                }
                catch ( InterruptedException ie )
                {
                    // sowhat ??
                }
            }

            if ( !signal )
            {
                TestCase.fail( "Timed out waiting for the queue to keep up" );
            }
        }
    }


    @Override
    public String toString()
    {
        return "Update Thread Signal Task";
    }
}
