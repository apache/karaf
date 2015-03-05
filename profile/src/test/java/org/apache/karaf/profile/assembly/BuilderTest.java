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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

public class BuilderTest {

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
