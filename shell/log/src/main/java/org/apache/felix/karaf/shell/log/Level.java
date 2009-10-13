/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.shell.log;

/**
 * Enumeration of available log levels for the log:set command and
 * the command completer
 */
public enum Level {

    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    DEFAULT;
    
    /**
     * Convert the list of values into a String array
     * 
     * @return all the values as a String array
     */
    public static String[] strings() {
        String[] values = new String[values().length];
        for (int i = 0 ; i < values.length ; i++) {
            values[i] = values()[i].name();
        }
        return values;
    }
    
    /**
     * Check if the string value represents the default level
     * 
     * @param level the level value
     * @return <code>true</code> if the value represents the {@link #DEFAULT} level
     */
    public static boolean isDefault(String level) {
        return valueOf(level).equals(DEFAULT);
    }
}
