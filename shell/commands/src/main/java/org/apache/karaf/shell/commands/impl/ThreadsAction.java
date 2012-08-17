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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;
import org.apache.karaf.shell.table.ShellTable;

/**
 * Command for showing the full tree of bundles that have been used to resolve
 * a given bundle.
 */
@Command(scope = "shell", name = "threads", description = "Prints the current threads (optionally with stacktraces)")
public class ThreadsAction extends AbstractAction {

    @Argument(name = "id", description="Show details for thread with this Id", required = false, multiValued = false)
    Long id;

    protected Object doExecute() throws Exception {
        ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
        
        if (id != null) {
            printThread(threadsBean, id);
        } else {
            printThreadList(threadsBean);
        }
        return null;
    }

    void printThread(ThreadMXBean threadsBean, Long id) {
        threadsBean.setThreadCpuTimeEnabled(true);
        ThreadInfo ti = threadsBean.getThreadInfo(id, Integer.MAX_VALUE);
        System.out.println("Thread " + ti.getThreadId() + " " + ti.getThreadName() + " " + ti.getThreadState());
        System.out.println("Stacktrace:");
        StackTraceElement[] st = ti.getStackTrace();
        for (StackTraceElement ste : st) {
            System.out.println(ste.getClassName() + "." + ste.getMethodName() + " line: " + ste.getLineNumber());
        }
    }

    void printThreadList(ThreadMXBean threadsBean) {
        ThreadInfo[] threadInfoAr = threadsBean.dumpAllThreads(false, false);
        ShellTable table = new ShellTable();
        table.column("Id");
        table.column("Name");
        table.column("State");
        table.column("CPU time");
        table.column("User time");
        for (ThreadInfo ti : threadInfoAr) {
            long id = ti.getThreadId();
            table.addRow().addContent(id, ti.getThreadName(), ti.getThreadState(), threadsBean.getThreadCpuTime(id) / 1000000, threadsBean.getThreadUserTime(id) / 1000000);
        }
        table.print(System.out);
    }

}
