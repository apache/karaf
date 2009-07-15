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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    public static MapBuilder map(String name, Object value) {
        return new MapBuilder().put(name, value);
    }

    public static String toString(Map<String, Object> attrs) {
        if (attrs == null) {
            return "NULL";
        }

        StringBuffer buf = new StringBuffer(128);
        List<String> keys = new ArrayList<String>(attrs.keySet());
        Collections.sort(keys);
        buf.append("{");

        for (int i = 0; i < keys.size(); i++) {
            Object name = keys.get(i);
            Object value = attrs.get(name);
            buf.append(name).append("=").append(value).append(",");
        }

        if (buf.length() > 1) {
            buf.delete(buf.length() - 1, buf.length());
        }

        buf.append("}");

        return buf.toString();
    }

    public static class MapBuilder {
        private Map<String, Object> map = new HashMap<String, Object>();

        public MapBuilder put(String name, Object value) {
            map.put(name, value);

            return this;
        }

        public Map<String, Object> toMap() {
            return map;
        }
    }
}
