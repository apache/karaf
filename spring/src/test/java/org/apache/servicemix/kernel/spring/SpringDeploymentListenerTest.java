/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.kernel.spring;

import java.io.File;
import java.util.Set;

import junit.framework.TestCase;

public class SpringDeploymentListenerTest extends TestCase {

    public void testPackagesExtraction() throws Exception {
        SpringDeploymentListener l = new SpringDeploymentListener();
        File f = new File(getClass().getClassLoader().getResource("META-INF/spring/spring-deployer.xml").toURI());
        Set<String> pkgs = l.getBeanPackages(l.parse(f));
        assertNotNull(pkgs);
        assertEquals(1, pkgs.size());
        assertEquals("org.apache.servicemix.kernel.spring", pkgs.iterator().next());
    }
}
