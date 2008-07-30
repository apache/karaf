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

/**
 * Donut representation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Donut {

    /**
     * All possible donut flavours.
     */
    public static final String[] FLAVOURS = { "unflavoured", "icing sugar",
        "chocolate", "toffee", "strawberry", "apple" };

    /**
     * The vendor's unique donut identifier.
     */
    private final long m_id;

    /**
     * The name of this donut's vendor.
     */
    private final String m_vendorName;

    /**
     * The m_flavour of this donut.
     */
    private final String m_flavour;

    /**
     * Create a new donut.
     * 
     * @param id
     *            the vendor's unique donut identifier
     * @param vendorName
     *            the name of this donut's vendor
     * @param flavour
     *            the m_flavour of this donut
     */
    public Donut(long id, String vendorName, String flavour) {
        this.m_id = id;
        this.m_vendorName = vendorName;
        this.m_flavour = flavour;
    }

    /**
     * Get the vendor's unique identifier of this donut.
     * 
     * @return the id
     */
    public long getId() {
        return m_id;
    }

    /**
     * Get the vendor name of this donut.
     * 
     * @return the name
     */
    public String getVendorName() {
        return m_vendorName;
    }

    /**
     * Get the flavour of this donut.
     * 
     * @return the flavour
     */
    public String getFlavour() {
        return m_flavour;
    }

    /**
     * Return the string representation of this donut.
     * 
     * @return this donut as a String
     */
    public String toString() {
        return m_id + " " + m_flavour + " (" + m_vendorName + ")";
    }
}
