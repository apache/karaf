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
import org.apache.karaf.shell.api.console.Session;
import org.jline.builtins.TTop;
import org.jline.terminal.Terminal;

import java.util.Arrays;


/**
 * Display threads infos in a table.
 */
@Command(scope = "shell", name = "ttop", description = "Display threads information")
@Service
public class TTopAction implements Action {

    @Option(name = "--order" , aliases = { "-o" }, description = "Comma separated list of sorting keys")
    String order;

    @Option(name = "--stats" , aliases = { "-t" }, description = "Comma separated list of stats to display")
    String stats;

    @Option(name = "--seconds" , aliases = { "-s" }, description = "Delay between updates in seconds")
    Integer seconds;

    @Option(name = "--millis" , aliases = { "-m" }, description = "Delay between updates in milliseconds")
    Integer millis;

    @Option(name = "--nthreads", aliases = { "-n" }, description = "Only display up to NTHREADS threads")
    int nthreads = -1;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        TTop ttop = new TTop((Terminal) session.get(".jline.terminal"));
        ttop.sort = order != null ? Arrays.asList(order.split(",")) : null;
        ttop.delay = seconds != null ? seconds * 1000 : ttop.delay;
        ttop.delay = millis != null ? millis : ttop.delay;
        ttop.stats = stats != null ? Arrays.asList(stats.split(",")) : null;
        ttop.nthreads = nthreads;
        ttop.run();
        return null;
    }

}
