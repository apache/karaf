/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.obr.command.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class FileUtil
{
    public static void downloadSource(
        PrintStream out, PrintStream err,
        URL srcURL, String dirStr, boolean extract)
    {
        // Get the file name from the URL.
        String fileName = (srcURL.getFile().lastIndexOf('/') > 0)
            ? srcURL.getFile().substring(srcURL.getFile().lastIndexOf('/') + 1)
            : srcURL.getFile();

        try
        {
            out.println("Connecting...");

            File dir = new File(dirStr);
            if (!dir.exists())
            {
                err.println("Destination directory does not exist.");
            }
            File file = new File(dir, fileName);

            URLConnection conn = srcURL.openConnection();
            int total = conn.getContentLength();

            if (total > 0)
            {
                out.println("Downloading " + fileName
                    + " ( " + total + " bytes ).");
            }
            else
            {
                out.println("Downloading " + fileName + ".");
            }
            try (var is = conn.getInputStream()) {
                Files.copy(is, file.toPath());
            }

            if (extract)
            {
                try (JarInputStream jis = new JarInputStream(Files.newInputStream(file.toPath()))) {
                    out.println("Extracting...");
                    unjar(jis, dir);
                }
                file.delete();
            }
        }
        catch (Exception ex)
        {
            err.println(ex);
        }
    }

    public static void unjar(JarInputStream jis, File dir)
        throws IOException
    {
        // Reusable buffer.
        byte[] buffer = new byte[4096];

        // Loop through JAR entries.
        for (JarEntry je = jis.getNextJarEntry();
             je != null;
             je = jis.getNextJarEntry())
        {
            if (je.getName().startsWith("/"))
            {
                throw new IOException("JAR resource cannot contain absolute paths.");
            }

            File target = new File(dir, je.getName());
            String canonicalizedDir = dir.getCanonicalPath();
            if (!canonicalizedDir.endsWith(File.separator)) {
                canonicalizedDir += File.separator;
            }
            if (!target.getCanonicalPath().startsWith(canonicalizedDir)) {
                throw new IOException("JAR resource cannot contain paths with .. characters");
            }

            // Check to see if the JAR entry is a directory.
            if (je.isDirectory())
            {
                if (!target.exists())
                {
                    if (!target.mkdirs())
                    {
                        throw new IOException("Unable to create target directory: "
                            + target);
                    }
                }
                // Just continue since directories do not have content to copy.
                continue;
            }

            int lastIndex = je.getName().lastIndexOf('/');
            String name = (lastIndex >= 0) ?
                je.getName().substring(lastIndex + 1) : je.getName();
            String destination = (lastIndex >= 0) ?
                je.getName().substring(0, lastIndex) : "";

            // JAR files use '/', so convert it to platform separator.
            destination = destination.replace('/', File.separatorChar);
            copy(jis, dir, name, destination, buffer);
        }
    }

    public static void copy(
        InputStream is, File dir, String destName, String destDir, byte[] buffer)
        throws IOException
    {
        if (destDir == null)
        {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(dir, destDir);
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new IOException("Unable to create target directory: "
                    + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new IOException("Target is not a directory: "
                + targetDir);
        }
        Files.copy(is, new File(targetDir, destName).toPath());
    }
}
