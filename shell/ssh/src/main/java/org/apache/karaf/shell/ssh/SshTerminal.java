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

import org.apache.karaf.shell.api.console.Terminal;
import org.apache.sshd.server.Environment;

public class SshTerminal implements Terminal {

    private Environment environment;


    public SshTerminal(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getWidth() {
        int width = 0;
        try {
            width = Integer.valueOf(this.environment.getEnv().get(Environment.ENV_COLUMNS));
        } catch (Throwable t) {
            // Ignore
        }
        return width > 0 ? width : 80;
    }

    @Override
    public int getHeight() {
        int height = 0;
        try {
            height = Integer.valueOf(this.environment.getEnv().get(Environment.ENV_LINES));
        } catch (Throwable t) {
            // Ignore
        }
        return height > 0 ? height : 24;
    }

    @Override
    public boolean isAnsiSupported() {
        return true;
    }

    @Override
    public boolean isEchoEnabled() {
        return true;
    }

    @Override
    public void setEchoEnabled(boolean enabled) {
        // TODO: how to disable echo over ssh ?
    }
}
