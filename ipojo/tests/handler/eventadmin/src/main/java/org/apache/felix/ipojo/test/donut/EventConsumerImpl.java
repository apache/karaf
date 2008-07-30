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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.test.util.EahTestUtils;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Implementation of a donut consumer.
 * 
 * @see Homer Simpson
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventConsumerImpl implements DonutConsumer, EventHandler {

    /**
     * The name of the donut consumer.
     */
    private String m_name;

    /**
     * The list of eaten donuts.
     */
    private List m_donuts = new ArrayList();

    /**
     * Is this consumer a slow eater ?
     */
    private boolean m_isSlow;

    /**
     * Process incoming donuts. This method is called by the receiveDonut
     * callback.
     * 
     * @param donut
     *            the received donut
     */
    private void doReceiveDonut(Donut donut) {
        synchronized (m_donuts) {
            m_donuts.add(donut);
            m_donuts.notify();
            if (EahTestUtils.TRACE) {
                System.err.println("[" + this.getClass().getSimpleName() + ":"
                        + m_name + "] Eating donut " + donut);
            }
        }
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
        while (now < future) {
            try {
                Thread.sleep(future - now);
            } catch (Exception e) {
            }
            now = System.currentTimeMillis();
        }
    }

    /**
     * Donut receiver callback. This method is called when a donut is received
     * on the listened topic.
     * 
     * @param donut
     *            the received donut
     */
    public void receiveDonut(Donut donut) {
        final Donut myDonut = donut;
        if (m_isSlow) {
            new Thread(new Runnable() {
                public void run() {
                    sleep(EahTestUtils.BLACK_LIST_TIME);
                    doReceiveDonut(myDonut);
                }
            }, m_name + " eating " + donut).start();
        } else {
            doReceiveDonut(donut);
        }
    }

    /**
     * Event donut receiver callback. This method is called when an event is
     * received on the listened topic.
     * 
     * @param event
     *            the received event
     */
    public void receiveEvent(Event event) {
        Object thing = event.getProperty("food");
        if (Donut.class.isInstance(thing)) {
            receiveDonut((Donut) thing);
        } else {
            if (EahTestUtils.TRACE) {
                System.err.println("[" + this.getClass().getSimpleName() + ":"
                        + m_name + "] D'oh ! Received an uneatable thing : "
                        + thing);
                throw new ClassCastException("I want DONUTS !");
            }
        }
    }

    /**
     * Event receiver callback. This method is called by the event admin service
     * when a event is received.
     * 
     * @param event
     *            the received event
     */
    public void handleEvent(Event event) {
        receiveEvent(event);
    }

    /**
     * Clear the eaten donuts list. (Useful before tests)
     */
    public void clearDonuts() {
        synchronized (m_donuts) {
            m_donuts.clear();
        }
    }

    /**
     * Get the first received donut and remove it from the eaten donut list.
     * 
     * @return the first received donut or null if no donut is available
     */
    public Donut getDonut() {
        Donut donut = null;
        synchronized (m_donuts) {
            if (!m_donuts.isEmpty()) {
                donut = (Donut) m_donuts.remove(0);
            }
        }
        return donut;
    }

    /**
     * Get the whole list of eaten donuts.
     * 
     * @return the array containing all eaten donuts
     */
    public Donut[] getAllDonuts() {
        Donut[] donuts = new Donut[0];
        synchronized (m_donuts) {
            donuts = (Donut[]) m_donuts.toArray(donuts);
            m_donuts.clear();
        }
        return donuts;
    }

    /**
     * Get the first donut if available or wait for an incoming donut. The
     * returned donut is removed from the eaten donut list.
     * 
     * @return the first available donut.
     */
    public Donut waitForDonut() {
        Donut donut = null;
        synchronized (m_donuts) {
            while (donut == null) {
                if (m_donuts.isEmpty()) {
                    try {
                        m_donuts.wait();
                    } catch (InterruptedException e) {
                        // Thanks Checkstyle to forbid empty catch statements
                        // ;-(
                    }
                } else {
                    donut = (Donut) m_donuts.remove(0);
                }
            }
        }
        return donut;
    }

    /**
     * Return the size of the eaten donut list.
     * 
     * @return the size of the eaten donut list
     */
    public int getNumberOfDonuts() {
        int length;
        synchronized (m_donuts) {
            length = m_donuts.size();
        }
        return length;
    }
}
