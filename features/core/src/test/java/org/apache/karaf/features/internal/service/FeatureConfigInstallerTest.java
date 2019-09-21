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
package org.apache.karaf.features.internal.service;

import org.apache.karaf.features.Feature;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FeatureConfigInstallerTest {

    private void substEqual(final String src, final String subst) {
        assertEquals(FeatureConfigInstaller.substFinalName(src), subst);
    }

    @Test
    public void testSubstFinalName() {
        final String karafBase = "/tmp/karaf.base";
        final String karafEtc = karafBase + "/etc";
        final String foo = "/foo";

        System.setProperty("karaf.base", karafBase);
        System.setProperty("karaf.etc", karafEtc);
        System.setProperty("foo", foo);

        substEqual("etc/test.cfg", karafBase + File.separator + "etc/test.cfg");
        substEqual("/etc/test.cfg", karafBase + File.separator + "/etc/test.cfg");
        substEqual("${karaf.etc}/test.cfg", karafEtc + "/test.cfg");
        substEqual("${karaf.base}/etc/test.cfg", karafBase + "/etc/test.cfg");
        substEqual("etc/${foo}/test.cfg", karafBase + File.separator + "etc/" + foo + "/test.cfg");
        substEqual("${foo}/test.cfg", foo + "/test.cfg");
        substEqual("etc${bar}/${bar}test.cfg", karafBase + File.separator + "etc/test.cfg");
        substEqual("${bar}/etc/test.cfg${bar}", karafBase + File.separator + "/etc/test.cfg");
        substEqual("${karaf.base}${bar}/etc/test.cfg", karafBase + "/etc/test.cfg");
        substEqual("etc${}/${foo}/test.cfg", karafBase + File.separator + "etc//test.cfg");
        substEqual("${foo}${bar}/${bar}${foo}", foo + "/" + foo);
    }

    @Test
    public void testGoodConfigName() throws Exception {
        String dataDir = "data4";

        Path karafEtc = Files.createTempDirectory("etc");

        System.setProperty("karaf.etc", karafEtc.toString());

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        assertEquals("goodConfigName", f100.getName());

        ConfigurationAdmin configAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        Configuration config = EasyMock.createMock(Configuration.class);

        EasyMock.expect(configAdmin.listConfigurations(EasyMock.anyString())).andReturn(null);
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(configAdmin.getConfiguration(EasyMock.anyString(), EasyMock.isNull())).andReturn(config);
        EasyMock.expectLastCall();

        EasyMock.replay(configAdmin);
        FeatureConfigInstaller installer = new FeatureConfigInstaller(configAdmin);
        installer.installFeatureConfigs(f100);

        EasyMock.verify(configAdmin);

        File installedFile = Paths.get(karafEtc.toString(), "config.cfg").toFile();
        assertTrue(installedFile.exists());

        installedFile.delete();
        karafEtc.toFile().delete();
    }

    @Test
    public void testBadConfigName() throws Exception {
        String dataDir = "data4";

        Path karafEtc = Files.createTempDirectory("etc");

        System.setProperty("karaf.etc", karafEtc.toString());

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f100 = repo.getFeatures()[1];
        assertEquals("badConfigName", f100.getName());

        ConfigurationAdmin configAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        Configuration config = EasyMock.createMock(Configuration.class);

        EasyMock.expect(configAdmin.listConfigurations(EasyMock.anyString())).andReturn(null);
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(configAdmin.getConfiguration(EasyMock.anyString(), EasyMock.isNull())).andReturn(config);
        EasyMock.expectLastCall();

        EasyMock.replay(configAdmin);
        FeatureConfigInstaller installer = new FeatureConfigInstaller(configAdmin);
        installer.installFeatureConfigs(f100);

        EasyMock.verify(configAdmin);

        // Verify the file was not installed
        File installedFile = Paths.get(karafEtc.toString(), "../../../../../../../../../../../../tmp/config").toFile();
        assertFalse(installedFile.exists());

        karafEtc.toFile().delete();
    }

    @Test
    public void testBadConfigFileName() throws Exception {
        String dataDir = "data4";

        Path karafEtc = Files.createTempDirectory("etc");
        Path karafBase = Files.createTempDirectory("base");

        System.setProperty("karaf.etc", karafEtc.toString());
        System.setProperty("karaf.base", karafBase.toString());

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f100 = repo.getFeatures()[2];
        assertEquals("badConfigFileName", f100.getName());

        ConfigurationAdmin configAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(configAdmin.listConfigurations(EasyMock.anyString())).andReturn(null);
        EasyMock.expectLastCall().anyTimes();

        EasyMock.replay(configAdmin);
        FeatureConfigInstaller installer = new FeatureConfigInstaller(configAdmin);
        try {
            installer.installFeatureConfigs(f100);
            fail("Failure expected on a bad config filename");
        } catch (Throwable t) {
            // expected
        }
        EasyMock.verify(configAdmin);

        // Verify the file was not installed
        File installedFile = Paths.get(karafBase.toString(), "../../../../../../../../../../../../tmp/config2").toFile();
        assertFalse(installedFile.exists());

        karafBase.toFile().delete();
        karafEtc.toFile().delete();
    }

}
