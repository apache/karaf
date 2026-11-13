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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.catalina.Globals;
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
    public void validPatterns() {
        assertTrue(WhiteboardFilter.isValid("/*"));
        assertTrue(WhiteboardFilter.isValid("/api/*"));
        assertTrue(WhiteboardFilter.isValid("*.json"));
        assertFalse(WhiteboardFilter.isValid("api"));
        assertFalse(WhiteboardFilter.isValid("*.json/x"));
        assertFalse(WhiteboardFilter.isValid("/api/*.json"));
    }

    @Test
    public void regexWithPatternsAndServlets() {
        WhiteboardFilter filter = filter(EnumSet.of(DispatcherType.REQUEST));
        assertTrue(filter.matches(DispatcherType.REQUEST, "/users.json", "other"));
        assertTrue(filter.matches(DispatcherType.REQUEST, "/api/users", "other"));
        assertTrue(filter.matches(DispatcherType.REQUEST, "/console", "admin"));
        assertFalse(filter.matches(DispatcherType.REQUEST, "/other", "other"));
        // a named dispatch has no path
        assertFalse(filter.matches(DispatcherType.REQUEST, null, "other"));
        assertTrue(filter.matches(DispatcherType.REQUEST, null, "admin"));
    }

    @Test
    public void dispatchers() {
        WhiteboardFilter filter = filter(EnumSet.of(DispatcherType.INCLUDE, DispatcherType.ERROR));
        assertFalse(filter.matches(DispatcherType.REQUEST, "/api/users", "other"));
        assertTrue(filter.matches(DispatcherType.INCLUDE, "/api/users", "other"));
        assertTrue(filter.matches(DispatcherType.ERROR, "/api/users", "other"));
    }

    @Test
    public void chain() throws Exception {
        List<String> calls = new ArrayList<>();
        WhiteboardFilters filters = new WhiteboardFilters();
        filters.set(List.of(
                filter("async", true, calls, "/*"),
                filter("sync", false, calls, "/*"),
                filter("other", true, calls, "/other/*")));
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR)).andReturn("/api").anyTimes();
        EasyMock.expect(request.getDispatcherType()).andReturn(DispatcherType.REQUEST).anyTimes();
        EasyMock.expect(request.isAsyncSupported()).andReturn(true).anyTimes();
        // the filter which does not support asynchronous requests disables them, as Tomcat does
        request.setAttribute(Globals.ASYNC_SUPPORTED_ATTR, Boolean.FALSE);
        EasyMock.expectLastCall().once();
        EasyMock.replay(request);

        filters.doFilter(request, null, "servlet", (req, res) -> calls.add("servlet"));

        assertEquals(List.of("async", "sync", "servlet"), calls);
        EasyMock.verify(request);
    }

    private static WhiteboardFilter filter(Set<DispatcherType> dispatcherTypes) {
        return new WhiteboardFilter((request, response, chain) -> { }, WhiteboardFilterTest.class.getClassLoader(),
                List.of(Pattern.compile(".*\\.json")), new String[]{"/api/*"}, new String[]{"admin"},
                dispatcherTypes, false);
    }

    private static WhiteboardFilter filter(String name, boolean asyncSupported, List<String> calls,
                                           String pattern) {
        Filter filter = (request, response, chain) -> {
            calls.add(name);
            chain.doFilter(request, response);
        };
        return new WhiteboardFilter(filter, WhiteboardFilterTest.class.getClassLoader(), List.of(),
                new String[]{pattern}, new String[0], EnumSet.of(DispatcherType.REQUEST), asyncSupported);
    }

}
