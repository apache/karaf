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
package org.apache.karaf.shell.commands.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "shell", name = "more", description = "File pager.")
@Service
public class MoreAction implements Action {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Option(name = "--lines", description = "stop after N lines")
    int lines;

    @Reference
    Terminal terminal;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        if (terminal == null || !isTty(System.out)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                checkInterrupted();
            }
            return null;
        } else {
            boolean echo = terminal.isEchoEnabled();
            terminal.setEchoEnabled(false);
            try {
                if (lines == 0) {
                    lines = terminal.getHeight();
                }
                LineSplitter reader = new LineSplitter(new BufferedReader(new InputStreamReader(System.in)), terminal.getWidth());
                int count = 0;
                int c;
                do {
                    do {
                        String line;
                        if ((line = reader.readLine()) == null) {
                            return null;
                        }
                        System.out.println(line);
                        checkInterrupted();
                    } while (++count < lines - 2);
                    c = -1;
                    while (c == -1) {
                        System.out.flush();
                        System.out.print("--More--");
                        System.out.flush();
                        c = session.getKeyboard().read();
                        switch (c) {
                            case 'q':
                            case -1:
                                c = 'q';
                                break;
                            case '\r':
                            case '\n':
                            case 14: // Down arrow
                                count--;
                                System.out.print("\r          \r");
                                break;
                            case ' ':
                                count = 0;
                                System.out.print("\r          \r");
                                break;
                            case 16: // Up arrow
                                // fall through
                            default:
                                c = -1;
                                System.out.print("\r          \r");
                                break;
                        }
                        if (c == 'q') {
                            break;
                        }
                    }
                } while (c != 'q');
            } catch (InterruptedException ie) {
            	log.debug("Interrupted by user");
            } finally {
                terminal.setEchoEnabled(echo);
            }
        }
        return null;
    }

    public static class LineSplitter {

        private final BufferedReader reader;
        private final int width;
        private final List<String> lines = new LinkedList<String>();

        public LineSplitter(BufferedReader reader, int width) {
            this.reader = reader;
            this.width = width;
        }

        public String readLine() throws IOException {
            if (lines.isEmpty()) {
                String str = reader.readLine();
                if (str == null) {
                    return null;
                }
                while (str.length() > width) {
                    lines.add(str.substring(0, width));
                    str = str.substring(width);
                }
                lines.add(str);
            }
            return lines.remove(0);
        }
    }

    protected boolean isTty(OutputStream out) {
        try {
            Method mth = out.getClass().getDeclaredMethod("getCurrent");
            mth.setAccessible(true);
            Object current = mth.invoke(out);
            return current == session.getConsole();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * This is for long running commands to be interrupted by ctrl-c
     *
     * @throws InterruptedException
     */
    public static void checkInterrupted() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

}
