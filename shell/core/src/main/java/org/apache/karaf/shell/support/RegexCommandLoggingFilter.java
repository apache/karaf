/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.shell.support;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.shell.api.console.CommandLoggingFilter;


/**
 *
 */
public class RegexCommandLoggingFilter implements CommandLoggingFilter {

    public static final String DEFAULT_REPLACEMENT = "*****";

    private static class ReplaceRegEx {
        private Pattern pattern;
        private int group=1;
        private String replacement;

        public ReplaceRegEx(String pattern, int group, String replacement) {
            this.pattern = Pattern.compile(";* *"+pattern);
            this.group = group;
            this.replacement = replacement;
        }

        public CharSequence filter(CharSequence command) {
            Matcher m = pattern.matcher(command);
            int offset = 0;
            while( m.find() ) {
                int origLen = command.length();
                command = new StringBuilder(command).replace(m.start(group)+offset, m.end(group)+offset, replacement).toString();
                offset += command.length() - origLen;
            }
            return command;
        }
    }

    private String pattern;
    private int group=1;
    private String replacement=DEFAULT_REPLACEMENT;

    ArrayList<ReplaceRegEx> regexs = new ArrayList<>();

    public CharSequence filter(CharSequence command) {
        if( pattern!=null ) {
            command = new ReplaceRegEx(pattern, group, replacement).filter(command);
        }
        for (ReplaceRegEx regex : regexs) {
            command = regex.filter(command);
        }
        return command;
    }

    public void addRegEx(String pattern) {
        addRegEx(pattern, 1);
    }
    public void addRegEx(String pattern, int group) {
        addRegEx(pattern, group, DEFAULT_REPLACEMENT);
    }

    public void addRegEx(String pattern, int group, String replacement) {
        regexs.add(new ReplaceRegEx(pattern, group, replacement));
    }

    public void addCommandOption(String option, String...commands) {
        StringBuilder pattern = new StringBuilder("(");
        for (String command : commands) {
            if( pattern.length() > 1 ) {
                pattern.append("|");
            }
            pattern.append(Pattern.quote(command));
        }
        pattern.append(") +.*?").append(Pattern.quote(option)).append(" +([^ ]+)");
        regexs.add(new ReplaceRegEx(pattern.toString(), 2, DEFAULT_REPLACEMENT));
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }
}
