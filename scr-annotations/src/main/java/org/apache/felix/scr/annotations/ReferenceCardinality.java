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
package org.apache.felix.scr.annotations;

/**
 * Options for {@link Reference#cardinality()} property.
 */
public enum ReferenceCardinality {

    /**
     * Optional, unary reference: No service required to be available for the
     * refernce to be satisfied. Only a single service is available through this
     * reference.
     */
    OPTIONAL_UNARY("0..1"),

    /**
     * Mandatory, unary reference: At least one service must be available for
     * the reference to be satisfied. Only a single service is available through
     * this reference.
     */
    MANDATORY_UNARY("1..1"),

    /**
     * Optional, multiple reference: No service required to be available for the
     * refernce to be satisfied. All matching services are available through
     * this reference.
     */
    OPTIONAL_MULTIPLE("0..n"),

    /**
     * Mandatory, multiple reference: At least one service must be available for
     * the reference to be satisified. All matching services are available
     * through this reference.
     */
    MANDATORY_MULTIPLE("1..n");

    private final String cardinalityString;

    private ReferenceCardinality(final String cardinalityString) {
        this.cardinalityString = cardinalityString;
    }

    /**
     * @return String representation of cardinality
     */
    public String getCardinalityString() {
        return this.cardinalityString;
    }

}
