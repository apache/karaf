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
package org.apache.karaf.shell.impl.console.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.felix.gogo.jline.ParsedLineImpl;
import org.apache.felix.gogo.runtime.EOFError;
import org.apache.felix.gogo.runtime.Parser.Program;
import org.apache.felix.gogo.runtime.Parser.Statement;
import org.apache.felix.gogo.runtime.SyntaxError;
import org.apache.felix.gogo.runtime.Token;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;
import org.jline.reader.ParsedLine;

public class KarafParser implements org.jline.reader.Parser {

    private final Session session;

    public KarafParser(Session session) {
        this.session = session;
    }

    @Override
    public ParsedLine parse(String line, int cursor, ParseContext parseContext) throws SyntaxError {
        try {
            return doParse(line, cursor, parseContext);
        } catch (EOFError e) {
            throw new org.jline.reader.EOFError(e.line(), e.column(), e.getMessage(), e.missing());
        } catch (SyntaxError e) {
            throw new org.jline.reader.SyntaxError(e.line(), e.column(), e.getMessage());
        }
    }

    private ParsedLine doParse(String line, int cursor, ParseContext parseContext) throws SyntaxError {
        Program program = null;
        List<Statement> statements = null;
        String repaired = line;
        while (program == null) {
            try {
                org.apache.felix.gogo.runtime.Parser parser = new org.apache.felix.gogo.runtime.Parser(repaired);
                program = parser.program();
                statements = parser.statements();
            } catch (EOFError e) {
                // Make sure we don't loop forever
                if (parseContext == ParseContext.COMPLETE && repaired.length() < line.length() + 1024) {
                    repaired = repaired + " " + e.repair();
                } else {
                    throw e;
                }
            }
        }
        // Find corresponding statement
        Statement statement = null;
        for (int i = statements.size() - 1; i >= 0; i--) {
            Statement s = statements.get(i);
            if (s.start() <= cursor) {
                boolean isOk = true;
                // check if there are only spaces after the previous statement
                if (s.start() + s.length() < cursor) {
                    for (int j = s.start() + s.length(); isOk && j < cursor; j++) {
                        isOk = Character.isWhitespace(line.charAt(j));
                    }
                }
                statement = s;
                break;
            }
        }
        if (statement != null && statement.tokens() != null && !statement.tokens().isEmpty()) {
            String cmdName = session.resolveCommand(statement.tokens().get(0).toString());
            String[] parts = cmdName.split(":");
            Command cmd = parts.length == 2 ? session.getRegistry().getCommand(parts[0], parts[1]) : null;
            Parser cmdParser = cmd != null ? cmd.getParser() : null;
            if (cmdParser != null) {
                final CommandLine cmdLine = cmdParser.parse(session, statement.toString(), cursor - statement.start());
                return new ParsedLine() {
                    @Override
                    public String word() {
                        return cmdLine.getCursorArgument();
                    }
                    @Override
                    public int wordCursor() {
                        return cmdLine.getArgumentPosition();
                    }
                    @Override
                    public int wordIndex() {
                        return cmdLine.getCursorArgumentIndex();
                    }
                    @Override
                    public List<String> words() {
                        return Arrays.asList(cmdLine.getArguments());
                    }
                    @Override
                    public String line() {
                        return cmdLine.getBuffer();
                    }
                    @Override
                    public int cursor() {
                        return cmdLine.getBufferPosition();
                    }
                };
            }
            if (repaired != line) {
                Token stmt = statement.subSequence(0, line.length() - statement.start());
                List<Token> tokens = new ArrayList<>(statement.tokens());
                Token last = tokens.get(tokens.size() - 1);
                tokens.set(tokens.size() - 1, last.subSequence(0, line.length() - last.start()));
                return new ParsedLineImpl(program, stmt, cursor, tokens);
            }
            return new ParsedLineImpl(program, statement, cursor, statement.tokens());
        } else {
            // TODO:
            return new ParsedLineImpl(program, program, cursor, Collections.singletonList(program));
        }
    }

}
