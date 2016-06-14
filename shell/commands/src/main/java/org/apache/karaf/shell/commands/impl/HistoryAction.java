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
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;

/**
 * History command
 */
@Command(scope = "shell", name="history", description="Prints command history.")
@Service
public class HistoryAction implements Action {

    @Option(name = "-c", aliases = { "--clear" }, description = "Clears the shell command history.", required = false, multiValued = false)
    private boolean clear;

    @Reference
    History history;

    @Override
    public Object execute() throws Exception {
        if (history != null) {
            if (clear) {
                history.clear();
            } else {
                for (int index = history.first(); index <= history.last(); index++) {
                    System.out.println(
                            "  " + SimpleAnsi.INTENSITY_BOLD + String.format("%3d", index) + SimpleAnsi.INTENSITY_NORMAL
                                    + "  " + history.get(index));
                }
            }
        }
        return null;
    }

}
