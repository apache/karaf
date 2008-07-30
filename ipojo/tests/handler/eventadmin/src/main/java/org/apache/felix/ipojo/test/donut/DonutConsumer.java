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
package org.apache.felix.ipojo.test.donut;

import org.osgi.service.event.Event;

/**
 * Specification of a donut consumer.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface DonutConsumer {

    /**
     * Donut receiver callback. This method is called when a donut is received
     * on the listened topic.
     * 
     * @param donut
     *            the received donut
     */
    void receiveDonut(Donut donut);

    /**
     * Event donut receiver callback. This method is called when an event is
     * received on the listened topic.
     * 
     * @param event
     *            the received event
     */
    void receiveEvent(Event event);

    /**
     * Clear the eaten donuts list. (Useful before tests)
     */
    void clearDonuts();

    /**
     * Get the first received donut and remove it from the eaten donut list.
     * 
     * @return the first received donut or null if no donut is available
     */
    Donut getDonut();

    /**
     * Get the whole list of eaten donuts.
     * 
     * @return the array containing all eaten donuts
     */
    Donut[] getAllDonuts();

    /**
     * Get the first donut if available or wait for an incoming donut. The
     * returned donut is removed from the eaten donut list.
     * 
     * @return the first available donut.
     */
    Donut waitForDonut();

    /**
     * Return the size of the eaten donut list.
     * 
     * @return the size of the eaten donut list
     */
    int getNumberOfDonuts();
}
