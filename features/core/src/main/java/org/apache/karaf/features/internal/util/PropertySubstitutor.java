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
package org.apache.karaf.features.internal.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Substitutes <code>${...}</code> placeholders in a string.
 *
 * <p>System properties take precedence over the supplied properties. Placeholders that can't be
 * resolved are left untouched, so callers can detect and report them. Nesting
 * (<code>${a${b}}</code>) is supported.</p>
 */
public final class PropertySubstitutor {

    private static final String MARKER = "${";

    private PropertySubstitutor() { }

    /**
     * Substitute the placeholders found in <code>value</code>.
     *
     * @param properties the properties to resolve placeholders against.
     * @param value the value to substitute, may be <code>null</code>.
     * @return the substituted value.
     */
    public static String substitute(Properties properties, String value) {
        if (value == null || value.indexOf('$') < 0 || !value.contains(MARKER)) {
            return value;
        }
        Deque<String> stack = new ArrayDeque<>();
        StringTokenizer tokenizer = new StringTokenizer(value, "${}", true);
        while (tokenizer.hasMoreTokens()) {
            processToken(tokenizer.nextToken(), stack, properties);
        }
        StringBuilder result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.insert(0, stack.pop());
        }
        return result.toString();
    }

    private static void processToken(String token, Deque<String> stack, Properties properties) {
        if ("}".equals(token)) {
            if (stack.size() < 2) {
                // unbalanced closing brace - keep it verbatim
                push(stack, token);
                return;
            }
            String name = stack.pop();
            String marker = stack.pop();
            if (MARKER.equals(marker)) {
                startProperty(name, properties, stack);
            } else {
                push(stack, marker);
                push(stack, MARKER + name + "}");
            }
        } else if ("$".equals(token)) {
            stack.push(token);
        } else {
            push(stack, token);
        }
    }

    private static void startProperty(String name, Properties properties, Deque<String> stack) {
        String value = System.getProperty(name);
        if (value == null && properties != null) {
            value = properties.getProperty(name);
        }
        push(stack, value == null ? MARKER + name + "}" : value);
    }

    private static void push(Deque<String> stack, String value) {
        if (stack.isEmpty()) {
            stack.push(value);
            return;
        }
        String top = stack.pop();
        if (MARKER.equals(top)) {
            stack.push(top);
            stack.push(value);
        } else {
            stack.push(top + value);
        }
    }

}
