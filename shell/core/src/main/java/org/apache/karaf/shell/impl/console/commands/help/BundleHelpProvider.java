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
package org.apache.karaf.shell.impl.console.commands.help;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.AnsiPrintingWikiVisitor;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.WikiParser;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.WikiVisitor;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class BundleHelpProvider implements HelpProvider {

    @Override
    public String getHelp(Session session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("bundle|")) {
                path = path.substring("bundle|".length());
            } else {
                return null;
            }
        }

        if (path.matches("[0-9]*")) {
            long id = Long.parseLong(path);
            BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
            Bundle bundle = bundleContext.getBundle(id);
            if (bundle != null) {
                String title = ShellUtil.getBundleName(bundle);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                ps.println("\n" + title);
                ps.println(ShellUtil.getUnderlineString(title));
                URL bundleInfo = bundle.getEntry("OSGI-INF/bundle.info");
                if (bundleInfo != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(bundleInfo.openStream()))) {
                        int maxSize = 80;
                        Terminal terminal = session.getTerminal();
                        if (terminal != null) {
                            maxSize = terminal.getWidth();
                        }
                        WikiVisitor visitor = new AnsiPrintingWikiVisitor(ps, maxSize);
                        WikiParser parser = new WikiParser(visitor);
                        parser.parse(reader);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                ps.close();
                return baos.toString();
            }
        }
        return null;
    }

}
