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
package org.apache.karaf.services.logging.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Shows the levels of the Karaf logging service configuration.
 */
@Command(scope = "log", name = "get", description = "Shows the currently set log level.")
@Service
public class GetLogLevel implements Action {

    @Argument(index = 0, name = "logger", description = "The name of the logger, ROOT, or ALL (default)", required = false, multiValued = false)
    String logger;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Reference
    ConfigurationAdmin configAdmin;

    @Override
    public Object execute() throws Exception {
        if (logger == null || LoggerLevels.ALL_LOGGER.equalsIgnoreCase(logger)) {
            ShellTable table = new ShellTable();
            table.column("Logger");
            table.column("Level");
            LoggerLevels.getLevels(configAdmin).forEach((name, level) -> table.addRow().addContent(name, level));
            table.print(System.out, !noFormat);
        } else {
            String name = LoggerLevels.ROOT_LOGGER.equalsIgnoreCase(logger) ? LoggerLevels.ROOT_LOGGER : logger;
            System.out.println(LoggerLevels.getEffectiveLevel(configAdmin, name));
        }
        return null;
    }

}
