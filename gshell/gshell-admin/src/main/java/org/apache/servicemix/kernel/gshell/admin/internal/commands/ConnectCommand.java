/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.servicemix.kernel.gshell.admin.internal.commands;

import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.command.CommandExecutor;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.osgi.framework.ServiceReference;

@CommandComponent(id="admin:connect", description="Connect to the given instance")
public class ConnectCommand extends AdminCommandSupport {

    @Argument(index=0, required=true, description="The instance name")
    private String instance = null;

    @Option(name="-u", aliases={"--username"}, metaVar="USERNAME", description="Remote user name")
    private String username = "smx";

    @Option(name="-p", aliases={"--password"}, metaVar="PASSWORD", description="Remote user password")
    private String password = "smx";

    protected Object doExecute() throws Exception {
        int port = getExistingInstance(instance).getPort();
        ServiceReference ref = getBundleContext().getServiceReference(org.apache.geronimo.gshell.command.CommandExecutor.class.getName());
        if (ref == null) {
            io.out.println("CommandExecutor service is unavailable.");
            return null;
        }
        try {
            CommandExecutor exec = (CommandExecutor) getBundleContext().getService(ref);
            if (exec == null) {
                io.out.println("CommandExecutor service is unavailable.");
                return null;
            }

            exec.execute("remote rsh -u " + username + " -p " + password + " -n " + instance + " tcp://localhost:" + port);
        }
        finally {
            getBundleContext().ungetService(ref);
        }

        return SUCCESS;
    }
}
