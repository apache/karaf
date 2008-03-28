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
package org.apache.felix.ipojo.test.composite;

import junit.framework.Test;

import org.apache.felix.ipojo.junit4osgi.OSGiTestSuite;
import org.apache.felix.ipojo.test.composite.exporter.ExportTestSuite;
import org.apache.felix.ipojo.test.composite.importer.ImportTestSuite;
import org.apache.felix.ipojo.test.composite.infrastructure.InfrastructureTestSuite;
import org.apache.felix.ipojo.test.composite.instance.SimpleInstance;
import org.apache.felix.ipojo.test.composite.instantiator.InstantiatorTestSuite;
import org.apache.felix.ipojo.test.composite.provides.ProvidesTestSuite;
import org.apache.felix.ipojo.test.composite.test.CompositeTest;
import org.osgi.framework.BundleContext;

public class CompositeTestSuite {
    
    public static Test suite(BundleContext bc) {
        OSGiTestSuite ots = new OSGiTestSuite("iPOJO Composites Test Suite", bc);    
        ots.addTest(InfrastructureTestSuite.suite(bc));
        ots.addTest(InstantiatorTestSuite.suite(bc));
        ots.addTest(ImportTestSuite.suite(bc));
        ots.addTest(ExportTestSuite.suite(bc));
        ots.addTestSuite(CompositeTest.class);
        ots.addTestSuite(SimpleInstance.class);
        ots.addTest(ProvidesTestSuite.suite(bc));

        return ots;
    }

}
