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
package org.apache.karaf.features;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.karaf.features.internal.service.FeatureConfigInstaller;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class AppendTest {

    private IMocksControl c;
    private Feature feature;
    private ConfigurationAdmin admin;
    private FeatureConfigInstaller installer;
    
    @Before
    public void before() throws Exception {
        System.setProperty("karaf.data", "data");
        System.setProperty("karaf.etc", "target");
        RepositoryImpl r = new RepositoryImpl(getClass().getResource("internal/service/f08.xml").toURI());
        Feature[] features = r.getFeatures();
        feature = features[0];
        checkFeature(feature);
        c = EasyMock.createControl();
        admin = c.createMock(ConfigurationAdmin.class);
        installer = new FeatureConfigInstaller(admin);
    }

    @Test
    public void testNoChange() throws Exception {
        Hashtable<String, Object> original = new Hashtable<>();
        original.put("javax.servlet.context.tempdir", "bar");
        expectConfig(admin, original);

        c.replay();
        installer.installFeatureConfigs(feature);
        c.verify();
    }

    @Test
    public void testAppendWhenFileExists() throws Exception {
        testAppendInternal(true);
    }
    
    @Test
    public void testAppendWhenNoFileExists() throws Exception {
        testAppendInternal(false);
    }

    private void testAppendInternal(boolean cfgFileExists) throws IOException, InvalidSyntaxException, FileNotFoundException {
        File cfgFile = new File("target/org.ops4j.pax.web.cfg");
        cfgFile.delete();
        if (cfgFileExists) {
            cfgFile.createNewFile();
        }
        Hashtable<String, Object> original = new Hashtable<>();
        original.put("foo", "bar");
        Configuration config = expectConfig(admin, original);
        Capture<Dictionary<String, ?>> captured = EasyMock.newCapture();
        config.update(EasyMock.capture(captured));
        expectLastCall();
        c.replay();
        installer.installFeatureConfigs(feature);
        c.verify();
        assertEquals("data/pax-web-jsp", captured.getValue().get("javax.servlet.context.tempdir"));
        Properties props = new Properties();
        props.load(new FileInputStream(cfgFile));
        String v = props.getProperty("javax.servlet.context.tempdir");
        assertTrue("${karaf.data}/pax-web-jsp".equals(v) || "data/pax-web-jsp".equals(v));
//        assertEquals("${karaf.data}/pax-web-jsp", props.getProperty("javax.servlet.context.tempdir"));
    }

    private Configuration expectConfig(ConfigurationAdmin admin, Hashtable<String, Object> original)
        throws IOException, InvalidSyntaxException {
        Configuration config = c.createMock(Configuration.class);
        expect(admin.listConfigurations(eq("(service.pid=org.ops4j.pax.web)")))
            .andReturn(new Configuration[] {
                                            config
        }).atLeastOnce();
        expect(config.getProcessedProperties(null)).andReturn(original).atLeastOnce();
        return config;
    }

    private void checkFeature(Feature feature) {
        ConfigInfo configInfo = feature.getConfigurations().get(0);
        assertTrue(configInfo.isAppend());

        Properties properties = configInfo.getProperties();
        String tempDir = properties.getProperty("javax.servlet.context.tempdir");
        assertEquals("data/pax-web-jsp", tempDir);
    }
}
