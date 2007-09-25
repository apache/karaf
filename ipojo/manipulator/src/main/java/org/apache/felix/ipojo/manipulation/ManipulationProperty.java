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
package org.apache.felix.ipojo.manipulation;

/**
 * Store properties for the manipulation process.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * 
 */
public class ManipulationProperty {
    
    /**
     * Logger info level.
     */
    public static final int INFO = 0;
    
    /**
     * Logger warning level. 
     */
    public static final int WARNING = 1;
    
    /**
     * Logger severe level. 
     */
    public static final int SEVERE = 2;
    
    /**
     * Activator internal package name.
     */
    protected static final String IPOJO_INTERNAL_PACKAGE_NAME = "org/apache/felix/ipojo/";

    /**
     * Ipojo internal package name for internal descriptor.
     */
    protected static final String IPOJO_INTERNAL_DESCRIPTOR = "L" + IPOJO_INTERNAL_PACKAGE_NAME;

    /**
     * iPOJO Package name.
     */
    protected static final String IPOJO_PACKAGE_NAME = "org.apache.felix.ipojo";

    /**
     * Helper array for byte code manipulation of primitive type.
     */
    protected static final String[][] PRIMITIVE_BOXING_INFORMATION = new String[][] { 
        {"V", "ILLEGAL", "ILLEGAL"}, 
        {"Z", "java/lang/Boolean", "booleanValue"},
        {"C", "java/lang/Character", "charValue"}, 
        {"B", "java/lang/Byte", "byteValue"}, 
        {"S", "java/lang/Short", "shortValue"}, 
        {"I", "java/lang/Integer", "intValue"},
        {"F", "java/lang/Float", "floatValue"}, 
        {"J", "java/lang/Long", "longValue"}, 
        {"D", "java/lang/Double", "doubleValue"}
    };
    
    /**
     * Internal logger implementation.
     */
    protected static class Logger {
        /**
         * Log method.
         * @param level : level
         * @param message : message to log
         */
        public void log(int level, String message) {
            if (level >= m_logLevel) {
                switch (level) {
                    case INFO:
                        System.err.println("[INFO] " + message);
                        break;
                    case WARNING:
                        System.err.println("[WARNING] " + message);
                        break;
                    case SEVERE:
                        System.err.println("[SEVERE] " + message);
                        break;
                    default:
                        System.err.println("[SEVERE] " + message);
                        break;
                }
            }
        }
    }

    /**
     * Manipulator logger.
     */
    private static Logger m_logger;

    /**
     * Default logger level.
     */
    private static int m_logLevel = WARNING;
    

    /**
     * Get the manipulator logger.
     * @return the logger used by the manipulator.
     */
    public static Logger getLogger() {
        if (m_logger == null) {
            m_logger = new Logger();
        }
        return m_logger;
    }
}
