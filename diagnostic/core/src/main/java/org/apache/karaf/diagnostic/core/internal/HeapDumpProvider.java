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
package org.apache.karaf.diagnostic.core.internal;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

/**
 * Create a heap dump.
 */
public class HeapDumpProvider implements DumpProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(HeapDumpProvider.class);

    public void createDump(DumpDestination destination) throws Exception {
        FileInputStream in = null;
        OutputStream out = null;
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            Class<?> diagnosticMXBeanClass = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            Object diagnosticMXBean = ManagementFactory.newPlatformMXBeanProxy(mBeanServer,
                "com.sun.management:type=HotSpotDiagnostic", diagnosticMXBeanClass);

            Method method = diagnosticMXBeanClass.getMethod("dumpHeap", String.class, boolean.class);
            method.invoke(diagnosticMXBean, "heapdump.txt", false);

            // copy the dump in the destination
            File heapDumpFile = new File("heapdump.txt");
            in = new FileInputStream(heapDumpFile);
            out = destination.add("heapdump.txt");
            LogDumpProvider.copy(in, out);
            // remove the original dump
            if (heapDumpFile.exists()) {
                heapDumpFile.delete();
            }
        } catch (Exception e) {
            LOGGER.warn("Can't create heapdump", e);
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

}
