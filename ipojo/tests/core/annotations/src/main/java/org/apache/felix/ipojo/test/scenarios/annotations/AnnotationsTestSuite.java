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
package org.apache.felix.ipojo.test.scenarios.annotations;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.felix.ipojo.junit4osgi.OSGiTestSuite;
import org.osgi.framework.BundleContext;

public class AnnotationsTestSuite extends TestSuite {

    public static Test suite(BundleContext bc) {
        OSGiTestSuite ots = new OSGiTestSuite("Annotations Test Suite", bc);
        ots.addTestSuite(ServiceProdiving.class);
        ots.addTestSuite(LifecycleController.class);
        ots.addTestSuite(LifecycleCallbacks.class);
        ots.addTestSuite(Properties.class);
        ots.addTestSuite(Architecture.class);
        ots.addTestSuite(Factory.class);
        ots.addTestSuite(Dependency.class);
        ots.addTestSuite(OptionalDependency.class);
        ots.addTestSuite(AggregateDependency.class);
        ots.addTestSuite(PolicyDependency.class);
        ots.addTestSuite(FilteredDependency.class);
        ots.addTestSuite(RankedDependency.class);
        
        // External handler annotations
        ots.addTestSuite(TemporalDependencies.class);
        ots.addTestSuite(JMX.class);
        ots.addTestSuite(WhiteBoard.class);
        ots.addTestSuite(Extender.class);
        ots.addTestSuite(EventAdmin.class);

        // Instantiate
        ots.addTestSuite(Instantiate.class);

        return ots;
    }

}

