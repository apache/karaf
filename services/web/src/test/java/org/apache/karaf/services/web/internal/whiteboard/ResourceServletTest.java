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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.karaf.services.web.internal.Contexts;
import org.apache.karaf.services.web.internal.TomcatServer;
import org.apache.karaf.services.web.internal.WebConfig;
import org.apache.tomcat.SimpleInstanceManager;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.servlet.context.ServletContextHelper;

/**
 * Runs the embedded Tomcat with a whiteboard context serving resources, protected by a servlet context helper.
 */
public class ResourceServletTest {

    private TomcatServer server;
    private String base;

    @Before
    public void setUp() throws Exception {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("host", "127.0.0.1");
        properties.put("http.port", "0");
        WebConfig config = WebConfig.from(properties);
        server = new TomcatServer(config, Files.createTempDirectory("tomcat").toFile());
        server.start();
        base = server.getEndpoints().get(0);

        ClassLoader classLoader = getClass().getClassLoader();
        ServletContextHelper helper = new ServletContextHelper() {
            @Override
            public URL getResource(String name) {
                return classLoader.getResource(name.substring(1));
            }

            @Override
            public Set<String> getResourcePaths(String path) {
                URL url = getResource(path);
                String[] children = url == null ? null : new File(url.getPath()).list();
                if (children == null) {
                    return null;
                }
                Set<String> paths = new HashSet<>();
                for (String child : children) {
                    paths.add(path + child);
                }
                return paths;
            }

            @Override
            public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
                if (request.getRequestURI().contains("forbidden")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return false;
                }
                return true;
            }
        };
        StandardContext context = Contexts.create("/test", classLoader, null, config);
        Contexts.markConfigured(context);
        context.setInstanceManager(new SimpleInstanceManager());
        // the filters are started with the context, the servlets are added to the running context
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName(SecurityFilter.NAME);
        filterDef.setFilter(new SecurityFilter(helper));
        filterDef.setFilterClass(SecurityFilter.class.getName());
        context.addFilterDef(filterDef);
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(SecurityFilter.NAME);
        filterMap.addURLPatternDecoded("/*");
        context.addFilterMap(filterMap);
        server.getHost().addChild(context);
        addServlet(context, "resources", new ResourceServlet("/www", helper), "/static/*", "/forbidden/*");
        addServlet(context, "exact", new ResourceServlet("/www/hello.txt", helper), "/favicon.txt");
    }

    private static void addServlet(StandardContext context, String name, Servlet servlet, String... patterns)
            throws Exception {
        StandardWrapper wrapper = (StandardWrapper) context.createWrapper();
        wrapper.setName(name);
        wrapper.setServlet(servlet);
        context.addChild(wrapper);
        for (String pattern : patterns) {
            context.addServletMappingDecoded(pattern, name);
        }
        wrapper.load();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void resource() throws Exception {
        HttpURLConnection connection = get("/test/static/hello.txt");
        assertEquals(200, connection.getResponseCode());
        assertTrue(connection.getContentType().startsWith("text/plain"));
        assertEquals("Hello resource", read(connection));
        assertEquals(200, get("/test/static/sub/nested.txt").getResponseCode());
        assertEquals("Hello resource", read(get("/test/favicon.txt")));
    }

    @Test
    public void notModified() throws Exception {
        HttpURLConnection connection = get("/test/static/hello.txt");
        String lastModified = connection.getHeaderField("Last-Modified");
        assertNotNull(lastModified);
        connection = (HttpURLConnection) new URL(base + "/test/static/hello.txt").openConnection();
        connection.setRequestProperty("If-Modified-Since", lastModified);
        assertEquals(304, connection.getResponseCode());
    }

    @Test
    public void notFound() throws Exception {
        assertEquals(404, get("/test/static/missing.txt").getResponseCode());
        // directories are not served
        assertEquals(404, get("/test/static/sub").getResponseCode());
        assertEquals(404, get("/test/static/sub/").getResponseCode());
        assertEquals(404, get("/test/static").getResponseCode());
    }

    @Test
    public void security() throws Exception {
        assertEquals(401, get("/test/forbidden/hello.txt").getResponseCode());
    }

    private HttpURLConnection get(String path) throws IOException {
        return (HttpURLConnection) new URL(base + path).openConnection();
    }

    private static String read(HttpURLConnection connection) throws IOException {
        try (InputStream is = connection.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

}
