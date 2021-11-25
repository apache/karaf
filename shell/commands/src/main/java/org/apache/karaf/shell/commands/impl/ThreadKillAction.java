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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "shell", name = "kill", description = "Interrupt a given thread")
@Service
public class ThreadKillAction implements Action {

    @Argument(name = "id", description = "Interrupt the thread with this id", required = true, multiValued = false)
    Long id;

    @Override
    public Object execute() throws Exception {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (threadGroup.getParent() != null) {
            threadGroup = threadGroup.getParent();
        }

        Thread[] threadList = new Thread[threadGroup.activeCount()];

        int count = threadGroup.enumerate(threadList, true);
        for (int i = 0; i < count; i++) {
            if (threadList[i].getId() == id) {
                try {
                    System.out.println("Interrupting " + threadList[i].getName() + " (" + threadList[i].getId() + ")");
                    threadList[i].interrupt();
                } catch (Exception e) {
                    System.err.println("Can't interrtupt: " + e.getMessage());
                }
                return null;
            }
        }

        System.err.println("kill " + id + " failed: no such thread");

        return null;
    }

}
