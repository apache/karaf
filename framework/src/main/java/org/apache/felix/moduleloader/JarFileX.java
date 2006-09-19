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
package org.apache.felix.moduleloader;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * The purpose of this class is to fix an apparent bug in the JVM in versions
 * 1.4.2 and lower where directory entries in ZIP/JAR files are not correctly
 * identified.
**/
public class JarFileX extends JarFile
{
    public JarFileX(File file) throws IOException
    {
        super(file);
    }

    public JarFileX(File file, boolean verify) throws IOException
    {
        super(file, verify);
    }

    public JarFileX(File file, boolean verify, int mode) throws IOException
    {
        super(file, verify, mode);
    }

    public JarFileX(String name) throws IOException
    {
        super(name);
    }

    public JarFileX(String name, boolean verify) throws IOException
    {
        super(name, verify);
    }

    public ZipEntry getEntry(String name)
    {
        ZipEntry entry = super.getEntry(name);
        if ((entry != null) && (entry.getSize() == 0) && !entry.isDirectory())
        {
            ZipEntry dirEntry = super.getEntry(name + '/');
            if (dirEntry != null)
            {
                entry = dirEntry;
            }
        }
        return entry;
    }

    public JarEntry getJarEntry(String name)
    {
        JarEntry entry = super.getJarEntry(name);
        if ((entry != null) && (entry.getSize() == 0) && !entry.isDirectory())
        {
            JarEntry dirEntry = super.getJarEntry(name + '/');
            if (dirEntry != null)
            {
                entry = dirEntry;
            }
        }
        return entry;
    }


}
