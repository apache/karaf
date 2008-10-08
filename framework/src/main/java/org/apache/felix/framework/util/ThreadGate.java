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
package org.apache.felix.framework.util;

/**
 * This class implements a simple one-shot gate for threads. The gate
 * starts closed and will block any threads that try to wait on it. Once
 * opened, all waiting threads will be released. The gate cannot be reused.
**/
public class ThreadGate
{
    private boolean m_open = false;

    /**
     * Open the gate and release any waiting threads.
    **/
    public synchronized void open()
    {
        m_open = true;
        notifyAll();
    }

    /**
     * Wait for the gate to open.
     * @throws java.lang.InterruptedException If the calling thread is interrupted;
     *         the gate still remains closed until opened.
    **/
    public synchronized void await(long timeout) throws InterruptedException
    {
        while (!m_open)
        {
            wait(timeout);
        }
    }
}