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
package org.apache.karaf.shell.console.impl.help;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

import jline.Terminal;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.basic.DefaultActionPreparator;
import org.apache.karaf.shell.console.HelpProvider;
import org.apache.karaf.shell.console.SubShell;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class SubShellHelpProvider implements HelpProvider {

    private BundleContext context;
    private ServiceTracker tracker;

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void start() {
        tracker = new ServiceTracker(context, SubShell.class.getName(), null);
        tracker.open();
    }
    
    public void stop() {
        tracker.close();
    }

    public String getHelp(CommandSession session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("subshell|")) {
                path = path.substring("subshell|".length());
            } else {
                return null;
            }
        }
        for (ServiceReference ref : tracker.getServiceReferences()) {
            if (path.equals(ref.getProperty("name"))) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                printSubShellHelp(session, ref.getBundle(), (SubShell) tracker.getService(ref), new PrintStream(baos, true));
                return baos.toString();
            }
        }
        return null;
    }

    private void printSubShellHelp(CommandSession session, Bundle bundle, SubShell subShell, PrintStream out) {
        Terminal term = session != null ? (Terminal) session.get(".jline.terminal") : null;
        out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("SUBSHELL").a(Ansi.Attribute.RESET));
        out.print("        ");
        if (subShell.getName() != null) {
            out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(subShell.getName()).a(Ansi.Attribute.RESET));
            out.println();
        }
        out.print("\t");
        out.println(subShell.getDescription());
        out.println();
        if (subShell.getDetailedDescription() != null) {
            out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("DETAILS").a(Ansi.Attribute.RESET));
            String desc = loadDescription(bundle, subShell.getDetailedDescription());
            while (desc.endsWith("\n")) {
                desc = desc.substring(0, desc.length()  -1);
            }
            DefaultActionPreparator.printFormatted("        ", desc, term != null ? term.getWidth() : 80, out);
        }
        out.println();
        out.println("${command-list|" + subShell.getName() + ":}");
    }

    protected String loadDescription(Bundle bundle, String desc) {
        if (desc.startsWith("classpath:")) {
            URL url = bundle.getResource(desc.substring("classpath:".length()));
            if (url == null) {
                desc = "Unable to load description from " + desc;
            } else {
                InputStream is = null;
                try {
                    is = url.openStream();
                    Reader r = new InputStreamReader(is);
                    StringWriter sw = new StringWriter();
                    int c;
                    while ((c = r.read()) != -1) {
                        sw.append((char) c);
                    }
                    desc = sw.toString();
                } catch (IOException e) {
                    desc = "Unable to load description from " + desc;
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
        return desc;
    }

}
