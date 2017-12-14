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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.service.RepositoryCacheImpl;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Constants;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class BuilderTest {

    @Test
    public void testCyclicRepos() throws Exception {
        Path workDir = Paths.get("target/distrib");
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

        Path mvnRepo = Paths.get("target/test-classes/repo");
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

    @Test
    @Ignore("This test can not run at this position as it needs the staticFramework kar which is not yet available")
    public void testBuilder() throws Exception {

        Path workDir = Paths.get("target/distrib");
        recursiveDelete(workDir);

        Builder builder = Builder.newInstance()
                .staticFramework()
                .profilesUris("jar:mvn:org.apache.karaf.demos.profiles/registry/4.0.0-SNAPSHOT!/")
                .environment("static")
                .profiles("karaf",
                          "example-loanbroker-bank1",
                          "example-loanbroker-bank2",
                          "example-loanbroker-bank3",
                          "example-loanbroker-broker",
                          "activemq-broker")
                .homeDirectory(workDir);

        try {
            builder.generateAssembly();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    //@Test
    //@Ignore("no need to run this test")
    public void consistencyReport() {
        Map<String, Features> features = new LinkedHashMap<>();
        Builder builder = new Builder();
        File[] uris = new File("src/test/resources/repositories").listFiles((dir, name) -> name.endsWith(".xml"));
        if (uris != null) {
            for (File f : uris) {
                features.put(f.getName(), new RepositoryImpl(f.toURI(), false).getFeaturesInternal());
            }
        }
        builder.generateConsistencyReport(features, new File("target/consistency.xml"), false);
        builder.generateConsistencyReport(features, new File("target/consistency-full.xml"), true);
    }

    private static void recursiveDelete(Path path) throws IOException {
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
