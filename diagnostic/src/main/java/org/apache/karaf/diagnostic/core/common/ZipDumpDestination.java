/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.core.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.karaf.diagnostic.core.DumpDestination;

/**
 * Class which packages dumps to ZIP archive.
 */
public class ZipDumpDestination implements DumpDestination {

    /**
     * Destination streem.
     */
    private ZipOutputStream outputStream;

    /**
     * Creates new dump in given directory.
     * 
     * @param directory Target directory.
     * @param name Name of the archive.
     */
    public ZipDumpDestination(File directory, String name) {
        this(new File(directory, name));
    }

    /**
     * Creates new dump in given file (zip archive). 
     * 
     * @param file Destination file.
     */
    public ZipDumpDestination(File file) {
        try {
            outputStream = new ZipOutputStream(new FileOutputStream(
                file));
        } catch (FileNotFoundException e) {
            // sometimes this can occur, but we simply re throw and let 
            // caller handle exception
            throw new RuntimeException("Unable to create dump destination", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public OutputStream add(String name) throws Exception {
        ZipEntry zipEntry = new ZipEntry(name);
        outputStream.putNextEntry(zipEntry);
        return new ClosingEntryOutputStreamWrapper(outputStream);
    }

    /**
     * Closes archive handle.
     */
    public void save() throws Exception {
        outputStream.close();
    }

}
