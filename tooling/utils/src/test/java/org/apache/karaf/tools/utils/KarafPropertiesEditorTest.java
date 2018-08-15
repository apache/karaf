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

package org.apache.karaf.tools.utils;

import com.google.common.io.Resources;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test the property editing system.
 * See src/test/resources/.../test-edit.xml.
 */
public class KarafPropertiesEditorTest {
    private static final String ETC_TO_START_WITH = "../../main/src/test/resources/test-karaf-home/etc/";

    @Test
    public void onceOver() throws Exception {
        KarafPropertyInstructionsModelStaxReader kipmsr = new KarafPropertyInstructionsModelStaxReader();
        URL editsUrl = Resources.getResource(KarafPropertiesEditorTest.class, "test-edits.xml");
        KarafPropertyEdits edits;
        try (InputStream editsStream = Resources.asByteSource(editsUrl).openStream()) {
            edits = kipmsr.read(editsStream, true);
        }

        Path path = FileSystems.getDefault().getPath("target");
        Path outputEtc = Files.createTempDirectory(path, "test-etc");
        outputEtc.toFile().deleteOnExit();

        KarafPropertiesEditor editor = new KarafPropertiesEditor();
        editor.setInputEtc(new File(ETC_TO_START_WITH))
                .setOutputEtc(outputEtc.toFile())
                .setEdits(edits);
        editor.run();

        File resultConfigProps = new File(outputEtc.toFile(), "config.properties");
        Properties properties = new Properties();
        try (InputStream resultInputStream = new FileInputStream(resultConfigProps)) {
            properties.load(resultInputStream);
        }
        assertEquals("equinox", properties.getProperty("karaf.framework"));
        assertEquals("prepended,root,toor", properties.getProperty("karaf.name"));

        resultConfigProps = new File(outputEtc.toFile(), "jre.properties");
        try (InputStream resultInputStream = new FileInputStream(resultConfigProps)) {
            properties.load(resultInputStream);
        }

        assertEquals("This is the cereal: shot from guns", properties.getProperty("test-add-one"));
        assertEquals("This is the gun that shoots cereal", properties.getProperty("test-add-two"));
        assertNull(properties.getProperty("test-add-three"));
    }
}
