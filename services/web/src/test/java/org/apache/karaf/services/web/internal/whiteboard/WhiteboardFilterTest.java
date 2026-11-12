/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.services.web.internal.whiteboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.junit.Test;

public class WhiteboardFilterTest {

    @Test
    public void patterns() {
        assertTrue(WhiteboardFilter.matches("/*", "/anything"));
        assertTrue(WhiteboardFilter.matches("/api/*", "/api"));
        assertTrue(WhiteboardFilter.matches("/api/*", "/api/users"));
        assertFalse(WhiteboardFilter.matches("/api/*", "/apis"));
        assertTrue(WhiteboardFilter.matches("/exact", "/exact"));
        assertFalse(WhiteboardFilter.matches("/exact", "/exact/more"));
        assertTrue(WhiteboardFilter.matches("*.json", "/api/users.json"));
        assertFalse(WhiteboardFilter.matches("*.json", "/api.json/users"));
        assertFalse(WhiteboardFilter.matches("*.json", "/api/users.jsonx"));
    }

    @Test
    public void regexWithPatternsAndServlets() throws Exception {
        List<String> filtered = new ArrayList<>();
        Filter delegate = (request, response, chain) -> {
            filtered.add(((HttpServletRequest) request).getServletPath());
            chain.doFilter(request, response);
        };
        WhiteboardFilter filter = new WhiteboardFilter(delegate, null, getClass().getClassLoader(),
                List.of(Pattern.compile(".*\\.json")), new String[]{"/api/*"}, new String[]{"admin"});
        FilterChain chain = (request, response) -> {
        };
        filter.doFilter(request("/users.json", "other"), null, chain);
        filter.doFilter(request("/api/users", "other"), null, chain);
        filter.doFilter(request("/console", "admin"), null, chain);
        filter.doFilter(request("/other", "other"), null, chain);
        assertEquals(List.of("/users.json", "/api/users", "/console"), filtered);
    }

    private static HttpServletRequest request(String servletPath, String servletName) {
        HttpServletMapping mapping = EasyMock.createMock(HttpServletMapping.class);
        EasyMock.expect(mapping.getServletName()).andReturn(servletName).anyTimes();
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getServletPath()).andReturn(servletPath).anyTimes();
        EasyMock.expect(request.getPathInfo()).andReturn(null).anyTimes();
        EasyMock.expect(request.getHttpServletMapping()).andReturn(mapping).anyTimes();
        EasyMock.replay(mapping, request);
        return request;
    }

}
