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
package org.apache.karaf.examples.jersey.webgui;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.karaf.examples.jersey.webgui.CounterDisplay;
import org.apache.karaf.examples.jersey.webgui.mocks.MockHttpServletResponse;
import org.apache.karaf.examples.jersey.webgui.mocks.MockLogService;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class CounterDisplayTest {

    @Test
    public void testDoGet() throws ServletException, IOException {
        MockLogService logservice = new MockLogService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("http://localhost:8181/karaf-jersey-example/");
        when(request.getPathInfo()).thenReturn("/");
        MockHttpServletResponse response = mock(MockHttpServletResponse.class, CALLS_REAL_METHODS);

        CounterDisplay servlet = new CounterDisplay();
        servlet.setLogservice(logservice);

        servlet.service(request, response);

        // Verify that the response from the servlet is as expected
        SoftAssertions expectedServletOutput = new SoftAssertions();
        expectedServletOutput.assertThat(response.getContentType()).isEqualTo("text/html");
        expectedServletOutput.assertThat(response.getStatus()).isEqualTo(200);
        expectedServletOutput.assertThat(response.getOutput().size()).isGreaterThan(0);
        expectedServletOutput.assertAll();
    }

    @Test
    public void testDoGetWithError() throws ServletException, IOException {
        MockLogService logservice = new MockLogService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("http://localhost:8181/karaf-jersey-example/");
        when(request.getPathInfo()).thenReturn("/");
        MockHttpServletResponse response = mock(MockHttpServletResponse.class, CALLS_REAL_METHODS);
        ServletOutputStream outputstream = mock(ServletOutputStream.class);
        doThrow(IOException.class).when(outputstream).write(anyInt());
        when(response.getOutputStream()).thenReturn(outputstream);

        CounterDisplay servlet = new CounterDisplay();
        servlet.setLogservice(logservice);

        servlet.service(request, response);

        // Verify that the response from the servlet is as expected
        SoftAssertions expectedServletOutput = new SoftAssertions();
        expectedServletOutput.assertThat(response.getStatus()).isEqualTo(500);
        expectedServletOutput.assertThat(response.getOutput().size()).isEqualTo(0);
        expectedServletOutput.assertAll();
    }

    @Test
    public void testDoGetWithNotFound() throws ServletException, IOException {
        MockLogService logservice = new MockLogService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("http://localhost:8181/karaf-jersey-example/");
        when(request.getPathInfo()).thenReturn("/notafileinservlet");
        MockHttpServletResponse response = mock(MockHttpServletResponse.class, CALLS_REAL_METHODS);
        ServletOutputStream outputstream = mock(ServletOutputStream.class);
        doThrow(IOException.class).when(outputstream).write(anyInt());
        when(response.getOutputStream()).thenReturn(outputstream);

        CounterDisplay servlet = new CounterDisplay();
        servlet.setLogservice(logservice);

        servlet.service(request, response);

        // Verify that the response from the servlet is as expected
        SoftAssertions expectedServletOutput = new SoftAssertions();
        expectedServletOutput.assertThat(response.getStatus()).isEqualTo(404);
        expectedServletOutput.assertThat(response.getOutput().size()).isEqualTo(0);
        expectedServletOutput.assertAll();
    }

    @Test
    public void testDoGetWithRedirect() throws ServletException, IOException {
        MockLogService logservice = new MockLogService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("http://localhost:8181/karaf-jersey-example");
        MockHttpServletResponse response = mock(MockHttpServletResponse.class, CALLS_REAL_METHODS);
        ServletOutputStream outputstream = mock(ServletOutputStream.class);
        doThrow(IOException.class).when(outputstream).write(anyInt());
        when(response.getOutputStream()).thenReturn(outputstream);

        CounterDisplay servlet = new CounterDisplay();
        servlet.setLogservice(logservice);

        servlet.service(request, response);

        // Verify that the response from the servlet is as expected
        SoftAssertions expectedServletOutput = new SoftAssertions();
        expectedServletOutput.assertThat(response.getStatus()).isEqualTo(302);
        expectedServletOutput.assertThat(response.getOutput().size()).isEqualTo(0);
        expectedServletOutput.assertAll();
    }

    @Test
    public void testGuessContentTypeFromResourceName() {
        CounterDisplay servlet = new CounterDisplay();
        assertEquals("text/html", servlet.guessContentTypeFromResourceName("index.html"));
        assertEquals("application/javascript", servlet.guessContentTypeFromResourceName("bundle.js"));
        assertEquals("text/css", servlet.guessContentTypeFromResourceName("index.css"));
    }
}
