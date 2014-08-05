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

import org.apache.sshd.SshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshServerFactory.class);

    private long idleTimeout;
    private boolean start;

    private SshServer server;

    public SshServerFactory(SshServer server) {
        this.server = server;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public void start() {
        if (start) {
            try {
                server.getProperties().put(SshServer.IDLE_TIMEOUT, new Long(idleTimeout).toString());
                server.start();
            } catch (Exception e) {
                LOGGER.info("Error updating SSH server", e);
            }
        }
    }

    public void stop() {
        if (start && server != null) {
            try {
                server.stop(true);
            } catch (Exception e) {
                LOGGER.info("Error stopping SSH server", e);
            } finally {
                server = null;
            }
        }
    }

}
