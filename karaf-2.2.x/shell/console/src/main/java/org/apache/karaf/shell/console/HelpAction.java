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
package org.apache.karaf.shell.console;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jline.Terminal;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.gogo.commands.basic.DefaultActionPreparator;
import org.apache.felix.service.command.Function;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Displays help on the available commands
 */
@Command(scope = "*", name = "help", description = "Displays this help or help about a command")
public class HelpAction extends AbstractAction {

    @Argument(name = "command", required = false, description = "The command to get help for")
    private String command;

    public Object doExecute() throws Exception {
        if (command == null) {
            Set<String> names = (Set<String>) session.get(".commands");
            if (!names.isEmpty()) {
                Terminal term = (Terminal) session.get(".jline.terminal");
                PrintStream out = System.out;
                SortedMap<String,String> commands = new TreeMap<String,String>();
                for (String name : names) {
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
                // Post process the commands list

                out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("COMMANDS").a(Ansi.Attribute.RESET));
                for (Map.Entry<String,String> entry : commands.entrySet()) {
                    out.print("        ");
                    String key = NameScoping.getCommandNameWithoutGlobalPrefix(session, entry.getKey());
                    out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(key).a(Ansi.Attribute.RESET));
                    if (entry.getValue() != null) {
                        DefaultActionPreparator.printFormatted("                ", entry.getValue(), term != null ? term.getWidth() : 80, out);
                    }
                }
                out.println();
            }
            return null;
        } else {
            return session.execute(command + " --help");
        }
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
