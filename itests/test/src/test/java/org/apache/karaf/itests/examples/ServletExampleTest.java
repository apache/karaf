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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ServletExampleTest extends BaseTest {

    private void setup() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-servlet-example-features/" + System.getProperty("karaf.version") + "/xml");
        installAndAssertFeature("http");
        installAndAssertFeature("http-whiteboard");
    }

    private void verify() throws Exception {
        String command = executeCommand("http:list");
        while (!command.contains("servlet-example")) {
            Thread.sleep(200);
            command = executeCommand("http:list");
        }
        System.out.println(command);

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

        String command = executeCommand("http:list");
        while (!command.contains("servlet-example/multipart")) {
            Thread.sleep(200);
            command = executeCommand("http:list");
        }

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

        String command = executeCommand("http:list");
        while (!command.contains("upload-example")) {
            Thread.sleep(200);
            command = executeCommand("http:list");
        }
        System.out.println(command);

        File file = new File(System.getProperty("karaf.data"), "test.txt");
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("test");
        fileWriter.flush();
        fileWriter.close();

        URL url = new URL("http://localhost:" + getHttpPort() + "/upload-example");
        String boundary = "===" + System.currentTimeMillis() + "===";
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        OutputStream outputStream = connection.getOutputStream();

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream), true);
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"test\"; filename=\"test.txt\"").append("\r\n");
        writer.append("Content-Type: text/plain; charset=UTF-8").append("\r\n");
        writer.append("Content-Transfer-Encoding: binary").append("\r\n");
        writer.append("\r\n");
        writer.flush();

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead = -1;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        fileInputStream.close();
        writer.append("\r\n");
        writer.append("\r\n");
        writer.append("--").append(boundary).append("--").append("\r\n");
        writer.flush();
        writer.close();

        Assert.assertEquals(200, connection.getResponseCode());
    }

}
