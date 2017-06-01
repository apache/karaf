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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.service.log.LogService;

import java.util.HashMap;
import java.util.Map;

@Command(scope = "log", name = "log", description = "Log a message.")
@Service
public class LogEntry implements Action {

    @Argument(index = 0, name = "message", description = "The message to log", required = true, multiValued = false)
    private String message;

    @Option(name = "--level", aliases = {"-l"}, description = "The level the message will be logged at", required = false, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "DEBUG", "INFO", "WARNING", "ERROR" })
    private String level = "INFO";

    @Reference
    LogService logService;

    private final Map<String,Integer> mappings = new HashMap<>();

    public LogEntry() {
        mappings.put("ERROR", 1);
        mappings.put("WARNING", 2);
        mappings.put("INFO", 3);
        mappings.put("DEBUG", 4);
    }

    @Override
    public Object execute() throws Exception {
        logService.log(toLevel(level.toUpperCase()), message);
        return null;
    }

    private int toLevel(String logLevel) {
        Integer level =  mappings.get(logLevel);
        if(level == null) {
            level = 3;
        }
        return level;
    }

}
