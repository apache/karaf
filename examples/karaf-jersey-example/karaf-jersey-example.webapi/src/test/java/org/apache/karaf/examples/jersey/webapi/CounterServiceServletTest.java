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
package org.apache.karaf.examples.jersey.webapi;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;

import org.apache.karaf.examples.jersey.servicedef.Counter;
import org.apache.karaf.examples.jersey.servicedef.beans.Count;
import org.apache.karaf.examples.jersey.services.CounterService;
import org.apache.karaf.examples.jersey.webapi.CounterServiceServlet;
import org.apache.karaf.examples.jersey.webapi.mocks.MockHttpServletResponse;
import org.apache.karaf.examples.jersey.webapi.mocks.MockLogService;
import org.glassfish.jersey.server.ServerProperties;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CounterServiceServletTest {
    static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testDoGet() throws ServletException, IOException {
        MockLogService logservice = new MockLogService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/karaf-jersey-example/api/counter"));
        when(request.getRequestURI()).thenReturn("/karaf-jersey-example/api/counter");
        when(request.getContextPath()).thenReturn("");
        when(request.getServletPath()).thenReturn("/karaf-jersey-example/api");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        MockHttpServletResponse response = mock(MockHttpServletResponse.class, CALLS_REAL_METHODS);

        CounterServiceServlet servlet = new CounterServiceServlet();
        servlet.setLogservice(logservice);
        Counter counterService = mock(Counter.class);
        when(counterService.currentValue()).thenReturn(new Count());
        servlet.setCounter(counterService);

        // When the servlet is activated it will be plugged into the http whiteboard and configured
        ServletConfig config = createServletConfigWithApplicationAndPackagenameForJerseyResources();
        servlet.init(config);

        servlet.service(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        ByteArrayOutputStream responseBody = response.getOutput();
        assertThat(response.getOutput().size()).isGreaterThan(0);
        Count counter = mapper.readValue(responseBody.toByteArray(), Count.class);
        assertEquals(0, counter.getCount());
    }

    @Test
    public void testDoGetAfterCounterIncrement() throws ServletException, IOException {
        MockLogService logservice = new MockLogService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/karaf-jersey-example/api/counter"));
        when(request.getRequestURI()).thenReturn("/karaf-jersey-example/api/counter");
        when(request.getContextPath()).thenReturn("");
        when(request.getServletPath()).thenReturn("/karaf-jersey-example/api");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        MockHttpServletResponse response = mock(MockHttpServletResponse.class, CALLS_REAL_METHODS);

        CounterServiceServlet servlet = new CounterServiceServlet();
        servlet.setLogservice(logservice);
        Counter counterService = new CounterService();
        servlet.setCounter(counterService);

        // When the servlet is activated it will be plugged into the http whiteboard and configured
        ServletConfig config = createServletConfigWithApplicationAndPackagenameForJerseyResources();
        servlet.init(config);

        // Increment the counter twice
        HttpServletRequest postToIncrementCounter = mock(HttpServletRequest.class);
        when(postToIncrementCounter.getMethod()).thenReturn("POST");
        when(postToIncrementCounter.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/karaf-jersey-example/api/counter"));
        when(postToIncrementCounter.getRequestURI()).thenReturn("/karaf-jersey-example/api/counter");
        when(postToIncrementCounter.getContextPath()).thenReturn("");
        when(postToIncrementCounter.getServletPath()).thenReturn("/karaf-jersey-example/api");
        when(postToIncrementCounter.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        HttpServletResponse postResponse = mock(HttpServletResponse.class);
        when(postResponse.getWriter()).thenReturn(mock(PrintWriter.class));
        servlet.service(postToIncrementCounter, postResponse);
        servlet.service(postToIncrementCounter, postResponse);

        servlet.service(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(200, response.getStatus());
        ByteArrayOutputStream responseBody = response.getOutput();
        assertThat(response.getOutput().size()).isGreaterThan(0);
        Count counter = mapper.readValue(responseBody.toByteArray(), Count.class);
        assertEquals(2, counter.getCount());
    }

    @Test
    public void testDoGetWithError() throws ServletException, IOException {
        MockLogService logservice = new MockLogService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/karaf-jersey-example/api/counter"));
        when(request.getRequestURI()).thenReturn("/karaf-jersey-example/api/counter");
        when(request.getContextPath()).thenReturn("");
        when(request.getServletPath()).thenReturn("/karaf-jersey-example/api");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        MockHttpServletResponse response = mock(MockHttpServletResponse.class, CALLS_REAL_METHODS);

        CounterServiceServlet servlet = new CounterServiceServlet();
        servlet.setLogservice(logservice);
        // Create a mock Counter service that causes the internal server error
        Counter counterService = mock(Counter.class);
        InternalServerErrorException exception = new InternalServerErrorException();
        when(counterService.currentValue()).thenThrow(exception);
        servlet.setCounter(counterService);

        // When the servlet is activated it will be plugged into the http whiteboard and configured
        ServletConfig config = createServletConfigWithApplicationAndPackagenameForJerseyResources();
        servlet.init(config);

        servlet.service(request, response);

        assertEquals(500, response.getStatus());
        assertEquals(0, response.getOutput().size());
    }

    private ServletConfig createServletConfigWithApplicationAndPackagenameForJerseyResources() {
        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList(ServerProperties.PROVIDER_PACKAGES)));
        when(config.getInitParameter(eq(ServerProperties.PROVIDER_PACKAGES))).thenReturn("org.apache.karaf.examples.jersey.webapi.resources");
        ServletContext servletContext = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
        return config;
    }
}
