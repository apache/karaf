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
package org.apache.karaf.maven.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Dictionary;
import java.util.regex.Pattern;

import org.junit.Test;
import shaded.org.apache.commons.io.FileUtils;

import static org.junit.Assert.assertTrue;

public class MavenConfigurationSupportTest {

    @Test
    public void sequenceFiles() throws IOException {
        File dataDir = new File("target/data");
        FileUtils.deleteDirectory(dataDir);
        dataDir.mkdirs();

        MavenConfigurationSupport support = new MavenConfigurationSupport() {
            @Override
            protected void doAction(String prefix, Dictionary<String, Object> config) throws Exception { }
        };

        File newFile = support.nextSequenceFile(dataDir, Pattern.compile("file-(\\d+).txt"), "file-%04d.txt");
        assertTrue(Pattern.compile("^file-\\d+\\.txt$").matcher(newFile.getName()).matches());

        try (FileWriter fw = new FileWriter(new File(dataDir, "file-abcd.txt"))) {
            fw.write("~");
        }
        newFile = support.nextSequenceFile(dataDir, Pattern.compile("file-(\\d+).txt"), "file-%04d.txt");
        assertTrue(Pattern.compile("^file-\\d+\\.txt$").matcher(newFile.getName()).matches());

        try (FileWriter fw = new FileWriter(new File(dataDir, "file-0004.txt"))) {
            fw.write("~");
        }
        newFile = support.nextSequenceFile(dataDir, Pattern.compile("file-(\\d+).txt"), "file-%04d.txt");
        assertTrue(Pattern.compile("^file-\\d+\\.txt$").matcher(newFile.getName()).matches());
    }

}
