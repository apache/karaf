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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.karaf.shell.api.console.Terminal;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractTerminal;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;

public class KarafTerminal extends AbstractTerminal implements org.jline.terminal.Terminal {

    private final Terminal terminal;

    public KarafTerminal(Terminal terminal) throws IOException {
        super("Karaf", terminal.getType());
        this.terminal = terminal;

        String type = terminal.getType();
        if (type == null && terminal.isAnsiSupported()) {
            type = "ansi";
        }
        String caps;
        try {
            caps = InfoCmp.getInfoCmp(type);
        } catch (Exception e) {
            try {
                caps = InfoCmp.getInfoCmp("ansi");
            } catch (InterruptedException e2) {
                throw new UnsupportedOperationException(e2);
            }
        }
        try {
            InfoCmp.parseInfoCmp(caps, bools, ints, strings);
        } catch (Exception e) {
            // TODO
        }
    }

    @Override
    public NonBlockingReader reader() {
        return null;
    }

    @Override
    public PrintWriter writer() {
        return null;
    }

    @Override
    public InputStream input() {
        return null;
    }

    @Override
    public OutputStream output() {
        return null;
    }

    @Override
    public Attributes getAttributes() {
        return null;
    }

    @Override
    public void setAttributes(Attributes attr) {

    }

    @Override
    public Size getSize() {
        int h = terminal.getHeight();
        int w = terminal.getWidth();
        return new Size(w, h);
    }

    @Override
    public void setSize(Size size) {
        throw new UnsupportedOperationException();
    }

}

