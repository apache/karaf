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
package org.apache.geronimo.gshell.spring;

import org.apache.geronimo.gshell.CommandLineBuilder;
import org.apache.geronimo.gshell.DefaultCommandExecutor;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.command.CommandExecutor;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.shell.Environment;

/**
 * A simple wrapper around the DefaultCommandExecutor to avoid
 * using constructor injection which causes a circular dependency
 * in spring.
 */
public class SpringCommandExecutor implements CommandExecutor {

    private CommandExecutor executor;
    private LayoutManager layoutManager;
    private CommandRegistry commandRegistry;
    private CommandLineBuilder commandLineBuilder;
    private Environment env;

    public void setLayoutManager(LayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    public void setCommandRegistry(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public void setCommandLineBuilder(CommandLineBuilder commandLineBuilder) {
        this.commandLineBuilder = commandLineBuilder;
    }

    public void setEnv(Environment env) {
        this.env = env;
    }
    
    public void init() {
        executor = new DefaultCommandExecutor(layoutManager, commandRegistry, commandLineBuilder, env);
    }

    public Object execute(String s) throws Exception {
        return executor.execute(s);
    }

    public Object execute(String s, Object[] objects) throws Exception {
        return executor.execute(s, objects);
    }

    public Object execute(Object... objects) throws Exception {
        return executor.execute(objects);
    }
}
