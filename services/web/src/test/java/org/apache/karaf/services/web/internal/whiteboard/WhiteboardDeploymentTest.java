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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.karaf.services.web.WebElement;
import org.apache.karaf.services.web.internal.TomcatServer;
import org.apache.karaf.services.web.internal.WebConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

/**
 * Deploys whiteboard contexts in Tomcat.
 */
public class WhiteboardDeploymentTest {

    private final TestBundles bundles = new TestBundles();
    private TomcatServer server;
    private WebConfig config;
    private String base;
    private Bundle web;
    private Bundle bundle;
    private WhiteboardDeployment deployment;

    @Before
    public void setUp() throws Exception {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("host", "127.0.0.1");
        properties.put("http.port", "0");
        config = WebConfig.from(properties);
        server = new TomcatServer(config, Files.createTempDirectory("tomcat").toFile());
        server.start();
        base = server.getEndpoints().get(0);
        web = bundles.bundle(1);
        bundle = bundles.bundle(10);
    }

    @After
    public void tearDown() {
        if (deployment != null) {
            deployment.undeploy();
        }
        server.stop();
    }

    @Test
    public void filtersAndListenersAreAddedToTheRunningContext() throws Exception {
        TextServlet servlet = new TextServlet("hello");
        WhiteboardService servletService = servlet(servlet, "/hello");
        deploy(model(null, servletService));
        assertEquals("hello", read(get("/hello")));

        HeaderFilter filter = new HeaderFilter();
        RecordingListener listener = new RecordingListener();
        WhiteboardService filterService = bundles.register(WhiteboardService.Kind.FILTER, bundle, using -> filter,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
        WhiteboardService listenerService = bundles.register(WhiteboardService.Kind.LISTENER, bundle,
                using -> listener, HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
        WhiteboardContextModel extended = model(null, servletService, filterService, listenerService);
        // the context is not restarted, so a listener registering a filter when initialized does not loop
        assertTrue(deployment.accepts(extended));
        deployment.update(extended);
        assertEquals(1, listener.initialized);
        assertEquals("filtered", get("/hello").getHeaderField("X-Filter"));

        deployment.update(model(null, servletService));
        assertEquals(1, filter.destroyed);
        assertEquals(1, listener.destroyed);
        assertNull(get("/hello").getHeaderField("X-Filter"));
        // the servlet kept running
        assertEquals(1, servlet.initialized);
        assertEquals(0, servlet.destroyed);
    }

    @Test
    public void listenersAndRequestsSeeTheServletContextOfTheBundle() throws Exception {
        RecordingListener listener = new RecordingListener();
        TextServlet servlet = new TextServlet(null);
        deploy(model(null, servlet(servlet, "/context"), bundles.register(WhiteboardService.Kind.LISTENER, bundle,
                using -> listener, HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true")));

        // the resources of the servlet context helper, rather than the ones of the Tomcat context
        assertNotNull(listener.context.getResource("/www/hello.txt"));
        assertSame(servlet.getServletContext(), listener.context);
        assertEquals("true", read(get("/context")));

        // notified as the context stops
        deployment.undeploy();
        deployment = null;
        assertEquals(1, listener.destroyed);
    }

    @Test
    public void securityIsCheckedByTheHelperOfTheServletBundle() throws Exception {
        Bundle helperBundle = bundles.bundle(20);
        // a service factory: the bundle of the servlet allows the requests, the one of the helper does not
        WhiteboardService helper = bundles.register(WhiteboardService.Kind.HELPER, helperBundle,
                using -> new SecurityHelper(using == bundle),
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "secure",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/secure");
        deploy(model(helper, servlet(new TextServlet("secured"), "/hello")));
        assertEquals("secured", read(get("/secure/hello")));
    }

    @Test
    public void includedResourcesAndFilters() throws Exception {
        WhiteboardService resource = bundles.register(WhiteboardService.Kind.RESOURCE, bundle, using -> null,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/static/*",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/www");
        WhiteboardService page = servlet(new IncludingServlet("/static/hello.txt"), "/page");
        // applied to the included resource, matched with its path rather than the one of the request
        WhiteboardService filter = bundles.register(WhiteboardService.Kind.FILTER, bundle,
                using -> new MarkingFilter("[included]"),
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, ".*\\.txt",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, "INCLUDE");
        deploy(model(null, resource, page, filter));
        assertEquals("[included]Hello resource", read(get("/page")));
        assertEquals("Hello resource", read(get("/static/hello.txt")));
    }

    @Test
    public void emptyResourcePrefix() throws Exception {
        WhiteboardService resource = bundles.register(WhiteboardService.Kind.RESOURCE, bundle, using -> null,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/files/*",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "");
        deploy(model(null, resource));
        assertEquals("Hello resource", read(get("/files/www/hello.txt")));
        List<WebElement> servlets = new ArrayList<>();
        deployment.describe(new ArrayList<>(), servlets, new ArrayList<>());
        assertEquals(List.of("/files/*", "prefix:"), servlets.get(0).getPatterns());
    }

    @Test
    public void removedServiceIsUndeployed() throws Exception {
        TextServlet servlet = new TextServlet("hello");
        WhiteboardService servletService = servlet(servlet, "/hello");
        WhiteboardContextModel model = model(null, servletService);
        deploy(model);
        assertEquals(200, get("/hello").getResponseCode());

        assertTrue(deployment.uses(servletService));
        // as the whiteboard does when the service is unregistered
        servletService.removed();
        deployment.remove(servletService);
        assertEquals(1, servlet.destroyed);
        assertEquals(404, get("/hello").getResponseCode());

        // a model computed before the removal does not deploy it again
        deployment.update(model);
        assertEquals(404, get("/hello").getResponseCode());
        assertEquals(1, servlet.initialized);
        assertFalse(deployment.uses(servletService));
    }

    private WhiteboardService servlet(HttpServlet servlet, String pattern) {
        return bundles.register(WhiteboardService.Kind.SERVLET, bundle, using -> servlet,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, pattern);
    }

    private static WhiteboardContextModel model(WhiteboardService helper, WhiteboardService... services) {
        WhiteboardContextModel model = helper == null
                ? new WhiteboardContextModel(HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME, "/", null)
                : new WhiteboardContextModel(helper.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME),
                        helper.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), helper);
        for (WhiteboardService service : services) {
            model.add(service);
        }
        model.resolve(new ArrayList<>(), new HashMap<>());
        return model;
    }

    private void deploy(WhiteboardContextModel model) {
        deployment = new WhiteboardDeployment(model, server.getHost(), config, web.getBundleContext());
        deployment.deploy(model);
    }

    private HttpURLConnection get(String path) throws IOException {
        return (HttpURLConnection) new URL(base + path).openConnection();
    }

    private static String read(HttpURLConnection connection) throws IOException {
        try (InputStream is = connection.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    /**
     * Writes the given text, or whether the servlet context of the request is the one of the servlet.
     */
    private static final class TextServlet extends HttpServlet {

        private final String text;
        private volatile int initialized;
        private volatile int destroyed;

        TextServlet(String text) {
            this.text = text;
        }

        @Override
        public void init() {
            initialized++;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.getWriter().print(text != null ? text
                    : Boolean.toString(request.getServletContext() == getServletContext()));
        }

        @Override
        public void destroy() {
            destroyed++;
        }

    }

    private static final class IncludingServlet extends HttpServlet {

        private final String path;

        IncludingServlet(String path) {
            this.path = path;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            request.getRequestDispatcher(path).include(request, response);
        }

    }

    private static final class HeaderFilter implements Filter {

        private volatile int destroyed;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            ((HttpServletResponse) response).setHeader("X-Filter", "filtered");
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
            destroyed++;
        }

    }

    private static final class MarkingFilter implements Filter {

        private final String mark;

        MarkingFilter(String mark) {
            this.mark = mark;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            response.getWriter().print(mark);
            chain.doFilter(request, response);
        }

    }

    private static final class RecordingListener implements ServletContextListener {

        private volatile ServletContext context;
        private volatile int initialized;
        private volatile int destroyed;

        @Override
        public void contextInitialized(ServletContextEvent event) {
            context = event.getServletContext();
            initialized++;
        }

        @Override
        public void contextDestroyed(ServletContextEvent event) {
            destroyed++;
        }

    }

    private static final class SecurityHelper extends ServletContextHelper {

        private final boolean allowed;

        SecurityHelper(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
            return allowed;
        }

    }

}
