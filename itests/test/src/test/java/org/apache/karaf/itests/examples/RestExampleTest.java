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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class RestExampleTest extends BaseTest {

    private static final String CXF_IGNORE = "CXF 4.x does not provide Karaf features (CXF-8766), so the CXF based examples cannot be installed, see #2461";

    private void setup() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-rest-example-features/" + System.getProperty("karaf.version") + "/xml");
        installAndAssertFeature("http");
        installAndAssertFeature("http-whiteboard");
        installAndAssertFeature("pax-web-karaf");
    }

    private String whiteboardLocation() throws Exception {
        return "http://localhost:" + getHttpPort() + "/booking/";
    }

    private void verify(String location) throws Exception {
        executeCommand("booking:add --url " + location + " 1 Foo TST001");

        String output = executeCommand("booking:list --url " + location);
        System.out.println(output);
        assertContains("TST001", output);
    }

    private void verifyAttachments() throws Exception {
        String location = "http://localhost:" + getHttpPort() + "/cxf/booking/all";

        String output = executeCommand("booking:list-all --url " + location);
        System.out.println(output);
        assertContains("stream: application/octet-stream: important information", output);
        assertContains("image: application/octet-stream: UGhvdG8=", output);
    }

    /**
     * Waits until the whiteboard service answers, returns the response body of the (empty) booking list.
     */
    private String waitForWhiteboard() throws Exception {
        long end = System.currentTimeMillis() + 60_000;
        String lastProblem = "no response";
        while (System.currentTimeMillis() < end) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(whiteboardLocation()).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                int status = connection.getResponseCode();
                if (status == 200) {
                    StringBuilder buffer = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line);
                        }
                    }
                    return buffer.toString();
                }
                lastProblem = "HTTP status " + status;
            } catch (Exception e) {
                lastProblem = e.toString();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            Thread.sleep(500);
        }
        throw new AssertionError("whiteboard service not available at " + whiteboardLocation() + " (last result: " + lastProblem + ")");
    }

    @Test
    @Ignore(CXF_IGNORE)
    public void testBlueprintWithHttpClient() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-blueprint");

        installAndAssertFeature("karaf-rest-example-client-http");

        verify("http://localhost:" + getHttpPort() + "/cxf/booking/");
    }

    @Test
    @Ignore(CXF_IGNORE)
    public void testBlueprintWithCxfClient() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-blueprint");

        installAndAssertFeature("karaf-rest-example-client-cxf");

        verify("http://localhost:" + getHttpPort() + "/cxf/booking/");
    }

    @Test
    @Ignore(CXF_IGNORE)
    public void testScrWithHttpClient() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-scr");

        installAndAssertFeature("karaf-rest-example-client-http");

        verify("http://localhost:" + getHttpPort() + "/cxf/booking/");
    }

    @Test
    @Ignore(CXF_IGNORE)
    public void testScrWithCxfClient() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-scr");

        installAndAssertFeature("karaf-rest-example-client-cxf");

        verify("http://localhost:" + getHttpPort() + "/cxf/booking/");
        verifyAttachments();
    }

    @Test
    @Ignore(CXF_IGNORE)
    public void testScrWithJerseyClient() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-scr");

        installAndAssertFeature("karaf-rest-example-client-jersey");

        verify("http://localhost:" + getHttpPort() + "/cxf/booking/");
    }

    @Test
    public void testWhiteboard() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-whiteboard");

        assertEquals("[]", waitForWhiteboard());
    }

    @Test
    public void testWhiteboardWithHttpClient() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-whiteboard");
        waitForWhiteboard();

        installAndAssertFeature("karaf-rest-example-client-http");

        verify(whiteboardLocation());
    }

    @Test
    public void testWhiteboardWithJerseyClient() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-whiteboard");
        waitForWhiteboard();

        installAndAssertFeature("karaf-rest-example-client-jersey");

        verify(whiteboardLocation());
    }

}
