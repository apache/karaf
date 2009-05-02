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
package org.apache.felix.karaf.gshell.config;

import java.util.Dictionary;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.osgi.service.cm.ConfigurationAdmin;

public class EditCommand extends ConfigCommandSupport {

    @Argument(required = true, description = "PID of the configuration")
    String pid;

    @Option(name = "--force", description = "Force the edition of this config, even if another one was under edition")
    boolean force;

    protected void doExecute(ConfigurationAdmin admin) throws Exception {
        String oldPid = (String) this.variables.get(PROPERTY_CONFIG_PID);
        if (oldPid != null && !oldPid.equals(pid) && !force) {
            io.err.println("Another config is being edited.  Cancel / update first, or use the --force option");
            return;
        }
        Dictionary props = admin.getConfiguration(pid).getProperties();
        this.variables.parent().set(PROPERTY_CONFIG_PID, pid);
        this.variables.parent().set(PROPERTY_CONFIG_PROPS, props);
    }
}
