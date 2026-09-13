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

import org.apache.felix.utils.properties.TypedProperties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Objects;

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

    /**
     * GH-2805: updating an existing cfg file (append or override) must write through a
     * temporary file and rename it into place, so that a concurrent writer (e.g. fileinstall
     * persisting the same configuration on the CM Event Dispatcher thread) can never observe
     * a partially written / corrupted cfg file, and no leftover temp file remains behind.
     */
    @Test
    public void testUpdateExistingConfigWritesAtomically() throws Exception {
        File tmpDir = Files.createTempDirectory("karaf-feature-config-installer-test").toFile();
        System.setProperty("karaf.etc", tmpDir.getAbsolutePath());

        File cfgFile = new File(tmpDir, "my.pid.cfg");
        try (FileWriter writer = new FileWriter(cfgFile)) {
            writer.write("existing.key=existing.value\n");
        }

        TypedProperties toAppend = new TypedProperties();
        toAppend.load(new StringReader("appended.key=appended.value\n"));

        FeatureConfigInstaller installer = new FeatureConfigInstaller(null, true);

        Method updateExistingConfig = FeatureConfigInstaller.class.getDeclaredMethod(
                "updateExistingConfig", TypedProperties.class, boolean.class, File.class, boolean.class);
        updateExistingConfig.setAccessible(true);
        updateExistingConfig.invoke(installer, toAppend, true, cfgFile, false);

        TypedProperties result = new TypedProperties();
        result.load(cfgFile);
        assertEquals("existing.value", result.get("existing.key"));
        assertEquals("appended.value", result.get("appended.key"));

        File[] leftovers = tmpDir.listFiles((dir, name) -> name.endsWith(".tmp"));
        assertTrue("no temporary file must be left behind after the atomic rename",
                Objects.requireNonNull(leftovers).length == 0);
    }

}
