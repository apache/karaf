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
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Sets a level in the Karaf logging service configuration, which applies it right away.
 */
@Command(scope = "log", name = "set", description = "Sets the log level.")
@Service
public class SetLogLevel implements Action {

    @Argument(index = 0, name = "level", description = "The log level to set (OFF, TRACE, DEBUG, INFO, WARN, ERROR, FATAL) or DEFAULT to unset", required = true, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "OFF", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "DEFAULT" })
    String level;

    @Argument(index = 1, name = "logger", description = "Logger name or ROOT (default)", required = false, multiValued = false)
    String logger;

    @Reference
    ConfigurationAdmin configAdmin;

    @Override
    public Object execute() throws Exception {
        LoggerLevels.setLevel(configAdmin, logger, level);
        return null;
    }

}
