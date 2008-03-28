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
package org.apache.felix.ipojo.junit4osgi;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;

/**
 * OSGi Test Case. Allow the injection of the bundle context.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class OSGiTestCase extends TestCase {

    protected BundleContext context;

    public void setBundleContext(BundleContext bc) {
        context = bc;
    }

    /**
     * Asserts that two doubles are equal. If they are not an AssertionFailedError is thrown with the given message.
     */
    static public void assertEquals(String message, double expected, double actual) {
        if (Double.compare(expected, actual) != 0) {
            fail(formatEqualsMessage(message, expected, actual));
        }
    }

    static String formatEqualsMessage(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }
        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }

    static String formatNotEqualsMessage(String message, Object o1, Object o2) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }
        return formatted + "o1:<" + o1 + "> is equals to o2:<" + o2 + ">";
    }

    static String formatContainsMessage(String message, Object[] array, Object txt) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }

        String arr = null;
        for (int i = 0; i < array.length; i++) {
            if (arr == null) {
                arr = "[" + array[i];
            } else {
                arr += "," + array[i];
            }
        }
        arr += "]";

        return formatted + "array:" + arr + " does not contains:<" + txt + ">";
    }

    static public void assertNotEquals(String message, Object o1, Object o2) {
        if (o1.equals(o2)) {
            fail(formatNotEqualsMessage(message, o1, o2));
        }
    }

    static public void assertContains(String message, String[] array, String txt) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(txt)) {
                return;
            }
        }
        fail(formatContainsMessage(message, array, txt));
    }

    static public void assertContains(String message, byte[] array, int txt) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == txt) {
                return;
            }
        }
        Byte[] bytes = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = new Byte(array[i]);
        }
        fail(formatContainsMessage(message, bytes, txt));
    }

    static public void assertContains(String message, short[] array, int txt) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == txt) {
                return;
            }
        }
        Short[] bytes = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = new Short(array[i]);
        }
        fail(formatContainsMessage(message, bytes, txt));
    }

    static public void assertContains(String message, int[] array, int txt) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == txt) {
                return;
            }
        }
        Integer[] bytes = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = new Integer(array[i]);
        }
        fail(formatContainsMessage(message, bytes, txt));
    }

    static public void assertContains(String message, long[] array, long txt) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == txt) {
                return;
            }
        }
        Long[] bytes = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = new Long(array[i]);
        }
        fail(formatContainsMessage(message, bytes, txt));
    }

    static public void assertContains(String message, float[] array, float txt) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == txt) {
                return;
            }
        }
        Float[] bytes = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = new Float(array[i]);
        }
        fail(formatContainsMessage(message, bytes, txt));
    }

    static public void assertContains(String message, double[] array, double txt) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == txt) {
                return;
            }
        }
        Double[] bytes = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = new Double(array[i]);
        }
        fail(formatContainsMessage(message, bytes, txt));
    }

    static public void assertContains(String message, char[] array, char txt) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == txt) {
                return;
            }
        }
        Character[] bytes = new Character[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = new Character(array[i]);
        }
        fail(formatContainsMessage(message, bytes, txt));
    }

}
