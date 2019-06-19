/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.action.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.runtime.Token;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.CommandException;
import org.apache.karaf.shell.support.NameScoping;
import org.apache.karaf.shell.support.converter.DefaultConverter;
import org.apache.karaf.shell.support.converter.GenericType;

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_RED;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL;

public class DefaultActionPreparator {

    public boolean prepare(Action action, Session session, List<Object> params) throws Exception {

        Command command = action.getClass().getAnnotation(Command.class);
        Map<Option, Field> options = new HashMap<>();
        Map<Argument, Field> arguments = new HashMap<>();
        List<Argument> orderedArguments = new ArrayList<>();

        for (Class<?> type = action.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Option option = field.getAnnotation(Option.class);
                if (option != null) {
                    options.put(option, field);
                }

                Argument argument = field.getAnnotation(Argument.class);
                if (argument != null) {
                    argument = replaceDefaultArgument(field, argument);
                    arguments.put(argument, field);
                    int index = argument.index();
                    while (orderedArguments.size() <= index) {
                        orderedArguments.add(null);
                    }
                    if (orderedArguments.get(index) != null) {
                        throw new IllegalArgumentException("Duplicate argument index: " + index + " on Action " + action.getClass().getName());
                    }
                    orderedArguments.set(index, argument);
                }
            }
        }
        assertIndexesAreCorrect(action.getClass(), orderedArguments);

        String commandErrorSt = COLOR_RED + "Error executing command " + command.scope() + ":" + INTENSITY_BOLD + command.name() + INTENSITY_NORMAL + COLOR_DEFAULT + ": ";
        for (Object param : params) {
            if (HelpOption.HELP.name().equals(param.toString())) {
                int termWidth = session.getTerminal() != null ? session.getTerminal().getWidth() : 80;
                termWidth = termWidth == 0 ? 80 : termWidth;
                boolean globalScope = NameScoping.isGlobalScope(session, command.scope());
                printUsage(action, options, arguments, System.out, globalScope, termWidth);
                return false;
            }
        }
        
        // Populate
        Map<Option, Object> optionValues = new HashMap<>();
        Map<Argument, Object> argumentValues = new HashMap<>();
        boolean processOptions = true;
        int argIndex = 0;
        for (Iterator<Object> it = params.iterator(); it.hasNext(); ) {
            Object param = it.next();

            String paramValue = null;
            if (param instanceof String) {
                paramValue = (String)param;
            }
            if (param instanceof Token) {
                paramValue = param.toString();
            }

            if (processOptions
                    && paramValue != null
                    && paramValue.startsWith("-")) {
                boolean isKeyValuePair = paramValue.indexOf('=') != -1;
                String name;
                Object value = null;
                if (isKeyValuePair) {
                    name = paramValue.substring(0, paramValue.indexOf('='));
                    value = paramValue.substring(paramValue.indexOf('=') + 1);
                } else {
                    name = paramValue;
                }
                Option option = null;
                for (Option opt : options.keySet()) {
                    if (name.equals(opt.name()) || Arrays.asList(opt.aliases()).contains(name)) {
                        option = opt;
                        break;
                    }
                }
                if (option == null) {
                    throw new CommandException(commandErrorSt
                                + "undefined option " + INTENSITY_BOLD + paramValue + INTENSITY_NORMAL + "\n"
                                + "Try <command> --help' for more information.",
                                        "Undefined option: " + paramValue);
                }
                Field field = options.get(option);
                if (value == null && (field.getType() == boolean.class || field.getType() == Boolean.class)) {
                    value = Boolean.TRUE;
                }
                if (value == null && it.hasNext()) {
                    value = it.next();
                }
                if (value == null) {
                        throw new CommandException(commandErrorSt
                                + "missing value for option " + INTENSITY_BOLD + paramValue + INTENSITY_NORMAL,
                                "Missing value for option: " + paramValue
                        );
                }
                if (option.multiValued()) {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>) optionValues.get(option);
                    if (l == null) {
                        l = new ArrayList<>();
                        optionValues.put(option, l);
                    }
                    l.add(value);
                } else {
                    optionValues.put(option, value);
                }
            } else {
                processOptions = false;
                if (argIndex >= orderedArguments.size()) {
                        throw new CommandException(commandErrorSt +
                                "too many arguments specified",
                                "Too many arguments specified"
                        );
                }
                Argument argument = orderedArguments.get(argIndex);
                if (!argument.multiValued()) {
                    argIndex++;
                }
                if (argument.multiValued()) {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>) argumentValues.get(argument);
                    if (l == null) {
                        l = new ArrayList<>();
                        argumentValues.put(argument, l);
                    }
                    l.add(param);
                } else {
                    argumentValues.put(argument, param);
                }
            }
        }
        // Check required arguments / options
        for (Option option : options.keySet()) {
            if (option.required() && optionValues.get(option) == null) {
                    throw new CommandException(commandErrorSt +
                            "option " + INTENSITY_BOLD + option.name() + INTENSITY_NORMAL + " is required",
                            "Option " + option.name() + " is required"
                    );
            }
        }
        for (Argument argument : orderedArguments) {
            if (argument.required() && argumentValues.get(argument) == null) {
                    throw new CommandException(commandErrorSt +
                            "argument " + INTENSITY_BOLD + argument.name() + INTENSITY_NORMAL + " is required",
                            "Argument " + argument.name() + " is required"
                    );
            }
        }
            
        // Convert and inject values
        for (Map.Entry<Option, Object> entry : optionValues.entrySet()) {
            Field field = options.get(entry.getKey());
            Object value;
            try {
                value = convert(action, entry.getValue(), field.getGenericType());
            } catch (Exception e) {
                    throw new CommandException(commandErrorSt +
                            "unable to convert option " + INTENSITY_BOLD + entry.getKey().name() + INTENSITY_NORMAL + " with value '"
                            + entry.getValue() + "' to type " + new GenericType(field.getGenericType()).toString(),
                            "Unable to convert option " + entry.getKey().name() + " with value '"
                                    + entry.getValue() + "' to type " + new GenericType(field.getGenericType()).toString(),
                            e
                    );
            }
            field.setAccessible(true);
            field.set(action, value);
        }
        for (Map.Entry<Argument, Object> entry : argumentValues.entrySet()) {
            Field field = arguments.get(entry.getKey());
            Object value;
            try {
                value = convert(action, entry.getValue(), field.getGenericType());
            } catch (Exception e) {
                    throw new CommandException(commandErrorSt +
                            "unable to convert argument " + INTENSITY_BOLD + entry.getKey().name() + INTENSITY_NORMAL + " with value '"
                            + entry.getValue() + "' to type " + new GenericType(field.getGenericType()).toString(),
                            "Unable to convert argument " + entry.getKey().name() + " with value '"
                                    + entry.getValue() + "' to type " + new GenericType(field.getGenericType()).toString(),
                            e
                    );
            }
            field.setAccessible(true);
            field.set(action, value);
        }
        return true;
    }

    protected Object convert(Action action, Object value, Type toType) throws Exception {
        if (toType == String.class) {
            return value != null ? value.toString() : null;
        }
        return new DefaultConverter(action.getClass().getClassLoader()).convert(value, toType);
    }

    private Argument replaceDefaultArgument(Field field, Argument argument) {
        if (Argument.DEFAULT.equals(argument.name())) {
            final Argument delegate = argument;
            final String name = field.getName();
            argument = new Argument() {
                public String name() {
                    return name;
                }

                public String description() {
                    return delegate.description();
                }

                public boolean required() {
                    return delegate.required();
                }

                public int index() {
                    return delegate.index();
                }

                public boolean multiValued() {
                    return delegate.multiValued();
                }

                public String valueToShowInHelp() {
                    return delegate.valueToShowInHelp();
                }

                public Class<? extends Annotation> annotationType() {
                    return delegate.annotationType();
                }

                @Override
                public boolean censor() {
                    return delegate.censor();
                }

                @Override
                public char mask() {
                    return delegate.mask();
                }
            };
        }
        return argument;
    }

    private void assertIndexesAreCorrect(Class<? extends Action> actionClass, List<Argument> orderedArguments) {
        for (int i = 0; i < orderedArguments.size(); i++) {
            if (orderedArguments.get(i) == null) {
                throw new IllegalArgumentException("Missing argument for index: " + i + " on Action " + actionClass.getName());
            }
        }
    }

    public void printUsage(Action action, Map<Option, Field> options, Map<Argument, Field> arguments, PrintStream out, boolean globalScope, int termWidth) {
        Command command = action.getClass().getAnnotation(Command.class);
        if (command != null) {
            List<Argument> argumentsSet = new ArrayList<>(arguments.keySet());
            argumentsSet.sort(Comparator.comparing(Argument::index));
            Set<Option> optionsSet = new HashSet<>(options.keySet());
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
            StringBuilder syntax = new StringBuilder();
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
                    printFormatted("                ", argument.description(), termWidth, out, true);
                    if (!argument.required()) {
                        if (argument.valueToShowInHelp() != null && argument.valueToShowInHelp().length() != 0) {
                            if (Argument.DEFAULT_STRING.equals(argument.valueToShowInHelp())) {
                                Object o = getDefaultValue(action, arguments.get(argument));
                                String defaultValue = getDefaultValueString(o);
                                if (defaultValue != null) {
                                    printDefaultsTo(out, defaultValue);
                                }
                            } else {
                                printDefaultsTo(out, argument.valueToShowInHelp());
                            }
                        }
                    } else {
                        printMeta(out, argument.required(), argument.multiValued());
                    }
                }
                out.println();
            }
            if (options.size() > 0) {
                out.println(INTENSITY_BOLD + "OPTIONS" + INTENSITY_NORMAL);
                for (Option option : optionsSet) {
                    StringBuilder opt = new StringBuilder(option.name());
                    for (String alias : option.aliases()) {
                        opt.append(", ").append(alias);
                    }
                    out.print("        ");
                    out.println(INTENSITY_BOLD + opt + INTENSITY_NORMAL);
                    printFormatted("                ", option.description(), termWidth, out, true);
                    if (option.valueToShowInHelp() != null && option.valueToShowInHelp().length() != 0) {
                        if (Option.DEFAULT_STRING.equals(option.valueToShowInHelp())) {
                            Object o = getDefaultValue(action, options.get(option));
                            String defaultValue = getDefaultValueString(o);
                            if (defaultValue != null) {
                                printDefaultsTo(out, defaultValue);
                            } else {
                              printMeta(out, option.required(), option.multiValued());
                            }
                        } else {
                            printDefaultsTo(out, option.valueToShowInHelp());
                        }
                    } else {
                        printMeta(out, option.required(), option.multiValued());
                    }
                }
                out.println();
            }
            if (command.detailedDescription().length() > 0) {
                out.println(INTENSITY_BOLD + "DETAILS" + INTENSITY_NORMAL);
                String desc = loadDescription(action.getClass(), command.detailedDescription());
                printFormatted("        ", desc, termWidth, out, true);
            }
        }
    }

    public Object getDefaultValue(Action action, Field field) {
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(action);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String loadDescription(Class<?> clazz, String desc) {
        if (desc != null && desc.startsWith("classpath:")) {
            desc = loadClassPathResource(clazz, desc.substring("classpath:".length()));
        }
        return desc;
    }

    public String getDefaultValueString(Object o) {
        if (o != null
                && (!(o instanceof Boolean) || ((Boolean) o))
                && (!(o instanceof Number) || ((Number) o).doubleValue() != 0.0)) {
            return o.toString();
        } else {
            return null;
        }
    }

    private void printDefaultsTo(PrintStream out, String value) {
        out.println("                (defaults to " + value + ")");
    }

    private void printMeta(PrintStream out, boolean required, boolean multivalued) {
        if (required || multivalued) {
            String text = "                (";
            if (required) {
                text += "required";
                if (multivalued) {
                    text += ", ";
                }
            }

            if (multivalued) {
                text += "multi-valued";
            }
            text += ")";
            out.println(text);
        }
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
