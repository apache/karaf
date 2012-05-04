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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jline.Terminal;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.commands.basic.DefaultActionPreparator;
import org.apache.karaf.shell.console.AbstractAction;
import org.apache.karaf.shell.console.NameScoping;
import org.apache.karaf.shell.console.SubShell;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Displays help on the available commands
 */
@Command(scope = "*", name = "help", description = "Displays this help or help about a command")
public class HelpAction extends AbstractAction {

    @Argument(name = "command", required = false, description = "The command to get help for")
    private String command;
    
    private HelpSystem provider;

    public void setProvider(HelpSystem provider) {
        this.provider = provider;
    }

    public Object doExecute() throws Exception {
        String help = provider.getHelp(session, command);
        if (help != null) {
            System.out.println(help);
        }
        return null;
    }

    private SortedMap<String, String> getCommandDescriptions(Set<String> names) {
        SortedMap<String,String> commands = new TreeMap<String,String>();
        for (String name : names) {
            if (command != null && !name.startsWith(command)) {
                continue;
            }
            String description = null;
            Function function = (Function) session.get(name);
            function = unProxy(function);
            if (function instanceof AbstractCommand) {
                try {
                    Method mth = AbstractCommand.class.getDeclaredMethod("createNewAction");
                    mth.setAccessible(true);
                    Action action = (Action) mth.invoke(function);
                    Class<? extends Action> clazz = action.getClass();
                    Command ann = clazz.getAnnotation(Command.class);
                    description = ann.description();
                } catch (Throwable e) {
                }
                if (name.startsWith("*:")) {
                    name = name.substring(2);
                }
                commands.put(name, description);
            }
        }
        return commands;
    }

    private void printMethodList(Terminal term, PrintStream out, SortedMap<String, String> commands) {
        out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("COMMANDS").a(Ansi.Attribute.RESET));
//        int max = 0;
//        for (Map.Entry<String,String> entry : commands.entrySet()) {
//            String key = NameScoping.getCommandNameWithoutGlobalPrefix(session, entry.getKey());
//            max = Math.max(max,key.length());
//        }
        for (Map.Entry<String,String> entry : commands.entrySet()) {
            out.print("        ");
            String key = NameScoping.getCommandNameWithoutGlobalPrefix(session, entry.getKey());
            out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(key).a(Ansi.Attribute.RESET));
            if (entry.getValue() != null) {
                DefaultActionPreparator.printFormatted("                ", entry.getValue(),
                        term != null ? term.getWidth() : 80, out);
            }
        }
        out.println();
    }

    private void printSubShellHelp(Bundle bundle, SubShell subShell, PrintStream out) {
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
            DefaultActionPreparator.printFormatted("        ", desc, term != null ? term.getWidth() : 80, out);
        }
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

    protected Function unProxy(Function function) {
        try {
            if (function.getClass().getName().contains("CommandProxy")) {
                Field contextField = function.getClass().getDeclaredField("context");
                Field referenceField = function.getClass().getDeclaredField("reference");
                contextField.setAccessible(true);
                referenceField.setAccessible(true);
                BundleContext context = (BundleContext) contextField.get(function);
                ServiceReference reference = (ServiceReference) referenceField.get(function);
                Object target = context.getService(reference);
                try {
                    if (target instanceof Function) {
                        function = (Function) target;
                    }
                } finally {
                    context.ungetService(reference);
                }
            }
        } catch (Throwable t) {
        }
        return function;
    }

}
