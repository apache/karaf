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
package org.apache.karaf.shell.impl.console;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.jline.Builtin;
import org.apache.felix.gogo.jline.Posix;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.jline.builtins.Options;

/**
 * Gogo's file built-ins ({@code cat}, {@code grep}, {@code ls}, {@code tac}...) open any file, directory or URL given
 * as argument, with the permissions of the Karaf process. These subclasses pass each of them to a {@link Check} before
 * the built-in opens it. Standard input isn't checked, so piping the output of a command into {@code grep},
 * {@code less} or {@code wc} keeps working for every user.
 */
final class FileAccessGuard {

    /**
     * Checks that the current user can access a file, directory or URL.
     */
    @FunctionalInterface
    interface Check {
        /**
         * @param command the built-in, for example {@code cat}
         * @param location the absolute URI of the file, directory or URL
         * @throws SecurityException to refuse access
         */
        void check(CommandSession session, String command, String location);
    }

    // Posix built-ins that take no file argument. Every other built-in has all its arguments checked.
    private static final Set<String> NO_FILE_ARGS = set("echo", "sleep", "pwd", "clear", "date", "tmux", "ttop", "watch");
    // Posix built-ins that read standard input for a "-" argument
    private static final Set<String> STDIN_DASH = set("cat", "head", "tail", "wc", "less", "grep");
    // Posix built-ins that open their arguments as URLs resolved against the current directory, not as paths
    private static final Set<String> URL_ARGS = set("cat", "sort");

    private FileAccessGuard() {
    }

    static class GuardedPosix extends Posix {

        private final Check check;

        GuardedPosix(CommandProcessor processor, Check check) {
            super(processor);
            this.check = check;
        }

        // Each built-in parses its arguments here, before it opens anything
        @Override
        protected Options parseOptions(CommandSession session, String[] usage, Object[] argv) throws Exception {
            Options opt = super.parseOptions(session, usage, argv);
            String command = String.valueOf(argv[0]);
            if (NO_FILE_ARGS.contains(command)) {
                return opt;
            }
            List<String> args = opt.args();
            if ("grep".equals(command) && !args.isEmpty()) {
                // grep PATTERN [FILES]
                args = args.subList(1, args.size());
            } else if ("ls".equals(command) && args.isEmpty()) {
                // ls lists the current directory
                args = Collections.singletonList(".");
            }
            for (String arg : args) {
                if (!("-".equals(arg) && STDIN_DASH.contains(command))) {
                    check.check(session, command, location(session, command, arg));
                }
            }
            return opt;
        }
    }

    static class GuardedBuiltin extends Builtin {

        // Same as Builtin.tac(), so that the arguments are parsed the same way
        private static final String[] TAC_USAGE = {
                "tac - capture stdin as String or List and optionally write to file.",
                "Usage: tac [-al] [FILE]",
                "  -a --append              append to FILE",
                "  -l --list                return List<String>",
                "  -? --help                show help"};

        private final Check check;

        GuardedBuiltin(Check check) {
            this.check = check;
        }

        // tac FILE writes its standard input to FILE
        @Override
        public Object tac(CommandSession session, String[] argv) throws IOException {
            for (String arg : Options.compile(TAC_USAGE).parse(argv).args()) {
                check.check(session, "tac", location(session, "tac", arg));
            }
            return super.tac(session, argv);
        }
    }

    // The location the built-in opens, made absolute and normalized so that ACL rules can match it.
    // A local file gets the same location whichever built-in opens it.
    static String location(CommandSession session, String command, String arg) {
        try {
            Path cwd = session.currentDir();
            Path path;
            if (URL_ARGS.contains(command)) {
                URI uri = cwd.toUri().resolve(arg).normalize();
                if (!"file".equalsIgnoreCase(uri.getScheme())) {
                    return uri.toString();
                }
                path = Paths.get(uri);
            } else {
                path = cwd.resolve(arg);
            }
            return path.toAbsolutePath().normalize().toUri().toString();
        } catch (RuntimeException e) {
            // The built-in fails on it too, but check what was asked for anyway
            return arg;
        }
    }

    private static Set<String> set(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }
}
