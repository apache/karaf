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
package org.apache.karaf.shell.commands.impl;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.karaf.shell.commands.info.InfoProvider;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@Command(scope = "shell", name = "info", description = "Prints system information.")
@Service
public class InfoAction implements Action {

    private NumberFormat fmtI = new DecimalFormat("###,###", new DecimalFormatSymbols(Locale.ENGLISH));
    private NumberFormat fmtDec = new DecimalFormat("###,###.##", new DecimalFormatSymbols(Locale.ENGLISH));
    private NumberFormat fmtD = new DecimalFormat("###,##0.000", new DecimalFormatSymbols(Locale.ENGLISH));

    private OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

    @Option(name="--memory-pools", aliases= {"-mp"}, description="Includes detailed information about memory pools")
    protected boolean showMemoryPools;

//    @Reference
    List<InfoProvider> infoProviders;

    public InfoAction() {
        fmtDec.setMinimumFractionDigits(2);
    }

    @Override
    public Object execute() throws Exception {
        int maxNameLen;

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();

        //
        // print Karaf informations
        //
        maxNameLen = 25;
        System.out.println("Karaf");
        printValue("Karaf version", maxNameLen, System.getProperty("karaf.version"));
        printValue("Karaf home", maxNameLen, System.getProperty("karaf.home"));
        printValue("Karaf base", maxNameLen, System.getProperty("karaf.base"));
        String osgi = getOsgiFramework();
        if (osgi != null) {
            printValue("OSGi Framework", maxNameLen, osgi);
        }
        System.out.println();

        System.out.println("JVM");
        printValue("Java Virtual Machine", maxNameLen, runtime.getVmName() + " version " + runtime.getVmVersion());
        printValue("Version", maxNameLen, System.getProperty("java.version"));
        printValue("Vendor", maxNameLen, runtime.getVmVendor());
        printValue("Pid", maxNameLen, getPid());
        printValue("Uptime", maxNameLen, printDuration(runtime.getUptime()));
        try {
            Class< ? > sunOS = Class.forName("com.sun.management.OperatingSystemMXBean");
            printValue("Process CPU time", maxNameLen, printDuration(getValueAsLong(sunOS, "getProcessCpuTime") / 1000000));
            printValue("Process CPU load", maxNameLen, fmtDec.format(getValueAsDouble(sunOS, "getProcessCpuLoad")));
            printValue("System CPU load", maxNameLen, fmtDec.format(getValueAsDouble(sunOS, "getSystemCpuLoad")));
        } catch (Throwable t) {
        }
        try {
            Class<?> unixOS = Class.forName("com.sun.management.UnixOperatingSystemMXBean");
            printValue("Open file descriptors", maxNameLen, printLong(getValueAsLong(unixOS, "getOpenFileDescriptorCount")));
            printValue("Max file descriptors", maxNameLen, printLong(getValueAsLong(unixOS, "getMaxFileDescriptorCount")));
        } catch (Throwable t) {
        }
        printValue("Total compile time", maxNameLen, printDuration(ManagementFactory.getCompilationMXBean().getTotalCompilationTime()));

        System.out.println("Threads");
        printValue("Live threads", maxNameLen, Integer.toString(threads.getThreadCount()));
        printValue("Daemon threads", maxNameLen, Integer.toString(threads.getDaemonThreadCount()));
        printValue("Peak", maxNameLen, Integer.toString(threads.getPeakThreadCount()));
        printValue("Total started", maxNameLen, Long.toString(threads.getTotalStartedThreadCount()));

        System.out.println("Memory");
        printValue("Current heap size", maxNameLen, printSizeInKb(mem.getHeapMemoryUsage().getUsed()));
        printValue("Maximum heap size", maxNameLen, printSizeInKb(mem.getHeapMemoryUsage().getMax()));
        printValue("Committed heap size", maxNameLen, printSizeInKb(mem.getHeapMemoryUsage().getCommitted()));
        printValue("Pending objects", maxNameLen, Integer.toString(mem.getObjectPendingFinalizationCount()));
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            String val = "Name = '" + gc.getName() + "', Collections = " + gc.getCollectionCount() + ", Time = " + printDuration(gc.getCollectionTime());
            printValue("Garbage collector", maxNameLen, val);
        }

        if (showMemoryPools) {
            List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
            System.out.println("Memory Pools");
            printValue("Total Memory Pools", maxNameLen, printLong(memoryPools.size()));
            String spaces4 = "   ";
            for (MemoryPoolMXBean pool : memoryPools)
            {
                String name = pool.getName();
                MemoryType type = pool.getType();
                printValue(spaces4 + "Pool (" + type + ")", maxNameLen, name);

                // PeakUsage/CurrentUsage
                MemoryUsage peakUsage = pool.getPeakUsage();
                MemoryUsage usage = pool.getUsage();

                if (usage != null && peakUsage != null) {
                    long init = peakUsage.getInit();
                    long used = peakUsage.getUsed();
                    long committed = peakUsage.getCommitted();
                    long max = peakUsage.getMax();
                    System.out.println(spaces4 + spaces4 + "Peak Usage");
                    printValue(spaces4 + spaces4 + spaces4 + "init", maxNameLen, printLong(init));
                    printValue(spaces4 + spaces4 + spaces4 + "used", maxNameLen, printLong(used));
                    printValue(spaces4 + spaces4 + spaces4 + "committed", maxNameLen, printLong(committed));
                    printValue(spaces4 + spaces4 + spaces4 + "max", maxNameLen, printLong(max));

                    init = usage.getInit();
                    used = usage.getUsed();
                    committed = usage.getCommitted();
                    max = usage.getMax();
                    System.out.println(spaces4 + spaces4 + "Current Usage");
                    printValue(spaces4 + spaces4 + spaces4 + "init", maxNameLen, printLong(init));
                    printValue(spaces4 + spaces4 + spaces4 + "used", maxNameLen, printLong(used));
                    printValue(spaces4 + spaces4 + spaces4 + "committed", maxNameLen, printLong(committed));
                    printValue(spaces4 + spaces4 + spaces4 + "max", maxNameLen, printLong(max));
                }
            }
        }

        System.out.println("Classes");
        printValue("Current classes loaded", maxNameLen, printLong(cl.getLoadedClassCount()));
        printValue("Total classes loaded", maxNameLen, printLong(cl.getTotalLoadedClassCount()));
        printValue("Total classes unloaded", maxNameLen, printLong(cl.getUnloadedClassCount()));

        System.out.println("Operating system");
        printValue("Name", maxNameLen, os.getName() + " version " + os.getVersion());
        printValue("Architecture", maxNameLen, os.getArch());
        printValue("Processors", maxNameLen, Integer.toString(os.getAvailableProcessors()));
        try {
            printValue("Total physical memory", maxNameLen, printSizeInKb(getSunOsValueAsLong(os, "getTotalPhysicalMemorySize")));
            printValue("Free physical memory", maxNameLen, printSizeInKb(getSunOsValueAsLong(os, "getFreePhysicalMemorySize")));
            printValue("Committed virtual memory", maxNameLen, printSizeInKb(getSunOsValueAsLong(os, "getCommittedVirtualMemorySize")));
            printValue("Total swap space", maxNameLen, printSizeInKb(getSunOsValueAsLong(os, "getTotalSwapSpaceSize")));
            printValue("Free swap space", maxNameLen, printSizeInKb(getSunOsValueAsLong(os, "getFreeSwapSpaceSize")));
        } catch (Throwable t) {
        }

        //Display Information from external information providers.
        Map<String, Map<Object, Object>> properties = new HashMap<>();
        if (infoProviders != null) {
            // dump all properties to Map, KARAF-425
            for (InfoProvider provider : infoProviders) {
                if (!properties.containsKey(provider.getName())) {
                    properties.put(provider.getName(), new Properties());
                }
                properties.get(provider.getName()).putAll(provider.getProperties());
            }

            List<String> sections = new ArrayList<>(properties.keySet());
            Collections.sort(sections);
            for (String section : sections) {
                List<Object> keys = new ArrayList<>(properties.get(section).keySet());
                if (keys.size() > 0) {
                    System.out.println(section);

                    keys.sort(Comparator.comparing(String::valueOf));

                    for (Object key : keys) {
                        printValue(String.valueOf(key), maxNameLen, String.valueOf(properties.get(section).get(key)));
                    }
                }
            }
        }
        return null;
    }

    private String getPid() {
        // In Java 9 the new process API can be used:
        // long pid = ProcessHandle.current().getPid();
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String[] parts = name.split("@");
        return parts[0];
    }

    private long getSunOsValueAsLong(OperatingSystemMXBean os, String name) throws Exception {
        Method mth = os.getClass().getMethod(name);
        return (Long) mth.invoke(os);
    }

    private long getValueAsLong(Class<?> osImpl, String name) throws Exception {
        if (osImpl.isInstance(os))
        {
            Method mth = osImpl.getMethod(name);
            return (Long) mth.invoke(os);
        }
        return -1;
    }

    private double getValueAsDouble(Class<?> osImpl, String name) throws Exception {
        if (osImpl.isInstance(os)) {
            Method mth = osImpl.getMethod(name);
            return (Double) mth.invoke(os);
        }
        return -1;
    }

    private String printLong(long i) {
        return fmtI.format(i);
    }

    private String printSizeInKb(double size) {
        return fmtI.format((long) (size / 1024)) + " kbytes";
    }

    protected String printDuration(double uptime) {
        uptime /= 1000;
        if (uptime < 60) {
            return fmtD.format(uptime) + " seconds";
        }
        uptime /= 60;
        if (uptime < 60) {
            long minutes = (long) uptime;
            String s = fmtI.format(minutes) + (minutes > 1 ? " minutes" : " minute");
            return s;
        }
        uptime /= 60;
        if (uptime < 24) {
            long hours = (long) uptime;
            long minutes = (long) ((uptime - hours) * 60);
            String s = fmtI.format(hours) + (hours > 1 ? " hours" : " hour");
            if (minutes != 0) {
                s += " " + fmtI.format(minutes) + (minutes > 1 ? " minutes" : " minute");
            }
            return s;
        }
        uptime /= 24;
        long days = (long) uptime;
        long hours = (long) ((uptime - days) * 24);
        String s = fmtI.format(days) + (days > 1 ? " days" : " day");
        if (hours != 0) {
            s += " " + fmtI.format(hours) + (hours > 1 ? " hours" : " hour");
        }
        return s;
    }

    void printSysValue(String prop, int pad) {
        printValue(prop, pad, System.getProperty(prop));
    }

    void printValue(String name, int pad, String value) {
        System.out.println(
                "  " + SimpleAnsi.INTENSITY_BOLD + name + SimpleAnsi.INTENSITY_NORMAL
                        + spaces(pad - name.length()) + "   " + value);
    }

    String spaces(int nb) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nb; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    String getOsgiFramework() {
        try {
            Callable<String> call = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
                    Bundle sysBundle = context.getBundle(0);
                    return sysBundle.getSymbolicName() + "-" + sysBundle.getVersion();
                }
            };
            return call.call();
        } catch (Throwable t) {
            // We're not in OSGi, just safely return null
            return null;
        }
    }

}
