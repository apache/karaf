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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.karaf.services.web.WebElement;
import org.apache.karaf.services.web.internal.Contexts;
import org.apache.karaf.services.web.internal.Deployment;
import org.apache.karaf.services.web.internal.WebConfig;
import org.apache.tomcat.SimpleInstanceManager;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
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
 * The servlets and the resources are added to and removed from the running context. A change of the servlet
 * context helper, of a filter or of a listener requires a new deployment of the context.
 */
public class WhiteboardDeployment implements Deployment {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhiteboardDeployment.class);

    private final Whiteboard whiteboard;
    private final StandardHost host;
    private final WebConfig config;
    private final BundleContext bundleContext;
    private final String name;
    private final String path;
    private final WhiteboardService helperService;
    private final String structureKey;

    private StandardContext context;
    private String failure;
    // the servlet context helper instances, by bundle using them
    private final Map<Bundle, Acquired<ServletContextHelper>> helpers = new HashMap<>();
    private final List<Acquired<?>> filtersAndListeners = new ArrayList<>();
    private final List<WebElement> filterElements = new ArrayList<>();
    private final List<WebElement> failedElements = new ArrayList<>();
    private final Map<String, DeployedServlet> servlets = new LinkedHashMap<>();

    public WhiteboardDeployment(WhiteboardContextModel model, Whiteboard whiteboard, StandardHost host,
                                WebConfig config, BundleContext bundleContext) {
        this.whiteboard = whiteboard;
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
     * @return {@code true} if the model can be applied to this deployment, {@code false} if the context has to be
     *         deployed again.
     */
    public boolean accepts(WhiteboardContextModel model) {
        return structureKey.equals(model.structureKey());
    }

    public void deploy(WhiteboardContextModel model) {
        context = Contexts.create(path, Whiteboard.class.getClassLoader(), null, config);
        Contexts.markConfigured(context);
        context.setInstanceManager(new SimpleInstanceManager());
        context.setDisplayName(name);
        if (helperService != null) {
            helperService.getInitParameters(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX)
                    .forEach(context::addParameter);
            ServletContextHelper helper = helper(helperService.getBundle());
            if (helper == null) {
                failure = "the servlet context helper is not available";
                release();
                context = null;
                return;
            }
            addFilter(SecurityFilter.NAME, new SecurityFilter(helper), SecurityFilter.class.getName(), true,
                    new String[]{"/*"}, new String[0], new String[0]);
        }
        for (WhiteboardService filter : model.getFilters()) {
            addFilter(filter);
        }
        for (WhiteboardService listener : model.getListeners()) {
            addListener(listener);
        }
        try {
            host.addChild(context);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to deploy the whiteboard context {} on {}", name, path, e);
        }
        if (!context.getState().isAvailable()) {
            failure = "the context failed to start, see the log";
            removeContext();
            release();
            return;
        }
        LOGGER.info("Whiteboard context {} deployed on {}", name, path);
        update(model);
    }

    /**
     * Adds and removes the servlets of the running context to match the model.
     */
    public void update(WhiteboardContextModel model) {
        if (context == null) {
            return;
        }
        Map<String, WhiteboardContextModel.ServletModel> wanted = new LinkedHashMap<>();
        for (WhiteboardContextModel.ServletModel servlet : model.getServlets()) {
            wanted.put(servlet.key(), servlet);
        }
        for (Iterator<Map.Entry<String, DeployedServlet>> it = servlets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, DeployedServlet> entry = it.next();
            if (!wanted.containsKey(entry.getKey())) {
                removeServlet(entry.getValue());
                it.remove();
            }
        }
        for (Map.Entry<String, WhiteboardContextModel.ServletModel> entry : wanted.entrySet()) {
            if (!servlets.containsKey(entry.getKey())) {
                servlets.put(entry.getKey(), addServlet(entry.getValue()));
            }
        }
    }

    private DeployedServlet addServlet(WhiteboardContextModel.ServletModel model) {
        WhiteboardService service = model.getService();
        Bundle bundle = service.getBundle();
        ServletContextHelper helper = helper(bundle);
        DeployedServlet deployed = new DeployedServlet(model);
        if (helper == null) {
            deployed.failure = "the servlet context helper is not available";
            return deployed;
        }
        Servlet servlet;
        boolean resource = service.getKind() == WhiteboardService.Kind.RESOURCE;
        if (resource) {
            servlet = new ResourceServlet(service.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX),
                    helper);
            deployed.className = ResourceServlet.class.getName();
        } else {
            deployed.acquired = acquire(service, Servlet.class);
            if (deployed.acquired == null) {
                deployed.failure = "the service is not a servlet";
                return deployed;
            }
            servlet = new WhiteboardServlet(deployed.acquired.instance, helper, classLoader(bundle,
                    deployed.acquired.instance));
            deployed.className = deployed.acquired.instance.getClass().getName();
        }
        String servletName = model.getName() != null ? model.getName() : deployed.className;
        if (context.findChild(servletName) != null) {
            servletName = servletName + "-" + service.getServiceId();
        }
        deployed.name = servletName;

        StandardWrapper wrapper = (StandardWrapper) context.createWrapper();
        wrapper.setName(servletName);
        wrapper.setServletClass(deployed.className);
        wrapper.setServlet(servlet);
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
                context.addServletMappingDecoded(pattern, servletName);
            }
            wrapper.load();
        } catch (ServletException | RuntimeException e) {
            LOGGER.error("Unable to deploy the servlet {} on {}", servletName, path, e);
            removeServlet(deployed);
            deployed.failure = "the servlet failed to start: " + e.getMessage();
        }
        return deployed;
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
        if (deployed.acquired != null) {
            deployed.acquired.release();
            deployed.acquired = null;
        }
    }

    private void addFilter(WhiteboardService service) {
        Acquired<Filter> acquired = acquire(service, Filter.class);
        ServletContextHelper helper = helper(service.getBundle());
        String filterName = service.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME);
        if (acquired == null || helper == null) {
            failedElements.add(Whiteboard.element(service, filterName, path, null, WebElement.State.FAILED,
                    acquired == null ? "the service is not a filter" : "the servlet context helper is not available"));
            if (acquired != null) {
                acquired.release();
            }
            return;
        }
        List<Pattern> regexes = new ArrayList<>();
        try {
            for (String regex : service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX)) {
                regexes.add(Pattern.compile(regex));
            }
        } catch (PatternSyntaxException e) {
            failedElements.add(Whiteboard.element(service, filterName, path, null, WebElement.State.FAILED,
                    "invalid regular expression " + e.getPattern()));
            acquired.release();
            return;
        }
        Filter filter = acquired.instance;
        if (filterName == null) {
            filterName = filter.getClass().getName();
        }
        if (context.findFilterDef(filterName) != null) {
            filterName = filterName + "-" + service.getServiceId();
        }
        String[] patterns = regexes.isEmpty()
                ? service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN) : new String[]{"/*"};
        String[] servletNames = regexes.isEmpty()
                ? service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET) : new String[0];
        String[] dispatchers = service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER);
        try {
            FilterDef filterDef = addFilter(filterName,
                    new WhiteboardFilter(filter, helper, classLoader(service.getBundle(), filter), regexes),
                    filter.getClass().getName(),
                    service.getBoolean(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED),
                    patterns, servletNames, dispatchers);
            service.getInitParameters(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX)
                    .forEach(filterDef::addInitParameter);
        } catch (IllegalArgumentException e) {
            failedElements.add(Whiteboard.element(service, filterName, path, null, WebElement.State.FAILED,
                    e.getMessage()));
            acquired.release();
            return;
        }
        filtersAndListeners.add(acquired);
        List<String> mappings = new ArrayList<>(Arrays.asList(patterns));
        for (String servletName : servletNames) {
            mappings.add("servlet:" + servletName);
        }
        for (String regex : service.getStrings(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX)) {
            mappings.add("regex:" + regex);
        }
        filterElements.add(Whiteboard.element(service, filterName, path, mappings, WebElement.State.DEPLOYED, null));
    }

    private FilterDef addFilter(String filterName, Filter filter, String className, boolean asyncSupported,
                                String[] patterns, String[] servletNames, String[] dispatchers) {
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(filterName);
        for (String pattern : patterns) {
            filterMap.addURLPatternDecoded(pattern);
        }
        for (String servletName : servletNames) {
            filterMap.addServletName(servletName);
        }
        for (String dispatcher : dispatchers) {
            filterMap.setDispatcher(dispatcher.toUpperCase(Locale.ENGLISH));
        }
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName(filterName);
        filterDef.setFilter(filter);
        filterDef.setFilterClass(className);
        filterDef.setAsyncSupported(Boolean.toString(asyncSupported));
        context.addFilterDef(filterDef);
        context.addFilterMap(filterMap);
        return filterDef;
    }

    private void addListener(WhiteboardService service) {
        Acquired<Object> acquired = acquire(service, Object.class);
        if (acquired == null) {
            failedElements.add(Whiteboard.element(service, null, path, null, WebElement.State.FAILED,
                    "the service is not available"));
            return;
        }
        Object listener = acquired.instance;
        if (listener instanceof ServletContextListener || listener instanceof HttpSessionListener) {
            context.addApplicationLifecycleListener(listener);
        }
        if (listener instanceof ServletContextAttributeListener || listener instanceof ServletRequestListener
                || listener instanceof ServletRequestAttributeListener
                || listener instanceof HttpSessionAttributeListener || listener instanceof HttpSessionIdListener) {
            context.addApplicationEventListener(listener);
        }
        filtersAndListeners.add(acquired);
    }

    /**
     * @return the servlet context helper used by the whiteboard services of the given bundle.
     */
    private ServletContextHelper helper(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        Acquired<ServletContextHelper> acquired = helpers.get(bundle);
        if (acquired == null) {
            if (helperService == null) {
                acquired = new Acquired<>(new ServletContextHelper(bundle) {
                }, null, null);
            } else {
                // as defined by the specification, the helper is got with the bundle context of the bundle using it
                BundleContext context = bundle.getBundleContext();
                if (context == null) {
                    return null;
                }
                acquired = acquire(helperService, context, ServletContextHelper.class);
                if (acquired == null) {
                    return null;
                }
            }
            helpers.put(bundle, acquired);
        }
        return acquired.instance;
    }

    private <T> Acquired<T> acquire(WhiteboardService service, Class<T> type) {
        return acquire(service, bundleContext, type);
    }

    @SuppressWarnings("unchecked")
    private <T> Acquired<T> acquire(WhiteboardService service, BundleContext context, Class<T> type) {
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
        return new Acquired<>((T) instance, service, serviceObjects);
    }

    private static ClassLoader classLoader(Bundle bundle, Object service) {
        BundleWiring wiring = bundle == null ? null : bundle.adapt(BundleWiring.class);
        ClassLoader classLoader = wiring == null ? null : wiring.getClassLoader();
        return classLoader != null ? classLoader : service.getClass().getClassLoader();
    }

    @Override
    public void undeploy() {
        if (context == null) {
            return;
        }
        for (DeployedServlet servlet : servlets.values()) {
            removeServlet(servlet);
        }
        servlets.clear();
        removeContext();
        release();
        LOGGER.info("Whiteboard context {} undeployed from {}", name, path);
    }

    private void removeContext() {
        try {
            host.removeChild(context);
        } catch (RuntimeException e) {
            LOGGER.warn("Error while undeploying the whiteboard context {}", path, e);
        }
        context = null;
    }

    private void release() {
        for (Acquired<?> acquired : filtersAndListeners) {
            acquired.release();
        }
        filtersAndListeners.clear();
        for (Acquired<?> acquired : helpers.values()) {
            acquired.release();
        }
        helpers.clear();
    }

    @Override
    public void describe(List<WebElement> contexts, List<WebElement> servletElements, List<WebElement> filters) {
        WebElement.State state = failure == null ? WebElement.State.DEPLOYED : WebElement.State.FAILED;
        if (helperService == null) {
            contexts.add(Whiteboard.element(null, name, path, null, state, failure));
        } else {
            contexts.add(Whiteboard.element(helperService, name, path, null, state, failure));
        }
        for (DeployedServlet servlet : servlets.values()) {
            WhiteboardService service = servlet.model.getService();
            WebElement.Type type = service.getKind() == WhiteboardService.Kind.RESOURCE
                    ? WebElement.Type.RESOURCE : WebElement.Type.SERVLET;
            List<String> patterns = Arrays.asList(servlet.model.getPatterns());
            if (type == WebElement.Type.RESOURCE) {
                patterns = new ArrayList<>(patterns);
                patterns.add("prefix:" + service.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX));
            }
            servletElements.add(new WebElement(type, servlet.name != null ? servlet.name : servlet.model.getName(),
                    path, patterns, servlet.className, service.getBundleId(), service.getServiceId(),
                    servlet.failure == null ? WebElement.State.DEPLOYED : WebElement.State.FAILED, servlet.failure));
        }
        filters.addAll(filterElements);
        for (WebElement element : failedElements) {
            if (element.getType() == WebElement.Type.FILTER) {
                filters.add(element);
            }
        }
    }

    /**
     * @return the elements which failed while deploying the context, to be logged.
     */
    public List<WebElement> getFailures() {
        List<WebElement> failures = new ArrayList<>(failedElements);
        for (DeployedServlet servlet : servlets.values()) {
            if (servlet.failure != null) {
                WhiteboardService service = servlet.model.getService();
                failures.add(Whiteboard.element(service, servlet.model.getName(), path,
                        Arrays.asList(servlet.model.getPatterns()), WebElement.State.FAILED, servlet.failure));
            }
        }
        return failures;
    }

    private static final class DeployedServlet {

        private final WhiteboardContextModel.ServletModel model;
        private String name;
        private String className;
        private StandardWrapper wrapper;
        private Acquired<Servlet> acquired;
        private String failure;

        DeployedServlet(WhiteboardContextModel.ServletModel model) {
            this.model = model;
        }

    }

    /**
     * A service object got for the web container, released when it is not used anymore.
     */
    private final class Acquired<T> {

        private final T instance;
        private final WhiteboardService owner;
        private final ServiceObjects<Object> serviceObjects;

        Acquired(T instance, WhiteboardService owner, ServiceObjects<Object> serviceObjects) {
            this.instance = instance;
            this.owner = owner;
            this.serviceObjects = serviceObjects;
        }

        void release() {
            // a service being unregistered is released by the framework, once the unregistering event is processed
            if (serviceObjects == null || !whiteboard.isRegistered(owner)) {
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
