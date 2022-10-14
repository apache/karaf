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
package org.apache.karaf.profile.assembly;

import org.junit.Test;
import org.osgi.framework.Constants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class BuilderTest {

    private final Path workDir = Paths.get("target/distrib");
    private final Path mvnRepo = Paths.get("target/test-classes/repo");


    private void setUp() throws Exception {
        recursiveDelete(workDir);

        // Create dummy etc/config.properties file
        Path config = workDir.resolve("etc/config.properties");
        Files.createDirectories(config.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(config)) {
            w.write(Constants.FRAMEWORK_SYSTEMPACKAGES + "= org.osgi.dto");
            w.newLine();
            w.write(Constants.FRAMEWORK_SYSTEMCAPABILITIES + "= ");
            w.newLine();
        }
    }

    @Test
    public void testCyclicRepos() throws Exception {
        setUp();

        Builder builder = Builder.newInstance()
                .repositories(Builder.Stage.Startup, true, "mvn:foo/baz/1.0/xml/features")
                .homeDirectory(workDir)
                .localRepository(mvnRepo.toString());
        try {
            builder.generateAssembly();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testFeatures() throws Exception {
        setUp();

        Builder builder = Builder.newInstance()
                .repositories(Builder.Stage.Startup, false, "mvn:foo/qux/1.0/xml/features")
                .homeDirectory(workDir)
                .localRepository(mvnRepo.toString())
                .features("qux-*");

        builder.generateAssembly();

        assertFeaturesBootContainsFeatures("qux-feature", "qux-feature2");
    }

    @Test
    public void testFirstStageBootFeatures() throws Exception {
        setUp();

        Builder builder = Builder.newInstance()
                .repositories(Builder.Stage.Startup, false, "mvn:foo/qux/1.0/xml/features")
                .homeDirectory(workDir).localRepository(mvnRepo.toString())
                .firstStageBootFeatures("another-feature")
                .features("qux-*");

        builder.generateAssembly();

        assertFeaturesBootContainsFeatures("qux-feature", "qux-feature2", "(another-feature)");
    }

    private void assertFeaturesBootContainsFeatures(String... expectedFeatures) throws IOException {
        Path featuresCfgFilePath = Paths.get(workDir.toString(), "/etc/org.apache.karaf.features.cfg");
        List<String> featuresCfgFile = Files.readAllLines(featuresCfgFilePath);

        assertTrue(featuresCfgFile.stream().anyMatch(line -> line.startsWith("featuresBoot")));

        for (String feature : expectedFeatures) {
            assertTrue(featuresCfgFile.stream().anyMatch(line -> line.contains(feature)));
        }
    }

    @Test
    public void testPidsToExtract() {
        String pidsToExtract = "\n" +
                "                        !jmx.acl.*,\n" +
                "                        !org.apache.karaf.command.acl.*,\n" +
                "                        *\n" +
                "                    ";
        List<String> p2e = Arrays.asList(pidsToExtract.split(","));
        Builder builder = Builder.newInstance().pidsToExtract(p2e);
        assertThat(builder.getPidsToExtract().get(0), equalTo("!jmx.acl.*"));
        assertThat(builder.getPidsToExtract().get(1), equalTo("!org.apache.karaf.command.acl.*"));
        assertThat(builder.getPidsToExtract().get(2), equalTo("*"));
    }

    private void recursiveDelete(Path path) throws IOException {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                    for (Path child : children) {
                        recursiveDelete(child);
                    }
                }
            }
            Files.delete(path);
        }
    }
}
