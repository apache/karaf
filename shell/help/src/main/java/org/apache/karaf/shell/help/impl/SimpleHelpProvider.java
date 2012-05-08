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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import jline.Terminal;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.basic.DefaultActionPreparator;
import org.apache.karaf.shell.console.HelpProvider;

public class SimpleHelpProvider implements HelpProvider {
    
    private Map<String, String> help;

    public Map<String, String> getHelp() {
        return help;
    }

    public void setHelp(Map<String, String> help) {
        this.help = help;
    }

    public String getHelp(CommandSession session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("simple|")) {
                path = path.substring("simple|".length());
            } else {
                return null;
            }
        }
        String str = help.get(path);
        if (str != null) {
            Terminal term = (Terminal) session.get(".jline.terminal");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DefaultActionPreparator.printFormatted("", str, term != null ? term.getWidth() : 80, new PrintStream(baos, true));
            str = baos.toString();
        }
        return str;
    }
}
