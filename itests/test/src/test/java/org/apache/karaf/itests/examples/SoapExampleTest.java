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
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SoapExampleTest extends BaseTest {

    @Test
    public void testBlueprint() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-soap-example-features/" + System.getProperty("karaf.version") + "/xml");

        installAndAssertFeature("karaf-soap-example-blueprint");
        installAndAssertFeature("karaf-soap-example-client");

        String url = "http://localhost:" + getHttpPort() + "/cxf/example";
        executeCommand("booking:add --url " + url + " 1 Foo TST001");

        String output = executeCommand("booking:list --url " + url);
        System.out.println(output);
        assertContains("TST001", output);
    }

    @Test
    public void testScr() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-soap-example-features/" + System.getProperty("karaf.version") + "/xml");

        installAndAssertFeature("karaf-soap-example-scr");

        URL url = new URL("http://localhost:" + getHttpPort() + "/cxf");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        }

        assertContains("BookingServiceSoap", builder.toString());
    }

}
