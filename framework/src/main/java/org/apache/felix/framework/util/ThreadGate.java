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
    private Object m_msg = null;
    private boolean m_initialized = false;

    /**
     * Open the gate and release any waiting threads.
    **/
    public synchronized void open()
    {
        m_open = true;
        notifyAll();
    }

    /**
     * Returns the message object associated with the gate; the
     * message is just an arbitrary object used to pass information
     * to the waiting threads.
     * @return the message object associated with the gate.
    **/
    public synchronized Object getMessage()
    {
        return m_msg;
    }

    /**
     * Sets the message object associated with the gate. The message
     * object can only be set once, subsequent calls to this method
     * are ignored.
     * @param msg the message object to associate with this gate.
    **/
    public synchronized void setMessage(Object msg)
    {
        if (!m_initialized)
        {
            m_msg = msg;
            m_initialized = true;
        }
    }

    /**
     * Wait for the gate to open.
     * @return <tt>true</tt> if the gate was opened or <tt>false</tt> if the timeout expired.
     * @throws java.lang.InterruptedException If the calling thread is interrupted;
     *         the gate still remains closed until opened.
    **/
    public synchronized boolean await(long timeout) throws InterruptedException
    {
        long start = System.currentTimeMillis();
        long remaining = timeout;
        while (!m_open)
        {
            wait(remaining);
            if (timeout > 0)
            {
                remaining = timeout - (System.currentTimeMillis() - start);
                if (remaining <= 0)
                {
                    break;
                }
            }
        }
        return m_open;
    }
}