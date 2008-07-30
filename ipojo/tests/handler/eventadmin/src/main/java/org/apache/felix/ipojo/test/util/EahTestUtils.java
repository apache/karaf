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
package org.apache.felix.ipojo.test.util;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.test.donut.Donut;
import org.osgi.framework.BundleContext;

/**
 * Useful variables used for the tests of the Event Admin Handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EahTestUtils {

    /**
     * Enable debug messages ?
     */
    public static final boolean TRACE = false;

    /**
     * The number of tests to execute.
     */
    public static final int NUMBER_OF_TESTS = 50;

    /**
     * The time that is normally necessary to cause a blacklist from the event
     * admin service.
     */
    public static final long BLACK_LIST_TIME = 5000L;
    
    /**
     * The long amount of time.
     */
    public static final long A_LONG_TIME = 1000L;

    /**
     * The bundle context.
     */
    private BundleContext m_context;

    /**
     * Construct a new Event Admin Handler Tests utility instance.
     * 
     * @param context
     *            the bundle context
     */
    public EahTestUtils(BundleContext context) {
        m_context = context;
    }

    /**
     * Return the (asynchronous) donut provider factory.
     * 
     * @return the (asynchronous) donut provider factory
     */
    public Factory getDonutProviderFactory() {
        return IPojoTestUtils.getFactoryByName(m_context, "donut-provider");
    }

    /**
     * Return the synchronous donut provider factory.
     * 
     * @return the synchronous donut provider factory
     */
    public Factory getSynchronousDonutProviderFactory() {
        return IPojoTestUtils.getFactoryByName(m_context,
                "synchronous-donut-provider");
    }

    /**
     * Return the (asynchronous) donut event provider factory.
     * 
     * @return the (asynchronous) donut event provider factory
     */
    public Factory getDonutEventProviderFactory() {
        return IPojoTestUtils.getFactoryByName(m_context,
                "donut-event-provider");
    }

    /**
     * Return the synchronous donut event provider factory.
     * 
     * @return the synchronous donut event provider factory
     */
    public Factory getSynchronousDonutEventProviderFactory() {
        return IPojoTestUtils.getFactoryByName(m_context,
                "synchronous-donut-event-provider");
    }

    /**
     * Return the event provider factory.
     * 
     * @return the event provider factory
     */
    public Factory getEventProviderFactory() {
        return IPojoTestUtils.getFactoryByName(m_context, "event-provider");
    }

    /**
     * Return the synchronous event provider factory.
     * 
     * @return the synchronous event provider factory
     */
    public Factory getSynchronousEventProviderFactory() {
        return IPojoTestUtils.getFactoryByName(m_context,
                "synchronous-event-provider");
    }

    /**
     * Return the donut consumer factory.
     * 
     * @return the donut consumer factory
     */
    public Factory getDonutConsumerFactory() {
        return IPojoTestUtils.getFactoryByName(m_context, "donut-consumer");
    }

    /**
     * Return the donut event consumer factory.
     * 
     * @return the donut event consumer factory
     */
    public Factory getDonutEventConsumerFactory() {
        return IPojoTestUtils.getFactoryByName(m_context,
                "donut-event-consumer");
    }

    /**
     * Return the event consumer factory.
     * 
     * @return the event consumer factory
     */
    public Factory getEventConsumerFactory() {
        return IPojoTestUtils.getFactoryByName(m_context, "event-consumer");
    }

    /**
     * Return the event tracker.
     * 
     * @return the event consumer factory
     */
    public Factory getEventTrackerFactory() {
        return IPojoTestUtils.getFactoryByName(m_context, "event-tracker");
    }

    /**
     * Utility method that causes the current thread to sleep.
     * 
     * @param millis
     *            the number of milliseconds to wait
     */
    public static void sleep(long millis) {
        long past = System.currentTimeMillis();
        long future = past + millis;
        long now = past;
        if (TRACE) {
            System.err.println("Sleeping for " + millis + "ms");
        }
        while (now < future) {
            try {
                Thread.sleep(future - now);
            } catch (Exception e) {
            }
            now = System.currentTimeMillis();
        }
    }

    /**
     * Return the index of the given donut flavour in the flavour array.
     * 
     * @return the index of the given flavour or -1 if not found
     */
    public static int flavourIndex(String flavour) {
        for (int i = 0; i < Donut.FLAVOURS.length; i++) {
            if (Donut.FLAVOURS[i].equals(flavour))
                return i;
        }
        return -1;
    }
}
