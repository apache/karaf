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
package org.apache.felix.ipojo.parser;

import java.util.StringTokenizer;

/**
 * Parse Utility Methods.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ParseUtils {

    /**
     * Parse the string form of an array as {a, b, c}.
     * 
     * @param str : the string form
     * @return the resulting string array
     */
    public static String[] parseArrays(String str) {
        // Remove { and }
        if (str.startsWith("{") && str.endsWith("}")) {
            String m = str.substring(1, str.length() - 1);
            // Check empty array
            m = m.trim();
            if (m.length() == 0) {
                return new String[0];
            }
            String[] values = split(m, ",");
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].trim();
            }
            return values;
        } else {
            return new String[] { str };
        }
    }
    
    /**
     * Split method. 
     * This method is equivalent of the String.split in java 1.4
     * @param toSplit : String to split
     * @param separator : separator
     * @return the token array 
     */
    public static String[] split(String toSplit, String separator) {
        StringTokenizer st = new StringTokenizer(toSplit, separator);
        String[] result = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreElements()) {
            result[i] = st.nextToken();
            i++;
        }
        return result;
    }
}
