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
package org.apache.karaf.shell.impl.action.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Option;
import org.jline.reader.ParsedLine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActionMaskingCallback {

    private final ActionCommand command;
    private final Set<String> booleanOptions;
    private final Map<String, Option> typedOptions;
    private final List<Argument> arguments;

    public static ActionMaskingCallback build(ActionCommand command) {
        Set<String> booleanOptions = new HashSet<>();
        Map<String, Option> typedOptions = new HashMap<>();
        List<Argument> arguments = new ArrayList<>();
        boolean censor = false;
        for (Class<?> type = command.getActionClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Option option = field.getAnnotation(Option.class);
                if (option != null) {
                    if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        booleanOptions.add(option.name());
                        booleanOptions.addAll(Arrays.asList(option.aliases()));
                    } else {
                        typedOptions.put(option.name(), option);
                        Arrays.asList(option.aliases()).forEach(alias -> typedOptions.put(alias, option));
                        censor |= option.censor();
                    }
                }
                Argument argument = field.getAnnotation(Argument.class);
                if (argument != null) {
                    arguments.add(argument);
                    censor |= argument.censor();
                }
            }
        }
        arguments.sort(Comparator.comparing(Argument::index));
        return censor ? new ActionMaskingCallback(command, booleanOptions, typedOptions, arguments) : null;
    }

    private ActionMaskingCallback(ActionCommand command, Set<String> booleanOptions, Map<String, Option> typedOptions, List<Argument> arguments) {
        this.command = command;
        this.booleanOptions = booleanOptions;
        this.typedOptions = typedOptions;
        this.arguments = arguments;
    }

    public String filter(String line, ParsedLine parsed) {
        int prev = 0;
        StringBuilder sb = new StringBuilder();
        int cur = line.indexOf(parsed.line());
        List<String> words = parsed.words();
        int state = 0;
        int arg = 0;
        for (int word = 0; word < words.size(); word++) {
            String wordStr = words.get(word);
            switch (state) {
                // command
                case 0:
                    cur = line.indexOf(wordStr, cur) + wordStr.length();
                    state++;
                    break;
                // option
                case 1:
                    if (wordStr.startsWith("-")) {
                        int idxEq = wordStr.indexOf('=');
                        if (idxEq > 0) {
                            String name = wordStr.substring(0, idxEq);
                            if (booleanOptions.contains(name)) {
                                break;
                            }
                            Option option = typedOptions.get(name);
                            if (option != null && option.censor()) {
                                cur = line.indexOf(wordStr, cur);
                                sb.append(line, prev, cur + idxEq + 1);
                                for (int i = idxEq + 1; i < wordStr.length(); i++) {
                                    sb.append(option.mask());
                                }
                                prev = cur = cur + wordStr.length();
                            }
                        } else {
                            String name = wordStr;
                            if (booleanOptions.contains(name)) {
                                break;
                            }
                            Option option = typedOptions.get(name);
                            if (option != null) {
                                // skip value
                                word++;
                                if (option.censor() && word < words.size()) {
                                    String val = words.get(word);
                                    cur = line.indexOf(val, cur);
                                    sb.append(line, prev, cur);
                                    for (int i = 0; i < val.length(); i++) {
                                        sb.append(option.mask());
                                    }
                                    prev = cur = cur + val.length();
                                }
                            }
                        }
                        break;
                    } else {
                        state = 2;
                        // fall through
                    }
                // argument
                case 2:
                    if (arg < arguments.size()) {
                        Argument argument = arguments.get(arg);
                        if (argument.censor()) {
                            cur = line.indexOf(wordStr, cur);
                            sb.append(line, prev, cur);
                            for (int i = 0; i < wordStr.length(); i++) {
                                sb.append(argument.mask());
                            }
                            prev = cur = cur + wordStr.length();
                        }
                        if (!argument.multiValued()) {
                            arg++;
                        }
                    }
                    break;
            }
        }
        if (prev < line.length()) {
            sb.append(line, prev, line.length());
        }
        return sb.toString();
    }

}
