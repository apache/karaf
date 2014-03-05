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
import java.util.ArrayList;
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
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.apache.karaf.shell.support.completers.NullCompleter;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArgumentCompleter implements Completer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentCompleter.class);

    final ActionCommand command;
    final Completer commandCompleter;
    final Completer optionsCompleter;
    final List<Completer> argsCompleters;
    final Map<String, Completer> optionalCompleters;
    final Map<Option, Field> fields = new HashMap<Option, Field>();
    final Map<String, Option> options = new HashMap<String, Option>();
    final Map<Integer, Field> arguments = new HashMap<Integer, Field>();
    boolean strict = true;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArgumentCompleter(ActionCommand command, boolean scoped) {
        this.command = command;
        Class<?> actionClass = command.getActionClass();
        // Command name completer
        Command cmd = actionClass.getAnnotation(Command.class);
        String[] names = scoped || Session.SCOPE_GLOBAL.equals(cmd.scope()) ? new String[] { cmd.name() } : new String[] { cmd.name(), cmd.scope() + ":" + cmd.name() };
        commandCompleter = new StringsCompleter(names);
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

        argsCompleters = new ArrayList<Completer>();
        optionsCompleter = new StringsCompleter(options.keySet());

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
                    if (clazz != null) {
                        if (value.length > 0 && clazz == StringsCompleter.class) {
                            completer = new StringsCompleter(value, ann.caseSensitive());
                        } else {
                            completer = command.getCompleter(clazz);
                        }
                    }
                } else {
                    completer = getDefaultCompleter(field);
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
        optionalCompleters = new HashMap<String, Completer>();
        for (Option option : fields.keySet()) {
            Completer completer = null;
            Field field = fields.get(option);
            if (field != null) {
                Completion ann = field.getAnnotation(Completion.class);
                if (ann != null) {
                    Class clazz = ann.value();
                    String[] value = ann.values();
                    if (clazz != null) {
                        if (clazz == StringsCompleter.class) {
                            completer = new StringsCompleter(value, ann.caseSensitive());
                        } else {
                            completer = command.getCompleter(clazz);
                        }
                    }
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

    private Completer getDefaultCompleter(Field field) {
        Completer completer = null;
        Class<?> type = field.getType();
        if (type.isAssignableFrom(File.class)) {
            completer = new FileCompleter();
        } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class)) {
            completer = new StringsCompleter(new String[] {"false", "true"}, false);
        } else if (type.isAssignableFrom(Enum.class)) {
            Set<String> values = new HashSet<String>();
            for (Object o : EnumSet.allOf((Class<Enum>) type)) {
                values.add(o.toString());
            }
            completer = new StringsCompleter(values, false);
        } else {
            // TODO any other completers we can add?
        }
        return completer;
    }

    /**
     *  If true, a completion at argument index N will only succeed
     *  if all the completions from 0-(N-1) also succeed.
     */
    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    /**
     *  Returns whether a completion at argument index N will succees
     *  if all the completions from arguments 0-(N-1) also succeed.
     */
    public boolean getStrict() {
        return this.strict;
    }

    public int complete(Session session, final CommandLine list, final List<String> candidates) {
        int argpos = list.getArgumentPosition();
        int argIndex = list.getCursorArgumentIndex();

        Completer comp = null;
        String[] args = list.getArguments();
        int index = 0;
        // First argument is command name
        if (index < argIndex) {
            // Verify scope
            if (!Session.SCOPE_GLOBAL.equals(command.getScope()) && !session.resolveCommand(args[index]).equals(command.getScope() + ":" + command.getName())) {
                return -1;
            }
            // Verify command name
            if (!verifyCompleter(session, commandCompleter, args[index])) {
                return -1;
            }
            index++;
        } else {
            comp = commandCompleter;
        }
        // Now, check options
        if (comp == null) {
            while (index < argIndex && args[index].startsWith("-")) {
                if (!verifyCompleter(session, optionsCompleter, args[index])) {
                    return -1;
                }
                Option option = options.get(args[index]);
                if (option == null) {
                    return -1;
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
        //Now check for if last Option has a completer
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
                    return -1;
                }
                index++;
                indexArg++;
            }
            comp = argsCompleters.get(indexArg >= argsCompleters.size() ? argsCompleters.size() - 1 : indexArg);
        }

        int ret = comp.complete(session, new ArgumentCommandLine(list.getCursorArgument(), argpos), candidates);

        if (ret == -1) {
            return -1;
        }

        int pos = ret + (list.getBufferPosition() - argpos);

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
    }

    protected boolean verifyCompleter(Session session, Completer completer, String argument) {
        List<String> candidates = new ArrayList<String>();
        return completer.complete(session, new ArgumentCommandLine(argument, argument.length()), candidates) != -1 && !candidates.isEmpty();
    }

    /**
     *  Returns true if the specified character is a whitespace
     *  parameter. Check to ensure that the character is not
     *  escaped and returns true from
     *  {@link #isDelimiterChar}.
     *
     *  @param  buffer the complete command buffer
     *  @param  pos    the index of the character in the buffer
     *  @return        true if the character should be a delimiter
     */
    public boolean isDelimiter(final String buffer, final int pos) {
        return !isEscaped(buffer, pos) && isDelimiterChar(buffer, pos);
    }

    public boolean isEscaped(final String buffer, final int pos) {
        return pos > 0 && buffer.charAt(pos) == '\\' && !isEscaped(buffer, pos - 1);
    }

    /**
     *  The character is a delimiter if it is whitespace, and the
     *  preceeding character is not an escape character.
     */
    public boolean isDelimiterChar(String buffer, int pos) {
        return Character.isWhitespace(buffer.charAt(pos));
    }

    static class ArgumentCommandLine implements CommandLine {
        private final String argument;
        private final int position;

        ArgumentCommandLine(String argument, int position) {
            this.argument = argument;
            this.position = position;
        }

        @Override
        public int getCursorArgumentIndex() {
            return 0;
        }

        @Override
        public String getCursorArgument() {
            return argument;
        }

        @Override
        public int getArgumentPosition() {
            return position;
        }

        @Override
        public String[] getArguments() {
            return new String[] { argument };
        }

        @Override
        public int getBufferPosition() {
            return position;
        }

        @Override
        public String getBuffer() {
            return argument;
        }
    }
}
