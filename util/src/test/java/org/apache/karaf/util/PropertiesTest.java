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
package org.apache.karaf.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

public class PropertiesTest extends TestCase {

    private final static String TEST_PROPERTIES_FILE = "test.properties";
    
    private Properties properties;
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream(TEST_PROPERTIES_FILE));
    }
    
    /**
     * <p>
     * Test getting property.
     * </p>
     * 
     * @throws Exception
     */
    public void testGettingProperty() throws Exception {
        assertEquals("test", properties.get("test"));
    }
    
    public void testLoadSave() throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("# ");
        pw.println("# The Main  ");
        pw.println("# ");
        pw.println("# Comment ");
        pw.println("# ");
        pw.println("");
        pw.println("# Another comment");
        pw.println("");
        pw.println("# A value comment");
        pw.println("key1 = val1");
        pw.println("");
        pw.println("# Another value comment");
        pw.println("key2 = ${key1}/foo");
        pw.println("");
        pw.println("# A third comment");
        pw.println("key3 = val3");
        pw.println("");


        Properties props = new Properties();
        props.load(new StringReader(sw.toString()));
        props.save(System.err);
        System.err.println("=====");

        props.put("key2", props.get("key2"));
        props.put("key3", "foo");
        props.save(System.err);
        System.err.println("=====");
    }
}
