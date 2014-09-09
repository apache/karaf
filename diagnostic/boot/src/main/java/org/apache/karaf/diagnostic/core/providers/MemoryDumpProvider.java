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
package org.apache.karaf.diagnostic.core.providers;

import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import org.apache.karaf.diagnostic.core.common.TextDumpProvider;

/**
 * Provider which dump the memory information in the memory.txt file.
 */
public class MemoryDumpProvider extends TextDumpProvider {

    public MemoryDumpProvider() {
        super("memory.txt");
    }

    @Override
    protected void writeDump(OutputStreamWriter outputStream) throws Exception {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        outputStream.write("Number of objects waiting finalization: " + memoryMXBean.getObjectPendingFinalizationCount() + "\n\n");

        outputStream.write("Heap:\n");
        outputStream.write("\tInit:      " + memoryMXBean.getHeapMemoryUsage().getInit() + "\n");
        outputStream.write("\tUser:      " + memoryMXBean.getHeapMemoryUsage().getUsed() + "\n");
        outputStream.write("\tCommitted: " + memoryMXBean.getHeapMemoryUsage().getCommitted() + "\n");
        outputStream.write("\tMax:       " + memoryMXBean.getHeapMemoryUsage().getMax() + "\n");

        outputStream.write("Non-Heap: \n");
        outputStream.write("\tInit:      " + memoryMXBean.getNonHeapMemoryUsage().getInit() + "\n");
        outputStream.write("\tUser:      " + memoryMXBean.getNonHeapMemoryUsage().getUsed() + "\n");
        outputStream.write("\tCommitted: " + memoryMXBean.getNonHeapMemoryUsage().getCommitted() + "\n");
        outputStream.write("\tMax:       " + memoryMXBean.getNonHeapMemoryUsage().getMax() + "\n");

    }

}
