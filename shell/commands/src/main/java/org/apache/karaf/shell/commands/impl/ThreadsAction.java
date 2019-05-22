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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

/**
 * Command for showing the full tree of bundles that have been used to resolve
 * a given bundle.
 */
@Command(scope = "shell", name = "threads", description = "Prints the current threads (optionally with stacktraces)")
@Service
public class ThreadsAction implements Action {

    @Option(name = "--tree" , description = "Display threads as a tree")
    boolean tree = false;

    @Option(name = "--list" , description = "Display threads as a list")
    boolean list = false;

    @Option(name = "-e", aliases = { "--empty-groups" }, description = "Show empty groups")
    boolean empty = false;

    @Option(name = "-t", aliases = { "--threshold" }, description = "Minimal number of interesting stack trace line to display a thread")
    int threshold = 1;

    @Option(name = "--locks", description = "Display locks")
    boolean locks = false;

    @Option(name = "--monitors", description = "Display monitors")
    boolean monitors = false;

    @Option(name = "--packages", description = "Pruned packages")
    List<String> packages = Arrays.asList("java.", "sun.");

    @Argument(name = "id", description="Show details for thread with this Id", required = false, multiValued = false)
    Long id;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Override
    public Object execute() throws Exception {
        Map<Long, ThreadInfo> threadInfos = new TreeMap<>();
        ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos;
        if (threadsBean.isObjectMonitorUsageSupported() && threadsBean.isSynchronizerUsageSupported()) {
            infos = threadsBean.dumpAllThreads(true, true);
        } else {
            infos = threadsBean.getThreadInfo(threadsBean.getAllThreadIds(), Integer.MAX_VALUE);
        }
        for (ThreadInfo info : infos) {
            threadInfos.put(info.getThreadId(), info);
        }

        if (id != null) {
            ThreadInfo ti = threadInfos.get(id);
            if (ti != null) {
                System.out.println("Thread " + ti.getThreadId() + " " + ti.getThreadName() + " " + ti.getThreadState());
                System.out.println("Stacktrace:");
                StackTraceElement[] st = ti.getStackTrace();
                for (StackTraceElement ste : st) {
                    System.out.println(ste.getClassName() + "." + ste.getMethodName() + " line: " + ste.getLineNumber());
                }
            }
        } else if (list) {
            ShellTable table = new ShellTable();
            table.column("Id");
            table.column("Name");
            table.column("State");
            table.column("CPU time");
            table.column("Usr time");
            for (ThreadInfo thread : threadInfos.values()) {
                long id = thread.getThreadId();
                table.addRow().addContent(
                        id,
                        thread.getThreadName(),
                        thread.getThreadState(),
                        threadsBean.getThreadCpuTime(id) / 1000000,
                        threadsBean.getThreadUserTime(id) / 1000000);
            }
            table.print(System.out, !noFormat);
        } else {
            ThreadGroup group = Thread.currentThread().getThreadGroup();
            while (group.getParent() != null) {
                group = group.getParent();
            }
            ThreadGroupData data = new ThreadGroupData(group, threadInfos);
            data.print();
        }
        return null;
    }

    public class ThreadGroupData {
        private final ThreadGroup group;
        private final List<ThreadGroupData> groups;
        private final List<ThreadData> threads;

        public ThreadGroupData(ThreadGroup group, Map<Long, ThreadInfo> infos) {
            this.group = group;
            int nbGroups;
            int nbThreads;
            ThreadGroup[] childGroups = new ThreadGroup[32];
            while (true) {
                nbGroups = group.enumerate(childGroups, false);
                if (nbGroups == childGroups.length) {
                    childGroups = new ThreadGroup[childGroups.length * 2];
                } else {
                    break;
                }
            }
            groups = new ArrayList<>();
            for (ThreadGroup tg : childGroups) {
                if (tg != null) {
                    groups.add(new ThreadGroupData(tg, infos));
                }
            }
            Thread[] childThreads = new Thread[32];
            while (true) {
                nbThreads = group.enumerate(childThreads, false);
                if (nbThreads == childThreads.length) {
                    childThreads = new Thread[childThreads.length * 2];
                } else {
                    break;
                }
            }
            threads = new ArrayList<>();
            for (Thread t : childThreads) {
                if (t != null) {
                    threads.add(new ThreadData(t, infos.get(t.getId())));
                }
            }
        }

        public void print() {
            if (tree) {
                printTree("");
            } else {
                printDump("");
            }
        }

        private void printTree(String indent) {
            if (empty || hasInterestingThreads()) {
                System.out.println(indent + "Thread Group \"" + group.getName() + "\"");
                for (ThreadGroupData tgd : groups) {
                    tgd.printTree(indent + "    ");
                }
                for (ThreadData td : threads) {
                    if (td.isInteresting()) {
                        td.printTree(indent + "    ");
                    }
                }
            }
        }

        private void printDump(String indent) {
            if (empty || hasInterestingThreads()) {
                for (ThreadGroupData tgd : groups) {
                    tgd.printDump(indent);
                }
                for (ThreadData td : threads) {
                    if (td.isInteresting()) {
                        td.printDump(indent);
                    }
                }
            }
        }

        public boolean hasInterestingThreads() {
            for (ThreadData td : threads) {
                if (td.isInteresting()) {
                    return true;
                }
            }
            for (ThreadGroupData tgd : groups) {
                if (tgd.hasInterestingThreads()) {
                    return true;
                }
            }
            return false;
        }
    }

    public class ThreadData {
        private final Thread thread;
        private ThreadInfo info;

        public ThreadData(Thread thread, ThreadInfo info) {
            this.thread = thread;
            this.info = info;
        }

        public void printTree(String indent) {
            System.out.println(indent + "    " + "\"" + thread.getName() + "\": " + thread.getState());
        }

        public void printDump(String indent) {
            if (info != null && isInteresting()) {
                printThreadInfo("    ");
                if (locks) {
                    printLockInfo("    ");
                }
                if (monitors) {
                    printMonitorInfo("    ");
                }
            }
        }

        public boolean isInteresting() {
            int nb = 0;
            if (info != null && info.getStackTrace() != null) {
                StackTraceElement[] stacktrace = info.getStackTrace();
                for (StackTraceElement ste : stacktrace) {
                    boolean interestingLine = true;
                    for (String pkg : packages) {
                        if (ste.getClassName().startsWith(pkg)) {
                            interestingLine = false;
                            break;
                        }
                    }
                    if (interestingLine) {
                        nb++;
                    }
                }
            }
            return nb >= threshold;
        }

        private void printThreadInfo(String indent) {
            // print thread information
            printThread(indent);

            // print stack trace with locks
            StackTraceElement[] stacktrace = info.getStackTrace();
            MonitorInfo[] monitors = info.getLockedMonitors();
            for (int i = 0; i < stacktrace.length; i++) {
                StackTraceElement ste = stacktrace[i];
                System.out.println(indent + "at " + ste.toString());
                for (MonitorInfo mi : monitors) {
                    if (mi.getLockedStackDepth() == i) {
                        System.out.println(indent + "  - locked " + mi);
                    }
                }
            }
            System.out.println();
        }

        private void printThread(String indent) {
            StringBuilder sb = new StringBuilder("\"" + info.getThreadName() + "\"" + " Id="
                    + info.getThreadId() + " in " + info.getThreadState());
            if (info.getLockName() != null) {
                sb.append(" on lock=").append(info.getLockName());
            }
            if (info.isSuspended()) {
                sb.append(" (suspended)");
            }
            if (info.isInNative()) {
                sb.append(" (running in native)");
            }
            System.out.println(sb.toString());
            if (info.getLockOwnerName() != null) {
                System.out.println(indent + " owned by " + info.getLockOwnerName() + " Id="
                        + info.getLockOwnerId());
            }
        }

        private void printMonitorInfo(String indent) {
            MonitorInfo[] monitors = info.getLockedMonitors();
            if (monitors != null && monitors.length > 0) {
                System.out.println(indent + "Locked monitors: count = " + monitors.length);
                for (MonitorInfo mi : monitors) {
                    System.out.println(indent + "  - " + mi + " locked at ");
                    System.out.println(indent + "      " + mi.getLockedStackDepth() + " "
                            + mi.getLockedStackFrame());
                }
                System.out.println();
            }
        }

        private void printLockInfo(String indent) {
            LockInfo[] locks = info.getLockedSynchronizers();
            if (locks != null && locks.length > 0) {
                System.out.println(indent + "Locked synchronizers: count = " + locks.length);
                for (LockInfo li : locks) {
                    System.out.println(indent + "  - " + li);
                }
                System.out.println();
            }
        }

    }

}
