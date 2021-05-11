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
package org.apache.karaf.config.core.impl;

import java.io.File;
import java.util.Hashtable;

import org.apache.karaf.util.config.ConfigurationPID;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class JsonConfigInstallerTest {

    public static final String[] a = {"a1", "a2", "a3"};

    public static final String[] b = {"b1", "b2", "b3"};

    public static final String[] c = {"c1", "c2", "c3"};

    @Test
    public void KARAF_7097_update_required() throws Exception {
        final String filename = "/org.example.Arrays~update_required.json";
        final String pid = ConfigurationPID.parseFilename(filename).getPid();
        final String path = getClass().getResource(filename).getPath();
        final File artifact = new File(path);

        final Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("a", a);
        properties.put("b", b);
        properties.put("c", c);
        final Configuration configuration = mock(Configuration.class);
        expect(configuration.getPid()).andReturn(pid);
        expect(configuration.getProperties()).andReturn(properties);
        configuration.update(anyObject());

        final ConfigurationAdmin configurationAdmin = mock(ConfigurationAdmin.class);
        expect(configurationAdmin.listConfigurations(String.format("(felix.fileinstall.filename=%s)", artifact.toURI()))).andReturn(new Configuration[]{configuration});
        expect(configurationAdmin.getConfiguration(pid, null)).andReturn(configuration);

        replay(configuration, configurationAdmin);

        final JsonConfigInstaller installer = new JsonConfigInstaller(configurationAdmin);
        installer.install(artifact);

        verify(configuration, configurationAdmin);
    }

    @Test
    public void KARAF_7097_no_update_required() throws Exception {
        final String filename = "/org.example.Arrays~no_update_required.json";
        final String pid = ConfigurationPID.parseFilename(filename).getPid();
        final String path = getClass().getResource(filename).getPath();
        final File artifact = new File(path);

        final Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("a", a);
        properties.put("b", b);
        properties.put("c", c);
        final Configuration configuration = mock(Configuration.class);
        expect(configuration.getPid()).andReturn(pid);
        expect(configuration.getProperties()).andReturn(properties);

        final ConfigurationAdmin configurationAdmin = mock(ConfigurationAdmin.class);
        expect(configurationAdmin.listConfigurations(String.format("(felix.fileinstall.filename=%s)", artifact.toURI()))).andReturn(new Configuration[]{configuration});
        expect(configurationAdmin.getConfiguration(pid, null)).andReturn(configuration);

        replay(configuration, configurationAdmin);

        final JsonConfigInstaller installer = new JsonConfigInstaller(configurationAdmin);
        installer.install(artifact);

        verify(configuration, configurationAdmin);
    }

}
