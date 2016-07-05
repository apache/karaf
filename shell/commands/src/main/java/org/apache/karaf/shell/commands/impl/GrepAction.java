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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    @Override
    public Object execute() throws Exception {
        if (after < 0) {
            after = context;
        }
        if (before < 0) {
            before = context;
        }
        List<String> lines = new ArrayList<>();

        String regexp = regex;
        if (wordRegexp) {
            regexp = "\\b" + regexp + "\\b";
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
        try (BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
            if (count) {
                printNumberOfLines(p, r.lines());
            } else {
                boolean firstPrint = true;
                int lineno = 1;
                String line;
                int lineMatch = 0;
                while ((line = r.readLine()) != null) {
                    if (line.length() >= 1 && line.charAt(0) != '\n') {
                        if (p.matcher(line).matches() ^ invertMatch) {
                            lines.add(matchXorInvertedMatch(line, p2, lineno));
                            lineMatch = lines.size();
                        } else {
                            if (lineMatch != 0 & lineMatch + after + before <= lines.size()) {
                                if (!firstPrint && before + after > 0) {
                                    System.out.println("--");
                                } else {
                                    firstPrint = false;
                                }
                                for (int i = 0; i < lineMatch + after; i++) {
                                    System.out.println(lines.get(i));
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
                }
                if (lineMatch > 0) {
                    if (!firstPrint && before + after > 0) {
                        System.out.println("--");
                    }
                    for (int i = 0; i < lineMatch + after && i < lines.size(); i++) {
                        System.out.println(lines.get(i));
                    }
                }
            }
        }
        return null;
    }

    private String matchXorInvertedMatch(final String line, final Pattern p2, final int lineno) {
        final String result;
        if (!invertMatch && color != ColorOption.never) {
            result = grepColoured(line, p2);
        } else {
            result = grepNotColoured(line, p2);
        }
        return lineNumber ? String.format("%d:%s", lineno, result) : result;
    }

    private String grepColoured(final String line, final Pattern p2) {
        Matcher matcher2 = p2.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (matcher2.find()) {
            if (!invertMatch && color != ColorOption.never) {
                matcher2.appendReplacement(sb, "\u001b[33;40m"
					   + matcher2.group()
					   + "\u001b[39;49m");
            } else {
                matcher2.appendReplacement(sb, matcher2.group());
            }
        }
        matcher2.appendTail(sb);
        return sb.toString();
    }

    private String grepNotColoured(final String line, final Pattern p2) {
        Matcher matcher2 = p2.matcher(line);
        StringBuffer sb = new StringBuffer();
        if (!onlyMatching) {
            while (matcher2.find()) {
                matcher2.appendReplacement(sb, matcher2.group());
            }
        } else if (!invertMatch) {
            int start = 0;
            int end = 0;
            boolean found = false;
            if (matcher2.find()) {
                found = true;
                start = matcher2.start();
                end = matcher2.end();
            }
            while (matcher2.find()) {
                end = matcher2.end();
            }
            if (found) {
                final String only = line.substring(start, end);
                sb.append(only);
            }
        }
        if (!onlyMatching) {
            matcher2.appendTail(sb);
        }
        return sb.toString();
    }

    private void printNumberOfLines(Pattern p, final Stream<String> lines) {
        final long numberOfLines = lines
            .filter(line -> line.length() > 1
                || line.length() == 1 && line.charAt(0) != '\n')
            .filter(line -> p.matcher(line).matches() ^ invertMatch)
            .count();
        System.out.println(numberOfLines);
    }

}
