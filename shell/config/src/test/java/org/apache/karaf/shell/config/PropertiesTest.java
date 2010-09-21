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
package org.apache.karaf.shell.config;

import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

public class PropertiesTest extends TestCase {

    public void testLoadSave() throws IOException {
        URL url = getClass().getClassLoader().getResource("OSGI-INF/metatype/metatype.properties");
        Properties props = new Properties();
        props.load(url);
        props.save(System.err);
        System.err.println("=====");

        props.put("storage.name", "foo bar");
        props.save(System.err);
        System.err.println("=====");
    }
}
