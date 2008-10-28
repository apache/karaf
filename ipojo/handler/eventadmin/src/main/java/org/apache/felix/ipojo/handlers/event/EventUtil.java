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

import org.apache.felix.ipojo.parser.ParseUtils;

/**
 * Utility methods.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventUtil {

    /**
     * The separator between topics.
     */
    public static final String TOPIC_SEPARATOR = ",";

    /**
     * The separator between topics.
     */
    public static final String TOPIC_TOKEN_SEPARATOR = "/";

    /**
     * The topic token alphabet.
     */
    private static final String TOKEN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";

    /**
     * Tests that the given topic match with the given topic pattern.
     * 
     * @param topic the topic to test
     * @param topicPattern the topic pattern
     * @return true if it matches.
     */
    public static final boolean matches(String topic, String topicPattern) {
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
    public static final boolean matches(String topic, String[] topicPatterns) {
        int n = topicPatterns.length;
        for (int i = 0; i < n; i++) {
            if (matches(topic, topicPatterns[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check the given topic scope is valid.
     * 
     * topicScope ::= "*" | ( topic "/*" ? )
     * 
     * @param topicScope the topic scope to check.
     * 
     * @return {@code true} if the given topic scope is valid, {@code false}
     *         otherwise.
     */
    public static final boolean isValidTopicScope(String topicScope) {
        if (topicScope.equals("*")) {
            // Wildcard character only accepted.
            return true;
        }

        // Remove trailing "/*" if present, to check the topic radix.
        String topicWithoutWildcard;
        if (topicScope.endsWith("/*")) {
            topicWithoutWildcard = topicScope.substring(0,
                    topicScope.length() - 2);
        } else {
            topicWithoutWildcard = topicScope;
        }

        // Validate the topic radix.
        return isValidTopic(topicWithoutWildcard);
    }

    /**
     * Check the given topic is valid.
     * 
     * topic ::= token ( ’/’ token ) *
     * 
     * @param topic the topic to check.
     * 
     * @return {@code true} if the given topic is valid, {@code false}
     *         otherwise.
     */
    public static final boolean isValidTopic(String topic) {
        if (topic.startsWith(TOPIC_TOKEN_SEPARATOR)
                || topic.endsWith(TOPIC_TOKEN_SEPARATOR)) {
            // A topic cannot start nor end with '/'.
            return false;
        }

        String[] tokens = ParseUtils.split(topic, TOPIC_TOKEN_SEPARATOR);
        if (tokens.length < 1) {
            // A topic must contain at least one token.
            return false;
        }

        // Check each token is valid.
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (!isValidToken(token)) {
                return false;
            }
        }

        // The topic is valid.
        return true;
    }

    /**
     * Check the given token is valid.
     * 
     * token ::= ( alphanum | "_" | "-" )+
     * 
     * @param token the token to check.
     * 
     * @return {@code true} if the given topic token is valid, {@code false}
     *         otherwise.
     */
    private static boolean isValidToken(String token) {
        int length = token.length();
        if (length < 1) {
            // Token must contain at least one character.
            return false;
        }

        for (int i = 0; i < length; i++) {
            // Each character in the token must belong to the token alphabet.
            if (TOKEN_ALPHABET.indexOf(token.charAt(i)) == -1) {
                return false;
            }
        }

        // The token is valid.
        return true;
    }

}
