/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.webconsole.plugins.memoryusage.internal;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MemoryUsageSupport implements NotificationListener
{

    // This is the name of the HotSpot Diagnostic MBean
    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    private final Logger log = LoggerFactory.getLogger(getClass());

    // the default dump location: the dumps folder in the bundle private data
    // or the current working directory
    private final File defaultDumpLocation;

    // the default threshold value
    private final int defaultThreshold;

    // the configured dump location
    private File dumpLocation;

    // the actual threshold (configured or dynamically set in the console UI)
    private int threshold;

    MemoryUsageSupport(final BundleContext context)
    {

        // the default dump location
        String propDumps = context.getProperty(MemoryUsageConstants.PROP_DUMP_LOCATION);
        if (propDumps == null)
        {
            propDumps = "dumps";
        }

        // ensure dump location is an absolute path/location
        File dumps = new File(propDumps);
        if (!dumps.isAbsolute())
        {
            File bundleDumps = context.getDataFile(propDumps);
            if (bundleDumps != null)
            {
                dumps = bundleDumps;
            }
        }
        this.defaultDumpLocation = dumps.getAbsoluteFile();

        // prepare the dump location
        setDumpLocation(null);

        // register for memory threshold notifications
        NotificationEmitter memEmitter = (NotificationEmitter) getMemory();
        memEmitter.addNotificationListener(this, null, null);

        // set the initial automatic dump threshold
        int defaultThreshold;
        String propThreshold = context.getProperty(MemoryUsageConstants.PROP_DUMP_THRESHOLD);
        if (propThreshold != null)
        {
            try
            {
                defaultThreshold = Integer.parseInt(propThreshold);
                setThreshold(defaultThreshold);
            }
            catch (Exception e)
            {
                // NumberFormatException - if propTreshold cannot be parsed to
                // int
                // IllegalArgumentException - if threshold is invalid
                defaultThreshold = -1;
            }
        }
        else
        {
            defaultThreshold = -1;
        }

        // default threshold has not been configured (correctly), assume fixed
        // default
        if (defaultThreshold < 0)
        {
            defaultThreshold = MemoryUsageConstants.DEFAULT_DUMP_THRESHOLD;
            setThreshold(defaultThreshold);
        }

        this.defaultThreshold = defaultThreshold;
    }

    void dispose()
    {
        NotificationEmitter memEmitter = (NotificationEmitter) getMemory();
        try
        {
            memEmitter.removeNotificationListener(this);
        }
        catch (ListenerNotFoundException e)
        {
            // don't care
        }
    }

    /**
     * Sets the threshold percentage.
     *
     * @param percentage The threshold as a percentage of memory consumption.
     *            This value may be 0 (zero) to switch off automatic heap dumps
     *            or in the range {@link #MIN_DUMP_THRESHOLD} to
     *            {@link #MAX_DUMP_THRESHOLD}. If set to a negative value,
     *            the default threshold is assumed.
     * @throws IllegalArgumentException if the percentage value is outside of
     *             the valid range of thresholds. The message is the percentage
     *             value which is not accepted.
     */
    final void setThreshold(int percentage)
    {
        if (percentage < 0)
        {
            percentage = defaultThreshold;
        }

        if (MemoryUsageConstants.isThresholdValid(percentage))
        {
            TreeSet<String> thresholdPools = new TreeSet<String>();
            TreeSet<String> noThresholdPools = new TreeSet<String>();
            List<MemoryPoolMXBean> pools = getMemoryPools();
            for (MemoryPoolMXBean pool : pools)
            {
                if (pool.isUsageThresholdSupported())
                {
                    long threshold = pool.getUsage().getMax() * percentage / 100;
                    pool.setUsageThreshold(threshold);
                    thresholdPools.add(pool.getName());
                }
                else
                {
                    noThresholdPools.add(pool.getName());
                }
            }
            this.threshold = percentage;

            log.info("Setting Automatic Memory Dump Threshold to {}% for pools {}", threshold, thresholdPools);
            log.info("Automatic Memory Dump cannot be set for pools {}", noThresholdPools);
        }
        else
        {
            throw new IllegalArgumentException(String.valueOf(percentage));
        }
    }

    final int getThreshold()
    {
        return threshold;
    }

    final void printMemory(final PrintHelper pw)
    {
        pw.title("Overall Memory Use", 0);
        pw.keyVal("Heap Dump Threshold", getThreshold() + "%");
        printOverallMemory(pw);

        pw.title("Memory Pools", 0);
        printMemoryPools(pw);

        pw.title("Heap Dumps", 0);
        listDumpFiles(pw);
    }

    final void printOverallMemory(final PrintHelper pw)
    {
        final MemoryMXBean mem = getMemory();

        pw.keyVal("Verbose Memory Output", (mem.isVerbose() ? "yes" : "no"));
        pw.keyVal("Pending Finalizable Objects", mem.getObjectPendingFinalizationCount());

        pw.keyVal("Overall Heap Memory Usage", mem.getHeapMemoryUsage());
        pw.keyVal("Overall Non-Heap Memory Usage", mem.getNonHeapMemoryUsage());
    }

    final void printMemoryPools(final PrintHelper pw)
    {
        final List<MemoryPoolMXBean> pools = getMemoryPools();
        for (MemoryPoolMXBean pool : pools)
        {
            final String title = String.format("%s (%s, %s)", pool.getName(), pool.getType(), (pool.isValid() ? "valid"
                : "invalid"));
            pw.title(title, 1);

            pw.keyVal("Memory Managers", Arrays.asList(pool.getMemoryManagerNames()));

            pw.keyVal("Peak Usage", pool.getPeakUsage());

            pw.keyVal("Usage", pool.getUsage());
            if (pool.isUsageThresholdSupported())
            {
                pw.keyVal("Usage Threshold", String.format("%d, %s, #exceeded=%d", pool.getUsageThreshold(), pool
                    .isUsageThresholdExceeded() ? "exceeded" : "not exceeded", pool.getUsageThresholdCount()));
            }
            else
            {
                pw.val("Usage Threshold: not supported");
            }
            pw.keyVal("Collection Usage", pool.getCollectionUsage());
            if (pool.isCollectionUsageThresholdSupported())
            {
                pw.keyVal("Collection Usage Threshold", String.format("%d, %s, #exceeded=%d", pool
                    .getCollectionUsageThreshold(), pool.isCollectionUsageThresholdExceeded() ? "exceeded"
                    : "not exceeded", pool.getCollectionUsageThresholdCount()));
            }
            else
            {
                pw.val("Collection Usage Threshold: not supported");
            }
        }
    }

    final String getMemoryPoolsJson()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("[");

        long usedTotal = 0;
        long initTotal = 0;
        long committedTotal = 0;
        long maxTotal = 0;

        final List<MemoryPoolMXBean> pools = getMemoryPools();
        for (MemoryPoolMXBean pool : pools)
        {
            buf.append("{");

            buf.append("'name':'").append(pool.getName()).append('\'');
            buf.append(",'type':'").append(pool.getType()).append('\'');

            MemoryUsage usage = pool.getUsage();
            usedTotal += formatNumber(buf, "used", usage.getUsed());
            initTotal += formatNumber(buf, "init", usage.getInit());
            committedTotal += formatNumber(buf, "committed", usage.getCommitted());
            maxTotal += formatNumber(buf, "max", usage.getMax());

            final long score = 100L * usage.getUsed() / usage.getMax();
            buf.append(",'score':'").append(score).append("%'");

            buf.append("},");
        }

        // totalisation
        buf.append("{");
        buf.append("'name':'Total','type':'TOTAL'");
        formatNumber(buf, "used", usedTotal);
        formatNumber(buf, "init", initTotal);
        formatNumber(buf, "committed", committedTotal);
        formatNumber(buf, "max", maxTotal);

        final long score = 100L * usedTotal / maxTotal;
        buf.append(",'score':'").append(score).append("%'");

        buf.append("}");

        buf.append("]");
        return buf.toString();
    }

    long formatNumber(final StringBuilder buf, final String title, final long value)
    {

        final BigDecimal KB = new BigDecimal(1000L);
        final BigDecimal MB = new BigDecimal(1000L * 1000);
        final BigDecimal GB = new BigDecimal(1000L * 1000 * 1000);

        BigDecimal bd = new BigDecimal(value);
        final String suffix;
        if (bd.compareTo(GB) > 0)
        {
            bd = bd.divide(GB);
            suffix = "GB";
        }
        else if (bd.compareTo(MB) > 0)
        {
            bd = bd.divide(MB);
            suffix = "MB";
        }
        else if (bd.compareTo(KB) > 0)
        {
            bd = bd.divide(KB);
            suffix = "kB";
        }
        else
        {
            suffix = "B";
        }
        bd = bd.setScale(2, RoundingMode.UP);
        buf.append(",'").append(title).append("':'").append(bd).append(suffix).append('\'');
        return value;
    }

    final String getDefaultDumpLocation()
    {
        return defaultDumpLocation.getAbsolutePath();
    }

    final void setDumpLocation(final String dumpLocation)
    {
        if (dumpLocation == null || dumpLocation.length() == 0)
        {
            this.dumpLocation = defaultDumpLocation;
        }
        else
        {
            this.dumpLocation = new File(dumpLocation).getAbsoluteFile();
        }

        log.info("Storing Memory Dumps in {}", this.dumpLocation);
    }

    final File getDumpLocation()
    {
        return dumpLocation;
    }

    final void listDumpFiles(final PrintHelper pw)
    {
        pw.title(dumpLocation.getAbsolutePath(), 1);
        File[] dumps = getDumpFiles();
        if (dumps == null || dumps.length == 0)
        {
            pw.keyVal("-- None", null);
        }
        else
        {
            long totalSize = 0;
            for (File dump : dumps)
            {
                // 32167397 2010-02-25 23:30 thefile
                pw
                    .val(String.format("%10d %tF %2$tR %s", dump.length(), new Date(dump.lastModified()), dump
                        .getName()));
                totalSize += dump.length();
            }
            pw.val(String.format("%d files, %d bytes", dumps.length, totalSize));
        }
    }

    final File getDumpFile(final String name)
    {
        // expect a non-empty string without slash
        if (name == null || name.length() == 0 || name.indexOf('/') >= 0)
        {
            return null;
        }

        File dumpFile = new File(dumpLocation, name);
        if (dumpFile.isFile())
        {
            return dumpFile;
        }

        return null;
    }

    final File[] getDumpFiles()
    {
        return dumpLocation.listFiles();
    }

    final boolean rmDumpFile(final String name)
    {
        if (name == null || name.length() == 0)
        {
            return false;
        }

        final File dumpFile = new File(dumpLocation, name);
        if (!dumpFile.exists())
        {
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
     * @throws NoSuchElementException If no provided mechanism is successfully
     *             used to create a heap dump
     */
    final File dumpHeap(String name, final boolean live)
    {
        // ensure dumplocation exists
        dumpLocation.mkdirs();

        File dump = dumpSunMBean(name, live);
        if (dump == null)
        {
            dump = dumpIbmDump(name);
        }

        if (dump == null)
        {
            throw new NoSuchElementException();
        }

        return dump;
    }

    final MemoryMXBean getMemory()
    {
        return ManagementFactory.getMemoryMXBean();
    }

    final List<MemoryPoolMXBean> getMemoryPools()
    {
        return ManagementFactory.getMemoryPoolMXBeans();
    }

    public void handleNotification(Notification notification, Object handback)
    {
        String notifType = notification.getType();
        if (notifType.equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED))
        {
            log.warn("Received Memory Threshold Exceed Notification, dumping Heap");
            try
            {
                File file = dumpHeap(null, true);
                log.warn("Heap dumped to " + file);
            }
            catch (NoSuchElementException e)
            {
                log.error("Failed dumping the heap, JVM does not provide known mechanism to create a Heap Dump");
            }
        }
    }

    static interface PrintHelper
    {
        void title(final String title, final int level);

        void val(final String value);

        void keyVal(final String key, final Object value);
    }

    // ---------- Various System Specific Heap Dump mechanisms

    /**
     * @see http://blogs.sun.com/sundararajan/entry/
     *      programmatically_dumping_heap_from_java
     */
    private File dumpSunMBean(String name, boolean live)
    {
        if (name == null)
        {
            name = "heap." + System.currentTimeMillis() + ".hprof";
        }

        File tmpFile = new File(dumpLocation, name);
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        try
        {
            server.invoke(new ObjectName(HOTSPOT_BEAN_NAME), "dumpHeap", new Object[]
                { tmpFile.getAbsolutePath(), live }, new String[]
                { String.class.getName(), boolean.class.getName() });

            log.debug("dumpSunMBean: Dumped Heap to {} using Sun HotSpot MBean", tmpFile);
            return tmpFile;
        }
        catch (Throwable t)
        {
            log.debug("dumpSunMBean: Dump by Sun HotSpot MBean not working", t);
            tmpFile.delete();
        }

        return null;
    }

    /**
     * @param name
     * @return
     * @see http
     *      ://publib.boulder.ibm.com/infocenter/javasdk/v5r0/index.jsp?topic
     *      =/com.ibm.java.doc.diagnostics.50/diag/tools/heapdump_enable.html
     */
    private File dumpIbmDump(String name)
    {
        try
        {
            // to try to indicate which file will contain the heap dump
            long minFileTime = System.currentTimeMillis();

            // call the com.ibm.jvm.Dump.HeapDump() method
            Class<?> c = ClassLoader.getSystemClassLoader().loadClass("com.ibm.jvm.Dump");
            Method m = c.getDeclaredMethod("HeapDump", (Class<?>[]) null);
            m.invoke(null, (Object[]) null);

            // find the file in the current working directory
            File dir = new File("").getAbsoluteFile();
            File[] files = dir.listFiles();
            if (files != null)
            {
                for (File file : files)
                {
                    if (file.isFile() && file.lastModified() > minFileTime)
                    {
                        if (name == null)
                        {
                            name = file.getName();
                        }
                        File target = new File(dumpLocation, name);
                        file.renameTo(target);

                        log.debug("dumpSunMBean: Dumped Heap to {} using IBM Dump.HeapDump()", target);
                        return target;
                    }
                }

                log.debug("dumpIbmDump: None of {} files '{}' is younger than {}", new Object[]
                    { files.length, dir, minFileTime });
            }
            else
            {
                log.debug("dumpIbmDump: Hmm '{}' does not seem to be a directory; isdir={} ??", dir, dir.isDirectory());
            }

            log.warn("dumpIbmDump: Heap Dump has been created but cannot be located");
            return dumpLocation;
        }
        catch (Throwable t)
        {
            log.debug("dumpIbmDump: Dump by IBM Dump class not working", t);
        }

        return null;
    }
}
