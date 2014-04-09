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
package org.apache.karaf.service.command;

public class ObjectClassMatcher {
    private ObjectClassMatcher() {
    }
    
    static boolean matchesAtLeastOneName(String[] names, String pattern) {
        for (String objectClass : names) {
            if (matchesName(objectClass, pattern)) {
                return true;
            }
        }
        return false;
    }

    static boolean matchesName(String name, String pattern) {
        return name.equals(pattern) || getShortName(name).equals(pattern);
    }
    
    static String getShortName(String name) {
        int idx = name.lastIndexOf(".");
        if (idx + 1 > name.length()) {
            idx = 0;
        }
        return name.substring(idx + 1);
    }
}
