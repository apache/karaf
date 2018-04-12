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
package org.apache.karaf.shell.support.completers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * <p>A file name completer takes the buffer and issues a list of
 * potential completions.</p>
 *
 * <p>This completer tries to behave as similar as possible to
 * <i>bash</i>'s file name completion (using GNU readline)
 * with the following exceptions:</p>
 *
 * <ul>
 * <li>Candidates that are directories will end with "/"</li>
 * <li>Wildcard regular expressions are not evaluated or replaced</li>
 * <li>The "~" character can be used to represent the user's home,
 * but it cannot complete to other users' homes, since java does
 * not provide any way of determining that easily</li>
 * </ul>
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class FileCompleter implements Completer
{
    private static String OS = System.getProperty("os.name").toLowerCase();

    // TODO: Handle files with spaces in them

    private static final boolean OS_IS_WINDOWS = isWindows();
    
    public static boolean isWindows() {
        return OS.contains("win");
    }

    public int complete(final Session session, CommandLine commandLine, final List<String> candidates) {
        throw new UnsupportedOperationException();
    }

    public void completeCandidates(final Session session, CommandLine commandLine, final List<Candidate> candidates) {
        // buffer can be null
        if (candidates == null) {
            return;
        }

        String buffer = commandLine.getCursorArgument().substring(0, commandLine.getArgumentPosition());
        if (OS_IS_WINDOWS) {
            buffer = buffer.replaceAll("/", File.separator);
        }

        Terminal terminal = (Terminal) session.get(".jline.terminal");

        Path current;
        String curBuf;
        int lastSep = buffer.lastIndexOf(separator());
        if (lastSep >= 0) {
            curBuf = buffer.substring(0, lastSep + 1);
            if (curBuf.startsWith("~")) {
                if (curBuf.startsWith("~" + separator())) {
                    current = getUserHome().resolve(curBuf.substring(2));
                } else {
                    current = getUserHome().getParent().resolve(curBuf.substring(1));
                }
            } else {
                current = getUserDir().resolve(curBuf);
            }
        } else {
            curBuf = "";
            current = getUserDir();
        }
        try {
            Files.newDirectoryStream(current, this::accept).forEach(p -> {
                String value = curBuf + p.getFileName().toString();
                if (Files.isDirectory(p)) {
                    String s = OS_IS_WINDOWS ? "\\\\" : "/";
                    candidates.add(new Candidate(
                            value + s,
                            getDisplay(terminal, p),
                            null, null,
                            s,
                            null,
                            false));
                } else {
                    candidates.add(new Candidate(value, getDisplay(terminal, p),
                            null, null, null, null, true));
                }
            });
        } catch (IOException e) {
            // Ignore
        }
    }

    protected boolean accept(Path path) {
        try {
            return !Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }

    protected String separator() {
        return File.separator;
    }

    protected Path getUserDir() {
        return Paths.get(System.getProperty("user.dir"));
    }

    protected Path getUserHome() {
        return Paths.get(System.getProperty("user.home"));
    }

    protected String getDisplay(Terminal terminal, Path p) {
        // TODO: use $LS_COLORS for output
        String name = p.getFileName().toString();
        if (Files.isDirectory(p)) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
            sb.append(name);
            sb.style(AttributedStyle.DEFAULT);
            sb.append(OS_IS_WINDOWS ? "\\\\" : "/");
            name = sb.toAnsi(terminal);
        } else if (Files.isSymbolicLink(p)) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
            sb.append(name);
            sb.style(AttributedStyle.DEFAULT);
            sb.append("@");
            name = sb.toAnsi(terminal);
        }
        return name;
    }

}
