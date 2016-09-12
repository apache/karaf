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
package org.apache.karaf.shell.commands.impl;

import org.apache.felix.gogo.api.Process;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileOrUriCompleter;
import org.jline.builtins.Source;
import org.jline.builtins.Source.StdInSource;
import org.jline.builtins.Source.URLSource;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(scope = "shell", name="grep", description="Prints lines matching the given pattern.", detailedDescription="classpath:grep.txt")
@Service
public class GrepAction implements Action {

    public static enum ColorOption {
        never,
        always,
        auto
    }

    @Argument(index = 0, name = "pattern", description = "Regular expression", required = true, multiValued = false)
    private String regex;

    @Argument(index = 1, multiValued = true)
    @Completion(FileOrUriCompleter.class)
    List<String> files;

    @Option(name = "-n", aliases = { "--line-number" }, description = "Prefixes each line of output with the line number within its input file.", required = false, multiValued = false)
    private boolean lineNumber;

    @Option(name = "-v", aliases = { "--invert-match" }, description = "Inverts the sense of matching, to select non-matching lines.", required = false, multiValued = false)
    private boolean invertMatch;

    @Option(name = "-w", aliases = { "--word-regexp" }, description = "Selects only those lines containing matches that form whole " +
                                                                      "words.  The test is that the matching substring must either be " +
                                                                      "at  the beginning of the line, or preceded by a non-word constituent " +
                                                                      "character.  Similarly, it must be either at the end of " +
                                                                      "the line or followed by a non-word constituent character.  " +
                                                                      "Word-constituent characters are letters, digits, and the underscore.", required = false, multiValued = false)
    private boolean wordRegexp;

    @Option(name = "-x", aliases = { "--line-regexp" }, description = "Selects only those matches that exactly match the whole line.", required = false, multiValued = false)
    private boolean lineRegexp;

    @Option(name = "-i", aliases = { "--ignore-case" }, description = "Ignores case distinctions in both the PATTERN and the input files.", required = false, multiValued = false)
    private boolean ignoreCase;

    @Option(name = "-c", aliases = { "--count" }, description = "only print a count of matching lines per FILE", required = false, multiValued = false)
    private boolean count;

    @Option(name = "--color", aliases = { "--colour" }, description = "use markers to distinguish the matching string. WHEN may be `always', `never' or `auto'", required = false, multiValued = false)
    private ColorOption color = ColorOption.auto;

    @Option(name = "-B", aliases = { "--before-context" }, description = "Print NUM lines of leading context before matching lines.  Places a line containing -- between contiguous groups of matches.", required = false, multiValued = false)
    private int before = -1;

    @Option(name = "-A", aliases = { "--after-context" }, description = "Print NUM lines of trailing context after matching lines.  Places a line containing -- between contiguous groups of matches.", required = false, multiValued = false)
    private int after = -1;

    @Option(name = "-C", aliases = { "--context" }, description = "Print NUM lines of output context.  Places a line containing -- between contiguous groups of matches.", required = false, multiValued = false)
    private int context = 0;

    @Option(name = "-o", aliases = { "--only-matching"}, description = "Print only the matching section of a line", required = false, multiValued = false)
    private boolean onlyMatching;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        if (color == ColorOption.auto && (Process.current() == null || !Process.current().isTty(1))) {
            color = ColorOption.never;
        }
        if (after < 0) {
            after = context;
        }
        if (before < 0) {
            before = context;
        }

        String regexp = regex;
        if (wordRegexp) {
            regex = regexp = "\\b" + regexp + "\\b";
        }
        if (lineRegexp) {
            regexp = "^" + regexp + "$";
        } else {
            regexp = ".*" + regexp + ".*";
        }
        Pattern p;
        Pattern p2;
        if (ignoreCase) {
            p = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
            p2 = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } else {
            p = Pattern.compile(regexp);
            p2 = Pattern.compile(regex);
        }

        List<Source> sources = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            files = Collections.singletonList("-");
        }
        Path pwd = Paths.get(System.getProperty("karaf.home", System.getProperty("user.dir")));
        for (String arg : files) {
            if ("-".equals(arg)) {
                sources.add(new StdInSource());
            } else {
                sources.add(new URLSource(pwd.toUri().resolve(arg).toURL(), arg));
            }
        }

        Terminal terminal = session != null ? (Terminal) session.get(".jline.terminal") : null;
        List<Object> output = new ArrayList<>();
        for (Source source : sources) {
            boolean firstPrint = true;
            int nb = 0;
            int lineno = 1;
            String line;
            int lineMatch = 0;
            List<String> lines = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(source.read()))) {
                while ((line = r.readLine()) != null) {
                    if (line.length() == 1 && line.charAt(0) == '\n') {
                        break;
                    }
                    if (p.matcher(line).matches() ^ invertMatch) {
                        AttributedStringBuilder sbl = new AttributedStringBuilder();
                        if (color != ColorOption.never) {
                            sbl.style(getSourceStyle());
                        }
                        if (!count && sources.size() > 1) {
                            sbl.append(source.getName());
                            sbl.append(":");
                        }
                        if (!count && lineNumber) {
                            sbl.append(String.format("%6d  ", lineno));
                        }
                        sbl.style(AttributedStyle.DEFAULT);
                        Matcher matcher2 = p2.matcher(line);
                        AttributedString aLine = AttributedString.fromAnsi(line);
                        AttributedStyle style;
                        if (!invertMatch && color != ColorOption.never) {
                            style = getMatchingStyle();
                        } else {
                            style = AttributedStyle.DEFAULT;
                        }
                        if (invertMatch) {
                            nb++;
                            sbl.append(aLine);
                        } else if (onlyMatching) {
                            while (matcher2.find()) {
                                int index = matcher2.start(0);
                                int cur = matcher2.end();
                                sbl.append(aLine.subSequence(index, cur), style);
                                nb++;
                            }
                        } else {
                            int cur = 0;
                            while (matcher2.find()) {
                                int index = matcher2.start(0);
                                AttributedString prefix = aLine.subSequence(cur, index);
                                sbl.append(prefix);
                                cur = matcher2.end();
                                sbl.append(aLine.subSequence(index, cur), style);
                                nb++;
                            }
                            sbl.append(aLine.subSequence(cur, aLine.length()));
                        }
                        lines.add(sbl.toAnsi(terminal));
                        lineMatch = lines.size();
                    } else {
                        if (lineMatch != 0 & lineMatch + after + before <= lines.size()) {
                            if (!count) {
                                if (!firstPrint && before + after > 0) {
                                    output.add("--");
                                } else {
                                    firstPrint = false;
                                }
                                for (int i = 0; i < lineMatch + after; i++) {
                                    output.add(lines.get(i));
                                }
                            }
                            while (lines.size() > before) {
                                lines.remove(0);
                            }
                            lineMatch = 0;
                        }
                        lines.add(line);
                        while (lineMatch == 0 && lines.size() > before) {
                            lines.remove(0);
                        }
                    }
                    lineno++;
                }
                if (!count && lineMatch > 0) {
                    if (!firstPrint && before + after > 0) {
                        output.add("--");
                    } else {
                        firstPrint = false;
                    }
                    for (int i = 0; i < lineMatch + after && i < lines.size(); i++) {
                        output.add(lines.get(i));
                    }
                }
                if (count) {
                    output.add(nb);
                }
            }
        }
        return output;
    }

    private AttributedStyle getSourceStyle() {
        return AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK + AttributedStyle.BRIGHT);
    }

    private AttributedStyle getMatchingStyle() {
        return AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
    }

}
