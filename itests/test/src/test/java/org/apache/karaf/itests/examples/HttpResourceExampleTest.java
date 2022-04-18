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
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HttpResourceExampleTest extends BaseTest {

    @Test(timeout = 600000L)
    public void test() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-http-resource-example-features/" + System.getProperty("karaf.version") + "/xml");

        installAndAssertFeature("pax-web-karaf");
        installAndAssertFeature("karaf-http-resource-example-whiteboard");

        String command = executeCommand("web:servlet-list");
        while (!command.contains("/example/*")) {
            Thread.sleep(200);
            command = executeCommand("web:servlet-list");
        }
        assertContains("ResourceServlet", command);
        assertContains("Whiteboard", command);

        URL url = new URL("http://localhost:" + getHttpPort() + "/example/index.html");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder buffer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }

        String output = buffer.toString();
        System.out.println(output);
        assertContains("<h2>Resource Example</h2>", output);
    }

}
