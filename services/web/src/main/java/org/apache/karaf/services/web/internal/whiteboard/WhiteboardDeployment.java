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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.karaf.services.web.WebElement;
import org.apache.karaf.services.web.internal.Contexts;
import org.apache.karaf.services.web.internal.Deployment;
import org.apache.karaf.services.web.internal.WebConfig;
import org.apache.tomcat.SimpleInstanceManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A whiteboard context deployed in Tomcat.
 * <p>
 * The listeners, the filters, the servlets and the resources are added to and removed from the running context. A
 * change of the servlet context helper requires a new deployment of the context.
 */
public class WhiteboardDeployment extends Deployment {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhiteboardDeployment.class);

    private final StandardHost host;
    private final WebConfig config;
    private final BundleContext bundleContext;
    private final String name;
    private final String path;
    private final WhiteboardService helperService;
    private final String structureKey;
    // applied by the servlets of the context
    private final WhiteboardFilters filterChain = new WhiteboardFilters();

    private StandardContext context;
    private String failure;
    private volatile boolean undeployed;
    // the services deployed in the context, read without the lock of the deployment by the threads removing a service
    private final Set<WhiteboardService> used = new HashSet<>();
    // the servlet context helper and the servlet context used by the services of each bundle
    private final Map<Bundle, BundleHelper> helpers = new HashMap<>();
    // in the order of their deployment
    private final Map<String, DeployedListener> listeners = new LinkedHashMap<>();
    private final Map<String, DeployedFilter> filters = new LinkedHashMap<>();
    private final Map<String, DeployedServlet> servlets = new LinkedHashMap<>();

    public WhiteboardDeployment(WhiteboardContextModel model, StandardHost host, WebConfig config,
                                BundleContext bundleContext) {
        this.host = host;
        this.config = config;
        this.bundleContext = bundleContext;
        this.name = model.getName();
        this.path = model.getPath();
        this.helperService = model.getHelper();
        this.structureKey = model.structureKey();
    }

    @Override
    public String getPath() {
        return path;
    }

    /**
     * @return {@code true} if the service is the servlet context helper of the context, or one of its servlets,
     *         filters or listeners: its removal requires the lock of the deployment.
     */
    public boolean uses(WhiteboardService service) {
        if (service == helperService) {
            return true;
        }
        synchronized (used) {
            return used.contains(service);
        }
    }

    /**
     * Marks a service as used before it is deployed, unless it has been removed meanwhile: a thread removing it then
     * either sees it used, or is seen by this one.
     */
    private boolean use(WhiteboardService service) {
        synchronized (used) {
            if (service.isRemoved()) {
                return false;
            }
            used.add(service);
            return true;
        }
    }

    /**
     * Marks a service as not used anymore, once it is undeployed.
     */
    private void unuse(Deployed deployed) {
        synchronized (used) {
            used.remove(deployed.service);
        }
    }

    /**
     * @return {@code true} if the model can be applied to this deployment, {@code false} if the context has to be
     *         deployed again.
     */
    public boolean accepts(WhiteboardContextModel model) {
        return !undeployed && structureKey.equals(model.structureKey());
    }

    public void deploy(WhiteboardContextModel model) {
        try {
            if (start()) {
                LOGGER.info("Whiteboard context {} deployed on {}", name, path);
                update(model);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Unable to deploy the whiteboard context {} on {}", name, path, e);
            failure = "the context failed to deploy: " + e.getMessage();
            remove();
        }
    }

    /**
     * @return {@code true} if the context is started, {@code false} if it failed.
     */
    private boolean start() {
        context = Contexts.create(path, Whiteboard.class.getClassLoader(), null, config);
        Contexts.markConfigured(context);
        context.setInstanceManager(new SimpleInstanceManager());
        context.setDisplayName(name);
        if (helperService != null) {
            helperService.getInitParameters(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX)
                    .forEach(context::addParameter);
        }
        try {
            host.addChild(context);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to deploy the whiteboard context {} on {}", name, path, e);
        }
        if (!context.getState().isAvailable()) {
            failure = "the context failed to start, see the log";
            remove();
            return false;
        }
        // checked once the context runs, its servlet context being given with the helper; the helper may have been
        // removed by another thread since the model was computed
        if (helperService != null && (helperService.isRemoved() || helper(helperService.getBundle()) == null)) {
            failure = "the servlet context helper is not available";
            remove();
            return false;
        }
        return true;
    }

    /**
     * Adds and removes the listeners, the filters and the servlets of the running context to match the model.
     */
    public void update(WhiteboardContextModel model) {
        if (context == null) {
            return;
        }
        // the services removed by another thread since the model was computed are not deployed again
        Map<String, WhiteboardContextModel.ServletModel> wantedServlets = new LinkedHashMap<>();
        for (WhiteboardContextModel.ServletModel servlet : model.getServlets()) {
            if (!servlet.getService().isRemoved()) {
                wantedServlets.put(servlet.key(), servlet);
            }
        }
        Map<String, WhiteboardService> wantedFilters = new LinkedHashMap<>();
        for (WhiteboardService filter : model.getFilters()) {
            if (!filter.isRemoved()) {
                wantedFilters.put(DeployedFilter.key(filter), filter);
            }
        }
        Map<String, WhiteboardService> wantedListeners = new LinkedHashMap<>();
        for (WhiteboardService listener : model.getListeners()) {
            if (!listener.isRemoved()) {
                wantedListeners.put(DeployedListener.key(listener), listener);
            }
        }
        removeIf((key, service) -> !wantedServlets.containsKey(key) && !wantedFilters.containsKey(key)
                && !wantedListeners.containsKey(key));

        // added in the order the servlet specification starts them: the listeners, the filters and the servlets
        for (Map.Entry<String, WhiteboardService> entry : wantedListeners.entrySet()) {
            if (!listeners.containsKey(entry.getKey()) && use(entry.getValue())) {
                listeners.put(entry.getKey(), addListener(entry.getValue()));
            }
        }
        boolean filtersAdded = false;
        for (Map.Entry<String, WhiteboardService> entry : wantedFilters.entrySet()) {
            if (!filters.containsKey(entry.getKey()) && use(entry.getValue())) {
                filters.put(entry.getKey(), addFilter(entry.getValue()));
                filtersAdded = true;
            }
        }
        if (filtersAdded) {
            publishFilters();
        }
        for (Map.Entry<String, WhiteboardContextModel.ServletModel> entry : wantedServlets.entrySet()) {
            if (!servlets.containsKey(entry.getKey()) && use(entry.getValue().getService())) {
                servlets.put(entry.getKey(), addServlet(entry.getValue()));
            }
        }
    }

    /**
     * Undeploys a service going away: its servlet, filter or listener, or the context for its servlet context helper.
     */
    public void remove(WhiteboardService service) {
        if (context == null) {
            return;
        }
        if (service == helperService) {
            undeploy();
        } else {
            removeIf((key, deployed) -> deployed == service);
        }
    }

    /**
     * Removes the servlets, then the filters, then the listeners which are not wanted anymore, in the reverse order of
     * their deployment.
     */
    private void removeIf(BiPredicate<String, WhiteboardService> unwanted) {
        removeServletsAndFilters(unwanted);
        List<DeployedListener> removedListeners = extract(listeners, unwanted);
        Collections.reverse(removedListeners);
        for (DeployedListener listener : removedListeners) {
            removeListener(listener);
            unuse(listener);
        }
    }

    private void removeServletsAndFilters(BiPredicate<String, WhiteboardService> unwanted) {
        for (DeployedServlet servlet : extract(servlets, unwanted)) {
            removeServlet(servlet);
            unuse(servlet);
        }
        List<DeployedFilter> removedFilters = extract(filters, unwanted);
        if (!removedFilters.isEmpty()) {
            // not applied anymore before they are destroyed
            publishFilters();
            for (DeployedFilter filter : removedFilters) {
                removeFilter(filter);
                unuse(filter);
            }
        }
    }

    private static <T extends Deployed> List<T> extract(Map<String, T> deployed,
                                                        BiPredicate<String, WhiteboardService> unwanted) {
        List<T> extracted = new ArrayList<>();
        for (Iterator<Map.Entry<String, T>> it = deployed.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, T> entry = it.next();
            if (unwanted.test(entry.getKey(), entry.getValue().service)) {
                extracted.add(entry.getValue());
                it.remove();
            }
        }
        return extracted;
    }

    private DeployedServlet addServlet(WhiteboardContextModel.ServletModel model) {
        WhiteboardService service = model.getService();
        Bundle bundle = service.getBundle();
        DeployedServlet deployed = new DeployedServlet(model);
        BundleHelper helper = helper(bundle);
        if (helper == null) {
            deployed.failure = "the servlet context helper is not available";
            return deployed;
        }
        Servlet servlet;
        boolean resource = service.getKind() == WhiteboardService.Kind.RESOURCE;
        if (resource) {
            servlet = new ResourceServlet(resourcePrefix(service), helper.instance());
        } else {
            deployed.acquired = acquire(service, Servlet.class);
            if (deployed.acquired == null) {
                deployed.failure = "the service is not a servlet";
                return deployed;
            }
            servlet = (Servlet) deployed.acquired.instance;
        }
        deployed.className = servlet.getClass().getName();
        // the name the filters select the servlet with
        String servletName = model.getName() != null ? model.getName() : deployed.className;
        // unique in the context
        deployed.name = context.findChild(servletName) == null ? servletName
                : servletName + "-" + service.getServiceId();

        StandardWrapper wrapper = (StandardWrapper) context.createWrapper();
        wrapper.setName(deployed.name);
        wrapper.setServletClass(deployed.className);
        wrapper.setServlet(new WhiteboardServlet(servlet, servletName, helper.instance(), helper.servletContext,
                classLoader(bundle, servlet), filterChain));
        if (!resource) {
            wrapper.setAsyncSupported(service.getBoolean(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED));
            service.getInitParameters(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX)
                    .forEach(wrapper::addInitParameter);
            if (service.getBoolean(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED)) {
                wrapper.setMultipartConfigElement(multipartConfig(service));
            }
        }
        try {
            context.addChild(wrapper);
            deployed.wrapper = wrapper;
            for (String pattern : model.getPatterns()) {
                context.addServletMappingDecoded(pattern, deployed.name);
            }
            wrapper.load();
        } catch (ServletException | RuntimeException e) {
            LOGGER.error("Unable to deploy the servlet {} on {}", deployed.name, path, e);
            removeServlet(deployed);
            deployed.failure = "the servlet failed to start: " + e.getMessage();
        }
        return deployed;
    }

    /**
     * @return the resource prefix, the empty string for the root of the resources.
     */
    private static String resourcePrefix(WhiteboardService service) {
        String prefix = service.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
        return prefix == null ? "" : prefix;
    }

    private static MultipartConfigElement multipartConfig(WhiteboardService service) {
        String location = service.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_LOCATION);
        return new MultipartConfigElement(location == null ? "" : location,
                getLong(service, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE, -1L),
                getLong(service, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXREQUESTSIZE, -1L),
                (int) getLong(service, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_FILESIZETHRESHOLD, 0));
    }

    private static long getLong(WhiteboardService service, String key, long def) {
        String value = service.getString(key);
        try {
            return value == null ? def : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void removeServlet(DeployedServlet deployed) {
        if (deployed.wrapper != null) {
            try {
                for (String pattern : deployed.wrapper.findMappings()) {
                    context.removeServletMapping(pattern);
                }
                context.removeChild(deployed.wrapper);
            } catch (RuntimeException e) {
                LOGGER.warn("Error while undeploying the servlet {} from {}", deployed.name, path, e);
            }
            deployed.wrapper = null;
        }
        deployed.release();
    }

    private DeployedFilter addFilter(WhiteboardService service) {
        DeployedFilter deployed = new DeployedFilter(service);
        Bundle bundle = service.getBundle();
        BundleHelper helper = helper(bundle);
        if (helper == null) {
            deployed.failure = "the servlet context helper is not available";
            return deployed;
        }
        List<Pattern> regexes = new ArrayList<>();
        try {
            for (String regex : service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX)) {
                regexes.add(Pattern.compile(regex));
            }
        } catch (PatternSyntaxException e) {
            deployed.failure = "invalid regular expression " + e.getPattern();
            return deployed;
        }
        String[] patterns = service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN);
        for (String pattern : patterns) {
            if (!WhiteboardFilter.isValid(pattern)) {
                deployed.failure = "invalid pattern " + pattern;
                return deployed;
            }
        }
        Set<DispatcherType> dispatcherTypes = EnumSet.noneOf(DispatcherType.class);
        for (String dispatcher : service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER)) {
            try {
                dispatcherTypes.add(DispatcherType.valueOf(dispatcher.toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                deployed.failure = "invalid dispatcher " + dispatcher;
                return deployed;
            }
        }
        if (dispatcherTypes.isEmpty()) {
            dispatcherTypes.add(DispatcherType.REQUEST);
        }
        deployed.acquired = acquire(service, Filter.class);
        if (deployed.acquired == null) {
            deployed.failure = "the service is not a filter";
            return deployed;
        }
        Filter filter = (Filter) deployed.acquired.instance;
        if (deployed.name == null) {
            deployed.name = filter.getClass().getName();
        }
        WhiteboardFilter whiteboardFilter = new WhiteboardFilter(filter, classLoader(bundle, filter), regexes,
                patterns, service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET),
                dispatcherTypes, service.getBoolean(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED));
        try {
            whiteboardFilter.init(ServletContexts.filterConfig(deployed.name, helper.servletContext,
                    service.getInitParameters(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX)));
        } catch (ServletException | RuntimeException e) {
            LOGGER.error("Unable to deploy the filter {} on {}", deployed.name, path, e);
            deployed.failure = "the filter failed to start: " + e.getMessage();
            deployed.release();
            return deployed;
        }
        deployed.filter = whiteboardFilter;
        return deployed;
    }

    /**
     * Applies the deployed filters to the requests, in their ranking order.
     */
    private void publishFilters() {
        List<DeployedFilter> deployed = new ArrayList<>();
        for (DeployedFilter filter : filters.values()) {
            if (filter.filter != null) {
                deployed.add(filter);
            }
        }
        deployed.sort((first, second) -> first.service.compareTo(second.service));
        List<WhiteboardFilter> chain = new ArrayList<>();
        for (DeployedFilter filter : deployed) {
            chain.add(filter.filter);
        }
        filterChain.set(chain);
    }

    private void removeFilter(DeployedFilter deployed) {
        if (deployed.filter != null) {
            try {
                deployed.filter.destroy();
            } catch (RuntimeException e) {
                LOGGER.warn("Error while undeploying the filter {} from {}", deployed.name, path, e);
            }
            deployed.filter = null;
        }
        deployed.release();
    }

    private DeployedListener addListener(WhiteboardService service) {
        DeployedListener deployed = new DeployedListener(service);
        Bundle bundle = service.getBundle();
        BundleHelper helper = helper(bundle);
        if (helper == null) {
            deployed.failure = "the servlet context helper is not available";
            return deployed;
        }
        deployed.acquired = acquire(service, Object.class);
        if (deployed.acquired == null) {
            deployed.failure = "the service is not available";
            return deployed;
        }
        Object instance = deployed.acquired.instance;
        WhiteboardListener listener = new WhiteboardListener(instance, helper.servletContext,
                classLoader(bundle, instance));
        if (listener.isEventListener()) {
            context.addApplicationEventListener(listener);
        }
        if (listener.isLifecycleListener()) {
            context.addApplicationLifecycleListener(listener);
        }
        try {
            // the context is already initialized
            listener.contextInitialized();
        } catch (RuntimeException e) {
            LOGGER.error("Unable to deploy the listener {} on {}", instance.getClass().getName(), path, e);
            deployed.failure = "the listener failed to start: " + e.getMessage();
            unregister(listener);
            deployed.release();
            return deployed;
        }
        deployed.listener = listener;
        return deployed;
    }

    private void removeListener(DeployedListener deployed) {
        if (deployed.listener != null) {
            try {
                deployed.listener.contextDestroyed();
            } catch (RuntimeException e) {
                LOGGER.warn("Error while undeploying the listener {} from {}", deployed.service, path, e);
            }
            unregister(deployed.listener);
            deployed.listener = null;
        }
        deployed.release();
    }

    private void unregister(WhiteboardListener listener) {
        context.setApplicationEventListeners(without(context.getApplicationEventListeners(), listener));
        context.setApplicationLifecycleListeners(without(context.getApplicationLifecycleListeners(), listener));
    }

    private static Object[] without(Object[] listeners, Object listener) {
        List<Object> remaining = new ArrayList<>(Arrays.asList(listeners));
        remaining.remove(listener);
        return remaining.toArray();
    }

    /**
     * @return the servlet context helper and the servlet context used by the whiteboard services of the given bundle,
     *         {@code null} if the helper is not available.
     */
    private BundleHelper helper(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        BundleHelper helper = helpers.get(bundle);
        if (helper != null && helper.acquired.bundleContext != null
                && helper.acquired.bundleContext != bundle.getBundleContext()) {
            // the bundle has been stopped since the helper was got: the framework has released it
            helpers.remove(bundle);
            helper = null;
        }
        if (helper == null) {
            Acquired acquired;
            if (helperService == null) {
                acquired = new Acquired(new ServletContextHelper(bundle) {
                }, null, null, null);
            } else {
                // as defined by the specification, the helper is got with the bundle context of the bundle using it
                BundleContext usingContext = bundle.getBundleContext();
                if (usingContext == null) {
                    return null;
                }
                acquired = acquire(helperService, usingContext, ServletContextHelper.class);
                if (acquired == null) {
                    return null;
                }
            }
            ServletContextHelper instance = (ServletContextHelper) acquired.instance;
            helper = new BundleHelper(acquired, ServletContexts.wrap(context.getServletContext(), instance,
                    classLoader(bundle, instance)));
            helpers.put(bundle, helper);
        }
        return helper;
    }

    private Acquired acquire(WhiteboardService service, Class<?> type) {
        return acquire(service, bundleContext, type);
    }

    @SuppressWarnings("unchecked")
    private static Acquired acquire(WhiteboardService service, BundleContext context, Class<?> type) {
        ServiceObjects<Object> serviceObjects;
        try {
            serviceObjects = (ServiceObjects<Object>) context.getServiceObjects(service.getReference());
        } catch (IllegalStateException e) {
            return null;
        }
        Object instance = serviceObjects == null ? null : serviceObjects.getService();
        if (instance == null) {
            return null;
        }
        if (!type.isInstance(instance)) {
            serviceObjects.ungetService(instance);
            return null;
        }
        return new Acquired(instance, service, serviceObjects, context);
    }

    private static ClassLoader classLoader(Bundle bundle, Object service) {
        BundleWiring wiring = bundle == null ? null : bundle.adapt(BundleWiring.class);
        ClassLoader classLoader = wiring == null ? null : wiring.getClassLoader();
        return classLoader != null ? classLoader : service.getClass().getClassLoader();
    }

    @Override
    public void undeploy() {
        undeployed = true;
        if (context == null) {
            return;
        }
        remove();
        LOGGER.info("Whiteboard context {} undeployed from {}", name, path);
    }

    /**
     * Removes the context, its servlets, its filters and its listeners, and releases the services they use.
     */
    private void remove() {
        if (context != null) {
            removeServletsAndFilters((key, service) -> true);
            // the listeners are notified by Tomcat as the context stops, once its sessions are destroyed
            Contexts.remove(host, context, path);
            context = null;
            for (DeployedListener listener : listeners.values()) {
                listener.release();
                unuse(listener);
            }
            listeners.clear();
        }
        releaseHelpers();
    }

    private void releaseHelpers() {
        for (BundleHelper helper : helpers.values()) {
            helper.acquired.release();
        }
        helpers.clear();
    }

    @Override
    public void describe(List<WebElement> contexts, List<WebElement> servletElements,
                         List<WebElement> filterElements) {
        contexts.add(Whiteboard.element(helperService, name, path, null,
                failure == null ? WebElement.State.DEPLOYED : WebElement.State.FAILED, failure));
        for (DeployedServlet servlet : servlets.values()) {
            WhiteboardService service = servlet.service;
            WebElement.Type type = service.getKind() == WhiteboardService.Kind.RESOURCE
                    ? WebElement.Type.RESOURCE : WebElement.Type.SERVLET;
            List<String> patterns = Arrays.asList(servlet.model.getPatterns());
            if (type == WebElement.Type.RESOURCE) {
                patterns = new ArrayList<>(patterns);
                patterns.add("prefix:" + resourcePrefix(service));
            }
            servletElements.add(new WebElement(type, servlet.name != null ? servlet.name : servlet.model.getName(),
                    path, patterns, servlet.className, service.getBundleId(), service.getServiceId(),
                    servlet.state(), servlet.failure));
        }
        for (DeployedFilter filter : filters.values()) {
            filterElements.add(Whiteboard.element(filter.service, filter.name, path, filterMappings(filter.service),
                    filter.state(), filter.failure));
        }
    }

    private static List<String> filterMappings(WhiteboardService service) {
        List<String> mappings = new ArrayList<>(
                Arrays.asList(service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN)));
        for (String servletName : service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET)) {
            mappings.add("servlet:" + servletName);
        }
        for (String regex : service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX)) {
            mappings.add("regex:" + regex);
        }
        return mappings;
    }

    /**
     * @return the elements which failed to deploy, to be logged.
     */
    public List<WebElement> getFailures() {
        List<WebElement> failures = new ArrayList<>();
        for (DeployedServlet servlet : servlets.values()) {
            if (servlet.failure != null) {
                failures.add(Whiteboard.element(servlet.service, servlet.model.getName(), path,
                        Arrays.asList(servlet.model.getPatterns()), WebElement.State.FAILED, servlet.failure));
            }
        }
        for (DeployedFilter filter : filters.values()) {
            if (filter.failure != null) {
                failures.add(Whiteboard.element(filter.service, filter.name, path, null, WebElement.State.FAILED,
                        filter.failure));
            }
        }
        for (DeployedListener listener : listeners.values()) {
            if (listener.failure != null) {
                failures.add(Whiteboard.element(listener.service, null, path, null, WebElement.State.FAILED,
                        listener.failure));
            }
        }
        return failures;
    }

    /**
     * A whiteboard service deployed in the context, or which failed to deploy.
     */
    private abstract static class Deployed {

        final WhiteboardService service;
        String name;
        Acquired acquired;
        String failure;

        Deployed(WhiteboardService service, String name) {
            this.service = service;
            this.name = name;
        }

        WebElement.State state() {
            return failure == null ? WebElement.State.DEPLOYED : WebElement.State.FAILED;
        }

        void release() {
            if (acquired != null) {
                acquired.release();
                acquired = null;
            }
        }

    }

    private static final class DeployedServlet extends Deployed {

        private final WhiteboardContextModel.ServletModel model;
        private String className;
        private StandardWrapper wrapper;

        DeployedServlet(WhiteboardContextModel.ServletModel model) {
            super(model.getService(), null);
            this.model = model;
        }

    }

    private static final class DeployedFilter extends Deployed {

        private WhiteboardFilter filter;

        DeployedFilter(WhiteboardService service) {
            super(service, service.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME));
        }

        static String key(WhiteboardService service) {
            return "filter|" + service.key();
        }

    }

    private static final class DeployedListener extends Deployed {

        private WhiteboardListener listener;

        DeployedListener(WhiteboardService service) {
            super(service, null);
        }

        static String key(WhiteboardService service) {
            return "listener|" + service.key();
        }

    }

    /**
     * The servlet context helper used by the services of a bundle, and the servlet context they see.
     */
    private static final class BundleHelper {

        private final Acquired acquired;
        private final ServletContext servletContext;

        BundleHelper(Acquired acquired, ServletContext servletContext) {
            this.acquired = acquired;
            this.servletContext = servletContext;
        }

        ServletContextHelper instance() {
            return (ServletContextHelper) acquired.instance;
        }

    }

    /**
     * A service object got for the web container, released when it is not used anymore.
     */
    private static final class Acquired {

        private final Object instance;
        private final WhiteboardService owner;
        private final ServiceObjects<Object> serviceObjects;
        // the bundle context the service is got with
        private final BundleContext bundleContext;

        Acquired(Object instance, WhiteboardService owner, ServiceObjects<Object> serviceObjects,
                 BundleContext bundleContext) {
            this.instance = instance;
            this.owner = owner;
            this.serviceObjects = serviceObjects;
            this.bundleContext = bundleContext;
        }

        void release() {
            // a service being unregistered is released by the framework, once the unregistering event is processed;
            // a service no longer tracked because its properties changed is still registered, and is released here
            if (serviceObjects == null || owner.isUnregistering()) {
                return;
            }
            try {
                serviceObjects.ungetService(instance);
            } catch (IllegalStateException | IllegalArgumentException e) {
                // the service is unregistered, the framework has released it
            }
        }

    }

}
