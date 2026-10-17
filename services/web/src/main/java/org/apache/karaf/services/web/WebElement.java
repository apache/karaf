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
package org.apache.karaf.services.web;

import java.util.Collections;
import java.util.List;

/**
 * A servlet context, a servlet or a filter known by the web container.
 */
public final class WebElement {

    public enum Type {
        /** A context of the OSGi Servlet Whiteboard, defined by a servlet context helper. */
        WHITEBOARD,
        /** A Web Application Bundle. */
        WAB,
        /** A servlet. */
        SERVLET,
        /** A whiteboard resource, served by a servlet of the web container. */
        RESOURCE,
        /** A filter. */
        FILTER,
        /** A whiteboard listener. */
        LISTENER
    }

    public enum State {
        DEPLOYED,
        /** Not deployed because another element with a higher ranking uses the same name, pattern or path. */
        SHADOWED,
        FAILED
    }

    private final Type type;
    private final String name;
    private final String contextPath;
    private final List<String> patterns;
    private final String className;
    private final long bundleId;
    private final long serviceId;
    private final State state;
    private final String message;

    public WebElement(Type type, String name, String contextPath, List<String> patterns, String className,
                      long bundleId, long serviceId, State state, String message) {
        this.type = type;
        this.name = name;
        this.contextPath = contextPath;
        this.patterns = patterns == null ? Collections.emptyList() : Collections.unmodifiableList(patterns);
        this.className = className;
        this.bundleId = bundleId;
        this.serviceId = serviceId;
        this.state = state;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the context path ({@code /} for the root context), or {@code null} when the element is not
     *         associated to any context.
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * @return the URL patterns of a servlet or of a filter, the servlet names a filter applies to are prefixed
     *         with {@code servlet:}.
     */
    public List<String> getPatterns() {
        return patterns;
    }

    public String getClassName() {
        return className;
    }

    public long getBundleId() {
        return bundleId;
    }

    /**
     * @return the id of the whiteboard service, or {@code -1} for the elements of a web application bundle.
     */
    public long getServiceId() {
        return serviceId;
    }

    public State getState() {
        return state;
    }

    /**
     * @return why the element is not deployed, or {@code null}.
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return type + " " + name + " [" + contextPath + "] " + state + (message != null ? " (" + message + ")" : "");
    }

}
