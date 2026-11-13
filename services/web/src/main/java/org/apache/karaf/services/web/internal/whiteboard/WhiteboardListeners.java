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
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The whiteboard listeners of a context, notified by a single listener registered in Tomcat, so that they can be added
 * to and removed from the running context without changing the listeners of Tomcat while it notifies them.
 * <p>
 * The listeners are notified in the order they are deployed, and in the reverse order for a destruction. A listener
 * failing does not prevent the others from being notified.
 */
final class WhiteboardListeners implements ServletContextListener, ServletContextAttributeListener,
        ServletRequestListener, ServletRequestAttributeListener, HttpSessionListener, HttpSessionAttributeListener,
        HttpSessionIdListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhiteboardListeners.class);

    private volatile List<WhiteboardListener> listeners = List.of();

    void set(List<WhiteboardListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    private void notify(Consumer<WhiteboardListener> event) {
        notify(listeners, event);
    }

    private void notifyReversed(Consumer<WhiteboardListener> event) {
        List<WhiteboardListener> reversed = new ArrayList<>(listeners);
        Collections.reverse(reversed);
        notify(reversed, event);
    }

    private static void notify(List<WhiteboardListener> listeners, Consumer<WhiteboardListener> event) {
        for (WhiteboardListener listener : listeners) {
            try {
                event.accept(listener);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("Error while notifying the listener {}", listener, e);
            }
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // the listeners are notified when they are deployed, the context being already initialized
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        notifyReversed(WhiteboardListener::contextDestroyed);
    }

    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        notify(listener -> listener.attributeAdded(event));
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
        notify(listener -> listener.attributeRemoved(event));
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        notify(listener -> listener.attributeReplaced(event));
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        notify(listener -> listener.requestInitialized(event));
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        notifyReversed(listener -> listener.requestDestroyed(event));
    }

    @Override
    public void attributeAdded(ServletRequestAttributeEvent event) {
        notify(listener -> listener.attributeAdded(event));
    }

    @Override
    public void attributeRemoved(ServletRequestAttributeEvent event) {
        notify(listener -> listener.attributeRemoved(event));
    }

    @Override
    public void attributeReplaced(ServletRequestAttributeEvent event) {
        notify(listener -> listener.attributeReplaced(event));
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        notify(listener -> listener.sessionCreated(event));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        notifyReversed(listener -> listener.sessionDestroyed(event));
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        notify(listener -> listener.attributeAdded(event));
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        notify(listener -> listener.attributeRemoved(event));
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        notify(listener -> listener.attributeReplaced(event));
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        notify(listener -> listener.sessionIdChanged(event, oldSessionId));
    }

}
