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
package org.apache.karaf.webconsole.gogo;

 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.nio.charset.StandardCharsets;
 import java.util.EnumSet;

 import org.apache.karaf.shell.api.console.SignalListener;
 import org.apache.karaf.shell.api.console.Terminal;
 import org.jline.terminal.impl.ExternalTerminal;

public class WebTerminal extends ExternalTerminal implements Terminal {

    public WebTerminal(int width, int height, InputStream input, OutputStream output) throws IOException {
        super("Karaf Web Terminal", "ansi", input, output, StandardCharsets.UTF_8);
        size.setColumns(width);
        size.setRows(height);
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
