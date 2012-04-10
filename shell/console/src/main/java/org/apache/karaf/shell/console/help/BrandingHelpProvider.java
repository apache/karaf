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
package org.apache.karaf.shell.console.help;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import jline.Terminal;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.HelpProvider;
import org.apache.karaf.shell.console.util.Branding;

import static org.apache.felix.gogo.commands.basic.DefaultActionPreparator.printFormatted;

public class BrandingHelpProvider implements HelpProvider {

    private Properties branding;

    public BrandingHelpProvider() {
        branding = Branding.loadBrandingProperties();
    }

    public String getHelp(CommandSession session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("branding|")) {
                path = path.substring("branding|".length());
            } else {
                return null;
            }
        }
        String str = branding.getProperty("help." + path);
        if (str != null) {
            Terminal term = (Terminal) session.get(".jline.terminal");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            printFormatted("", str, term != null ? term.getWidth() : 80, new PrintStream(baos, true));
            str = baos.toString();
        }
        return str;
    }
}
