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
package org.apache.felix.ipojo.junit4osgi.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.felix.ipojo.junit4osgi.OSGiTestSuite;
import org.osgi.framework.BundleContext;

/**
 * Simple OSGi Test suite.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TestOSGiTestSuite extends TestSuite {
    
    /**
     * Suite method.
     * @param bc the bundle context
     * @return the Test Suite.
     */
    public static Test suite(BundleContext bc) {
        TestSuite ts = new TestSuite();
        ts.setName("Test OSGi suite() method");
        ts.addTestSuite(TestTestCase.class);
        OSGiTestSuite ots = new OSGiTestSuite(TestOSGiTestCase.class, bc);
        ots.setBundleContext(bc);
        ts.addTest(ots);
        return ts;
    }

}
