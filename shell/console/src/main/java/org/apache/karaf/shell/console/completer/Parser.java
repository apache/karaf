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
// DWB14: parser loops if // comment at start of program
// DWB15: allow program to have trailing ';'
package org.apache.karaf.shell.console.completer;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    int current = 0;
    String text;
    boolean escaped;
    static final String SPECIAL = "<;|{[\"'$`(=";

    List<List<List<String>>> program;
    List<List<String>> statements;
    List<String> statement;
    int cursor;
    int start = -1;
    int c0;
    int c1;
    int c2;
    int c3;

    public Parser(String text, int cursor) {
        this.text = text;
        this.cursor = cursor;
    }

    void ws() {
        // derek: BUGFIX: loop if comment  at beginning of input
        //while (!eof() && Character.isWhitespace(peek())) {
        while (!eof() && (!escaped && Character.isWhitespace(peek()) || current == 0)) {
            if (current != 0 || !escaped && Character.isWhitespace(peek())) {
                current++;
            }
            if (peek() == '/' && current < text.length() - 2
                && text.charAt(current + 1) == '/') {
                comment();
            }
            if (current == 0) {
                break;
            }
        }
    }

    private void comment() {
        while (!eof() && peek() != '\n' && peek() != '\r') {
            next();
        }
    }

    boolean eof() {
        return current >= text.length();
    }

    char peek() {
        return peek(false);
    }

    char peek(boolean increment) {
        escaped = false;
        if (eof()) {
            return 0;
        }

        int last = current;
        char c = text.charAt(current++);

        if (c == '\\') {
            escaped = true;
            if (eof()) {
                throw new RuntimeException("Eof found after \\");
            }

            c = text.charAt(current++);

            switch (c) {
                case 't':
                    c = '\t';
                    break;
                case '\r':
                case '\n':
                    c = ' ';
                    break;
                case 'b':
                    c = '\b';
                    break;
                case 'f':
                    c = '\f';
                    break;
                case 'n':
                    c = '\n';
                    break;
                case 'r':
                    c = '\r';
                    break;
                case 'u':
                    c = unicode();
                    current += 4;
                    break;
                default:
                    // We just take the next character literally
                    // but have the escaped flag set, important for {},[] etc
            }
        }
        if (cursor > last && cursor <= current) {
            c0 = program != null ? program.size() : 0;
            c1 = statements != null ? statements.size() : 0;
            c2 = statement != null ? statement.size() : 0;
            c3 = (start >= 0) ? current - start : 0;
        }
        if (!increment) {
            current = last;
        }
        return c;
    }

    public List<List<List<String>>> program() {
        program = new ArrayList<List<List<String>>>();
        ws();
        if (!eof()) {
            program.add(pipeline());
            while (peek() == ';') {
                current++;
                List<List<String>> pipeline = pipeline();
                program.add(pipeline);
            }
        }
        if (!eof()) {
            throw new RuntimeException("Program has trailing text: " + context(current));
        }

        List<List<List<String>>> p = program;
        program = null;
        return p;
    }

    CharSequence context(int around) {
        return text.subSequence(Math.max(0, current - 20), Math.min(text.length(),
            current + 4));
    }

    public List<List<String>> pipeline() {
        statements = new ArrayList<List<String>>();
        statements.add(statement());
        while (peek() == '|') {
            current++;
            ws();
            if (!eof()) {
                statements.add(statement());
            }
            else {
                statements.add(new ArrayList<String>());
                break;
            }
        }
        List<List<String>> s = statements;
        statements = null;
        return s;
    }

    public List<String> statement() {
        statement = new ArrayList<String>();
        statement.add(value());
        while (!eof()) {
            ws();
            if (peek() == '|' || peek() == ';') {
                break;
            }

            if (!eof()) {
                statement.add(messy());
            }
        }
        List<String> s = statement;
        statement = null;
        return s;
    }

    public String messy()
    {
        start = current;
        char c = peek();
        if (c > 0 && SPECIAL.indexOf(c) < 0) {
            current++;
            try {
                while (!eof()) {
                    c = peek();
                    if (!escaped && (c == ';' || c == '|' || Character.isWhitespace(c))) {
                        break;
                    }
                    next();
                }
                return text.substring(start, current);
            } finally {
                start = -1;
            }
        }
        else {
            return value();
        }
    }

    String value() {
        ws();

        start = current;
        try {
            char c = next();
            if (!escaped) {
                switch (c) {
                    case '{':
                        return text.substring(start, find('}', '{'));
                    case '(':
                        return text.substring(start, find(')', '('));
                    case '[':
                        return text.substring(start, find(']', '['));
                    case '<':
                        return text.substring(start, find('>', '<'));
                    case '=':
                        return text.substring(start, current);
                    case '"':
                    case '\'':
                        quote(c);
                        break;
                }
            }

            // Some identifier or number
            while (!eof()) {
                c = peek();
                if (!escaped) {
                    if (Character.isWhitespace(c) || c == ';' || c == '|' || c == '=') {
                        break;
                    }
                    else if (c == '{') {
                        next();
                        find('}', '{');
                    }
                    else if (c == '(') {
                        next();
                        find(')', '(');
                    }
                    else if (c == '<') {
                        next();
                        find('>', '<');
                    }
                    else if (c == '[') {
                        next();
                        find(']', '[');
                    }
                    else if (c == '\'' || c == '"') {
                        next();
                        quote(c);
                        next();
                    }
                    else {
                        next();
                    }
                }
                else {
                    next();
                }
            }
            return text.substring(start, current);
        } finally {
            start = -1;
        }
    }

    boolean escaped() {
        return escaped;
    }

    char next() {
        return peek(true);
    }

    char unicode() {
        if (current + 4 > text.length()) {
            throw new IllegalArgumentException("Unicode \\u escape at eof at pos ..."
                + context(current) + "...");
        }

        String s = text.subSequence(current, current + 4).toString();
        int n = Integer.parseInt(s, 16);
        return (char) n;
    }

    int find(char target, char deeper) {
        int start = current;
        int level = 1;

        while (level != 0) {
            if (eof()) {
                throw new RuntimeException("Eof found in the middle of a compound for '"
                    + target + deeper + "', begins at " + context(start));
            }

            char c = next();
            if (!escaped) {
                if (c == target) {
                    level--;
                } else {
                    if (c == deeper) {
                        level++;
                    } else {
                        if (c == '"') {
                            quote('"');
                        } else {
                            if (c == '\'') {
                                quote('\'');
                            }
                            else {
                                if (c == '`') {
                                    quote('`');
                                }
                            }
                        }
                    }
                }
            }
        }
        return current;
    }

    int quote(char which) {
        while (!eof() && (peek() != which || escaped)) {
            next();
        }

        return current++;
    }

    CharSequence findVar() {
        int start = current;
        char c = peek();

        if (c == '{') {
            next();
            int end = find('}', '{');
            return text.subSequence(start, end);
        }
        if (c == '(') {
            next();
            int end = find(')', '(');
            return text.subSequence(start, end);
        }

        if (Character.isJavaIdentifierPart(c)) {
            while (c == '$') {
                c = next();
            }
            while (!eof() && (Character.isJavaIdentifierPart(c) || c == '.') && c != '$') {
                next();
                c = peek();
            }
            return text.subSequence(start, current);
        }
        throw new IllegalArgumentException(
            "Reference to variable does not match syntax of a variable: "
                + context(start));
    }

    public String toString() {
        return "..." + context(current) + "...";
    }

    public String unescape() {
        StringBuilder sb = new StringBuilder();
        while (!eof()) {
            sb.append(next());
        }
        return sb.toString();
    }

}
