/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.shell.support.table;

class StringUtil {

    /**
     * Returns length of the string.
     * 
     * @param string String.
     * @return Length.
     */
    public static int length(String string) {
        return string == null ? 0 : string.length();
    }

    /**
     * Utility method to repeat string.
     * 
     * @param string String to repeat.
     * @param times Number of times.
     * @return Repeat string.
     */
    public static String repeat(String string, int times) {
        if (times <= 0) {
            return "";
        }
        else if (times % 2 == 0) {
            return repeat(string+string, times/2);
        }
        else {
           return string + repeat(string+string, times/2);
        }
    }
}
