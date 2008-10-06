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
package org.apache.felix.ipojo.handlers.event;

/**
 * Utility methods.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventUtil {

    /**
     * Tests that the given topic match with the given topic pattern.
     * 
     * @param topic the topic to test
     * @param topicPattern the topic pattern
     * @return true if it matches.
     */
    public static boolean matches(String topic, String topicPattern) {
        if (topicPattern.equals("*")) {
            return true;
        }
        int star;
        if ((star = topicPattern.indexOf("*")) > 0) {
            return topic.startsWith(topicPattern.substring(0, star - 1));
        } else {
            return topic.equals(topicPattern);
        }
    }

    /**
     * Tests that the given topic match with the given topic patterns.
     * 
     * @param topic the topic to test
     * @param topicPatterns the topic patterns
     * @return true if it matches.
     */
    public static boolean matches(String topic, String[] topicPatterns) {
        int n = topicPatterns.length;
        for (int i = 0; i < n; i++) {
            if (matches(topic, topicPatterns[i])) {
                return true;
            }
        }
        return false;
    }

}
