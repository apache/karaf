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
package org.apache.karaf.shell.commands;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.commands.info.InfoProvider;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;

@Command(scope = "shell", name = "info", description = "Prints system information.")
public class InfoAction extends OsgiCommandSupport {

    private NumberFormat fmtI = new DecimalFormat("###,###", new DecimalFormatSymbols(Locale.ENGLISH));
    private NumberFormat fmtD = new DecimalFormat("###,##0.000", new DecimalFormatSymbols(Locale.ENGLISH));

    private List<InfoProvider> infoProviders = new LinkedList<InfoProvider>();

    protected Object doExecute() throws Exception {
        int maxNameLen;

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
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
        printValue("OSGi Framework", maxNameLen, bundleContext.getBundle(0).getSymbolicName() + " - " +
                bundleContext.getBundle(0).getVersion());
        System.out.println();

        System.out.println("JVM");
        printValue("Java Virtual Machine", maxNameLen, runtime.getVmName() + " version " + runtime.getVmVersion());
        printValue("Version", maxNameLen, System.getProperty("java.version"));
        printValue("Vendor", maxNameLen, runtime.getVmVendor());
        printValue("Uptime", maxNameLen, printDuration(runtime.getUptime()));
        try {
            printValue("Process CPU time", maxNameLen, printDuration(getSunOsValueAsLong(os, "getProcessCpuTime") / 1000000));
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
        Map<String, Map<Object, Object>> properties = new HashMap<String, Map<Object, Object>>();
        if (infoProviders != null) {

            // dump all properties to Map, KARAF-425
            for (InfoProvider provider : infoProviders) {
                if (!properties.containsKey(provider.getName())) {
                    properties.put(provider.getName(), new Properties());
                }
                properties.get(provider.getName()).putAll(provider.getProperties());
            }

            List<String> sections = new ArrayList<String>(properties.keySet());
            Collections.sort(sections);
            for (String section : sections) {
                List<Object> keys = new ArrayList<Object>(properties.get(section).keySet());
                if (keys.size() > 0) {
                    System.out.println(section);

                    Collections.sort(keys, new Comparator<Object>() {
                        public int compare(Object o1, Object o2) {
                            return String.valueOf(o1).compareTo(String.valueOf(o2));
                        }
                    });

                    for (Object key : keys) {
                        printValue(String.valueOf(key), maxNameLen, String.valueOf(properties.get(section).get(key)));
                    }
                }
            }
        }

        return null;
    }

    private long getSunOsValueAsLong(OperatingSystemMXBean os, String name) throws Exception {
        Method mth = os.getClass().getMethod(name);
        return (Long) mth.invoke(os);
    }

    private String printLong(long i) {
        return fmtI.format(i);
    }

    private String printSizeInKb(double size) {
        return fmtI.format((long) (size / 1024)) + " kbytes";
    }

    private String printDuration(double uptime) {
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
        System.out.println(Ansi.ansi().a("  ")
                .a(Ansi.Attribute.INTENSITY_BOLD).a(name).a(spaces(pad - name.length())).a(Ansi.Attribute.RESET)
                .a("   ").a(value).toString());
    }

    String spaces(int nb) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nb; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public List<InfoProvider> getInfoProviders() {
        return infoProviders;
    }

    public void setInfoProviders(List<InfoProvider> infoProviders) {
        this.infoProviders = infoProviders;
    }
}
