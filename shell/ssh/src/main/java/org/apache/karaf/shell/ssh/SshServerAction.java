/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.sshd.SshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "ssh", name = "sshd", description = "Creates a SSH server")
@Service
public class SshServerAction implements Action
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Option(name = "-p", aliases = { "--port" }, description = "The port to setup the SSH server (Default: 8101)", required = false, multiValued = false)
    private int port = 8101;

    @Option(name = "-b", aliases = { "--background" }, description = "The service will run in the background (Default: true)", required = false, multiValued = false)
    private boolean background = true;

    @Option(name = "-i", aliases = { "--idle-timeout" }, description = "The session idle timeout (Default: 1800000ms)", required = false, multiValued = false)
    private long idleTimeout = 1800000;

    @Reference
    private SshServer server;

    public void setServer(SshServer server) {
        this.server = server;
    }

    public Object execute() throws Exception {
        log.debug("Created server: {}", server);

        // port number
        server.setPort(port);

        // idle timeout
        server.getProperties().put(SshServer.IDLE_TIMEOUT, Long.toString(idleTimeout));

        // starting the SSHd server
        server.start();

        System.out.println("SSH server listening on port " + port + " (idle timeout " + idleTimeout + "ms)");

        if (!background) {
            synchronized (this) {
                log.debug("Waiting for server to shutdown");

                wait();
            }

            server.stop();
        }

        return null;
    }
}
