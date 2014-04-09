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
package org.apache.karaf.diagnostic.core.internal;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.karaf.diagnostic.core.common.TextDumpProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * Provider which dumps runtime environment information to file named environment.txt.
 */
public class EnvironmentDumpProvider extends TextDumpProvider {
    
    private static final String KEY_VALUE_FORMAT = "%1$s\t: %2$s";
    private static final String INDENT_KEY_VALUE_FORMAT = "    "+KEY_VALUE_FORMAT;
    private final BundleContext bundleContext;

    /**
     * Creates new dump entry which contains information about the runtime environment.
     */
    public EnvironmentDumpProvider(final BundleContext context) {
        super("environment.txt");
        this.bundleContext = context;
    }

    @Override
    protected void writeDump(final OutputStreamWriter outputStream) throws Exception {
    if( null == outputStream) {
        return;
    }
    final PrintWriter outPW = new PrintWriter(outputStream);
    // current date/time
    final DateFormat dateTimeFormatInstance = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH);
    outPW.printf(KEY_VALUE_FORMAT,"Dump timestamp", dateTimeFormatInstance.format(new Date(System.currentTimeMillis()))).println();
    outPW.println();
    // karaf information
    dumpKarafInformation(outPW);
    outPW.println();
    // OSGi information
    dumpOSGiInformation(outPW);
    outPW.println();
    // OS information
    dumpOSInformation(outPW);
    outPW.println();
    // general information about JVM
    dumpVMInformation(outPW, dateTimeFormatInstance);
    outPW.println();
    // threads
    dumpThreadsInformation(outPW);
    outPW.println();
    // classes
    dumpClassesInformation(outPW);
    outPW.println();
    // memory
    dumpMemoryInformation(outPW);
    outPW.println();
    // garbage collector
    dumpGCInformation(outPW);
    }

    private void dumpKarafInformation(final PrintWriter outPW) {
        outPW.printf(KEY_VALUE_FORMAT, "Karaf", System.getProperty("karaf.name", "root") + ' ' + System.getProperty("karaf.version", "")).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "home", System.getProperty("karaf.home", "")).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "base", System.getProperty("karaf.base", "")).println();
    }

    private void dumpOSGiInformation(final PrintWriter outPW) {
        if( null == bundleContext ) {
            return;
        }
        outPW.println("OSGi:");
        final Bundle[] bundles = bundleContext.getBundles();
        for (final Bundle bundle : bundles) {
            if( null == bundle || !!!"osgi.core".equals(bundle.getSymbolicName())) {
                continue;
            }
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "version", bundle.getVersion()).println();
            break;
        }
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "framework", bundleContext.getBundle(0).getSymbolicName() + " - " +
                bundleContext.getBundle(0).getVersion()).println();
    }

    private void dumpOSInformation(final PrintWriter outPW) {
        final OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
        if( null == mxBean) {
            return;
        }
        outPW.printf(KEY_VALUE_FORMAT, "Operating System", mxBean.getName() + ' ' + mxBean.getVersion()).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "architecture", mxBean.getArch()).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "processors", mxBean.getAvailableProcessors()).println();
//        outPW.printf(INDENT_KEY_VALUE_FORMAT, "current system load average", mxBean.getSystemLoadAverage()).println();
    }

    private void dumpVMInformation(final PrintWriter outPW,
        final DateFormat dateTimeFormatInstance) {
        final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        if( mxBean == null ) {
            return;
        }
        outPW.printf(KEY_VALUE_FORMAT,"Instance name", mxBean.getName()).println();
        outPW.printf(KEY_VALUE_FORMAT,"Start time", dateTimeFormatInstance.format(new Date(mxBean.getStartTime()))).println();
        outPW.printf(KEY_VALUE_FORMAT,"Uptime", printDuration(mxBean.getUptime())).println();
        outPW.println();
        outPW.printf(KEY_VALUE_FORMAT, "Java VM", mxBean.getVmName() + " " + mxBean.getVmVersion()).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "vendor", mxBean.getVmVendor()).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "version", System.getProperty("java.version")).println();
        outPW.println();
        outPW.println("Input arguments:");
        final List<String> inputArguments = mxBean.getInputArguments();
        for (final String argument : inputArguments) {
            if( argument != null && argument.contains("=")) {
                final String[] split = argument.split("=");
                outPW.printf(INDENT_KEY_VALUE_FORMAT, split[0], split[1]).println();
            } else {
                outPW.printf(INDENT_KEY_VALUE_FORMAT, argument,"").println();
            }
        }
        outPW.println("Classpath:");
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "boot classpath", mxBean.getBootClassPath()).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "library path", mxBean.getLibraryPath()).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "classpath", mxBean.getClassPath()).println();
        outPW.println("System properties:");
        final Map<String, String> systemProperties = mxBean.getSystemProperties();
        for (final Entry<String, String> property : systemProperties.entrySet()) {
            outPW.printf(INDENT_KEY_VALUE_FORMAT, property.getKey(), property.getValue()).println();
        }
        outPW.println();
        // JIT information
        final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
        if( compilationMXBean != null ) {
            outPW.printf(KEY_VALUE_FORMAT, "JIT compiler", compilationMXBean.getName()).println();
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "total compile time", printDuration(compilationMXBean.getTotalCompilationTime())).println();
        }
    }

    private void dumpThreadsInformation(final PrintWriter outPW) {
        final ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        if( null == mxBean) {
            return;
        }
        outPW.println("Threads:");
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "live", formatLong(mxBean.getThreadCount())).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "daemon", formatLong(mxBean.getDaemonThreadCount())).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "peak", formatLong(mxBean.getPeakThreadCount())).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "total", formatLong(mxBean.getTotalStartedThreadCount())).println();
    }

    private void dumpClassesInformation(final PrintWriter outPW) {
        final ClassLoadingMXBean mxBean = ManagementFactory.getClassLoadingMXBean();
        if( null == mxBean) {
            return;
        }
        outPW.println("Classes:");
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "loaded", formatLong(mxBean.getLoadedClassCount())).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "total", formatLong(mxBean.getTotalLoadedClassCount())).println();
        outPW.printf(INDENT_KEY_VALUE_FORMAT, "unloaded", formatLong(mxBean.getUnloadedClassCount())).println();
    }

    private void dumpMemoryInformation(final PrintWriter outPW) {
        final MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();
        if( null == mxBean) {
            return;
        }
        final MemoryUsage heapMemoryUsage = mxBean.getHeapMemoryUsage();
        final MemoryUsage nonHeapMemoryUsage = mxBean.getNonHeapMemoryUsage();
        if( heapMemoryUsage != null ) {
            outPW.println("HEAP Memory:");
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "commited", printMemory(heapMemoryUsage.getCommitted())).println();
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "init", printMemory(heapMemoryUsage.getInit())).println();
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "used", printMemory(heapMemoryUsage.getUsed())).println();
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "maximal", printMemory(heapMemoryUsage.getMax())).println();
        }
        if( nonHeapMemoryUsage != null ) {
            outPW.println("NON-HEAP Memory:");
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "commited", printMemory(nonHeapMemoryUsage.getCommitted())).println();
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "init", printMemory(nonHeapMemoryUsage.getInit())).println();
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "used", printMemory(nonHeapMemoryUsage.getUsed())).println();
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "maximal", printMemory(nonHeapMemoryUsage.getMax())).println();
        }
    }

    private void dumpGCInformation(final PrintWriter outPW) {
        final List<GarbageCollectorMXBean> mxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        if( null == mxBeans || mxBeans.isEmpty()) {
            return;
        }
        final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        if( memoryMxBean != null ) {
            outPW.printf(INDENT_KEY_VALUE_FORMAT, "pending objects", formatLong(memoryMxBean.getObjectPendingFinalizationCount())).println();
        }
        final String gcFormat ="'%1$s' collections: %2$s\ttime: %3$s";
        outPW.println();
        for (final GarbageCollectorMXBean mxBean : mxBeans) {
            if( null == mxBean) {
                continue;
            }
            outPW.printf(KEY_VALUE_FORMAT, "Garbage Collectors", String.format(gcFormat, mxBean.getName(), formatLong(mxBean.getCollectionCount()), printDuration(mxBean.getCollectionTime()))).println();
        }
    }


    private String formatLong(final long longValue) {
        final NumberFormat fmtI = new DecimalFormat("###,###", new DecimalFormatSymbols(Locale.ENGLISH));
        return fmtI.format(longValue);
    }

    private String printMemory(final long bytes) {
        if( bytes <= 1024) {
            return formatLong(bytes)+" bytes";
        }
        return formatLong(bytes/1024)+" kbytes";
    }

    /**
     * Prints the duration in a human readable format as X days Y hours Z minutes etc.
     *
     * @param uptime the uptime in millis
     * @return the time used for displaying on screen or in logs
     */
    private String printDuration(double uptime) {
        // Code based on code taken from Karaf
        // https://svn.apache.org/repos/asf/karaf/trunk/shell/commands/src/main/java/org/apache/karaf/shell/commands/impl/InfoAction.java

        uptime /= 1000;
        if (uptime < 60) {
            final NumberFormat fmtD = new DecimalFormat("###,##0.000", new DecimalFormatSymbols(Locale.ENGLISH));
            return fmtD.format(uptime) + " seconds";
        }
        uptime /= 60;
        if (uptime < 60) {
            final long minutes = (long) uptime;
            final String s = formatLong(minutes) + (minutes > 1 ? " minutes" : " minute");
            return s;
        }
        uptime /= 60;
        if (uptime < 24) {
            final long hours = (long) uptime;
            final long minutes = (long) ((uptime - hours) * 60);
            String s = formatLong(hours) + (hours > 1 ? " hours" : " hour");
            if (minutes != 0) {
                s += " " + formatLong(minutes) + (minutes > 1 ? " minutes" : " minute");
            }
            return s;
        }
        uptime /= 24;
        final long days = (long) uptime;
        final long hours = (long) ((uptime - days) * 24);
        String s = formatLong(days) + (days > 1 ? " days" : " day");
        if (hours != 0) {
            s += " " + formatLong(hours) + (hours > 1 ? " hours" : " hour");
        }
        return s;
    }

}
