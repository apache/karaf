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
 * Provides an atomic integer.
 */
class AtomicInteger
{
    private int m_value;

    public AtomicInteger()
    {
        m_value = 0;
    }//constructor

    /**
     * Constructs a new <tt>AtomicInteger</tt>
     * with a given initial value.
     *
     * @param value the initial value.
     */
    public AtomicInteger(int value)
    {
        m_value = value;
    }//constructor

    /**
     * Increments this <tt>AtomicInteger</tt> by one.
     *
     * @return the resulting value.
     */
    public synchronized int increment()
    {
        return ++m_value;
    }//increment

    /**
     * Decrements this <tt>AtomicInteger</tt> by one.
     *
     * @return the resulting value.
     */
    public synchronized int decrement()
    {
        return --m_value;
    }//decrement

    /**
     * Sets the value of this <tt>AtomicInteger</tt>.
     *
     * @param i the new value.
     */
    public synchronized void set(int i)
    {
        m_value = i;
    }//set

    /**
     * Returns the value of this <tt>AtomicInteger</tt>.
     *
     * @return the actual value.
     */
    public synchronized int get()
    {
        return m_value;
    }//get
}//class AtomicInteger
