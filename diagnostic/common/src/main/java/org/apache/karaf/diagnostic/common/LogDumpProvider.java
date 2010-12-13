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
package org.apache.karaf.diagnostic.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;

/**
 * Dump provider which copies log files from data/log directory to
 * destination.
 * 
 * @author ldywicki
 */
public class LogDumpProvider implements DumpProvider {

    /**
     * Attach log entries from directory.
     */
    public void createDump(DumpDestination destination) throws Exception {
        File logDir = new File("data/log");
        File[] listFiles = logDir.listFiles();

        // ok, that's not the best way of doing that..
        for (File file : listFiles) {
            FileInputStream inputStream = new FileInputStream(file);

            OutputStream outputStream = destination.add("log/" + file.getName());

            copy(inputStream, outputStream);
        }
    }

    /**
     * Rewrites data from input stream to output stream. This code is very common
     * but we would avoid additional dependencies in diagnostic stuff.
     * 
     * @param inputStream Source stream.
     * @param outputStream Destination stream.
     * @throws IOException When IO operation fails.
     */
	private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = inputStream.read(buffer))) {
            outputStream.write(buffer, 0, n);
        }
	}

}
