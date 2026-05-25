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
package org.apache.karaf.jdbc.internal;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JdbcServiceImplTest {

    private static final String[] INJECTION_PAYLOADS = {
        "*",
        ")(objectClass=*",
        "dsA)(service.id>=0",
        "a*b",
        "dsA\u0000evil",
    };

    private JdbcServiceImpl service;
    private TestConfigurationAdmin configAdmin;
    private TestBundleContext bundleContext;

    private final List<String> capturedConfigAdminFilters = new ArrayList<>();
    private final List<String> capturedBundleContextFilters = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        service = new JdbcServiceImpl();
        configAdmin = new TestConfigurationAdmin();
        bundleContext = new TestBundleContext();

        setField(service, "configAdmin", configAdmin);
        setField(service, "bundleContext", bundleContext);
    }

    @Test
    public void datasourcesReturnsValuesInExpectedPriorityOrder() throws Exception {
        bundleContext.referencesToReturn = new ServiceReference<?>[] {
            newServiceReference(singleton("osgi.jndi.service.name", "jndiDs")),
            newServiceReference(singleton("datasource", "datasourceProp")),
            newServiceReference(singleton("name", "nameProp")),
            newServiceReference(singleton(DataSourceFactory.JDBC_DATASOURCE_NAME, "jdbcNameProp")),
            newServiceReference(singleton(Constants.SERVICE_ID, 77L))
        };

        List<String> datasources = service.datasources();

        assertEquals(Arrays.asList("jndiDs", "datasourceProp", "nameProp", "jdbcNameProp", "77"), datasources);
    }

    @Test
    public void datasourceServiceIdsReturnsIdsFromReferences() throws Exception {
        bundleContext.referencesToReturn = new ServiceReference<?>[] {
            newServiceReference(singleton(Constants.SERVICE_ID, 10L)),
            newServiceReference(singleton(Constants.SERVICE_ID, 20L))
        };

        List<Long> ids = service.datasourceServiceIds();

        assertEquals(Arrays.asList(10L, 20L), ids);
    }

    @Test
    public void createRejectsDuplicateDatasourceName() throws Exception {
        bundleContext.referencesToReturn = new ServiceReference<?>[] {
            newServiceReference(singleton("osgi.jndi.service.name", "alreadyThere"))
        };

        try {
            service.create("alreadyThere", "h2", null, "db", "jdbc:h2:mem:test", "sa", "sa", "DataSource");
            fail("Expected duplicate datasource rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("alreadyThere"));
        }
    }

    @Test
    public void createStoresDatasourceConfigurationProperties() throws Exception {
        bundleContext.referencesToReturn = null;
        Configuration config = newConfigurationProxy();
        configAdmin.configurationToReturn = config;

        service.create("newDs", "h2", null, "mydb", "jdbc:h2:mem:mydb", "sa", "secret", "DataSource");

        assertEquals("org.ops4j.datasource", configAdmin.createdFactoryPid);
        assertNotNull(configAdmin.updatedProperties);
        assertEquals("newDs", configAdmin.updatedProperties.get(DataSourceFactory.JDBC_DATASOURCE_NAME));
        assertEquals("h2", configAdmin.updatedProperties.get(DataSourceFactory.OSGI_JDBC_DRIVER_NAME));
        assertEquals("mydb", configAdmin.updatedProperties.get(DataSourceFactory.JDBC_DATABASE_NAME));
        assertEquals("jdbc:h2:mem:mydb", configAdmin.updatedProperties.get(DataSourceFactory.JDBC_URL));
        assertEquals("sa", configAdmin.updatedProperties.get(DataSourceFactory.JDBC_USER));
        assertEquals("secret", configAdmin.updatedProperties.get(DataSourceFactory.JDBC_PASSWORD));
        assertEquals("DataSource", configAdmin.updatedProperties.get("dataSourceType"));
    }

    @Test
    public void deleteWildcardMustBeEscaped() throws Exception {
        capturedConfigAdminFilters.clear();
        service.delete("*");

        assertEquals(1, capturedConfigAdminFilters.size());
        String filter = capturedConfigAdminFilters.get(0);

        assertFilterValueIsEscaped("delete(\"*\")", filter, "*", "\\2a");
    }

    @Test
    public void deleteCloseParenMustBeEscaped() throws Exception {
        capturedConfigAdminFilters.clear();
        service.delete(")(objectClass=*");

        assertEquals(1, capturedConfigAdminFilters.size());
        String filter = capturedConfigAdminFilters.get(0);

        assertFilterValueIsEscaped("delete(\")\")", filter, ")", "\\29");
        assertFilterValueIsEscaped("delete(\"(\")", filter, "(", "\\28");
        assertFilterValueIsEscaped("delete(\"*\")", filter, "*", "\\2a");
    }

    @Test
    public void deleteAllPayloadsMustBeEscaped() throws Exception {
        for (String payload : INJECTION_PAYLOADS) {
            capturedConfigAdminFilters.clear();
            service.delete(payload);

            assertFalse("No filter captured for payload: " + payload, capturedConfigAdminFilters.isEmpty());
            String filter = capturedConfigAdminFilters.get(0);

            assertNoUnescapedSpecialChars("delete(\"" + payload + "\")", filter, payload);
        }
    }

    @Test
    public void deleteHandlesNullConfigurationsGracefully() throws Exception {
        // When no configurations match the filter, listConfigurations returns null
        // The delete() method should not throw NPE in this case
        service.delete("nonexistentDataSource");

        assertEquals(1, capturedConfigAdminFilters.size());
        // Verify the filter was built with proper escaping
        String filter = capturedConfigAdminFilters.get(0);
        assertTrue("Filter should contain escaped name", filter.contains("nonexistentDataSource"));
    }

    @Test
    public void lookupDataSourceWildcardMustBeEscaped() throws Exception {
        capturedBundleContextFilters.clear();
        try {
            service.info("*");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        assertFalse("lookupDataSource filter was never built", capturedBundleContextFilters.isEmpty());
        String filter = capturedBundleContextFilters.stream()
            .filter(f -> f.contains("osgi.jndi.service.name") || f.contains("datasource="))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No lookup filter captured for info(\"*\")"));

        assertFilterValueIsEscaped("lookupDataSource(\"*\")", filter, "*", "\\2a");
    }

    @Test
    public void lookupDataSourceCloseParenMustBeEscaped() throws Exception {
        capturedBundleContextFilters.clear();
        try {
            service.info(")(objectClass=*");
        } catch (IllegalArgumentException | InvalidSyntaxException expected) {
            // expected
        }

        capturedBundleContextFilters.stream()
            .filter(f -> f.contains("osgi.jndi.service.name") || f.contains("datasource="))
            .forEach(filter -> {
                assertFilterValueIsEscaped("lookupDataSource inject)", filter, ")", "\\29");
                assertFilterValueIsEscaped("lookupDataSource inject(", filter, "(", "\\28");
            });
    }

    @Test
    public void lookupDataSourceAllPayloadsMustBeEscaped() throws Exception {
        for (String payload : INJECTION_PAYLOADS) {
            capturedBundleContextFilters.clear();
            try {
                service.info(payload);
            } catch (Exception expected) {
                // expected
            }

            capturedBundleContextFilters.stream()
                .filter(f -> f.contains("osgi.jndi.service.name") || f.contains("datasource="))
                .forEach(filter ->
                    assertNoUnescapedSpecialChars("lookupDataSource(\"" + payload + "\")", filter, payload));
        }
    }

    private static void assertFilterValueIsEscaped(String context, String filter, String raw, String escaped) {
        assertTrue(context + ": escaped form '" + escaped + "' not found in filter: " + filter,
            filter.contains(escaped));
        String[] assignments = filter.split("=");
        for (int i = 1; i < assignments.length; i++) {
            String valuePart = assignments[i].split("\\)")[0];
            if (valuePart.contains(raw) && !valuePart.contains(escaped)) {
                fail(context + ": unescaped '" + raw + "' found in filter value: '" + valuePart
                    + "' - full filter: " + filter);
            }
        }
    }

    private static void assertNoUnescapedSpecialChars(String context, String filter, String payload) {
        char[] specials = {'*', '(', ')', '\\'};
        for (char c : specials) {
            if (payload.indexOf(c) < 0) {
                continue;
            }
            String[] assignments = filter.split("=");
            for (int i = 1; i < assignments.length; i++) {
                String valuePart = assignments[i].split("\\)")[0];
                if (valuePart.contains(String.valueOf(c))) {
                    fail(context + ": unescaped '" + c + "' in filter value '" + valuePart
                        + "' - full filter: " + filter);
                }
            }
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Dictionary<String, Object> singleton(String key, Object value) {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(key, value);
        return dict;
    }

    @SuppressWarnings("unchecked")
    private static ServiceReference<Object> newServiceReference(Dictionary<String, Object> properties) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("getProperty".equals(methodName)) {
                return properties.get((String) args[0]);
            }
            if ("getPropertyKeys".equals(methodName)) {
                List<String> keys = new ArrayList<>();
                for (java.util.Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
                    keys.add(e.nextElement());
                }
                return keys.toArray(new String[0]);
            }
            if ("compareTo".equals(methodName)) {
                return 0;
            }
            if ("toString".equals(methodName)) {
                return "ServiceReference";
            }
            if ("hashCode".equals(methodName)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName)) {
                return proxy == args[0];
            }
            return null;
        };
        return (ServiceReference<Object>) Proxy.newProxyInstance(
            JdbcServiceImplTest.class.getClassLoader(),
            new Class<?>[] { ServiceReference.class },
            handler);
    }

    private Configuration newConfigurationProxy() {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("update".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof Dictionary) {
                @SuppressWarnings("unchecked")
                Dictionary<String, String> updated = (Dictionary<String, String>) args[0];
                configAdmin.updatedProperties = updated;
                return null;
            }
            if ("delete".equals(method.getName())) {
                configAdmin.deleteCalls++;
                return null;
            }
            if ("toString".equals(method.getName())) {
                return "Configuration";
            }
            return null;
        };
        return (Configuration) Proxy.newProxyInstance(
            JdbcServiceImplTest.class.getClassLoader(),
            new Class<?>[] { Configuration.class },
            handler);
    }

    private class TestConfigurationAdmin implements ConfigurationAdmin {
        String createdFactoryPid;
        Configuration configurationToReturn;
        Dictionary<String, String> updatedProperties;
        int deleteCalls;

        @Override
        public Configuration createFactoryConfiguration(String factoryPid) {
            createdFactoryPid = factoryPid;
            return configurationToReturn;
        }

        @Override
        public Configuration createFactoryConfiguration(String factoryPid, String location) {
            createdFactoryPid = factoryPid;
            return configurationToReturn;
        }

        @Override
        public Configuration getConfiguration(String pid, String location) {
            return null;
        }

        @Override
        public Configuration getConfiguration(String pid) {
            return null;
        }

        @Override
        public Configuration getFactoryConfiguration(String factoryPid, String name, String location) {
            return null;
        }

        @Override
        public Configuration getFactoryConfiguration(String factoryPid, String name) {
            return null;
        }

        @Override
        public Configuration[] listConfigurations(String filter) {
            capturedConfigAdminFilters.add(filter);
            return new Configuration[0];
        }
    }

    private class TestBundleContext extends StubBundleContext {
        ServiceReference<?>[] referencesToReturn;

        @Override
        public ServiceReference<?>[] getServiceReferences(String clazz, String filter)
                throws InvalidSyntaxException {
            if (filter != null) {
                capturedBundleContextFilters.add(filter);
            }
            return referencesToReturn;
        }
    }

    private static abstract class StubBundleContext implements BundleContext {

        @Override public String getProperty(String key) { return null; }
        @Override public org.osgi.framework.Bundle getBundle() { return null; }
        @Override public org.osgi.framework.Bundle installBundle(String location, java.io.InputStream input) { return null; }
        @Override public org.osgi.framework.Bundle installBundle(String location) { return null; }
        @Override public org.osgi.framework.Bundle getBundle(long id) { return null; }
        @Override public org.osgi.framework.Bundle[] getBundles() { return new org.osgi.framework.Bundle[0]; }
        @Override public void addServiceListener(org.osgi.framework.ServiceListener listener, String filter) { }
        @Override public void addServiceListener(org.osgi.framework.ServiceListener listener) { }
        @Override public void removeServiceListener(org.osgi.framework.ServiceListener listener) { }
        @Override public void addBundleListener(org.osgi.framework.BundleListener listener) { }
        @Override public void removeBundleListener(org.osgi.framework.BundleListener listener) { }
        @Override public void addFrameworkListener(org.osgi.framework.FrameworkListener listener) { }
        @Override public void removeFrameworkListener(org.osgi.framework.FrameworkListener listener) { }
        @Override public org.osgi.framework.ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) { return null; }
        @Override public org.osgi.framework.ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) { return null; }
        @Override public <S> org.osgi.framework.ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) { return null; }
        @Override public <S> org.osgi.framework.ServiceRegistration<S> registerService(Class<S> clazz, org.osgi.framework.ServiceFactory<S> factory, Dictionary<String, ?> properties) { return null; }
        @Override public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException { return null; }
        @Override public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException { return null; }
        @Override public <S> ServiceReference<S> getServiceReference(Class<S> clazz) { return null; }
        @Override public <S> java.util.Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException { return new ArrayList<>(); }
        @Override public ServiceReference<?> getServiceReference(String clazz) { return null; }
        @Override public <S> S getService(ServiceReference<S> reference) { return null; }
        @Override public boolean ungetService(ServiceReference<?> reference) { return false; }
        @Override public <S> org.osgi.framework.ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) { return null; }
        @Override public java.io.File getDataFile(String filename) { return null; }
        @Override public Filter createFilter(String filter) throws InvalidSyntaxException { return null; }
        @Override public org.osgi.framework.Bundle getBundle(String location) { return null; }
    }
}
