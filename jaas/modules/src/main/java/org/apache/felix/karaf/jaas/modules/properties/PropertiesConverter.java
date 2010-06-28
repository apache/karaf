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
package org.apache.felix.karaf.jaas.modules.properties;

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

import java.util.Properties;

/**
 * Custom converter to transform a string into a Properties instance.
 * (to avoid removing \ from the values as is done by the default blueprint converter)
 */
public class PropertiesConverter implements Converter {

    public boolean canConvert(Object from, ReifiedType type) {
        return String.class.isAssignableFrom(from.getClass())
                && Properties.class.equals(type.getRawClass());
    }

    public Object convert(Object from, ReifiedType type) throws Exception {
        Properties properties = new Properties();

        String text = (String) from;
        for (String line : text.split("[\\r\\n]+")) {
            int index = line.indexOf('=');
            if (index > 0) {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();
                properties.put(key, value.replaceAll("\\\\", "/"));
            }
        }

        return properties;
    }
}
