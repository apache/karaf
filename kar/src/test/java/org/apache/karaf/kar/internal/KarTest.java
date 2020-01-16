/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.kar.internal;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KarTest {

    @Test
    public void karExtractTest() throws Exception {
        File base = new File("target/test");
        base.mkdirs();

        Kar kar = new Kar(new URI("https://repo1.maven.org/maven2/org/apache/karaf/features/framework/4.2.2/framework-4.2.2.kar"));
        File repoDir = new File("target/test/framework-repo");
        repoDir.mkdirs();
        File resourcesDir = new File("target/test/framework-resources");
        resourcesDir.mkdirs();

        kar.extract(repoDir, resourcesDir);

        File[] repoDirFiles = repoDir.listFiles();
        Assert.assertEquals(1, repoDirFiles.length);
        Assert.assertEquals("org", repoDirFiles[0].getName());
        File[] resourceDirFiles = resourcesDir.listFiles();
        Assert.assertEquals(6, resourceDirFiles.length);
    }

    @Test
    public void badKarExtractTest() throws Exception {
        File base = new File("target/test");
        base.mkdirs();
        File badKarFile = new File(base,"bad.kar");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(badKarFile));
        ZipEntry entry = new ZipEntry("../../../../foo.bar");
        zos.putNextEntry(entry);

        byte[] data = "Test Data".getBytes();
        zos.write(data, 0, data.length);
        zos.closeEntry();
        zos.close();

        Kar kar = new Kar(new URI("file:target/test/bad.kar"));
        File repoDir = new File("target/test/repo");
        repoDir.mkdirs();
        File resourceDir = new File("target/test/resources");
        resourceDir.mkdirs();
        kar.extract(repoDir, resourceDir);

        File[] repoDirFiles = repoDir.listFiles();
        Assert.assertEquals(0, repoDirFiles.length);
        File[] resourceDirFiles = resourceDir.listFiles();
        Assert.assertEquals(0, resourceDirFiles.length);

        badKarFile.delete();
    }

    @Test
    public void badEncodedKarExtractTest() throws Exception {
        File base = new File("target/test");
        base.mkdirs();
        File badKarFile = new File(base,"badencoded.kar");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(badKarFile));
        // Use the encoded form of ".." here
        ZipEntry entry = new ZipEntry("%2e%2e/%2e%2e/%2e%2e/%2e%2e/foo.bar");
        zos.putNextEntry(entry);

        byte[] data = "Test Data".getBytes();
        zos.write(data, 0, data.length);
        zos.closeEntry();
        zos.close();

        Kar kar = new Kar(new URI("file:target/test/badencoded.kar"));
        File repoDir = new File("target/test/repo");
        repoDir.mkdirs();
        File resourceDir = new File("target/test/resources");
        resourceDir.mkdirs();
        kar.extract(repoDir, resourceDir);

        File[] repoDirFiles = repoDir.listFiles();
        Assert.assertEquals(0, repoDirFiles.length);
        File[] resourceDirFiles = resourceDir.listFiles();
        Assert.assertEquals(0, resourceDirFiles.length);

        badKarFile.delete();
    }

}
