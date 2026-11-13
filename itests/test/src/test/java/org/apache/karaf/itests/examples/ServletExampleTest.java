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

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.BaseTest;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ServletExampleTest extends BaseTest {

    private void setup() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-servlet-example-features/" + System.getProperty("karaf.version") + "/xml");
        installAndAssertFeature("http");
        installAndAssertFeature("http-whiteboard");
        installAndAssertFeature("pax-web-karaf");
        featureService.installFeature("pax-web-jsp", EnumSet.noneOf(FeaturesService.Option.class));
        installAndAssertFeature("pax-web-jsp");
    }

    // the servlets are registered asynchronously, wait (bounded) until web:servlet-list shows the expected one
    private String awaitServletList(String expected) {
        String[] last = new String[1];
        try {
            Awaitility.await("servlet " + expected)
                    .atMost(60, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        last[0] = executeCommand("web:servlet-list");
                        return last[0].contains(expected);
                    });
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            throw new AssertionError("Servlet " + expected + " not found in web:servlet-list:\n" + last[0], e);
        }
        return last[0];
    }

    private void verify() throws Exception {
        System.out.println(awaitServletList("servlet-example"));

        URL url = new URL("http://localhost:" + getHttpPort() + "/servlet-example");
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
        assertContains("<h1>Example Servlet</h1>", output);
    }

    @Test
    public void testWithRegistration() throws Exception {
        setup();

        installAndAssertFeature("karaf-servlet-example-registration");

        verify();
    }

    @Test
    public void testWithAnnotation() throws Exception {
        setup();

        installAndAssertFeature("karaf-servlet-example-annotation");

        awaitServletList("/multipart");

        verify();
    }

    @Test
    public void testWithBlueprint() throws Exception {
        setup();

        installAndAssertFeature("karaf-servlet-example-blueprint");

        verify();
    }

    @Test
    public void testWithScr() throws Exception {
        setup();

        installAndAssertFeature("karaf-servlet-example-scr");

        verify();
    }

    @Test
    public void testUploadServlet() throws Exception {
        setup();

        installAndAssertFeature("karaf-servlet-example-upload");

        System.out.println(awaitServletList("upload-example"));

        File file = new File(System.getProperty("karaf.data"), "test.txt");
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("test");
        fileWriter.flush();
        fileWriter.close();

        URL url = new URL("http://localhost:" + getHttpPort() + "/upload-example");
        String boundary = "----FormBoundary" + System.currentTimeMillis();

        // Build the entire multipart body as bytes to avoid PrintWriter/OutputStream mixing issues
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] fileContent;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileContent = fileInputStream.readAllBytes();
        }
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"test\"; filename=\"test.txt\"\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "\r\n";
        body.write(header.getBytes(StandardCharsets.UTF_8));
        body.write(fileContent);
        String footer = "\r\n--" + boundary + "--\r\n";
        body.write(footer.getBytes(StandardCharsets.UTF_8));
        byte[] bodyBytes = body.toByteArray();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setFixedLengthStreamingMode(bodyBytes.length);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(bodyBytes);
        }

        Assert.assertEquals(200, connection.getResponseCode());
    }

}
