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
package org.apache.karaf.shell.commands.meta;

import static org.apache.karaf.shell.commands.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf.shell.commands.ansi.SimpleAnsi.INTENSITY_NORMAL;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.commands.Action;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.HelpOption;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.commands.ansi.SimpleAnsi;
import org.apache.karaf.shell.console.Completer;

public class ActionMetaData {

    private final Class<? extends Action> actionClass;
    private final Command command;
    private final Map<Option, Field> options;
    private final Map<Argument, Field> arguments;
    List<Argument> orderedArguments;
    private final Completer[] completers;

    public ActionMetaData(Class<? extends Action> actionClass, Command command, Map<Option, Field> options, Map<Argument, Field> args,
            List<Argument> orderedArguments, Completer[] completers) {
        super();
        this.actionClass = actionClass;
        this.command = command;
        this.options = options;
        this.arguments = args;
        this.orderedArguments = orderedArguments;
        this.completers = completers;
    }

    public Class<? extends Action> getActionClass() {
        return actionClass;
    }
    
    public Command getCommand() {
        return command;
    }

    public Map<Option, Field> getOptions() {
        return options;
    }

    public Map<Argument, Field> getArguments() {
        return arguments;
    }

    public Completer[] getCompleters() {
        return completers;
    }

    public List<Argument> getOrderedArguments() {
        return orderedArguments;
    }

    public void printUsage(Action action, PrintStream out, boolean globalScope, int termWidth) {
        if (command != null) {
            List<Argument> argumentsSet = new ArrayList<Argument>(arguments.keySet());
            Collections.sort(argumentsSet, new Comparator<Argument>() {
                public int compare(Argument o1, Argument o2) {
                    return Integer.valueOf(o1.index()).compareTo(Integer.valueOf(o2.index()));
                }
            });
            Set<Option> optionsSet = new HashSet<Option>(options.keySet());
            optionsSet.add(HelpOption.HELP);
            if (command != null && (command.description() != null || command.name() != null)) {
                out.println(INTENSITY_BOLD + "DESCRIPTION" + INTENSITY_NORMAL);
                out.print("        ");
                if (command.name() != null) {
                    if (globalScope) {
                        out.println(INTENSITY_BOLD + command.name() + INTENSITY_NORMAL);
                    } else {
                        out.println(command.scope() + ":" + INTENSITY_BOLD + command.name() + INTENSITY_NORMAL);
                    }
                    out.println();
                }
                out.print("\t");
                out.println(command.description());
                out.println();
            }
            StringBuffer syntax = new StringBuffer();
            if (command != null) {
                if (globalScope) {
                    syntax.append(command.name());
                } else {
                    syntax.append(String.format("%s:%s", command.scope(), command.name()));
                }
            }
            if (options.size() > 0) {
                syntax.append(" [options]");
            }
            if (arguments.size() > 0) {
                syntax.append(' ');
                for (Argument argument : argumentsSet) {
                    if (!argument.required()) {
                        syntax.append(String.format("[%s] ", argument.name()));
                    } else {
                        syntax.append(String.format("%s ", argument.name()));
                    }
                }
            }

            out.println(INTENSITY_BOLD + "SYNTAX" + INTENSITY_NORMAL);
            out.print("        ");
            out.println(syntax.toString());
            out.println();
            if (arguments.size() > 0) {
                out.println(INTENSITY_BOLD + "ARGUMENTS" + INTENSITY_NORMAL);
                for (Argument argument : argumentsSet) {
                    out.print("        ");
                    out.println(INTENSITY_BOLD + argument.name() + INTENSITY_NORMAL);
                    ActionMetaData.printFormatted("                ", argument.description(), termWidth, out, true);
                    if (!argument.required()) {
                        if (argument.valueToShowInHelp() != null && argument.valueToShowInHelp().length() != 0) {
                            if (Argument.DEFAULT_STRING.equals(argument.valueToShowInHelp())) {
                                Object o = getDefaultValue(action, argument);
                                String defaultValue = getDefaultValueString(o);
                                if (defaultValue != null) {
                                    printDefaultsTo(out, defaultValue);
                                }
                            } else {
                                printDefaultsTo(out, argument.valueToShowInHelp());
                            }
                        }
                    }
                }
                out.println();
            }
            if (options.size() > 0) {
                out.println(INTENSITY_BOLD + "OPTIONS" + INTENSITY_NORMAL);
                for (Option option : optionsSet) {
                    String opt = option.name();
                    for (String alias : option.aliases()) {
                        opt += ", " + alias;
                    }
                    out.print("        ");
                    out.println(INTENSITY_BOLD + opt + INTENSITY_NORMAL);
                    ActionMetaData.printFormatted("                ", option.description(), termWidth, out, true);
                    if (option.valueToShowInHelp() != null && option.valueToShowInHelp().length() != 0) {
                        if (Option.DEFAULT_STRING.equals(option.valueToShowInHelp())) {
                            Object o = getDefaultValue(action, option);
                            String defaultValue = getDefaultValueString(o);
                            if (defaultValue != null) {
                                printDefaultsTo(out, defaultValue);
                            }
                        } else {
                            printDefaultsTo(out, option.valueToShowInHelp());
                        }
                    }
                }
                out.println();
            }
            if (command.detailedDescription().length() > 0) {
                out.println(INTENSITY_BOLD + "DETAILS" + INTENSITY_NORMAL);
                String desc = getDetailedDescription();
                ActionMetaData.printFormatted("        ", desc, termWidth, out, true);
            }
        }
    }
    
    public Object getDefaultValue(Action action, Argument argument) {
        try {
            arguments.get(argument).setAccessible(true);
            return arguments.get(argument).get(action);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object getDefaultValue(Action action, Option option) {
        try {
            options.get(option).setAccessible(true);
            return options.get(option).get(action);
        } catch (Exception e) {
            return null;
        }
    }
    
    public String getDetailedDescription() {
        String desc = command.detailedDescription();
        return loadDescription(actionClass, desc);
    }
    
    private String loadDescription(Class<?> clazz, String desc) {
        if (desc != null && desc.startsWith("classpath:")) {
            desc = loadClassPathResource(clazz, desc.substring("classpath:".length()));
        }
        return desc;
    }
    
    public String getDefaultValueString(Object o) {
        if (o != null && (!(o instanceof Boolean) || ((Boolean)o))
            && (!(o instanceof Number) || ((Number)o).doubleValue() != 0.0)) {
            return o.toString();
        } else {
            return null;
        }
    }

    private void printDefaultsTo(PrintStream out, String value) {
        out.println("                (defaults to " + value + ")");
    }

    static void printFormatted(String prefix, String str, int termWidth, PrintStream out, boolean prefixFirstLine) {
        int pfxLen = prefix.length();
        int maxwidth = termWidth - pfxLen;
        Pattern wrap = Pattern.compile("(\\S\\S{" + maxwidth + ",}|.{1," + maxwidth + "})(\\s+|$)");
        int cur = 0;
        while (cur >= 0) {
            int lst = str.indexOf('\n', cur);
            String s = (lst >= 0) ? str.substring(cur, lst) : str.substring(cur);
            if (s.length() == 0) {
                out.println();
            } else {
                Matcher m = wrap.matcher(s);
                while (m.find()) {
                    if (cur > 0 || prefixFirstLine) {
                        out.print(prefix);
                    }
                    out.println(m.group());
                }
            }
            if (lst >= 0) {
                cur = lst + 1;
            } else {
                break;
            }
        }
    }

    private String loadClassPathResource(Class<?> clazz, String path) {
        InputStream is = clazz.getResourceAsStream(path);
        if (is == null) {
            is = clazz.getClassLoader().getResourceAsStream(path);
        }
        if (is == null) {
            return "Unable to load description from " + path;
        }
    
        try {
            Reader r = new InputStreamReader(is);
            StringWriter sw = new StringWriter();
            int c;
            while ((c = r.read()) != -1) {
                sw.append((char) c);
            }
            return sw.toString();
        } catch (IOException e) {
            return "Unable to load description from " + path;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
