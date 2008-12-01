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

import java.io.PrintStream;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;

/**
 * OSGi Junit Runner.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface OSGiJunitRunner {
    
    /**
     * Set the output stream of the runner.
     * @param ps the print stream.
     */
    void setResultPrinter(PrintStream ps);
    
    /**
     * Run the tests.
     * @return the list of TestResult.
     */
    List/*<TestResult>*/  run();
    
    /**
     * Run the tests from the given bundle.
     * @param bundleId the bundle id containing the tests. 
     * @return the list of the test results.
     */
    List/*<TestResult>*/ run(long bundleId);
    
    /**
     * Get the list of available test suites.
     * @return the list of Test objects.
     */
    List/*<Test>*/ getTests();
    
    /**
     * Get the tests from the given bundle. 
     * @param bundleId the bundle id.
     * @return the list of Test contained in the given bundle.
     */
    List/*<Test>*/ getTests(long bundleId);
    
    /**
     * Run the given test.
     * @param test the test to execute.
     * @return the result.
     */
    TestResult run(Test test);

}
