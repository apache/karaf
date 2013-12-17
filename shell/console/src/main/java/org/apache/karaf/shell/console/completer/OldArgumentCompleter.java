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
package org.apache.karaf.shell.console.completer;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.CommandWithAction;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.NameScoping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OldArgumentCompleter implements Completer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OldArgumentCompleter.class);

    public static final String ARGUMENTS_LIST = "ARGUMENTS_LIST";

    final Completer commandCompleter;
    final Completer optionsCompleter;
    final List<Completer> argsCompleters;
    final Map<String, Completer> optionalCompleters;
    final CommandWithAction function;
    final Map<Option, Field> fields = new HashMap<Option, Field>();
    final Map<String, Option> options = new HashMap<String, Option>();
    final Map<Integer, Field> arguments = new HashMap<Integer, Field>();
    boolean strict = true;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OldArgumentCompleter(CommandSession session, CommandWithAction function, String command) {
        this.function = function;
        // Command name completer
        commandCompleter = new StringsCompleter(getNames(session, command));
        // Build options completer
        for (Class<?> type = function.getActionClass(); type != null; type = type.getSuperclass()) {
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
//        options.put(HelpOption.HELP.name(), HelpOption.HELP);
        optionsCompleter = new StringsCompleter(options.keySet());
        // Build arguments completers
        argsCompleters = new ArrayList<Completer>();

        if (function instanceof CompletableFunction) {
            Map<String, Completer> opt;
            try {
                //
                opt = ((CompletableFunction) function).getOptionalCompleters();
            } catch (Throwable t) {
                opt = new HashMap<String, Completer>();
            }
            optionalCompleters = opt;
            List<Completer> fcl = ((CompletableFunction) function).getCompleters();
            if (fcl != null) {
                for (Completer c : fcl) {
                    argsCompleters.add(c == null ? NullCompleter.INSTANCE : c);
                }
            } else {
                argsCompleters.add(NullCompleter.INSTANCE);
            }
        } else {
            optionalCompleters = new HashMap<String, Completer>();
            final Map<Integer, Method> methods = new HashMap<Integer, Method>();
            for (Class<?> type = function.getActionClass(); type != null; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    CompleterValues completerMethod = method.getAnnotation(CompleterValues.class);
                    if (completerMethod != null) {
                        int index = completerMethod.index();
                        Integer key = index;
                        if (index >= arguments.size() || index < 0) {
                            LOGGER.warn("Index out of range on @CompleterValues on class " + type.getName() + " for index: " + key + " see: " + method);
                        }
                        if (methods.containsKey(key)) {
                            LOGGER.warn("Duplicate @CompleterMethod annotations on class " + type.getName() + " for index: " + key + " see: " + method);
                        } else {
                            methods.put(key, method);
                        }
                    }
                }
            }
            for (int i = 0, size = arguments.size(); i < size; i++) {
                Completer argCompleter = NullCompleter.INSTANCE;
                Method method = methods.get(i);
                if (method != null) {
                    // lets invoke the method
                    Action action = function.createNewAction();
                    try {
                        Object value = method.invoke(action);
                        if (value instanceof String[]) {
                            argCompleter = new StringsCompleter((String[]) value);
                        } else if (value instanceof Collection) {
                            argCompleter = new StringsCompleter((Collection<String>) value);
                        } else {
                            LOGGER.warn("Could not use value " + value + " as set of completions!");
                        }
                    } catch (IllegalAccessException e) {
                        LOGGER.warn("Could not invoke @CompleterMethod on " + function + ". " + e, e);
                    } catch (InvocationTargetException e) {
                        Throwable target = e.getTargetException();
                        if (target == null) {
                            target = e;
                        }
                        LOGGER.warn("Could not invoke @CompleterMethod on " + function + ". " + target, target);
                    } finally {
                        try {
                            function.releaseAction(action);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to release action: " + action + ". " + e, e);
                        }
                    }
                } else {
                    Field field = arguments.get(i);
                    Class<?> type = field.getType();
                    if (type.isAssignableFrom(File.class)) {
                        argCompleter = new FileCompleter(session);
                    } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class)) {
                        argCompleter = new StringsCompleter(new String[] {"false", "true"}, false);
                    } else if (type.isAssignableFrom(Enum.class)) {
                        Set<String> values = new HashSet<String>();
                        for (Object o : EnumSet.allOf((Class<Enum>) type)) {
                            values.add(o.toString());
                        }
                        argCompleter = new StringsCompleter(values, false);
                    } else {
                        // TODO any other completers we can add?
                    }
                }
                argsCompleters.add(argCompleter);
            }
        }
    }

    private String[] getNames(CommandSession session, String scopedCommand) {
        String command = NameScoping.getCommandNameWithoutGlobalPrefix(session, scopedCommand);
        String[] s = command.split(":");
        if (s.length == 1) {
            return s;
        } else {
            return new String[] { command, s[1] };
        }
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

    public int complete(final String buffer, final int cursor,
                        final List<String> candidates) {
        ArgumentList list = delimit(buffer, cursor);
        int argpos = list.getArgumentPosition();
        int argIndex = list.getCursorArgumentIndex();

        //Store the argument list so that it can be used by completers.
        CommandSession commandSession = CommandSessionHolder.getSession();
        if(commandSession != null) {
            commandSession.put(ARGUMENTS_LIST,list);
        }

        Completer comp = null;
        String[] args = list.getArguments();
        int index = 0;
        // First argument is command name
        if (index < argIndex) {
            // Verify command name
            if (!verifyCompleter(commandCompleter, args[index])) {
                return -1;
            }
            index++;
        } else {
            comp = commandCompleter;
        }
        // Now, check options
        if (comp == null) {
            while (index < argIndex && args[index].startsWith("-")) {
                if (!verifyCompleter(optionsCompleter, args[index])) {
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
                        if (optionalCompleters != null && name != null) {
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
                if (!verifyCompleter(sub, args[index])) {
                    return -1;
                }
                index++;
                indexArg++;
            }
            comp = argsCompleters.get(indexArg >= argsCompleters.size() ? argsCompleters.size() - 1 : indexArg);
        }

        int ret = comp.complete(list.getCursorArgument(), argpos, candidates);

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

    protected boolean verifyCompleter(Completer completer, String argument) {
        List<String> candidates = new ArrayList<String>();
        return completer.complete(argument, argument.length(), candidates) != -1 && !candidates.isEmpty();
    }

    public ArgumentList delimit(final String buffer, final int cursor) {
        Parser parser = new Parser(buffer, cursor);
        try {
            List<List<List<String>>> program = parser.program();
            List<String> pipe = program.get(parser.c0).get(parser.c1);
            return new ArgumentList(pipe.toArray(new String[pipe.size()]), parser.c2, parser.c3, cursor);
        } catch (Throwable t) {
            return new ArgumentList(new String[] { buffer }, 0, cursor, cursor);
        }
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

    /**
     *  The result of a delimited buffer.
     */
    public static class ArgumentList {
        private String[] arguments;
        private int cursorArgumentIndex;
        private int argumentPosition;
        private int bufferPosition;

        /**
         *  @param  arguments           the array of tokens
         *  @param  cursorArgumentIndex the token index of the cursor
         *  @param  argumentPosition    the position of the cursor in the
         *                              current token
         *  @param  bufferPosition      the position of the cursor in
         *                              the whole buffer
         */
        public ArgumentList(String[] arguments, int cursorArgumentIndex,
            int argumentPosition, int bufferPosition) {
            this.arguments = arguments;
            this.cursorArgumentIndex = cursorArgumentIndex;
            this.argumentPosition = argumentPosition;
            this.bufferPosition = bufferPosition;
        }

        public void setCursorArgumentIndex(int cursorArgumentIndex) {
            this.cursorArgumentIndex = cursorArgumentIndex;
        }

        public int getCursorArgumentIndex() {
            return this.cursorArgumentIndex;
        }

        public String getCursorArgument() {
            if ((cursorArgumentIndex < 0)
                || (cursorArgumentIndex >= arguments.length)) {
                return null;
            }

            return arguments[cursorArgumentIndex];
        }

        public void setArgumentPosition(int argumentPosition) {
            this.argumentPosition = argumentPosition;
        }

        public int getArgumentPosition() {
            return this.argumentPosition;
        }

        public void setArguments(String[] arguments) {
            this.arguments = arguments;
        }

        public String[] getArguments() {
            return this.arguments;
        }

        public void setBufferPosition(int bufferPosition) {
            this.bufferPosition = bufferPosition;
        }

        public int getBufferPosition() {
            return this.bufferPosition;
        }
    }
}
