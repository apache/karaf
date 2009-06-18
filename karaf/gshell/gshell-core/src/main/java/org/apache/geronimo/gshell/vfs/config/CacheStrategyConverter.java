/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.gshell.vfs.config;

import org.osgi.service.blueprint.container.Converter;
import org.apache.commons.vfs.CacheStrategy;

public class CacheStrategyConverter implements Converter {

    public boolean canConvert(Object o, Class aClass) {
        return o instanceof String && aClass == CacheStrategy.class;
    }

    public Object convert(Object o, Class aClass) throws Exception {
        if (canConvert(o, aClass)) {
            String text = o.toString();
            if (text.equalsIgnoreCase("MANUAL")) {
                return CacheStrategy.MANUAL;
            }
            else if (text.equalsIgnoreCase("ON_RESOLVE")) {
                return CacheStrategy.ON_RESOLVE;
            }
            else if (text.equalsIgnoreCase("ON_CALL")) {
                return CacheStrategy.ON_CALL;
            }
            else {
                throw new IllegalArgumentException("Unknown cache strategy: " + text);
            }
        }
        return null;
    }

}
