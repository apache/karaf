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
package org.apache.karaf.instance.command;

import java.util.List;

import org.apache.karaf.instance.command.completers.InstanceCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;

@Command(scope = "instance", name = "connect", description = "Connects to an existing container instance.")
@Service
public class ConnectCommand extends InstanceCommandSupport {

    @Option(name="-u", aliases={"--username"}, description="Remote user name", required = false, multiValued = false)
    private String username;

    @Option(name = "-p", aliases = {"--password"}, description = "Remote password", required = false, multiValued = false)
    private String password;

    @Option(name = "-k", aliases = {"--keyfile"}, description = "Remote key file to use for key authentication", required = false, multiValued = false)
    private String keyFile;

    @Argument(index = 0, name="name", description="The name of the container instance", required = true, multiValued = false)
    @Completion(InstanceCompleter.class)
    private String instance = null;

    @Argument(index = 1, name = "command", description = "Optional command to execute", required = false, multiValued = true)
    private List<String> command;

    @Reference
    Session session;

    protected Object doExecute() throws Exception {
        String cmdStr = "";
        if (command != null) {
            StringBuilder sb = new StringBuilder();
            for (String cmd : command) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(cmd);
            }
            cmdStr = "'" + sb.toString().replaceAll("'", "\\'") + "'";
        }

        int port = getExistingInstance(instance).getSshPort();
        if (username != null) {
            if (password == null) {
                if (keyFile == null) {
                    session.execute("ssh:ssh -q -l " + username + " -p " + port + " localhost " + cmdStr);
                } else {
                    session.execute("ssh:ssh -q -l " + username + " -p " + port + " -k " + keyFile + " localhost " + cmdStr);
                }
            } else {
                session.execute("ssh:ssh -q -l " + username + " -P " + password + " -p " + port + " localhost " + cmdStr);
            }
        } else {
            if (keyFile == null) {
                session.execute("ssh:ssh -q -p " + port + " localhost " + cmdStr);
            } else {
                session.execute("ssh:ssh -q -p " + port + " -k " + keyFile + " localhost " + cmdStr);
            }
        }
        return null;
    }

}
