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
package org.apache.karaf.jms.internal;

import jakarta.jms.ConnectionFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ManagedServiceFactory that creates and registers jakarta.jms.ConnectionFactory
 * OSGi services based on ConfigAdmin configurations.
 */
public class ConnectionFactoryRegistration implements ManagedServiceFactory {

    private final BundleContext bundleContext;
    private final Map<String, ServiceRegistration<ConnectionFactory>> registrations = new ConcurrentHashMap<>();

    public ConnectionFactoryRegistration(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getName() {
        return "Karaf JMS ConnectionFactory Manager";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        deleted(pid);

        String type = (String) properties.get("type");
        String url = (String) properties.get("url");
        String user = (String) properties.get("user");
        String password = (String) properties.get("password");

        if (type == null) {
            throw new ConfigurationException("type", "JMS connection factory type is required");
        }

        ConnectionFactory cf = createConnectionFactory(type, url, user, password);
        if (cf == null) {
            throw new ConfigurationException("type", "Unknown JMS connection factory type: " + type);
        }

        Hashtable<String, Object> serviceProps = new Hashtable<>();
        for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            serviceProps.put(key, properties.get(key));
        }

        ServiceRegistration<ConnectionFactory> reg = bundleContext.registerService(
                ConnectionFactory.class, cf, serviceProps);
        registrations.put(pid, reg);
    }

    @Override
    public void deleted(String pid) {
        ServiceRegistration<ConnectionFactory> reg = registrations.remove(pid);
        if (reg != null) {
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                // already unregistered
            }
        }
    }

    public void destroy() {
        registrations.keySet().forEach(this::deleted);
    }

    private ConnectionFactory createConnectionFactory(String type, String url, String user, String password) {
        switch (type.toLowerCase()) {
            case "activemq":
                return createActiveMQConnectionFactory(url, user, password);
            case "artemis":
                return createArtemisConnectionFactory(url, user, password);
            default:
                return null;
        }
    }

    private ConnectionFactory createActiveMQConnectionFactory(String url, String user, String password) {
        try {
            Class<?> clazz = bundleContext.getBundle().loadClass("org.apache.activemq.ActiveMQConnectionFactory");
            Object cf = clazz.getConstructor(String.class, String.class, String.class)
                    .newInstance(user, password, url);
            return (ConnectionFactory) cf;
        } catch (ClassNotFoundException e) {
            // Try to load from any bundle
            try {
                for (org.osgi.framework.Bundle bundle : bundleContext.getBundles()) {
                    try {
                        Class<?> clazz = bundle.loadClass("org.apache.activemq.ActiveMQConnectionFactory");
                        Object cf = clazz.getConstructor(String.class, String.class, String.class)
                                .newInstance(user, password, url);
                        return (ConnectionFactory) cf;
                    } catch (ClassNotFoundException ignored) {
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create ActiveMQ ConnectionFactory", ex);
            }
            throw new RuntimeException("ActiveMQ client not available. Install the activemq feature first.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ActiveMQ ConnectionFactory", e);
        }
    }

    private ConnectionFactory createArtemisConnectionFactory(String url, String user, String password) {
        try {
            for (org.osgi.framework.Bundle bundle : bundleContext.getBundles()) {
                try {
                    Class<?> clazz = bundle.loadClass("org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory");
                    Object cf;
                    if (user != null) {
                        cf = clazz.getConstructor(String.class, String.class, String.class)
                                .newInstance(url, user, password);
                    } else {
                        cf = clazz.getConstructor(String.class).newInstance(url);
                    }
                    return (ConnectionFactory) cf;
                } catch (ClassNotFoundException ignored) {
                }
            }
            throw new RuntimeException("Artemis client not available. Install the artemis feature first.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Artemis ConnectionFactory", e);
        }
    }
}
