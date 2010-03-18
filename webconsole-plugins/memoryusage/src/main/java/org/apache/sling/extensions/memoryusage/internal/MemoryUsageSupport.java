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
package org.apache.sling.extensions.memoryusage.internal;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MemoryUsageSupport implements NotificationListener {

    // This is the name of the HotSpot Diagnostic MBean
    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    private final File dumpLocation;

    private int threshold;

    MemoryUsageSupport(final BundleContext context) {
        final String slingHome = context.getProperty("sling.home");
        if (slingHome != null) {
            dumpLocation = new File(slingHome, "dumps");
        } else {
            dumpLocation = new File("dumps");
        }
        dumpLocation.mkdirs();

        NotificationEmitter memEmitter = (NotificationEmitter) getMemory();
        memEmitter.addNotificationListener(this, null, null);
    }

    void dispose() {
        NotificationEmitter memEmitter = (NotificationEmitter) getMemory();
        try {
            memEmitter.removeNotificationListener(this);
        } catch (ListenerNotFoundException lnfes) {
            // TODO: not expected really ?
        }
    }

    final void setThreshold(final int percentage) {
        if (percentage < 50 || percentage > 100) {
            // wrong value
        } else {
            List<MemoryPoolMXBean> pools = getMemoryPools();
            for (MemoryPoolMXBean pool : pools) {
                if (pool.isUsageThresholdSupported()) {
                    long threshold = pool.getUsage().getMax() * percentage
                        / 100;
                    pool.setUsageThreshold(threshold);
                }
            }
            this.threshold = percentage;
        }
    }

    final int getThreshold() {
        return threshold;
    }

    final void printMemory(final PrintHelper pw) {
        pw.title("Overall Memory Use", 0);
        pw.keyVal("Heap Dump Threshold", getThreshold() + "%");
        printOverallMemory(pw);

        pw.title("Memory Pools", 0);
        printMemoryPools(pw);

        pw.title("Heap Dumps", 0);
        listDumpFiles(pw);
    }

    final void printOverallMemory(final PrintHelper pw) {
        final MemoryMXBean mem = getMemory();

        pw.keyVal("Verbose Memory Output", (mem.isVerbose() ? "yes" : "no"));
        pw.keyVal("Pending Finalizable Objects",
            mem.getObjectPendingFinalizationCount());

        pw.keyVal("Overall Heap Memory Usage", mem.getHeapMemoryUsage());
        pw.keyVal("Overall Non-Heap Memory Usage", mem.getNonHeapMemoryUsage());
    }

    final void printMemoryPools(final PrintHelper pw) {
        final List<MemoryPoolMXBean> pools = getMemoryPools();
        for (MemoryPoolMXBean pool : pools) {
            final String title = String.format("%s (%s, %s)", pool.getName(),
                pool.getType(), (pool.isValid() ? "valid" : "invalid"));
            pw.title(title, 1);

            pw.keyVal("Memory Managers",
                Arrays.asList(pool.getMemoryManagerNames()));

            pw.keyVal("Peak Usage", pool.getPeakUsage());

            pw.keyVal("Usage", pool.getUsage());
            if (pool.isUsageThresholdSupported()) {
                pw.keyVal("Usage Threshold", String.format(
                    "%d, %s, #exceeded=%d", pool.getUsageThreshold(),
                    pool.isUsageThresholdExceeded()
                            ? "exceeded"
                            : "not exceeded", pool.getUsageThresholdCount()));
            } else {
                pw.val("Usage Threshold: not supported");
            }
            pw.keyVal("Collection Usage", pool.getCollectionUsage());
            if (pool.isCollectionUsageThresholdSupported()) {
                pw.keyVal("Collection Usage Threshold", String.format(
                    "%d, %s, #exceeded=%d", pool.getCollectionUsageThreshold(),
                    pool.isCollectionUsageThresholdExceeded()
                            ? "exceeded"
                            : "not exceeded",
                    pool.getCollectionUsageThresholdCount()));
            } else {
                pw.val("Collection Usage Threshold: not supported");
            }
        }
    }

    final void listDumpFiles(final PrintHelper pw) {
        pw.title(dumpLocation.getAbsolutePath(), 1);
        File[] dumps = getDumpFiles();
        if (dumps == null || dumps.length == 0) {
            pw.keyVal("-- None", null);
        } else {
            long totalSize = 0;
            for (File dump : dumps) {
                // 32167397 2010-02-25 23:30 thefile
                pw.val(String.format("%10d %tF %2$tR %s", dump.length(),
                    new Date(dump.lastModified()), dump.getName()));
                totalSize += dump.length();
            }
            pw.val(String.format("%d files, %d bytes", dumps.length, totalSize));
        }
    }

    final File getDumpFile(final String name) {
        // expect a non-empty string without slash
        if (name == null || name.length() == 0 || name.indexOf('/') >= 0) {
            return null;
        }

        File dumpFile = new File(dumpLocation, name);
        if (dumpFile.isFile()) {
            return dumpFile;
        }

        return null;
    }

    final File[] getDumpFiles() {
        return dumpLocation.listFiles();
    }

    final boolean rmDumpFile(final String name) {
        if (name == null || name.length() == 0) {
            return false;
        }

        final File dumpFile = new File(dumpLocation, name);
        if (!dumpFile.exists()) {
            return false;
        }

        dumpFile.delete();
        return true;
    }

    /**
     * Dumps the heap to a temporary file
     *
     * @param live <code>true</code> if only live objects are to be returned
     * @return
     * @see http://blogs.sun.com/sundararajan/entry/
     *      programmatically_dumping_heap_from_java
     */
    final File dumpHeap(String name, final boolean live) throws Exception {
        try {
            if (name == null) {
                name = "heap." + System.currentTimeMillis() + ".bin";
            }
            dumpLocation.mkdirs();
            File tmpFile = new File(dumpLocation, name);
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.invoke(new ObjectName(HOTSPOT_BEAN_NAME), "dumpHeap",
                new Object[] { tmpFile.getAbsolutePath(), live }, new String[] {
                    String.class.getName(), boolean.class.getName() });
            return tmpFile;
            // } catch (JMException je) {
            // } catch (IOException ioe) {
        } finally {

        }

        // failure
        // return null;
    }

    final MemoryMXBean getMemory() {
        return ManagementFactory.getMemoryMXBean();
    }

    final List<MemoryPoolMXBean> getMemoryPools() {
        return ManagementFactory.getMemoryPoolMXBeans();
    }

    public void handleNotification(Notification notification, Object handback) {
        Logger log = LoggerFactory.getLogger(getClass());
        String notifType = notification.getType();
        if (notifType.equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
            try {
                File file = dumpHeap(null, true);
                log.warn("Heap dumped to " + file);
            } catch (Exception e) {
                log.error("Failed dumping heap", e);
            }
        }
    }

    static interface PrintHelper {
        void title(final String title, final int level);

        void val(final String value);

        void keyVal(final String key, final Object value);
    }
}
