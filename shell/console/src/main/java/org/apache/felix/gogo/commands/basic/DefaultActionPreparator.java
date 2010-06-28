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
package org.apache.felix.gogo.commands.basic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.io.PrintStream;

import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.basic.ActionPreparator;
import org.apache.felix.gogo.commands.converter.DefaultConverter;
import org.osgi.service.command.CommandSession;

public class DefaultActionPreparator implements ActionPreparator {

    protected static final Option HELP = new Option() {
        public String name()
        {
            return "--help";
        }

        public String[] aliases()
        {
            return new String[] { };
        }

        public String description()
        {
            return "Display this help message";
        }

        public boolean required()
        {
            return false;
        }

        public boolean multiValued()
        {
            return false;
        }

        public Class<? extends Annotation> annotationType()
        {
            return Option.class;
        }
    };

    public boolean prepare(Action action, CommandSession session, List<Object> params) throws Exception
    {
        Map<Option, Field> options = new HashMap<Option, Field>();
        Map<Argument, Field> arguments = new HashMap<Argument, Field>();
        List<Argument> orderedArguments = new ArrayList<Argument>();
        // Introspect
        for (Class type = action.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Option option = field.getAnnotation(Option.class);
                if (option != null) {
                    options.put(option, field);
                }
                Argument argument = field.getAnnotation(Argument.class);
                if (argument != null) {
                    arguments.put(argument, field);
                    int index = argument.index();
                    while (orderedArguments.size() <= index) {
                        orderedArguments.add(null);
                    }
                    if (orderedArguments.get(index) != null) {
                        throw new IllegalArgumentException("Duplicate argument index: " + index);
                    }
                    orderedArguments.set(index, argument);
                }
            }
        }
        // Check indexes are correct
        for (int i = 0; i < orderedArguments.size(); i++) {
            if (orderedArguments.get(i) == null) {
                throw new IllegalArgumentException("Missing argument for index: " + i);
            }
        }
        // Populate
        Map<Option, Object> optionValues = new HashMap<Option, Object>();
        Map<Argument, Object> argumentValues = new HashMap<Argument, Object>();
        boolean processOptions = true;
        int argIndex = 0;
        for (Iterator<Object> it = params.iterator(); it.hasNext();) {
            Object param = it.next();
            // Check for help
            if (HELP.name().equals(param) || Arrays.asList(HELP.aliases()).contains(param)) {
                printUsage(session, action.getClass().getAnnotation(Command.class), options.keySet(), arguments.keySet(), System.out);
                return false;
            }
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
                    if (name.equals(opt.name()) || Arrays.binarySearch(opt.aliases(), name) >= 0) {
                        option = opt;
                        break;
                    }
                }
                if (option == null) {
                    throw new IllegalArgumentException("Undefined option: " + param);
                }
                Field field = options.get(option);
                if (value == null && (field.getType() == boolean.class || field.getType() == Boolean.class)) {
                    value = Boolean.TRUE;
                }
                if (value == null && it.hasNext()) {
                    value = it.next();
                }
                if (value == null) {
                    throw new IllegalArgumentException("Missing value for option " + param);
                }
                if (option.multiValued()) {
                    List<Object> l = (List<Object>) optionValues.get(option);
                    if (l == null) {
                        l = new ArrayList<Object>();
                        optionValues.put(option,  l);
                    }
                    l.add(value);
                } else {
                    optionValues.put(option, value);
                }
            } else {
                processOptions = false;
                if (argIndex >= orderedArguments.size()) {
                    throw new IllegalArgumentException("Too many arguments specified");
                }
                Argument argument = orderedArguments.get(argIndex);
                if (!argument.multiValued()) {
                    argIndex++;
                }
                if (argument.multiValued()) {
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
                throw new IllegalArgumentException("Option " + option.name() + " is required");
            }
        }
        for (Argument argument : arguments.keySet()) {
            if (argument.required() && argumentValues.get(argument) == null) {
                throw new IllegalArgumentException("Argument " + argument.name() + " is required");
            }
        }
        // Convert and inject values
        for (Map.Entry<Option, Object> entry : optionValues.entrySet()) {
            Field field = options.get(entry.getKey());
            Object value = convert(action, session, entry.getValue(), field.getGenericType());
            field.setAccessible(true);
            field.set(action, value);
        }
        for (Map.Entry<Argument, Object> entry : argumentValues.entrySet()) {
            Field field = arguments.get(entry.getKey());
            Object value = convert(action, session, entry.getValue(), field.getGenericType());
            field.setAccessible(true);
            field.set(action, value);
        }
        return true;
    }

    protected void printUsage(CommandSession session, Command command, Set<Option> options, Set<Argument> arguments, PrintStream out)
    {
        options = new HashSet<Option>(options);
        options.add(HELP);
        if (command != null && command.description() != null && command.description().length() > 0)
        {
            out.println(command.description());
            out.println();
        }
        String syntax = "syntax: ";
        if (command != null)
        {
            syntax += command.scope() + ":" + command.name();
        }
        if (options.size() > 0)
        {
            syntax += " [options]";
        }
        if (arguments.size() > 0)
        {
            syntax += " [arguments]";
        }
        out.println(syntax);
        out.println();
        if (arguments.size() > 0)
        {
            out.println("arguments:");
            for (Argument argument : arguments)
            {
                out.print("  ");
                out.print(argument.name());
                out.print("  ");
                out.print(argument.description());
                out.println();
            }
            out.println();
        }
        if (options.size() > 0)
        {
            out.println("options:");
            for (Option option : options)
            {
                out.print("  ");
                out.print(option.name());
                out.print("  ");
                if (option.aliases().length > 0)
                {
                    out.print("(");
                    out.print(Arrays.toString(option.aliases()));
                    out.print(")  ");
                }
                out.print(option.description());
                out.println();
            }
            out.println();
        }
    }

    protected Object convert(Action action, CommandSession session, Object value, Type toType) throws Exception
    {
        return new DefaultConverter(action.getClass().getClassLoader()).convert(value, toType);
    }

}
