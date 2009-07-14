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

package org.apache.felix.sigil.model.common;

import java.io.Serializable;

/**
 * Immutable class representing cardinality constraints between two entities.
 * 
 */
public class Cardinality implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final Cardinality ZERO_TO_MANY = new Cardinality(0, -1);
    public static final Cardinality ONE_TO_MANY = new Cardinality(1, -1);
    public static final Cardinality ZERO_TO_ONE = new Cardinality(0, 1);
    public static final Cardinality ONE_TO_ONE = new Cardinality(1, 1);

    private int min;
    private int max;

    /**
     * @param min
     *            >=0 (usually 0 or 1)
     * @param max
     *            >=min or -1 to indicate an unbounded maximum
     */
    public Cardinality(int min, int max) {
        if (min < 0) {
            throw new IllegalArgumentException("Min cannot be less than 0");
        }

        if ((max < min) && (max != -1)) {
            throw new IllegalArgumentException("Max cannot be less than min");
        }

        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public String toString() {
        return min + ".." + ((max == -1) ? ("n") : (Integer.toString(max)));
    }

    public boolean isDefined(Cardinality cardinality) {
        return (min <= cardinality.min) && ((max == -1) || (max >= cardinality.max));
    }

    public boolean isSingleton() {
        return (min == 1) && (max == 1);
    }

    public static Cardinality parse(String stringRep) throws IllegalArgumentException {
        stringRep = stringRep.trim();

        int dotdot = stringRep.indexOf("..");

        if (dotdot == -1) {
            throw new IllegalArgumentException("Invalid cardinality string representation, expected ..");
        }

        String minStr = stringRep.substring(0, dotdot);
        String maxStr = stringRep.substring(dotdot + 2);

        int min = Integer.parseInt(minStr);
        int max = min;

        if ("n".equals(maxStr)) {
            max = -1;
        }
        else {
            max = Integer.parseInt(maxStr);
        }

        return cardinality(min, max);
    }

    public static Cardinality cardinality(int min, int max) {
        Cardinality c = null;

        if (min == 0) {
            if (max == 1) {
                c = ZERO_TO_ONE;
            }
            else if (max == -1) {
                c = ZERO_TO_MANY;
            }
        }
        else if (min == 1) {
            if (max == 1) {
                c = ONE_TO_ONE;
            }
            else if (max == -1) {
                c = ONE_TO_MANY;
            }
        }

        if (c == null)
            c = new Cardinality(min, max);

        return c;
    }

    public int hashCode() {
        return max ^ min;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        try {
            Cardinality c = (Cardinality) o;

            return (min == c.min) && (max == c.max);
        }
        catch (ClassCastException cce) {
            return false;
        }
    }
}
