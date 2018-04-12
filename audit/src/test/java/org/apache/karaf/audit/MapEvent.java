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
package org.apache.karaf.audit;

import javax.security.auth.Subject;
import java.util.Map;

public class MapEvent implements Event {

    final long timestamp;
    final Map<String, Object> map;

    public MapEvent(Map<String, Object> map) {
        this.map = map;
        this.timestamp = System.currentTimeMillis();
    }

    public MapEvent(Map<String, Object> map, long timestamp) {
        this.map = map;
        this.timestamp = timestamp;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public Subject subject() {
        return null;
    }

    @Override
    public String type() {
        return (String) map.get("type");
    }

    @Override
    public String subtype() {
        return (String) map.get("subtype");
    }

    @Override
    public Iterable<String> keys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String key) {
        return map.get(key);
    }
}
