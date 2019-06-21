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
package org.apache.karaf.util.bundles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.osgi.framework.Constants;

public class BundleUtils {

    public static File fixBundleWithUpdateLocation(InputStream is, String uri) throws IOException {
        File file = Files.createTempFile("update-", ".jar").toFile();
        try (ZipInputStream zis = new ZipInputStream(is);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {

            byte[] buf = new byte[8192];
            zos.setLevel(0);
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int n;
                while (-1 != (n = zis.read(buf))) {
                    baos.write(buf, 0, n);
                }
                if (entry.getName().equals(JarFile.MANIFEST_NAME)) {
                    Manifest man = new Manifest(new ByteArrayInputStream(baos.toByteArray()));
                    if (man.getMainAttributes().getValue(Constants.BUNDLE_UPDATELOCATION) == null) {
                        man.getMainAttributes().putValue(Constants.BUNDLE_UPDATELOCATION, uri);
                    }
                    baos.reset();
                    man.write(baos);
                }
                byte[] data = baos.toByteArray();
                CRC32 crc = new CRC32();
                crc.update(data);
                entry = new ZipEntry(entry.getName());
                entry.setSize(data.length);
                entry.setCrc(crc.getValue());
                zos.putNextEntry(entry);
                zos.write(data);
                zis.closeEntry();
                zos.closeEntry();
            }
        }
        return file;
    }

}
