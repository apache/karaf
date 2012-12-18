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

import junit.framework.TestCase;

public class DeployerUtilsTest extends TestCase {

    public void testVersions() {
        assertVersion("org.apache.servicemix.bundles.ant-1.7.0-1.0-m3-SNAPSHOT.jar",
                      "org.apache.servicemix.bundles.ant-1.7.0", "1.0.0.m3-SNAPSHOT", "jar");
        assertVersion("org.apache.activemq.core-1.0-SNAPSHOT.xml",
                      "org.apache.activemq.core", "1.0.0.SNAPSHOT", "xml");
        assertVersion("org.apache.activemq.core-1.0.0-SNAPSHOT.xml",
                      "org.apache.activemq.core", "1.0.0.SNAPSHOT", "xml");
        assertVersion("org.apache.activemq.core-1.0.0.xml",
                      "org.apache.activemq.core", "1.0.0", "xml");
        assertVersion("geronimo-servlet_2.5_spec-1.1.2.jar",
                      "geronimo-servlet_2.5_spec", "1.1.2", "jar");
        assertVersion("spring-aop-2.5.1.jar",
                      "spring-aop", "2.5.1", "jar");
    }

    private void assertVersion(String s, String... expectedParts) {
        String[] parts = DeployerUtils.extractNameVersionType(s);
        assertEquals(expectedParts.length, parts.length);
        for (int i = 0; i < expectedParts.length; i++) {
            assertEquals(expectedParts[i], parts[i]);
        }
    }

}
