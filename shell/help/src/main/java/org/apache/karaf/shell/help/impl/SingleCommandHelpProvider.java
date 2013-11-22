/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.help.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.console.HelpProvider;

public class SingleCommandHelpProvider implements HelpProvider {

    public static final String COMMANDS = ".commands";

    private ThreadIO io;
    
    public SingleCommandHelpProvider(ThreadIO io) {
        this.io = io;
    }

    public String getHelp(CommandSession session, String path) {

        String subshell = (String) session.get("SUBSHELL");

        if (subshell != null && !subshell.trim().isEmpty()) {
            if (!path.startsWith(subshell)) {
                path = subshell + ":" + path;
            }
        }

        if (path.indexOf('|') > 0) {
            if (path.startsWith("command|")) {
                path = path.substring("command|".length());
            } else {
                return null;
            }
        }
        Set<String> names = (Set<String>) session.get(COMMANDS);
        if (path != null && names.contains(path)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            io.setStreams(new ByteArrayInputStream(new byte[0]), new PrintStream(baos, true), new PrintStream(baos, true));
            try {
                session.execute(path + " --help");
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                io.close();
            }
            return baos.toString();
        }
        return null;
    }
}
