/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.assertEquals;

public class LogDumpProviderTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testPaxLoggingFiles() throws Exception {
        File log = writeLog("karaf.log", "pax logging");
        Hashtable<String, Object> paxLogging = new Hashtable<>();
        paxLogging.put("log4j2.rootLogger.level", "INFO");
        paxLogging.put("log4j2.appender.rolling.fileName", log.getPath());

        Map<String, String> dump = dump(paxLogging, null);

        assertEquals(Collections.singletonMap("log/karaf.log", "pax logging"), dump);
    }

    @Test
    public void testKarafLoggingFile() throws Exception {
        File log = writeLog("custom.log", "karaf logging");
        Hashtable<String, Object> karafLogging = new Hashtable<>();
        karafLogging.put("level", "INFO");
        karafLogging.put("file", log.getPath());

        Map<String, String> dump = dump(null, karafLogging);

        assertEquals(Collections.singletonMap("log/custom.log", "karaf logging"), dump);
    }

    @Test
    public void testKarafLoggingDefaultFile() throws Exception {
        writeLog("karaf.log", "default");
        System.setProperty("karaf.log", tmp.getRoot().getPath());
        try {
            Map<String, String> dump = dump(null, null);

            assertEquals(Collections.singletonMap("log/karaf.log", "default"), dump);
        } finally {
            System.clearProperty("karaf.log");
        }
    }

    private File writeLog(String name, String content) throws Exception {
        File log = new File(tmp.getRoot(), name);
        Files.write(log.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return log;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> dump(Dictionary<String, Object> paxLogging, Dictionary<String, Object> karafLogging) throws Exception {
        ConfigurationAdmin configurationAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(configurationAdmin.listConfigurations("(service.pid=org.ops4j.pax.logging)"))
                .andReturn(configurations(paxLogging)).anyTimes();
        EasyMock.expect(configurationAdmin.listConfigurations("(service.pid=org.apache.karaf.logging)"))
                .andReturn(configurations(karafLogging)).anyTimes();
        ServiceReference<ConfigurationAdmin> ref = EasyMock.createMock(ServiceReference.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bundleContext.getServiceReference(ConfigurationAdmin.class)).andReturn(ref);
        EasyMock.expect(bundleContext.getService(ref)).andReturn(configurationAdmin);
        EasyMock.expect(bundleContext.ungetService(ref)).andReturn(true);
        EasyMock.replay(configurationAdmin, ref, bundleContext);

        Map<String, String> entries = new HashMap<>();
        new LogDumpProvider(bundleContext).createDump(new DumpDestination() {
            @Override
            public OutputStream add(String name) {
                return new ByteArrayOutputStream() {
                    @Override
                    public void close() {
                        entries.put(name, new String(toByteArray(), StandardCharsets.UTF_8));
                    }
                };
            }

            @Override
            public void save() {
            }
        });

        EasyMock.verify(bundleContext);
        return entries;
    }

    private static Configuration[] configurations(Dictionary<String, Object> properties) throws Exception {
        if (properties == null) {
            return null;
        }
        Configuration configuration = EasyMock.createMock(Configuration.class);
        EasyMock.expect(configuration.getProcessedProperties(null)).andReturn(properties).anyTimes();
        EasyMock.replay(configuration);
        return new Configuration[] {configuration};
    }
}
