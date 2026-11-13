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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeEvent;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequestAttributeEvent;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

import org.apache.karaf.services.web.internal.ContextClassLoader;

/**
 * A whiteboard listener deployed in Tomcat: it is notified with the servlet context of its bundle, and with the class
 * loader of its bundle as thread context class loader.
 * <p>
 * Being added to the running context, a servlet context listener is notified when it is deployed, rather than by
 * Tomcat when the context starts. It is notified when it is undeployed, or by Tomcat when the context stops.
 */
final class WhiteboardListener implements ServletContextListener, ServletContextAttributeListener,
        ServletRequestListener, ServletRequestAttributeListener, HttpSessionListener, HttpSessionAttributeListener,
        HttpSessionIdListener {

    private final Object listener;
    private final ServletContext servletContext;
    private final ClassLoader classLoader;

    WhiteboardListener(Object listener, ServletContext servletContext, ClassLoader classLoader) {
        this.listener = listener;
        this.servletContext = servletContext;
        this.classLoader = classLoader;
    }

    /**
     * @return {@code true} if Tomcat notifies it of the attributes of the context, of the requests and of the sessions.
     */
    boolean isEventListener() {
        return listener instanceof ServletContextAttributeListener || listener instanceof ServletRequestListener
                || listener instanceof ServletRequestAttributeListener
                || listener instanceof HttpSessionAttributeListener || listener instanceof HttpSessionIdListener;
    }

    /**
     * @return {@code true} if Tomcat notifies it of the destruction of the context, and of the creation and the
     *         destruction of the sessions.
     */
    boolean isLifecycleListener() {
        return listener instanceof ServletContextListener || listener instanceof HttpSessionListener;
    }

    void contextInitialized() {
        if (listener instanceof ServletContextListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletContextListener) listener).contextInitialized(new ServletContextEvent(servletContext));
            }
        }
    }

    void contextDestroyed() {
        if (listener instanceof ServletContextListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletContextListener) listener).contextDestroyed(new ServletContextEvent(servletContext));
            }
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        contextInitialized();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        contextDestroyed();
    }

    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        if (listener instanceof ServletContextAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletContextAttributeListener) listener).attributeAdded(
                        new ServletContextAttributeEvent(servletContext, event.getName(), event.getValue()));
            }
        }
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
        if (listener instanceof ServletContextAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletContextAttributeListener) listener).attributeRemoved(
                        new ServletContextAttributeEvent(servletContext, event.getName(), event.getValue()));
            }
        }
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        if (listener instanceof ServletContextAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletContextAttributeListener) listener).attributeReplaced(
                        new ServletContextAttributeEvent(servletContext, event.getName(), event.getValue()));
            }
        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        if (listener instanceof ServletRequestListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletRequestListener) listener).requestInitialized(
                        new ServletRequestEvent(servletContext, event.getServletRequest()));
            }
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        if (listener instanceof ServletRequestListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletRequestListener) listener).requestDestroyed(
                        new ServletRequestEvent(servletContext, event.getServletRequest()));
            }
        }
    }

    @Override
    public void attributeAdded(ServletRequestAttributeEvent event) {
        if (listener instanceof ServletRequestAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletRequestAttributeListener) listener).attributeAdded(new ServletRequestAttributeEvent(
                        servletContext, event.getServletRequest(), event.getName(), event.getValue()));
            }
        }
    }

    @Override
    public void attributeRemoved(ServletRequestAttributeEvent event) {
        if (listener instanceof ServletRequestAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletRequestAttributeListener) listener).attributeRemoved(new ServletRequestAttributeEvent(
                        servletContext, event.getServletRequest(), event.getName(), event.getValue()));
            }
        }
    }

    @Override
    public void attributeReplaced(ServletRequestAttributeEvent event) {
        if (listener instanceof ServletRequestAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((ServletRequestAttributeListener) listener).attributeReplaced(new ServletRequestAttributeEvent(
                        servletContext, event.getServletRequest(), event.getName(), event.getValue()));
            }
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        if (listener instanceof HttpSessionListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((HttpSessionListener) listener).sessionCreated(event);
            }
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        if (listener instanceof HttpSessionListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((HttpSessionListener) listener).sessionDestroyed(event);
            }
        }
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        if (listener instanceof HttpSessionAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((HttpSessionAttributeListener) listener).attributeAdded(event);
            }
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        if (listener instanceof HttpSessionAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((HttpSessionAttributeListener) listener).attributeRemoved(event);
            }
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        if (listener instanceof HttpSessionAttributeListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((HttpSessionAttributeListener) listener).attributeReplaced(event);
            }
        }
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        if (listener instanceof HttpSessionIdListener) {
            try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
                ((HttpSessionIdListener) listener).sessionIdChanged(event, oldSessionId);
            }
        }
    }

}
