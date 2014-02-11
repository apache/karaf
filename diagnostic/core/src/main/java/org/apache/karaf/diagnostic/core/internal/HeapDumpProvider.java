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

import com.sun.management.HotSpotDiagnosticMXBean;
import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;

import javax.management.MBeanServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;

/**
 * Create a heap dump.
 */
public class HeapDumpProvider implements DumpProvider {

    @Override
    public void createDump(DumpDestination destination) throws Exception {
        FileInputStream in = null;
        OutputStream out = null;
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            HotSpotDiagnosticMXBean diagnosticMXBean = ManagementFactory.newPlatformMXBeanProxy(mBeanServer,
                    "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
            diagnosticMXBean.dumpHeap("heapdump.txt", false);
            // copy the dump in the destination
            File heapDumpFile = new File("heapdump.txt");
            in = new FileInputStream(heapDumpFile);
            out = destination.add("heapdump.txt");
            byte[] buffer = new byte[2048];
            while ((in.read(buffer) != -1)) {
                out.write(buffer);
            }
            in.close();
            out.flush();
            out.close();
            // remove the original dump
            if (heapDumpFile.exists()) {
                heapDumpFile.delete();
            }
        } catch (Exception e) {
            // nothing to do
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
