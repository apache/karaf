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
package org.apache.karaf.admin.command;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.admin.Instance;

@Command(scope = "admin", name = "start", description = "Starts an existing container instance.")
public class StartCommand extends AdminCommandSupport {
                      
    @Option(name = "-d", aliases = { "--debug"}, description = "Start the instance in debug mode", required = false, multiValued = false)
    private boolean debug; 
    
    @Option(name = "-o", aliases = { "--java-opts"}, description = "Java options when launching the instance", required = false, multiValued = false)
    private String javaOpts;

    @Option(name = "-w", aliases = { "--wait"}, description = "Wait for the instance to be fully started", required = false, multiValued = false)
    private boolean wait;

    @Argument(index = 0, name = "name", description = "The name of the container instance", required = true, multiValued = false)
    private String instance = null;

    static final String DEBUG_OPTS = " -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005";
    static final String DEFAULT_OPTS = "-server -Xmx512M -Dcom.sun.management.jmxremote";

    protected Object doExecute() throws Exception {
        Instance child = getExistingInstance(instance);
        String opts = javaOpts;
        if (opts == null) {
            opts = child.getJavaOpts();
        }
        if (opts == null) {
            opts = DEFAULT_OPTS;
        }
        if (debug) {
            opts += DEBUG_OPTS;
        }
        if (wait) {
            String state = child.getState();
            if (Instance.STOPPED.equals(state)) {
                child.start(opts);
            }
            if (!Instance.STARTED.equals(state)) {
                do {
                    Thread.sleep(500);
                    state = child.getState();
                } while (Instance.STARTING.equals(state));
            }
        } else {
            child.start(opts);
        }
        return null;
    }
}
