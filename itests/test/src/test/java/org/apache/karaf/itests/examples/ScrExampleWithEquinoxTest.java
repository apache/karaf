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

import org.apache.karaf.itests.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ScrExampleWithEquinoxTest extends BaseTest {

    @Configuration
    public Option[] config() {
        List<Option> config = new LinkedList<>(Arrays.asList(super.config()));
        config.add(KarafDistributionOption.editConfigurationFilePut("etc/config.properties", "karaf.framework", "equinox"));
        return config.toArray(new Option[config.size()]);
    }

    @Test
    public void test() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-scr-example-features/" + System.getProperty("karaf.version") + "/xml");

        installAndAssertFeature("karaf-scr-example-client");

        String output = executeCommand("scr:info BookingServiceMemoryImpl");
        System.out.println(output);
        assertContains("\"state\":32", output);

        output = executeCommand("scr:info ConsoleClient");
        System.out.println(output);
        assertContains("\"state\":32", output);
    }

}
