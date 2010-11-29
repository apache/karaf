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
package org.apache.karaf.shell.console.jline;

import jline.Terminal;
import jline.UnsupportedTerminal;
import jline.AnsiWindowsTerminal;
import jline.NoInterruptUnixTerminal;
import org.fusesource.jansi.internal.WindowsSupport;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class TerminalFactory {

    private Terminal term;

    public synchronized Terminal getTerminal() throws Exception {
        if (term == null) {
            init();
        }
        return term;
    }

    public void init() throws Exception {
        if ("jline.UnsupportedTerminal".equals(System.getProperty("jline.terminal"))) {
            term = new UnsupportedTerminal();
            return;
        }
        
        boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
        try {
            if (windows) {
                AnsiWindowsTerminal t = new KarafWindowsTerminal();
                t.setDirectConsole(true);
                t.init();
                term = t;
            } else {
                NoInterruptUnixTerminal t = new NoInterruptUnixTerminal();
                t.init();
                term = t;
            }
        } catch (Throwable e) {
            System.out.println("Using an unsupported terminal: " + e.toString());
            term = new UnsupportedTerminal();
        }
    }

    public synchronized void destroy() throws Exception {
        if (term != null) {
            term.restore();
            term = null;
        }
    }

    public static class KarafWindowsTerminal extends AnsiWindowsTerminal {

        public KarafWindowsTerminal() throws Exception {
            super();
        }

        @Override
        public int readCharacter(InputStream in) throws IOException {
            if (isSystemIn(in)) {
                return WindowsSupport.readByte();
            }
            else {
                return super.readCharacter(in);
            }
        }

        private boolean isSystemIn(InputStream in) throws IOException {
            assert in != null;

            if (in == System.in) {
                return true;
            }
            while (in instanceof FilterInputStream) {
                try {
                    Field f = FilterInputStream.class.getDeclaredField("in");
                    f.setAccessible(true);
                    in = (InputStream) f.get(in);
                } catch (Throwable t) {
                    break;
                }
                if (in == System.in) {
                    return true;
                }
            }
            if (in instanceof FileInputStream && ((FileInputStream) in).getFD() == FileDescriptor.in) {
                return true;
            }

            return false;
        }

    }

}
