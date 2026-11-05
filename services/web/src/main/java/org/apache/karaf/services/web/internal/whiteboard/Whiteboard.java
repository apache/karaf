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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.karaf.services.web.WebElement;
import org.apache.karaf.services.web.internal.Contexts;
import org.apache.karaf.services.web.internal.ModelListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tracks the services of the OSGi Servlet Whiteboard, and computes the contexts to deploy.
 */
public class Whiteboard {

    static final String DEFAULT_SELECT = "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "="
            + HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME + ")";

    private static final String[] LISTENER_CLASSES = {
            "jakarta.servlet.ServletContextListener",
            "jakarta.servlet.ServletContextAttributeListener",
            "jakarta.servlet.ServletRequestListener",
            "jakarta.servlet.ServletRequestAttributeListener",
            "jakarta.servlet.http.HttpSessionListener",
            "jakarta.servlet.http.HttpSessionAttributeListener",
            "jakarta.servlet.http.HttpSessionIdListener"
    };

    private final BundleContext bundleContext;
    private final ModelListener listener;
    private final List<ServiceTracker<Object, WhiteboardService>> trackers = new ArrayList<>();
    // updated before the listener is notified, a tracker only knows a service once addingService returns
    private final Set<WhiteboardService> services = ConcurrentHashMap.newKeySet();

    public Whiteboard(BundleContext bundleContext, ModelListener listener) {
        this.bundleContext = bundleContext;
        this.listener = listener;
    }

    public void open() throws InvalidSyntaxException {
        track(WhiteboardService.Kind.HELPER, "(&(objectClass=org.osgi.service.servlet.context.ServletContextHelper)"
                + "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)"
                + "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=*))");
        track(WhiteboardService.Kind.SERVLET, "(&(objectClass=jakarta.servlet.Servlet)"
                + "(|(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=*)"
                + "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=*)))");
        track(WhiteboardService.Kind.RESOURCE, "(&(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + "=*)"
                + "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX + "=*))");
        track(WhiteboardService.Kind.FILTER, "(&(objectClass=jakarta.servlet.Filter)"
                + "(|(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN + "=*)"
                + "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET + "=*)"
                + "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX + "=*)))");
        StringBuilder listenerFilter = new StringBuilder("(&(")
                .append(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER).append("=true)(|");
        for (String listenerClass : LISTENER_CLASSES) {
            listenerFilter.append("(objectClass=").append(listenerClass).append(')');
        }
        track(WhiteboardService.Kind.LISTENER, listenerFilter.append("))").toString());
    }

    private void track(WhiteboardService.Kind kind, String filter) throws InvalidSyntaxException {
        ServiceTracker<Object, WhiteboardService> tracker = new ServiceTracker<>(bundleContext,
                bundleContext.createFilter(filter), new ServiceTrackerCustomizer<Object, WhiteboardService>() {
            @Override
            public WhiteboardService addingService(ServiceReference<Object> reference) {
                WhiteboardService service = new WhiteboardService(kind, reference);
                services.add(service);
                listener.changed(false);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<Object> reference, WhiteboardService service) {
                service.changed();
                listener.changed(false);
            }

            @Override
            public void removedService(ServiceReference<Object> reference, WhiteboardService service) {
                services.remove(service);
                listener.changed(true);
            }
        });
        trackers.add(tracker);
        tracker.open();
    }

    public void close() {
        for (ServiceTracker<Object, WhiteboardService> tracker : trackers) {
            tracker.close();
        }
        trackers.clear();
    }

    /**
     * @return {@code true} if the service is registered, {@code false} once it is being unregistered.
     */
    public boolean isRegistered(WhiteboardService service) {
        return services.contains(service);
    }

    /**
     * Computes the whiteboard contexts to deploy.
     *
     * @param usedPaths the context paths already used by web application bundles.
     * @param failures  the services which can not be deployed, with the reason.
     */
    public Collection<WhiteboardContextModel> model(Set<String> usedPaths, List<WebElement> failures) {
        List<WhiteboardService> sorted = new ArrayList<>();
        for (WhiteboardService service : services) {
            // the services of a stopping bundle are undeployed together, rather than one by one when they go
            Bundle bundle = service.getBundle();
            if (bundle != null && bundle.getState() != Bundle.STOPPING) {
                sorted.add(service);
            }
        }
        Collections.sort(sorted);

        // the contexts: by name then by path, the helper with the highest ranking wins
        Map<String, WhiteboardContextModel> contexts = new LinkedHashMap<>();
        Map<String, WhiteboardContextModel> paths = new LinkedHashMap<>();
        List<WhiteboardService> helpers = new ArrayList<>();
        for (WhiteboardService service : sorted) {
            if (service.getKind() == WhiteboardService.Kind.HELPER) {
                helpers.add(service);
            }
        }
        // the default context has the lowest ranking
        helpers.add(null);
        for (WhiteboardService helper : helpers) {
            String name = helper == null ? HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME
                    : helper.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);
            String path = helper == null ? "/"
                    : Contexts.normalizePath(helper.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH));
            if (path == null) {
                failures.add(element(helper, name, null, null, WebElement.State.FAILED, "invalid context path"));
            } else if (contexts.containsKey(name)) {
                if (helper != null) {
                    failures.add(element(helper, name, path, null, WebElement.State.SHADOWED,
                            "the context name is already used"));
                }
            } else if (usedPaths.contains(path) || paths.containsKey(path)) {
                failures.add(element(helper, name, path, null, WebElement.State.SHADOWED,
                        "the context path is already used"));
            } else {
                WhiteboardContextModel context = new WhiteboardContextModel(name, path, helper);
                contexts.put(name, context);
                paths.put(path, context);
            }
        }

        // the services, associated to all the contexts they select
        for (WhiteboardService service : sorted) {
            if (service.getKind() == WhiteboardService.Kind.HELPER) {
                continue;
            }
            String select = service.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
            Filter filter;
            try {
                filter = FrameworkUtil.createFilter(select == null ? DEFAULT_SELECT : select);
            } catch (InvalidSyntaxException e) {
                failures.add(element(service, null, null, null, WebElement.State.FAILED,
                        "invalid context selection " + select));
                continue;
            }
            boolean matched = false;
            for (WhiteboardContextModel context : contexts.values()) {
                if (context.matches(filter)) {
                    context.add(service);
                    matched = true;
                }
            }
            if (!matched) {
                failures.add(element(service, null, null, null, WebElement.State.FAILED,
                        "no servlet context matching " + (select == null ? DEFAULT_SELECT : select)));
            }
        }

        for (WhiteboardContextModel context : contexts.values()) {
            context.resolve(failures);
        }
        return contexts.values();
    }

    /**
     * Describes a whiteboard service, or the default context of the whiteboard when the service is {@code null}.
     */
    static WebElement element(WhiteboardService service, String name, String path, List<String> patterns,
                              WebElement.State state, String message) {
        if (service == null) {
            return new WebElement(WebElement.Type.WHITEBOARD, name, path, patterns, null, -1, -1, state, message);
        }
        WebElement.Type type;
        switch (service.getKind()) {
            case HELPER:
                type = WebElement.Type.WHITEBOARD;
                break;
            case RESOURCE:
                type = WebElement.Type.RESOURCE;
                break;
            case FILTER:
                type = WebElement.Type.FILTER;
                break;
            case LISTENER:
                type = WebElement.Type.LISTENER;
                break;
            default:
                type = WebElement.Type.SERVLET;
        }
        if (patterns == null) {
            String property = patternProperty(service.getKind());
            patterns = property == null ? List.of() : List.of(service.getStrings(property));
        }
        String[] objectClass = (String[]) service.getReference().getProperty("objectClass");
        return new WebElement(type, name, path, patterns, objectClass == null ? null : String.join(",", objectClass),
                service.getBundleId(), service.getServiceId(), state, message);
    }

    private static String patternProperty(WhiteboardService.Kind kind) {
        switch (kind) {
            case RESOURCE:
                return HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN;
            case FILTER:
                return HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN;
            case SERVLET:
                return HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;
            default:
                return null;
        }
    }

}
