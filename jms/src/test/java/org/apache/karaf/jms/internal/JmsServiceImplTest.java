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
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.JMSContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The connection factory name given to the JMS service must be matched literally, never as an OSGi filter.
 * Configuration Admin and the service registry are faked, but the filters are evaluated with the real OSGi filter parser.
 */
public class JmsServiceImplTest {

    private static final String[] FILTER_PAYLOADS = {
        "*",
        "prod*",
        "x)(name=*",
        "x)(|(name=*)",
        "\\",
    };

    private final Map<String, Dictionary<String, Object>> configurations = new LinkedHashMap<>();
    private final Map<ServiceReference<ConnectionFactory>, Map<String, Object>> connectionFactories = new LinkedHashMap<>();

    private String karafBase;
    private JmsServiceImpl service;

    @Before
    public void setUp() {
        karafBase = System.getProperty("karaf.base");
        System.setProperty("karaf.base", "target");
        service = new JmsServiceImpl();
        service.setConfigAdmin(proxy(ConfigurationAdmin.class, this::configAdmin));
        service.setBundleContext(proxy(BundleContext.class, this::bundleContext));
    }

    @After
    public void tearDown() {
        if (karafBase == null) {
            System.clearProperty("karaf.base");
        } else {
            System.setProperty("karaf.base", karafBase);
        }
    }

    @Test
    public void deleteRemovesOnlyTheNamedConnectionFactory() throws Exception {
        addConfiguration("prod-east");
        addConfiguration("prod-west");
        addConfiguration("test");

        service.delete("prod-east");

        assertEquals(Arrays.asList("prod-west", "test"), new ArrayList<>(configurations.keySet()));
    }

    @Test
    public void deleteDoesNotTreatTheNameAsAFilter() throws Exception {
        addConfiguration("prod-east");
        addConfiguration("test");

        for (String name : FILTER_PAYLOADS) {
            service.delete(name);
            assertEquals("delete(\"" + name + "\")", Arrays.asList("prod-east", "test"), new ArrayList<>(configurations.keySet()));
        }
    }

    @Test
    public void deleteMatchesNamesContainingFilterCharacters() throws Exception {
        addConfiguration("cf(1)*\\");
        addConfiguration("test");

        service.delete("cf(1)*\\");

        assertEquals(Collections.singletonList("test"), new ArrayList<>(configurations.keySet()));
    }

    @Test
    public void lookupResolvesTheNamedConnectionFactory() throws Exception {
        addConnectionFactory(10L, 100, "prod");
        addConnectionFactory(11L, 0, "test");

        assertEquals("jms/test", service.info("jms/test", null, null).get("product"));
        assertEquals("jms/test", service.info("test", null, null).get("product"));
        assertEquals("jms/test", service.info("11", null, null).get("product"));
    }

    @Test
    public void lookupDoesNotTreatTheNameAsAFilter() throws Exception {
        addConnectionFactory(10L, 100, "prod");
        addConnectionFactory(11L, 0, "test");

        for (String name : FILTER_PAYLOADS) {
            try {
                service.info(name, null, null);
                fail("info(\"" + name + "\") should not resolve a connection factory");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().startsWith("No JMS connection factory found"));
            }
        }
    }

    @Test
    public void lookupMatchesNamesContainingFilterCharacters() throws Exception {
        addConnectionFactory(10L, 100, "prod");
        addConnectionFactory(11L, 0, "cf(1)*\\");

        assertEquals("jms/cf(1)*\\", service.info("jms/cf(1)*\\", null, null).get("product"));
    }

    private void addConfiguration(String name) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, JmsServiceImpl.FACTORY_PID + "." + configurations.size());
        properties.put("service.factoryPid", JmsServiceImpl.FACTORY_PID);
        properties.put("name", name);
        configurations.put(name, properties);
    }

    private void addConnectionFactory(long serviceId, int ranking, String name) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.SERVICE_ID, serviceId);
        properties.put(Constants.SERVICE_RANKING, ranking);
        properties.put("name", name);
        properties.put("osgi.jndi.service.name", "jms/" + name);
        connectionFactories.put(serviceReference(properties), properties);
    }

    private Object configAdmin(Method method, Object[] args) throws Exception {
        if ("listConfigurations".equals(method.getName())) {
            Filter filter = FrameworkUtil.createFilter((String) args[0]);
            List<Configuration> matching = new ArrayList<>();
            for (Map.Entry<String, Dictionary<String, Object>> entry : configurations.entrySet()) {
                if (filter.match(entry.getValue())) {
                    matching.add(configuration(entry.getKey()));
                }
            }
            return matching.isEmpty() ? null : matching.toArray(new Configuration[0]);
        }
        throw new UnsupportedOperationException(method.getName());
    }

    private Configuration configuration(String name) {
        return proxy(Configuration.class, (method, args) -> {
            if ("delete".equals(method.getName())) {
                configurations.remove(name);
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private Object bundleContext(Method method, Object[] args) throws Exception {
        switch (method.getName()) {
            case "getServiceReferences":
                if (args[0] != ConnectionFactory.class) {
                    throw new UnsupportedOperationException("getServiceReferences(" + args[0] + ")");
                }
                Filter filter = args[1] == null ? null : FrameworkUtil.createFilter((String) args[1]);
                return connectionFactories.entrySet().stream()
                        .filter(entry -> filter == null || filter.matches(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
            case "getService":
                return connectionFactory((String) ((ServiceReference<?>) args[0]).getProperty("osgi.jndi.service.name"));
            case "ungetService":
                return true;
            default:
                throw new UnsupportedOperationException(method.getName());
        }
    }

    /**
     * A connection factory whose JMS provider name is its JNDI name, so a test can tell which factory was resolved.
     */
    private static ConnectionFactory connectionFactory(String jndiName) {
        ConnectionMetaData metaData = proxy(ConnectionMetaData.class, (method, args) -> {
            switch (method.getName()) {
                case "getJMSProviderName":
                    return jndiName;
                case "getProviderVersion":
                    return "1.0";
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
        JMSContext context = proxy(JMSContext.class, (method, args) -> {
            switch (method.getName()) {
                case "getMetaData":
                    return metaData;
                case "close":
                    return null;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
        return proxy(ConnectionFactory.class, (method, args) -> {
            if ("createContext".equals(method.getName())) {
                return context;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    /**
     * A service reference ordered like the OSGi service registry: higher ranking first, then lower service id.
     */
    @SuppressWarnings("unchecked")
    private static ServiceReference<ConnectionFactory> serviceReference(Map<String, Object> properties) {
        return proxy(ServiceReference.class, (method, args) -> {
            switch (method.getName()) {
                case "getProperty":
                    return properties.get((String) args[0]);
                case "getPropertyKeys":
                    return properties.keySet().toArray(new String[0]);
                case "compareTo":
                    ServiceReference<?> other = (ServiceReference<?>) args[0];
                    int byRanking = Integer.compare((Integer) properties.get(Constants.SERVICE_RANKING),
                            (Integer) other.getProperty(Constants.SERVICE_RANKING));
                    return byRanking != 0 ? byRanking
                            : Long.compare((Long) other.getProperty(Constants.SERVICE_ID),
                                    (Long) properties.get(Constants.SERVICE_ID));
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
    }

    private interface Handler {
        Object invoke(Method method, Object[] args) throws Exception;
    }

    private static <T> T proxy(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(JmsServiceImplTest.class.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "toString":
                            return type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                        default:
                            return handler.invoke(method, args);
                    }
                }));
    }
}
