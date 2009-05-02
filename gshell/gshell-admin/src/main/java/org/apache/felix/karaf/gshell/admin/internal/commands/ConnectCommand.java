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
package org.apache.felix.karaf.gshell.admin.internal.commands;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.shell.ShellContextHolder;

public class ConnectCommand extends AdminCommandSupport {

    @Argument(index=0, required=true, description="The instance name")
    private String instance = null;

    @Option(name="-u", aliases={"--username"}, token="USERNAME", description="Remote user name")
    private String username = "smx";

    @Option(name="-p", aliases={"--password"}, token="PASSWORD", description="Remote user password")
    private String password = "smx";

    protected Object doExecute() throws Exception {
        int port = getExistingInstance(instance).getPort();
        ShellContextHolder.get().getShell().execute("ssh -l " + username + " -P " + password + " -p " + port + " localhost");
        return Result.SUCCESS;
    }
}
