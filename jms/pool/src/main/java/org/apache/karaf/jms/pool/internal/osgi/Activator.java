/**
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
package org.apache.karaf.jms.pool.internal.osgi;

import org.apache.karaf.jms.pool.internal.PooledConnectionFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import java.util.Hashtable;
import java.util.function.Consumer;
import java.util.function.Function;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<ConnectionFactory, Activator.ConnectionFactoryData> {

    public static final String PROP_PREFIX = "karaf.jms.";

    public static final String PROP_OPT_IN = PROP_PREFIX + "wrap";

    public static final String PROP_POOL = PROP_PREFIX + "pool.";

    private static final transient Logger LOG = LoggerFactory.getLogger(Activator.class);

    private BundleContext context;
    private ServiceTracker<ConnectionFactory, ConnectionFactoryData> cfTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        cfTracker = new ServiceTracker<>(
                context,
                context.createFilter("(&(objectClass=javax.jms.ConnectionFactory)(" + PROP_OPT_IN + "=*))"),
                this);
        cfTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        cfTracker.close();
    }

    @Override
    public ConnectionFactoryData addingService(ServiceReference<ConnectionFactory> reference) {
        ConnectionFactoryData data = new ConnectionFactoryData(context, reference);
        try {
            data.init();
            return data;
        } catch (Throwable t) {
            LOG.warn("Error creating pooled JMS ConnectionFactory", t);
            data.destroy();
            return null;
        }
    }

    @Override
    public void modifiedService(ServiceReference<ConnectionFactory> reference, ConnectionFactoryData service) {
    }

    @Override
    public void removedService(ServiceReference<ConnectionFactory> reference, ConnectionFactoryData service) {
        service.destroy();
    }

    class ConnectionFactoryData {

        private final BundleContext context;
        private final ServiceReference<ConnectionFactory> reference;
        private ConnectionFactory connectionFactory;
        private PooledConnectionFactory pooledConnectionFactory;
        private ServiceRegistration<ConnectionFactory> registration;

        ConnectionFactoryData(BundleContext context, ServiceReference<ConnectionFactory> reference) {
            this.context = context;
            this.reference = reference;
        }

        void init() throws Exception {
            connectionFactory = context.getService(reference);
            PooledConnectionFactory pcf = new PooledConnectionFactory(connectionFactory);
            populate(pcf);
            register(pcf);
        }

        void destroy() {
            unregister();
            if (connectionFactory != null) {
                try {
                    context.ungetService(reference);
                } catch (Exception e) {
                    // Ignore
                } finally {
                    connectionFactory = null;
                }
            }
        }

        void populate(PooledConnectionFactory pcf) {
            setObject(PROP_POOL + "maxConnections", Integer::parseInt, pcf::setMaxConnections);
            setObject(PROP_POOL + "maximumActiveSessionPerConnection", Integer::parseInt, pcf::setMaximumActiveSessionPerConnection);
            setObject(PROP_POOL + "idleTimeout", Integer::parseInt, pcf::setIdleTimeout);
            setObject(PROP_POOL + "blockIfSessionPoolIsFull", Boolean::parseBoolean, pcf::setBlockIfSessionPoolIsFull);
            setObject(PROP_POOL + "blockIfSessionPoolIsFullTimeout", Long::parseLong, pcf::setBlockIfSessionPoolIsFullTimeout);
            setObject(PROP_POOL + "expiryTimeout", Long::parseLong, pcf::setExpiryTimeout);
            setObject(PROP_POOL + "createConnectionOnStartup", Boolean::parseBoolean, pcf::setCreateConnectionOnStartup);
            setObject(PROP_POOL + "useAnonymousProducers", Boolean::parseBoolean, pcf::setUseAnonymousProducers);
        }

        <T> void setObject(String name, Function<String, T> parser, Consumer<T> setter) {
            Object o = reference.getProperty(name);
            if (o != null) {
                setter.accept(parser.apply(o.toString()));
            }
        }

        void register(PooledConnectionFactory pcf) {
            this.pooledConnectionFactory = pcf;
            Hashtable<String, Object> props = new Hashtable<>();
            int ranking = 0;
            for (String key : reference.getPropertyKeys()) {
                Object value = reference.getProperty(key);
                if (Constants.SERVICE_RANKING.equals(key)) {
                    if (value instanceof Integer) {
                        ranking = (Integer) value;
                    }
                } else if (!key.startsWith("service.")
                        && !key.startsWith(PROP_PREFIX)) {
                    props.put(key, value);
                }
            }
            props.put(Constants.SERVICE_RANKING, ranking + 1);
            pcf.start();
            BundleContext context = reference.getBundle().getBundleContext();
            registration = context.registerService(ConnectionFactory.class, pooledConnectionFactory, props);
        }

        void unregister() {
            if (registration != null) {
                try {
                    registration.unregister();
                } catch (Exception e) {
                    // Ignore
                } finally {
                    registration = null;
                }
            }
            if (pooledConnectionFactory != null) {
                try {
                    pooledConnectionFactory.stop();
                } catch (Exception e) {
                    // Ignore
                } finally {
                    pooledConnectionFactory = null;
                }
            }
        }

    }

}
