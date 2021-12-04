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
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Command(scope = "ssh", name = "sshd", description = "Creates a SSH server")
@Service
public class SshServerAction implements Action
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Option(name = "-p", aliases = { "--port" }, description = "The port to setup the SSH server", required = false, multiValued = false)
    private int port = 8101;

    @Option(name = "-b", aliases = { "--background" }, description = "The service will run in the background", required = false, multiValued = false)
    private boolean background = true;

    @Option(name = "-i", aliases = { "--idle-timeout" }, description = "The session idle timeout in milliseconds", required = false, multiValued = false)
    private long idleTimeout = 1800000;
    
    @Option(name = "-n", aliases = { "--nio-workers" }, description = "The number of NIO worker threads to use", required = false, multiValued = false)
    private int nioWorkers = 2;

    @Option(name = "-c", aliases = { "--max-concurrent-sessions" }, description = "The maximum number of concurrent sessions opened on the ssh server", required = false, multiValued = false)
    private int maxConcurrentSessions = -1;

    @Option(name = "-w", aliases = { "--welcome-banner" }, description = "The welcome banner to display when logging in", required = false, multiValued = false)
    private String welcomeBanner;
    
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
        CoreModuleProperties.IDLE_TIMEOUT.set(server, Duration.ofMillis(idleTimeout));
        
        // nio-workers
        CoreModuleProperties.NIO_WORKERS.set(server, nioWorkers);

        // max-concurrent-sessions
        if (maxConcurrentSessions != -1) {
            CoreModuleProperties.MAX_CONCURRENT_SESSIONS.set(server, maxConcurrentSessions);
        }
        
        // welcome banner
        if (welcomeBanner != null) {
            CoreModuleProperties.WELCOME_BANNER.set(server, welcomeBanner);
        } 
        
        // starting the SSHd server
        server.start();

        System.out.println("SSH server listening on port " + port + " (idle timeout " + idleTimeout + "ms) " + " (nio worker Threads " + nioWorkers + ") ");

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
