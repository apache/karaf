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
package org.apache.felix.ipojo.test;

import junit.framework.Test;

import org.apache.felix.ipojo.junit4osgi.OSGiTestSuite;
import org.apache.felix.ipojo.test.scenarios.architecture.ArchitectureTestSuite;
import org.apache.felix.ipojo.test.scenarios.bad.BadTestSuite;
import org.apache.felix.ipojo.test.scenarios.configuration.ConfigurationTestSuite;
import org.apache.felix.ipojo.test.scenarios.controller.LifeCycleControllerTestSuite;
import org.apache.felix.ipojo.test.scenarios.core.CoreTestSuite;
import org.apache.felix.ipojo.test.scenarios.dependency.DependencyTestSuite;
import org.apache.felix.ipojo.test.scenarios.factory.FactoryTestSuite;
import org.apache.felix.ipojo.test.scenarios.handler.ExternalHandlerTestSuite;
import org.apache.felix.ipojo.test.scenarios.lifecycle.LifeCycleCallbackTest;
import org.apache.felix.ipojo.test.scenarios.manipulation.ManipulationTestSuite;
import org.apache.felix.ipojo.test.scenarios.service.providing.ProvidedServiceTestSuite;
import org.osgi.framework.BundleContext;

public class IPOJOTestSuite {
    
    public static Test suite(BundleContext bc) {
        OSGiTestSuite ots = new OSGiTestSuite("IPojo Core Test Suite", bc);
        ots.addTest(ManipulationTestSuite.suite(bc));
        ots.addTest(CoreTestSuite.suite(bc));
        ots.addTest(FactoryTestSuite.suite(bc));
        ots.addTest(ProvidedServiceTestSuite.suite(bc));
        ots.addTest(LifeCycleControllerTestSuite.suite(bc));
        ots.addTest(LifeCycleCallbackTest.suite(bc));
        ots.addTest(DependencyTestSuite.suite(bc));
        ots.addTest(ArchitectureTestSuite.suite(bc));
        ots.addTest(ConfigurationTestSuite.suite(bc));
        ots.addTest(ExternalHandlerTestSuite.suite(bc));
        ots.addTest(BadTestSuite.suite(bc));
        return ots;
    }

}
