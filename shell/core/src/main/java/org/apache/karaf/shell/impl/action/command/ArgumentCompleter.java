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

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.ArgumentCommandLine;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.apache.karaf.shell.support.completers.NullCompleter;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.apache.karaf.shell.support.completers.UriCompleter;
import org.apache.karaf.shell.support.converter.GenericType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArgumentCompleter implements Completer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentCompleter.class);

    final ActionCommand command;
    final CandidateCompleter commandCompleter;
    final CandidateCompleter optionsCompleter;
    final List<Completer> argsCompleters;
    final Map<String, Completer> optionalCompleters;
    final Map<Option, Field> fields = new HashMap<>();
    final Map<String, Option> options = new HashMap<>();
    final Map<Integer, Field> arguments = new HashMap<>();

    @SuppressWarnings("rawtypes")
    public ArgumentCompleter(ActionCommand command, boolean scoped) {
        this.command = command;
        Class<?> actionClass = command.getActionClass();
        // Command name completer
        Command cmd = actionClass.getAnnotation(Command.class);
        String[] names = scoped || Session.SCOPE_GLOBAL.equals(cmd.scope()) ? new String[] { cmd.name() } : new String[] { cmd.name(), cmd.scope() + ":" + cmd.name() };
        commandCompleter = new CandidateCompleter();
        for (String name : names) {
            commandCompleter.addCandidate(name, cmd.description(), actionClass.getName());
        }
        // Build options completer
        for (Class<?> type = actionClass; type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Option option = field.getAnnotation(Option.class);
                if (option != null) {
                    fields.put(option, field);
                    options.put(option.name(), option);
                    String[] aliases = option.aliases();
                    if (aliases != null) {
                        for (String alias : aliases) {
                            options.put(alias, option);
                        }
                    }
                }
                Argument argument = field.getAnnotation(Argument.class);
                if (argument != null) {
                    Integer key = argument.index();
                    if (arguments.containsKey(key)) {
                        LOGGER.warn("Duplicate @Argument annotations on class " + type.getName() + " for index: " + key + " see: " + field);
                    } else {
                        arguments.put(key, field);
                    }
                }
            }
        }
        options.put(HelpOption.HELP.name(), HelpOption.HELP);

        optionsCompleter = new CandidateCompleter();
        for (Map.Entry<String, Option> entry : options.entrySet()) {
            optionsCompleter.addCandidate(
                    entry.getKey(),
                    entry.getValue().description(),
                    actionClass.getName() + "/" + entry.getValue().name());
        }

        argsCompleters = new ArrayList<>();

        boolean multi = false;
        for (int key = 0; key < arguments.size(); key++) {
            Completer completer = null;
            Field field = arguments.get(key);
            if (field != null) {
                Argument argument = field.getAnnotation(Argument.class);
                multi = (argument != null && argument.multiValued());
                Completion ann = field.getAnnotation(Completion.class);
                if (ann != null) {
                    Class<?> clazz = ann.value();
                    String[] value = ann.values();
                    if (value.length > 0) {
                        completer = new StringsCompleter(Arrays.asList(value), ann.caseSensitive());
                    } else {
                        completer = command.getCompleter(clazz);
                    }
                } else {
                    completer = getDefaultCompleter(field, multi);
                }
            }
            if (completer == null) {
                completer = NullCompleter.INSTANCE;
            }
            argsCompleters.add(completer);
        }
        if (argsCompleters.isEmpty() || !multi) {
            argsCompleters.add(NullCompleter.INSTANCE);
        }
        optionalCompleters = new HashMap<>();
        for (Option option : fields.keySet()) {
            Completer completer = null;
            Field field = fields.get(option);
            if (field != null) {
                Completion ann = field.getAnnotation(Completion.class);
                if (ann != null) {
                    try {
                        Class clazz = ann.value();
                        String[] value = ann.values();
                        if (value.length > 0) {
                            completer = new StringsCompleter(Arrays.asList(value), ann.caseSensitive());
                        } else {
                            completer = command.getCompleter(clazz);
                        }
                    } catch (Throwable t) {
                        // Ignore in case the completer class is not even available
                    }
                } else {
                    completer = getDefaultCompleter(field, option.multiValued());
                }
            }
            if (completer == null) {
                completer = NullCompleter.INSTANCE;
            }
            optionalCompleters.put(option.name(), completer);
            if (option.aliases() != null) {
                for (String alias : option.aliases()) {
                    optionalCompleters.put(alias, completer);
                }
            }
        }
    }

    @SuppressWarnings({
     "unchecked", "rawtypes"
    })
    private Completer getDefaultCompleter(Field field, boolean multi) {
        Completer completer = null;
        Class<?> type = field.getType();
        GenericType genericType = new GenericType(field.getGenericType());
        if (Collection.class.isAssignableFrom(genericType.getRawClass()) && multi) {
            type = genericType.getActualTypeArgument(0).getRawClass();
        }
        if (type.isAssignableFrom(URI.class)) {
            completer = new UriCompleter();
        } else if (type.isAssignableFrom(File.class)) {
            completer = new FileCompleter();
        } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class)) {
            completer = new StringsCompleter(Arrays.asList("false", "true"));
        } else if (Enum.class.isAssignableFrom(type)) {
            Set<String> values = new HashSet<>();
            for (Object o : EnumSet.allOf((Class<Enum>) type)) {
                values.add(o.toString());
            }
            completer = new StringsCompleter(values);
        }
        return completer;
    }

    public int complete(Session session, final CommandLine commandLine, final List<String> candidates) {
        List<Candidate> cands = new ArrayList<>();
        completeCandidates(session, commandLine, cands);
        for (Candidate cand : cands) {
            candidates.add(cand.value());
        }
        return 0;
    }

    @Override
    public void completeCandidates(Session session, final CommandLine list, List<Candidate> candidates) {
        int argIndex = list.getCursorArgumentIndex();

        Completer comp = null;
        String[] args = list.getArguments();
        int index = 0;
        // First argument is command name
        if (index < argIndex) {
            // Verify scope
            if (!Session.SCOPE_GLOBAL.equals(command.getScope()) && !session.resolveCommand(args[index]).equals(command.getScope() + ":" + command.getName())) {
                return;
            }
            // Verify command name
            if (!verifyCompleter(session, commandCompleter, args[index])) {
                return;
            }
            index++;
        } else {
            comp = commandCompleter;
        }
        // Now, check options
        if (comp == null) {
            while (index < argIndex && args[index].startsWith("-")) {
                if (!verifyCompleter(session, optionsCompleter, args[index])) {
                    return;
                }
                Option option = options.get(args[index]);
                if (option == null) {
                    return;
                }
                Field field = fields.get(option);
                if (field != null && field.getType() != boolean.class && field.getType() != Boolean.class) {
                    if (++index == argIndex) {
                        comp = NullCompleter.INSTANCE;
                    }
                }
                index++;
            }
            if (comp == null && index >= argIndex && index < args.length && args[index].startsWith("-")) {
                comp = optionsCompleter;
            }
        }
        // Now check for if last Option has a completer
        int lastAgurmentIndex = argIndex - 1;
        if (lastAgurmentIndex >= 1) {
            Option lastOption = options.get(args[lastAgurmentIndex]);
            if (lastOption != null) {
                Field lastField = fields.get(lastOption);
                if (lastField != null && lastField.getType() != boolean.class && lastField.getType() != Boolean.class) {
                    Option option = lastField.getAnnotation(Option.class);
                    if (option != null) {
                        Completer optionValueCompleter = null;
                        String name = option.name();
                        if (name != null) {
                            optionValueCompleter = optionalCompleters.get(name);
                            if (optionValueCompleter == null) {
                                String[] aliases = option.aliases();
                                if (aliases.length > 0) {
                                    for (int i = 0; i < aliases.length && optionValueCompleter == null; i++) {
                                        optionValueCompleter = optionalCompleters.get(option.aliases()[i]);
                                    }
                                }
                            }
                        }
                        if(optionValueCompleter != null) {
                            comp = optionValueCompleter;
                        }
                    }
                }
            }
        }

        // Check arguments
        if (comp == null) {
            int indexArg = 0;
            while (index < argIndex) {
                Completer sub = argsCompleters.get(indexArg >= argsCompleters.size() ? argsCompleters.size() - 1 : indexArg);
                if (!verifyCompleter(session, sub, args[index])) {
                    return;
                }
                index++;
                indexArg++;
            }
            comp = argsCompleters.get(indexArg >= argsCompleters.size() ? argsCompleters.size() - 1 : indexArg);
        }

        comp.completeCandidates(session, list, candidates);

        /* TODO:JLINE
        if (pos == -1) {
            return -1;
        }
        */

        /**
         *  Special case: when completing in the middle of a line, and the
         *  area under the cursor is a delimiter, then trim any delimiters
         *  from the candidates, since we do not need to have an extra
         *  delimiter.
         *
         *  E.g., if we have a completion for "foo", and we
         *  enter "f bar" into the buffer, and move to after the "f"
         *  and hit TAB, we want "foo bar" instead of "foo  bar".
         */

        /* TODO:JLINE
        String buffer = list.getBuffer();
        int cursor = list.getBufferPosition();
        if ((buffer != null) && (cursor != buffer.length()) && isDelimiter(buffer, cursor)) {
            for (int i = 0; i < candidates.size(); i++) {
                String val = candidates.get(i);

                while ((val.length() > 0)
                    && isDelimiter(val, val.length() - 1)) {
                    val = val.substring(0, val.length() - 1);
                }

                candidates.set(i, val);
            }
        }

        return pos;
        */
    }

    protected boolean verifyCompleter(Session session, Completer completer, String argument) {
        List<Candidate> candidates = new ArrayList<>();
        completer.completeCandidates(session, new ArgumentCommandLine(argument, argument.length()), candidates);
        return !candidates.isEmpty();
    }

    /**
     * Returns true if the specified character is a whitespace
     * parameter. Check to ensure that the character is not
     * escaped and returns true from
     * {@link #isDelimiterChar}.
     *
     * @param buffer the complete command buffer.
     * @param pos the index of the character in the buffer.
     * @return true if the character should be a delimiter, false else.
     */
    public boolean isDelimiter(final String buffer, final int pos) {
        return !isEscaped(buffer, pos) && isDelimiterChar(buffer, pos);
    }

    public boolean isEscaped(final String buffer, final int pos) {
        return pos > 0 && buffer.charAt(pos) == '\\' && !isEscaped(buffer, pos - 1);
    }

    /**
     * The character is a delimiter if it is whitespace, and the
     * preceding character is not an escape character.
     *
     * @param buffer the complete command buffer.
     * @param pos the index of the character in the buffer.
     * @return true if the character should be a delimiter, false else.
     */
    public boolean isDelimiterChar(String buffer, int pos) {
        return Character.isWhitespace(buffer.charAt(pos));
    }

    static class CandidateCompleter implements Completer {

        private final List<Candidate> candidates = new ArrayList<>();

        public void addCandidate(String value, String desc) {
            addCandidate(value, desc, null);
        }

        public void addCandidate(String value, String desc, String key) {
            if (desc.endsWith(".")) {
                desc = desc.substring(0, desc.length() - 1);
            }
            candidates.add(new Candidate(value, value, null, desc, null, key, true));
        }

        @Override
        public int complete(Session session, CommandLine commandLine, List<String> candidates) {
            List<Candidate> cands = new ArrayList<>();
            completeCandidates(session, commandLine, cands);
            for (Candidate cand : cands) {
                candidates.add(cand.value());
            }
            return 0;
        }

        @Override
        public void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
            candidates.addAll(this.candidates);
        }
    }

}
