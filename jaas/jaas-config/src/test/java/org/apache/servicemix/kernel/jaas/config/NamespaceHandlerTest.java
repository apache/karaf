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
package org.apache.servicemix.kernel.jaas.config;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.apache.servicemix.kernel.jaas.boot.ProxyLoginModule;
import junit.framework.TestCase;

public class NamespaceHandlerTest extends TestCase {

    Mockery context = new Mockery();

    public void testConfig() throws Exception {
        final Dictionary headers = new Hashtable();
        headers.put(Constants.BUNDLE_VERSION, "1.0.0.SNAPSHOT");
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final Bundle bundle = context.mock(Bundle.class);

        context.checking(new Expectations() {{
            allowing(bundleContext).getBundle(); will(returnValue(bundle));
            allowing(bundle).getSymbolicName(); will(returnValue("symbolic-name"));
            allowing(bundle).getBundleId(); will(returnValue(Long.valueOf(32)));
            allowing(bundle).getHeaders(); will(returnValue(headers));
            one(bundleContext).registerService(with(any(String[].class)),
                                               with(any(Config.class)),
                                               with(any(Dictionary.class)));
        }});
        
        AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "classpath:config.xml" }, false) {
            protected DefaultListableBeanFactory createBeanFactory() {
                DefaultListableBeanFactory f = super.createBeanFactory();
                f.addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));
                return f;
            }
        };
        ctx.refresh();
        Object obj = ctx.getBean("realm");
        assertNotNull(obj);
        assertTrue(obj instanceof Config);
        Config cfg = (Config) obj;
        assertNotNull(cfg.getBundleContext());
        assertEquals("realm", cfg.getName());
        assertNotNull(cfg.getModules());
        assertEquals(1, cfg.getModules().length);
        assertNotNull(cfg.getModules()[0]);
        assertEquals("org.apache.servicemix.kernel.jaas.config.SimpleLoginModule", cfg.getModules()[0].getClassName());
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
        assertEquals("org.apache.servicemix.kernel.jaas.config.SimpleLoginModule", options.get(ProxyLoginModule.PROPERTY_MODULE));
        assertEquals("32", options.get(ProxyLoginModule.PROPERTY_BUNDLE));
    }
}
