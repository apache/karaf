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
package org.apache.felix.karaf.jaas.config;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import junit.framework.TestCase;
import org.apache.felix.karaf.jaas.boot.ProxyLoginModule;
import org.apache.felix.karaf.jaas.config.impl.Config;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;

public class NamespaceHandlerTest extends TestCase {

    public void testConfig() throws Exception {
        final Dictionary headers = new Hashtable();
        headers.put(Constants.BUNDLE_VERSION, "1.0.0.SNAPSHOT");

        final BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        final Bundle bundle = EasyMock.createMock(Bundle.class);
        final ServiceRegistration reg = EasyMock.createMock(ServiceRegistration.class);

        expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
        expect(bundle.getSymbolicName()).andReturn("symbolic-name").anyTimes();
        expect(bundle.getBundleId()).andReturn(Long.valueOf(32)).anyTimes();
        expect(bundle.getHeaders()).andReturn(headers).anyTimes();
        expect(bundleContext.registerService(aryEq(new String[] { JaasRealm.class.getName() }),
                                             anyObject(), EasyMock.<Dictionary>anyObject())).andReturn(reg);
        expect(bundleContext.registerService(aryEq(new String[] { KeystoreInstance.class.getName() }),
                                             anyObject(), EasyMock.<Dictionary>anyObject())).andReturn(reg);

        replay(bundleContext, bundle);

        AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "classpath:config.xml" }, false) {
            protected DefaultListableBeanFactory createBeanFactory() {
                DefaultListableBeanFactory f = super.createBeanFactory();
                f.addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));
                return f;
            }
        };
        ctx.refresh();

        verify(bundleContext, bundle);

        // Test realm
        Object obj = ctx.getBean("realm");
        assertNotNull(obj);
        assertTrue(obj instanceof Config);
        Config cfg = (Config) obj;
        assertNotNull(cfg.getBundleContext());
        assertEquals("realm", cfg.getName());
        assertNotNull(cfg.getModules());
        assertEquals(1, cfg.getModules().length);
        assertNotNull(cfg.getModules()[0]);
        assertEquals("org.apache.felix.karaf.jaas.config.SimpleLoginModule", cfg.getModules()[0].getClassName());
        assertEquals("required", cfg.getModules()[0].getFlags());
        assertNotNull(cfg.getModules()[0].getOptions());
        assertEquals(1, cfg.getModules()[0].getOptions().size());
        assertEquals("value", cfg.getModules()[0].getOptions().get("key"));
        AppConfigurationEntry[] entries = cfg.getEntries();
        assertNotNull(entries);
        assertEquals(1, entries.length);
        assertNotNull(entries[0]);
        assertEquals(ProxyLoginModule.class.getName(), entries[0].getLoginModuleName());
        assertEquals(AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, entries[0].getControlFlag());
        Map<String,?> options = entries[0].getOptions();
        assertNotNull(options);
        assertEquals(3, options.size());
        assertEquals("value", options.get("key"));
        assertEquals("org.apache.felix.karaf.jaas.config.SimpleLoginModule", options.get(ProxyLoginModule.PROPERTY_MODULE));
        assertEquals("32", options.get(ProxyLoginModule.PROPERTY_BUNDLE));

        // Test keystore
        obj = ctx.getBean("keystore");
        assertNotNull(obj);
        assertTrue(obj instanceof KeystoreInstance);
        KeystoreInstance ks = (KeystoreInstance) obj;
        assertEquals("ks", ks.getName());
        assertEquals(1, ks.getRank());
        assertNotNull(ks.getPrivateKey("myalias"));
    }
}
