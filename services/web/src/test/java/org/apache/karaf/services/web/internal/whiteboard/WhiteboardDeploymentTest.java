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
import static org.junit.Assert.assertNotSame;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
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

    @Test
    public void textResourceForwardedToByAServletUsingTheWriter() throws Exception {
        WhiteboardService resource = bundles.register(WhiteboardService.Kind.RESOURCE, bundle, using -> null,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/static/*",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/www");
        deploy(model(null, resource, servlet(new ForwardingServlet("/static/utf8.txt"), "/forward")));
        HttpURLConnection connection = get("/forward");
        // the UTF-8 resource is written with the ISO-8859-1 writer, its length in bytes is not the one of the file
        connection.setReadTimeout(5000);
        try (InputStream is = connection.getInputStream()) {
            assertEquals("h\u00e9llo", new String(is.readAllBytes(), StandardCharsets.ISO_8859_1).trim());
        }
    }

    @Test
    public void modifiedFilterIsReplacedWithoutServingRequestsWithoutIt() throws Exception {
        CountDownLatch replacing = new CountDownLatch(1);
        CountDownLatch replaced = new CountDownLatch(1);
        LifecycleFilter filter = new LifecycleFilter(replacing, replaced);
        WhiteboardService servletService = servlet(new TextServlet("hello"), "/hello");
        WhiteboardService filterService = bundles.register(WhiteboardService.Kind.FILTER, bundle, using -> filter,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
        deploy(model(null, servletService, filterService));
        assertEquals("filtered", get("/hello").getHeaderField("X-Filter"));

        // its properties change, the same instance is initialized again once destroyed
        filterService.changed();
        Thread update = new Thread(() -> deployment.update(model(null, servletService, filterService)));
        update.start();
        assertTrue(replacing.await(5, TimeUnit.SECONDS));
        assertEquals(503, get("/hello").getResponseCode());
        replaced.countDown();
        update.join(5000);

        assertEquals("filtered", get("/hello").getHeaderField("X-Filter"));
        assertEquals(List.of("init", "destroy", "init"), filter.events);
    }

    @Test
    public void removedFilterIsDestroyedOnceItsRequestsComplete() throws Exception {
        CountDownLatch filtering = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BlockingFilter filter = new BlockingFilter(filtering, release);
        WhiteboardService filterService = bundles.register(WhiteboardService.Kind.FILTER, bundle, using -> filter,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
        deploy(model(null, servlet(new TextServlet("hello"), "/hello"), filterService));
        Thread request = new Thread(() -> {
            try {
                read(get("/hello"));
            } catch (IOException e) {
                // checked by the filter events
            }
        });
        request.start();
        assertTrue(filtering.await(5, TimeUnit.SECONDS));

        Thread removal = new Thread(() -> deployment.remove(filterService));
        removal.start();
        removal.join(200);
        assertEquals(List.of("filtering"), filter.events);
        release.countDown();
        removal.join(5000);
        request.join(5000);

        assertEquals(List.of("filtering", "filtered", "destroy"), filter.events);
    }

    @Test
    public void failingListenerDoesNotPreventTheDeployment() throws Exception {
        ServletContextListener listener = new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent event) {
                throw new NoClassDefFoundError("org/missing/Import");
            }
        };
        WhiteboardService listenerService = bundles.register(WhiteboardService.Kind.LISTENER, bundle,
                using -> listener, HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
        deploy(model(null, listenerService, servlet(new TextServlet("hello"), "/hello")));

        assertEquals("hello", read(get("/hello")));
        List<WebElement> failures = deployment.getFailures();
        assertEquals(1, failures.size());
        assertEquals(WebElement.Type.LISTENER, failures.get(0).getType());
    }

    @Test
    public void requestListeners() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        ServletRequestListener listener = new ServletRequestListener() {
            @Override
            public void requestInitialized(ServletRequestEvent event) {
                events.add("initialized");
            }

            @Override
            public void requestDestroyed(ServletRequestEvent event) {
                events.add("destroyed");
            }
        };
        WhiteboardService servletService = servlet(new TextServlet("hello"), "/hello");
        WhiteboardService listenerService = bundles.register(WhiteboardService.Kind.LISTENER, bundle,
                using -> listener, HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
        deploy(model(null, servletService, listenerService));
        assertEquals("hello", read(get("/hello")));
        assertEquals(List.of("initialized", "destroyed"), events);

        deployment.update(model(null, servletService));
        assertEquals("hello", read(get("/hello")));
        assertEquals(List.of("initialized", "destroyed"), events);
    }

    @Test
    public void servletContextOfARestartedBundle() throws Exception {
        TextServlet first = new TextServlet("first");
        TextServlet second = new TextServlet("second");
        WhiteboardService firstService = servlet(first, "/first");
        deploy(model(null, firstService));

        // the bundle is stopped and started again: its class loader may have changed
        bundles.restart(bundle);
        deployment.update(model(null, firstService, servlet(second, "/second")));

        assertEquals("second", read(get("/second")));
        assertNotSame(first.getServletContext(), second.getServletContext());
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

    private static final class ForwardingServlet extends HttpServlet {

        private final String path;

        ForwardingServlet(String path) {
            this.path = path;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            response.setCharacterEncoding("ISO-8859-1");
            response.getWriter();
            request.getRequestDispatcher(path).forward(request, response);
        }

    }

    /**
     * Records its lifecycle, and blocks when it is initialized again until released.
     */
    private static final class LifecycleFilter implements Filter {

        private final List<String> events = new CopyOnWriteArrayList<>();
        private final CountDownLatch replacing;
        private final CountDownLatch replaced;

        LifecycleFilter(CountDownLatch replacing, CountDownLatch replaced) {
            this.replacing = replacing;
            this.replaced = replaced;
        }

        @Override
        public void init(FilterConfig config) {
            events.add("init");
            if (events.size() > 1) {
                replacing.countDown();
                try {
                    replaced.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            ((HttpServletResponse) response).setHeader("X-Filter", "filtered");
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
            events.add("destroy");
        }

    }

    /**
     * Blocks the requests it filters until released.
     */
    private static final class BlockingFilter implements Filter {

        private final List<String> events = new CopyOnWriteArrayList<>();
        private final CountDownLatch filtering;
        private final CountDownLatch release;

        BlockingFilter(CountDownLatch filtering, CountDownLatch release) {
            this.filtering = filtering;
            this.release = release;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            events.add("filtering");
            filtering.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            chain.doFilter(request, response);
            events.add("filtered");
        }

        @Override
        public void destroy() {
            events.add("destroy");
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
