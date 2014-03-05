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
package org.apache.karaf.shell.impl.console;

import jline.TerminalSupport;
import org.apache.karaf.shell.api.console.Terminal;

public class KarafTerminal extends TerminalSupport {

    private final Terminal terminal;

    public KarafTerminal(Terminal terminal) {
        super(true);
        this.terminal = terminal;
    }

    @Override
    public synchronized boolean isAnsiSupported() {
        return terminal.isAnsiSupported();
    }

    @Override
    public int getWidth() {
        return terminal.getWidth();
    }

    @Override
    public int getHeight() {
        return terminal.getHeight();
    }

    @Override
    public synchronized boolean isEchoEnabled() {
        return terminal.isEchoEnabled();
    }

    @Override
    public synchronized void setEchoEnabled(boolean enabled) {
        terminal.setEchoEnabled(enabled);
    }
}
