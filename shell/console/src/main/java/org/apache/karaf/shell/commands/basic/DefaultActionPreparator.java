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
package org.apache.karaf.shell.commands.basic;

import static org.apache.karaf.shell.commands.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf.shell.commands.ansi.SimpleAnsi.COLOR_RED;
import static org.apache.karaf.shell.commands.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf.shell.commands.ansi.SimpleAnsi.INTENSITY_NORMAL;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandException;
import org.apache.karaf.shell.commands.HelpOption;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.commands.converter.DefaultConverter;
import org.apache.karaf.shell.commands.converter.GenericType;
import org.apache.karaf.shell.commands.meta.ActionMetaData;
import org.apache.karaf.shell.commands.meta.ActionMetaDataFactory;
import org.apache.karaf.shell.console.NameScoping;

public class DefaultActionPreparator implements ActionPreparator {

    public boolean prepare(Action action, CommandSession session, List<Object> params) throws Exception {
        ActionMetaData actionMetaData = new ActionMetaDataFactory().create(action.getClass());
        Map<Option, Field> options = actionMetaData.getOptions();
        Map<Argument, Field> arguments = actionMetaData.getArguments();
        List<Argument> orderedArguments = actionMetaData.getOrderedArguments();
        Command command2 = actionMetaData.getCommand();

        if (command2 == null) {
            // to avoid NPE with subshell
            return true;
        }

        String commandErrorSt = (command2 != null) ? COLOR_RED
                + "Error executing command " + command2.scope() + ":" 
                + INTENSITY_BOLD + command2.name() + INTENSITY_NORMAL
                + COLOR_DEFAULT + ": " : "";
        for (Iterator<Object> it = params.iterator(); it.hasNext(); ) {
            Object param = it.next();
            if (HelpOption.HELP.name().equals(param)) {
                int termWidth = getWidth(session);
                boolean globalScope = NameScoping.isGlobalScope(session, actionMetaData.getCommand().scope());
                actionMetaData.printUsage(action, System.out, globalScope, termWidth);
                return false;
            }
        }
        
        // Populate
        Map<Option, Object> optionValues = new HashMap<Option, Object>();
        Map<Argument, Object> argumentValues = new HashMap<Argument, Object>();
        boolean processOptions = true;
        int argIndex = 0;
        for (Iterator<Object> it = params.iterator(); it.hasNext(); ) {
            Object param = it.next();

            if (processOptions && param instanceof String && ((String) param).startsWith("-")) {
                boolean isKeyValuePair = ((String) param).indexOf('=') != -1;
                String name;
                Object value = null;
                if (isKeyValuePair) {
                    name = ((String) param).substring(0, ((String) param).indexOf('='));
                    value = ((String) param).substring(((String) param).indexOf('=') + 1);
                } else {
                    name = (String) param;
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
                                + "undefined option " + INTENSITY_BOLD + param + INTENSITY_NORMAL + "\n"
                                + "Try <command> --help' for more information.",
                                        "Undefined option: " + param);
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
                                + "missing value for option " + INTENSITY_BOLD + param + INTENSITY_NORMAL,
                                "Missing value for option: " + param
                        );
                }
                if (option.multiValued()) {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>) optionValues.get(option);
                    if (l == null) {
                        l = new ArrayList<Object>();
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
                        l = new ArrayList<Object>();
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
                value = convert(action, session, entry.getValue(), field.getGenericType());
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
                value = convert(action, session, entry.getValue(), field.getGenericType());
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

    protected Object convert(Action action, CommandSession session, Object value, Type toType) throws Exception {
        if (toType == String.class) {
            return value != null ? value.toString() : null;
        }
        return new DefaultConverter(action.getClass().getClassLoader()).convert(value, toType);
    }

    private int getWidth(CommandSession session) {
        Object cols = session.get("COLUMNS");
        return  (cols != null && cols instanceof Integer) ? (Integer)cols : 80;
    }
}
