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
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.karaf.services.web.WebElement;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

/**
 * A whiteboard context to deploy: a servlet context helper and the services associated to it.
 */
public class WhiteboardContextModel {

    private final String name;
    private final String path;
    // null for the default context of the whiteboard
    private final WhiteboardService helper;
    private final List<WhiteboardService> filters = new ArrayList<>();
    private final List<WhiteboardService> listeners = new ArrayList<>();
    private final List<WhiteboardService> candidateServlets = new ArrayList<>();
    private final List<ServletModel> servlets = new ArrayList<>();

    WhiteboardContextModel(String name, String path, WhiteboardService helper) {
        this.name = name;
        this.path = path;
        this.helper = helper;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public WhiteboardService getHelper() {
        return helper;
    }

    public List<WhiteboardService> getFilters() {
        return filters;
    }

    public List<WhiteboardService> getListeners() {
        return listeners;
    }

    public List<ServletModel> getServlets() {
        return servlets;
    }

    boolean matches(Filter filter) {
        if (helper != null) {
            return filter.match(helper.getReference());
        }
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, name);
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, path);
        properties.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
        return filter.match(properties);
    }

    void add(WhiteboardService service) {
        switch (service.getKind()) {
            case FILTER:
                filters.add(service);
                break;
            case LISTENER:
                listeners.add(service);
                break;
            default:
                candidateServlets.add(service);
        }
    }

    /**
     * Selects the servlets and resources to deploy: when several use the same name or the same pattern, the one
     * with the highest ranking is deployed and the others are shadowed.
     */
    void resolve(List<WebElement> failures) {
        Set<String> names = new HashSet<>();
        Set<String> patterns = new HashSet<>();
        for (WhiteboardService service : candidateServlets) {
            boolean resource = service.getKind() == WhiteboardService.Kind.RESOURCE;
            String[] servletPatterns = service.getStrings(resource
                    ? HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN
                    : HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
            String servletName = resource
                    ? "resource-" + service.getServiceId()
                    : service.getString(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME);
            String conflict = null;
            if (servletName != null && names.contains(servletName)) {
                conflict = "the servlet name " + servletName + " is already used";
            }
            for (String pattern : servletPatterns) {
                if (conflict == null && patterns.contains(pattern)) {
                    conflict = "the pattern " + pattern + " is already used";
                }
            }
            if (conflict != null) {
                failures.add(Whiteboard.element(service, servletName, path, Arrays.asList(servletPatterns),
                        WebElement.State.SHADOWED, conflict));
                continue;
            }
            if (servletName != null) {
                names.add(servletName);
            }
            patterns.addAll(Arrays.asList(servletPatterns));
            servlets.add(new ServletModel(service, servletName, servletPatterns));
        }
    }

    /**
     * @return a key changing when the helper, the filters or the listeners change, which requires to restart the
     *         context, unlike the servlets which are added and removed while the context runs.
     */
    String structureKey() {
        StringBuilder key = new StringBuilder(name).append('|').append(path).append('|');
        key.append(helper == null ? "default" : helper.key());
        for (WhiteboardService filter : filters) {
            key.append("|f").append(filter.key());
        }
        for (WhiteboardService listener : listeners) {
            key.append("|l").append(listener.key());
        }
        return key.toString();
    }

    /**
     * A servlet or a resource to deploy in a context.
     */
    public static final class ServletModel {

        private final WhiteboardService service;
        private final String name;
        private final String[] patterns;
        private final String key;

        ServletModel(WhiteboardService service, String name, String[] patterns) {
            this.service = service;
            this.name = name;
            this.patterns = patterns;
            this.key = service.key() + "|" + name + "|" + String.join(",", patterns);
        }

        public WhiteboardService getService() {
            return service;
        }

        /**
         * @return the name of the servlet, or {@code null} to use the class name of the servlet.
         */
        public String getName() {
            return name;
        }

        public String[] getPatterns() {
            return patterns;
        }

        String key() {
            return key;
        }

    }

}
