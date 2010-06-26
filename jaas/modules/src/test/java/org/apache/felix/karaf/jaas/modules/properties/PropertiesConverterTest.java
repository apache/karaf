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

import junit.framework.TestCase;
import org.osgi.service.blueprint.container.ReifiedType;

import java.util.Properties;
import java.util.List;

/**
 * Test cases for {@link org.apache.felix.karaf.jaas.modules.properties.PropertiesConverter}
 */
public class PropertiesConverterTest extends TestCase {

    private PropertiesConverter converter;

    public void setUp() {
        converter = new PropertiesConverter();
    }

    /*
     * Test the canConvert method
     */
    public void testCanConvert() {
        assertTrue(converter.canConvert("a string", new ReifiedType(Properties.class)));
        assertFalse(converter.canConvert(new Object(), new ReifiedType(Properties.class)));
        assertFalse(converter.canConvert("a string", new ReifiedType(List.class)));
    }

    /*
     * Test the convert method when dealing with unix paths (no \)
     */
    public void testConvertWithUnixPathNames() throws Exception {
        Properties properties =
                (Properties) converter.convert("users = /opt/karaf/etc/users.properties",
                        new ReifiedType(Properties.class));
        assertNotNull(properties);
        assertEquals("/opt/karaf/etc/users.properties", properties.get("users"));
    }

    /*
     * Test the convert method when dealing with windows paths (avoid escaping \)
     */
    public void testConvertWithWindowsPathNames() throws Exception {
        Properties properties =
                (Properties) converter.convert("users = c:\\opt\\karaf/etc/users.properties",
                        new ReifiedType(Properties.class));
        assertNotNull(properties);
        assertEquals("c:/opt/karaf/etc/users.properties", properties.get("users"));
    }
}
