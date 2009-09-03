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
package org.apache.felix.fileinstall.internal;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.io.File;
import java.io.FilenameFilter;

/**
 * A Scanner object is able to detect and report new, modified
 * and deleted files.
 *
 * The scanner use an internal checksum to identify the signature
 * of a file or directory.  The checksum will change if the file
 * or any of the directory's child is modified.
 *
 * In addition, if the scanner detects a change on a given file, it
 * will wait until the checksum does not change anymore before reporting
 * the change on this file.  This allows to not report the change until
 * a big copy if complete for example.
 */
public class Scanner {

    final File directory;
    final FilenameFilter filter;

    // Store checksums of files or directories
    Map/* <File, Long> */ lastChecksums = new HashMap/* <File, Long> */();
    Map/* <File, Long> */ storedChecksums = new HashMap/* <File, Long> */();

    /**
     * Create a scanner for the specified directory
     *
     * @param directory the directory to scan
     */
    public Scanner(File directory)
    {
        this(directory, null);
    }

    /**
     * Create a scanner for the specified directory and file filter
     *
     * @param directory the directory to scan
     * @param filter a filter for file names
     */
    public Scanner(File directory, FilenameFilter filter)
    {
        this.directory = directory;
        this.filter = filter;
    }

    /**
     * Initialize the list of known files.
     * This should be called before the first scan to initialize
     * the list of known files.  The purpose is to be able to detect
     * files that have been deleted while the scanner was inactive.
     *
     * @param files a list of known files
     */
    public void initialize(Collection/*<File>*/ files)
    {
        for (Iterator it = files.iterator(); it.hasNext();)
        {
            storedChecksums.put(it.next(), Long.valueOf(0));
        }
    }

    /**
     * Report a set of new, modified or deleted files.
     * Modifications are checked against a computed checksum on some file
     * attributes to detect any modification.
     * Upon restart, such checksums are not known so that all files will
     * be reported as modified. 
     *
     * @return a list of changes on the files included in the directory
     */
    public Set/*<File>*/ scan()
    {
        File[] list = directory.listFiles(filter);
        if (list == null)
        {
            return null;
        }
        Set/*<File>*/ files = new HashSet/*<File>*/();
        Set/*<File>*/ removed = new HashSet/*<File>*/(storedChecksums.keySet());
        for (int i = 0; i < list.length; i++)
        {
            File file  = list[i];
            long lastChecksum = lastChecksums.get(file) != null ? ((Long) lastChecksums.get(file)).longValue() : 0;
            long storedChecksum = storedChecksums.get(file) != null ? ((Long) storedChecksums.get(file)).longValue() : 0;
            long newChecksum = checksum(file);
            lastChecksums.put(file, Long.valueOf(newChecksum));
            // Only handle file when it does not change anymore and it has changed since last reported
            if (newChecksum == lastChecksum && newChecksum != storedChecksum)
            {
                storedChecksums.put(file, Long.valueOf(newChecksum));
                files.add(file);
            }
            removed.remove(file);
        }
        for (Iterator it = removed.iterator(); it.hasNext();)
        {
            File file = (File) it.next();
            // Make sure we'll handle a file that has been deleted
            files.addAll(removed);
            // Remove no longer used checksums
            lastChecksums.remove(file);
            storedChecksums.remove(file);
        }
        return files;
    }

    /**
     * Compute a cheksum for the file or directory that consists of the name, length and the last modified date
     * for a file and its children in case of a directory
     *
     * @param file the file or directory
     * @return a checksum identifying any change
     */
    static long checksum(File file)
    {
        CRC32 crc = new CRC32();
        checksum(file, crc);
        return crc.getValue();
    }

    private static void checksum(File file, CRC32 crc)
    {
        crc.update(file.getName().getBytes());
        if (file.isFile())
        {
            checksum(file.lastModified(), crc);
            checksum(file.length(), crc);
        }
        else if (file.isDirectory())
        {
            File[] children = file.listFiles();
            if (children != null)
            {
                for (int i = 0; i < children.length; i++)
                {
                    checksum(children[i], crc);
                }
            }
        }
    }

    private static void checksum(long l, CRC32 crc)
    {
        for (int i = 0; i < 8; i++)
        {
            crc.update((int) (l & 0x000000ff));
            l >>= 8;
        }
    }

}
