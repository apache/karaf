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
package org.apache.felix.deploymentadmin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class will write all entries encountered in an inputstream to disk. An index of files written to disk is kept in an index file in the
 * order they were encountered. Each file is compressed using GZIP. All the work is done on a separate thread.
 */
class ExplodingOutputtingInputStream extends OutputtingInputStream implements Runnable {

    private final Thread m_task;
    private final File m_contentDir;
    private final File m_indexFile;
    private final PipedInputStream m_input;

    /**
     * Creates an instance of this class.
     *
     * @param inputStream The input stream that will be written to disk as individual entries as it's read.
     * @param indexFile File to be used to write the index of all encountered files.
     * @param contentDir File to be used as the directory to hold all files encountered in the stream.
     * @throws IOException If a problem occurs reading the stream resources.
     */
    public ExplodingOutputtingInputStream(InputStream inputStream, File indexFile, File contentDir) throws IOException {
        this(inputStream, new PipedOutputStream(), indexFile,  contentDir);
    }

    public void close() throws IOException {
        super.close();
        waitFor();
    }

    private ExplodingOutputtingInputStream(InputStream inputStream, PipedOutputStream output, File index, File root) throws IOException {
        super(inputStream, output);
        m_contentDir = root;
        m_indexFile = index;
        m_input = new PipedInputStream(output);
        m_task = new Thread(this, "LiQ - ExplodingIncomingThread");
        m_task.start();
    }

    public void waitFor() {
        try {
            m_task.join();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void run() {
        ZipInputStream input = null;
        PrintWriter writer = null;
        try {
            input = new ZipInputStream(m_input);
            writer = new PrintWriter(new FileWriter(m_indexFile));
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                File current = new File(m_contentDir, entry.getName());
                if (entry.isDirectory()) {
                    current.mkdirs();
                }
                else {
                    writer.println(entry.getName());
                    File parent = current.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }
                    OutputStream output = null;
                    try {
                        output = new GZIPOutputStream(new FileOutputStream(current));
                        byte[] buffer = new byte[4096];
                        for (int i = input.read(buffer); i > -1; i = input.read(buffer)) {
                            output.write(buffer, 0, i);
                        }
                    }
                    finally {
                        output.close();
                    }
                }
                input.closeEntry();
                writer.flush();
            }
        } catch (IOException ex) {
            // TODO: log this
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                }
                catch (IOException e) {
                    // Not much we can do
                }
            }
            if (writer != null) {
                writer.close();
            }
        }
    }


    public static void replace(File target, File source) {
        delete(target, true);
        source.renameTo(target);
    }

    private static void delete(File root, boolean deleteRoot) {
        if (root.isDirectory()) {
            File[] childs = root.listFiles();
            for (int i = 0; i < childs.length; i++) {
                delete(childs[i], true);
            }
        }
        if (deleteRoot) {
            root.delete();
        }
    }

    public static void merge(File targetIndex, File target, File sourceIndex, File source) throws IOException {
        List targetFiles = readIndex(targetIndex);
        List sourceFiles = readIndex(sourceIndex);
        List result = new ArrayList(targetFiles);

        File manifestFile = new File(source, (String) sourceFiles.remove(0));
        Manifest resultManifest = new Manifest(new GZIPInputStream(new FileInputStream(manifestFile)));

        resultManifest.getMainAttributes().remove(new Name(Constants.DEPLOYMENTPACKAGE_FIXPACK));

        for (Iterator i = result.iterator(); i.hasNext();) {
            String targetFile = (String) i.next();
            if(!resultManifest.getEntries().containsKey(targetFile) && !targetFile.equals("META-INF/MANIFEST.MF")) {
                i.remove();
            }
        }

        for (Iterator iter = sourceFiles.iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            if (targetFiles.contains(path)) {
                (new File(target, path)).delete();
            }
            else {
                result.add(path);
            }
            (new File(source, path)).renameTo(new File(target, path));
        }

        targetFiles.removeAll(sourceFiles);

        for (Iterator iter = resultManifest.getEntries().keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            Attributes sourceAttribute = (Attributes) resultManifest.getEntries().get(path);
            if ("true".equals(sourceAttribute.remove(new Name(Constants.DEPLOYMENTPACKAGE_MISSING)))) {
                targetFiles.remove(path);
            }
        }

        for (Iterator iter = targetFiles.iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            (new File(target, path)).delete();
        }

        GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(new File(target, "META-INF/MANIFEST.MF")));
        resultManifest.write(outputStream);
        outputStream.close();
        writeIndex(targetIndex, result);
    }

    public static List readIndex(File index) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(index));
            List result = new ArrayList();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                result.add(line);
            }
            return result;
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    // Not much we can do
                }
            }
        }
    }

    private static void writeIndex(File index, List input) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(index));
            for (Iterator iterator = input.iterator(); iterator.hasNext();) {
                writer.println(iterator.next());
            }
        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
