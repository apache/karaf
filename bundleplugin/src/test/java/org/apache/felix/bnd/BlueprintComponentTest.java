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
package org.apache.felix.bnd;

import java.util.Set;

import aQute.lib.spring.XMLType;
import junit.framework.TestCase;

public class BlueprintComponentTest extends TestCase {

    public void testPackages() throws Exception {
        XMLType t = new XMLType(getClass().getResource("blueprint.xsl"), null, ".*\\.xml");
        Set<String> s = t.analyze(getClass().getResourceAsStream("bp.xml"));
        assertEquals(14, s.size());
        assertTrue(s.contains("java.lang"));
        for (int i = 1; i <= 13; i++) {
            assertTrue(s.contains("p" + i));
        }
    }
}
