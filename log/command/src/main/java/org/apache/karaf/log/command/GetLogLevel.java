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
package org.apache.karaf.log.command;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.table.ShellTable;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * Get the log level
 */
@Command(scope = "log", name = "get", description = "Shows the currently set log level.")
public class GetLogLevel extends LogCommandSupport {

    @Argument(index = 0, name = "logger", description = "The name of the logger, ALL or ROOT (default)", required = false, multiValued = false)
    String logger;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    protected Object doExecute() throws Exception {
        Map<String, String> loggers = logService.getLevel(logger);

        ShellTable table = new ShellTable();
        table.column("Logger");
        table.column("Level");

        for (String logger : loggers.keySet()) {
            table.addRow().addContent(logger, loggers.get(logger));
        }

        table.print(System.out, !noFormat);

        return null;
    }

}
