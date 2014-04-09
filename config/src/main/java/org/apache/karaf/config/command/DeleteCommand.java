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
package org.apache.karaf.config.command;

import org.apache.karaf.config.command.completers.ConfigurationCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "config", name = "delete", description = "Delete a configuration.")
@Service
public class DeleteCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "pid", description = "PID of the configuration", required = true, multiValued = false)
    @Completion(ConfigurationCompleter.class)
    String pid;

    @Option(name = "--force", aliases = {}, description = "Force the edition of this config, even if another one was under edition", required = false, multiValued = false)
    boolean force;

    protected Object doExecute() throws Exception {
        String oldPid = (String) this.session.get(PROPERTY_CONFIG_PID);
        if (oldPid != null && oldPid.equals(pid) && !force) {
            System.err.println("This config is being edited.  Cancel / update first, or use the --force option");
            return null;
        }

        this.configRepository.delete(pid);
        if (oldPid != null && oldPid.equals(pid) && !force) {
            this.session.put(PROPERTY_CONFIG_PID, null);
            this.session.put(PROPERTY_CONFIG_PROPS, null);
        }
        return null;
    }

}
