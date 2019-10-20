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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CamelExampleTest extends BaseTest {

    public void setup() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-camel-example-features/" + System.getProperty("karaf.version") + "/xml");
    }

    @Test(timeout = 60000)
    public void testJavaDSL() throws Exception {
        setup();
        installAndAssertFeature("karaf-camel-example-java");
        verify();
    }

    @Test(timeout = 60000)
    public void testBlueprintDSL() throws Exception {
        setup();
        installAndAssertFeature("karaf-camel-example-blueprint");
        verify();
    }

    public void verify() throws Exception {
        String output = executeCommand("camel:route-list");
        while (!output.contains("Started")) {
            Thread.sleep(500);
            output = executeCommand("camel:route-list");
        }
        System.out.println(output);

        URL url = new URL("http://localhost:9090/example");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setDoInput(true);

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
        writer.println("{ \"notification\": { \"type\": \"email\", \"to\": \"foo@bar.com\", \"message\": \"this is a test\" }}");
        writer.flush();
        writer.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder buffer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();

        output = buffer.toString();
        System.out.println(output);
        assertEquals("{ \"status\": \"email sent\", \"to\": \"foo@bar.com\", \"subject\": \"Notification\" }", output);

        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setDoInput(true);

        writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
        writer.println("{ \"notification\": { \"type\": \"http\", \"service\": \"http://foo\" }}");
        writer.flush();
        writer.close();

        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        buffer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();

        output = buffer.toString();
        System.out.println(output);
        assertEquals("{ \"status\": \"http requested\", \"service\": \"http://foo\" }", output);
    }

}
