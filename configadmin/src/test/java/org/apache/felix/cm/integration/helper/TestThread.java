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


/**
 * The <code>TestThread</code> class is a base helper class for the
 * {@link org.apache.felix.cm.integration.ConfigUpdateStressTest}. It implements
 * basic mechanics to be able to run two task at quasi the same time.
 * <p>
 * It is not important to have exact timings because running the tests multiple
 * times and based on low-level Java VM timings thread execution will in the end
 * be more or less random.
 */
abstract class TestThread extends Thread
{
    private final Object flag = new Object();

    private volatile boolean notified;


    @Override
    public void run()
    {
        synchronized ( flag )
        {
            if ( !notified )
            {
                try
                {
                    flag.wait();
                }
                catch ( InterruptedException ie )
                {
                    // TODO: log
                }
            }
        }

        doRun();
    }


    protected abstract void doRun();


    public abstract void cleanup();


    public void trigger()
    {
        synchronized ( flag )
        {
            notified = true;
            flag.notifyAll();
        }
    }
}