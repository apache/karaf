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
package org.apache.felix.ipojo.junit4osgi;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.osgi.framework.BundleContext;

/**
 * OSGi Test Suite.
 * Allow the injection of the bundle context.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class OSGiTestSuite extends TestSuite {

    protected BundleContext context;

    public OSGiTestSuite(Class clazz, BundleContext bc) {
        super(clazz);
        context = bc;
    }

    public OSGiTestSuite(BundleContext bc) {
        super();
        context = bc;
    }

    public OSGiTestSuite(String name, BundleContext bc) {
        super(name);
        context = bc;
    }

    public OSGiTestSuite(Class clazz, String name, BundleContext bc) {
        super(clazz, name);
        context = bc;
    }

    public void setBundleContext(BundleContext bc) {
        context = bc;
    }

    /**
     * Adds the tests from the given class to the suite
     */
    public void addTestSuite(Class testClass) {
        if (OSGiTestCase.class.isAssignableFrom(testClass)) {
            addTest(new OSGiTestSuite(testClass, context));
        } else if (TestCase.class.isAssignableFrom(testClass)) {
            addTest(new TestSuite(testClass));
        } else {
            System.out.println("Error : the " + testClass + " is not a valid test class");
        }
    }

    public void runTest(Test test, TestResult result) {
        if (test instanceof OSGiTestSuite) {
            ((OSGiTestSuite) test).context = context;
            test.run(result);
        } else if (test instanceof OSGiTestCase) {
            ((OSGiTestCase) test).context = context;
            test.run(result);
        } else {
            test.run(result);
        }

    }

}
