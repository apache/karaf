/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.itests.examples;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.itests.BaseTest;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NamespaceHandlerExampleTest extends BaseTest {

    private static final RolePrincipal[] ADMIN_ROLES = {
            new RolePrincipal(BundleService.SYSTEM_BUNDLES_ROLE),
            new RolePrincipal("admin"),
            new RolePrincipal("manager")
    };

    @Test
    public void test() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-url-namespace-handler-example-features/" + System.getProperty("karaf.version") + "/xml");

        installAndAssertFeature("karaf-url-namespace-handler-example");

        String output = executeCommand("bundle:install example:mvn:commons-lang/commons-lang/2.6", ADMIN_ROLES);
        System.out.println(output);
        assertContains("Thanks for using the Example URL !", output);
    }

}
