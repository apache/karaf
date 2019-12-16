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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Map;

import org.apache.karaf.shell.api.console.SignalListener;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.server.Environment;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Size;
import org.jline.terminal.impl.ExternalTerminal;

public class SshTerminal extends ExternalTerminal implements Terminal {

    private Environment environment;

    public SshTerminal(Environment environment, InputStream input, OutputStream output, String encoding) throws IOException {
        super("Karaf SSH terminal",
              environment.getEnv().get(Environment.ENV_TERM),
              input,
              output,
              Charset.forName(encoding));
        this.environment = environment;
        this.environment.addSignalListener(this::handleSignal);
        for (Map.Entry<PtyMode, Integer> e : environment.getPtyModes().entrySet()) {
            switch (e.getKey()) {
                case VINTR:
                    attributes.setControlChar(ControlChar.VINTR, e.getValue());
                    break;
                case VQUIT:
                    attributes.setControlChar(ControlChar.VQUIT, e.getValue());
                    break;
                case VERASE:
                    attributes.setControlChar(ControlChar.VERASE, e.getValue());
                    break;
                case VKILL:
                    attributes.setControlChar(ControlChar.VKILL, e.getValue());
                    break;
                case VEOF:
                    attributes.setControlChar(ControlChar.VEOF, e.getValue());
                    break;
                case VEOL:
                    attributes.setControlChar(ControlChar.VEOL, e.getValue());
                    break;
                case VEOL2:
                    attributes.setControlChar(ControlChar.VEOL2, e.getValue());
                    break;
                case VSTART:
                    attributes.setControlChar(ControlChar.VSTART, e.getValue());
                    break;
                case VSTOP:
                    attributes.setControlChar(ControlChar.VSTOP, e.getValue());
                    break;
                case VSUSP:
                    attributes.setControlChar(ControlChar.VSUSP, e.getValue());
                    break;
                case VDSUSP:
                    attributes.setControlChar(ControlChar.VDSUSP, e.getValue());
                    break;
                case VREPRINT:
                    attributes.setControlChar(ControlChar.VREPRINT, e.getValue());
                    break;
                case VWERASE:
                    attributes.setControlChar(ControlChar.VWERASE, e.getValue());
                    break;
                case VLNEXT:
                    attributes.setControlChar(ControlChar.VLNEXT, e.getValue());
                    break;
                case VSTATUS:
                    attributes.setControlChar(ControlChar.VSTATUS, e.getValue());
                    break;
                case VDISCARD:
                    attributes.setControlChar(ControlChar.VDISCARD, e.getValue());
                    break;
                case ECHO:
                    attributes.setLocalFlag(LocalFlag.ECHO, e.getValue() != 0);
                    break;
                case ICANON:
                    attributes.setLocalFlag(LocalFlag.ICANON, e.getValue() != 0);
                    break;
                case ISIG:
                    attributes.setLocalFlag(LocalFlag.ISIG, e.getValue() != 0);
                    break;
                case ICRNL:
                    attributes.setInputFlag(InputFlag.ICRNL, e.getValue() != 0);
                    break;
                case INLCR:
                    attributes.setInputFlag(InputFlag.INLCR, e.getValue() != 0);
                    break;
                case IGNCR:
                    attributes.setInputFlag(InputFlag.IGNCR, e.getValue() != 0);
                    break;
                case OCRNL:
                    attributes.setOutputFlag(OutputFlag.OCRNL, e.getValue() != 0);
                    break;
                case ONLCR:
                    attributes.setOutputFlag(OutputFlag.ONLCR, e.getValue() != 0);
                    break;
                case ONLRET:
                    attributes.setOutputFlag(OutputFlag.ONLRET, e.getValue() != 0);
                    break;
                case OPOST:
                    attributes.setOutputFlag(OutputFlag.OPOST, e.getValue() != 0);
                    break;
            }
        }
        int w = Integer.parseInt(this.environment.getEnv().get(Environment.ENV_COLUMNS));
        int h = Integer.parseInt(this.environment.getEnv().get(Environment.ENV_LINES));
        setSize(new Size(w, h));
    }

    protected void handleSignal(org.apache.sshd.common.channel.Channel channel, org.apache.sshd.server.Signal signal) {
        if (signal == org.apache.sshd.server.Signal.INT) {
            raise(Signal.INT);
        } else if (signal == org.apache.sshd.server.Signal.QUIT) {
            raise(Signal.QUIT);
        } else if (signal == org.apache.sshd.server.Signal.TSTP) {
            raise(Signal.TSTP);
        } else if (signal == org.apache.sshd.server.Signal.CONT) {
            raise(Signal.CONT);
        } else if (signal == org.apache.sshd.server.Signal.WINCH) {
            int w = Integer.parseInt(this.environment.getEnv().get(Environment.ENV_COLUMNS));
            int h = Integer.parseInt(this.environment.getEnv().get(Environment.ENV_LINES));
            setSize(new Size(w, h));
            raise(Signal.WINCH);
        }
    }

    @Override
    public int getWidth() {
        return size.getColumns();
    }

    @Override
    public int getHeight() {
        return size.getRows();
    }

    @Override
    public void addSignalListener(SignalListener listener) {
        // TODO:JLINE
    }

    @Override
    public void addSignalListener(SignalListener listener, org.apache.karaf.shell.api.console.Signal... signal) {
        // TODO:JLINE
    }

    @Override
    public void addSignalListener(SignalListener listener, EnumSet<org.apache.karaf.shell.api.console.Signal> signals) {
        // TODO:JLINE
    }

    @Override
    public void removeSignalListener(SignalListener listener) {
        // TODO:JLINE
    }

    @Override
    public boolean isAnsiSupported() {
        return true;
    }

    @Override
    public boolean isEchoEnabled() {
        return echo();
    }

    @Override
    public void setEchoEnabled(boolean enabled) {
        echo(enabled);
    }

}
