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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

import org.apache.felix.ipojo.handlers.event.publisher.Publisher;
import org.apache.felix.ipojo.test.util.EahTestUtils;

/**
 * Implementation of a donut vendor that send raw events instead of sending
 * Donut objects.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * 
 */
public class DonutEventProviderImpl implements DonutProvider {

    /**
     * The donut current serial number.
     */
    private long m_serial = 0L;

    /**
     * The name of the donut vendor.
     */
    private String m_name;

    /**
     * A random generator.
     */
    private Random m_random;

    /**
     * The event publisher of the donut vendor.
     */
    private Publisher m_publisher;

    /**
     * Construct a new donut provider. The initial serial number is randomly
     * generated.
     */
    public DonutEventProviderImpl() {
        m_random = new Random(System.currentTimeMillis());
    }

    /**
     * Sell a donut with a random flavour.
     * 
     * @return the sold donut
     */
    public Donut sellDonut() {
        Dictionary rawEvent = new Hashtable();
        String flavour = Donut.FLAVOURS[m_random.nextInt(Donut.FLAVOURS.length)];
        Donut donut = new Donut(m_serial++, m_name, flavour);
        rawEvent.put("food", donut);
        rawEvent.put("flavour", flavour);
        m_publisher.send(rawEvent);
        if (EahTestUtils.TRACE) {
            System.err.println("[" + this.getClass().getSimpleName() + ":"
                    + m_name + "] Selling event donut " + donut);
        }
        return donut;
    }
}
