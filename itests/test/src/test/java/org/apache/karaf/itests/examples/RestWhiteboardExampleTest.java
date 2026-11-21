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
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests the karaf-rest-example-whiteboard feature: the Jakarta REST whiteboard (OSGi Technology implementation)
 * serving the SCR booking resource on the context path configured by the bundle's configurator resource.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class RestWhiteboardExampleTest extends BaseTest {

    private String bookingUrl() throws Exception {
        return "http://localhost:" + getHttpPort() + "/booking/";
    }

    private void setup() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-rest-example-features/" + System.getProperty("karaf.version") + "/xml");
        installAndAssertFeature("karaf-rest-example-whiteboard");
        // the configurator creates the whiteboard runtime configuration, then the servlet and the resource are
        // registered asynchronously: wait (bounded) until the resource answers
        awaitEmptyBookingList();
    }

    private void awaitEmptyBookingList() throws Exception {
        String url = bookingUrl();
        String[] last = new String[1];
        try {
            Awaitility.await("booking resource on " + url)
                    .atMost(60, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        last[0] = request("GET", url, null);
                        return last[0].startsWith("200 ");
                    });
        } catch (ConditionTimeoutException e) {
            throw new AssertionError("Booking resource not available on " + url + ", last response: " + last[0]
                    + "\n" + executeCommand("web:servlet-list"), e);
        }
        assertEquals("200 []", last[0]);
    }

    /**
     * @return the status code and the body, separated by a space (or the exception, when the connection fails)
     */
    private static String request(String method, String url, String json) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            if (json != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                connection.setRequestProperty("Accept", "application/json");
            }
            int status = connection.getResponseCode();
            InputStream in = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            if (in != null) {
                try (InputStream is = in) {
                    is.transferTo(body);
                }
            }
            return status + " " + body.toString(StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return e.toString();
        }
    }

    @Test
    public void testWhiteboard() throws Exception {
        setup();

        String url = bookingUrl();
        assertEquals("204 ", request("POST", url, "{\"id\":1,\"customer\":\"Foo\",\"flight\":\"TST001\"}"));

        String output = request("GET", url, null);
        System.out.println(output);
        assertContains("200 ", output);
        assertContains("\"flight\":\"TST001\"", output);

        output = request("GET", url + "1", null);
        System.out.println(output);
        assertContains("\"customer\":\"Foo\"", output);
    }

    @Test
    public void testWhiteboardWithJerseyClient() throws Exception {
        setup();

        installAndAssertFeature("karaf-rest-example-client-jersey");

        String url = bookingUrl();
        executeCommand("booking:add --url " + url + " 1 Foo TST001");

        String output = executeCommand("booking:list --url " + url);
        System.out.println(output);
        assertContains("1 Foo TST001", output);
    }

}
