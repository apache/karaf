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
package org.apache.felix.gogo.runtime;

import java.util.*;
import java.util.regex.Pattern;


//
// NOTE: this file is included to fix FELIX-5805 but should be removed
// once the fix is released as part of gogo.
//
//
public class Parser
{

    public static abstract class Executable extends Token
    {
        public Executable(Token cs)
        {
            super(cs);
        }
    }

    public static class Operator extends Executable
    {
        public Operator(Token cs) {
            super(cs);
        }
    }

    public static class Statement extends Executable
    {
        private final List<Token> tokens;
        private final List<Token> redirections;

        public Statement(Token cs, List<Token> tokens, List<Token> redirections)
        {
            super(cs);
            this.tokens = tokens;
            this.redirections = redirections;
        }

        public List<Token> tokens()
        {
            return tokens;
        }

        public List<Token> redirections() {
            return redirections;
        }
    }

    /**
     * pipe1 ; pipe2 ; ...
     */
    public static class Program extends Token
    {
        private final List<Executable> tokens;

        public Program(Token cs, List<Executable> tokens)
        {
            super(cs);
            this.tokens = tokens;
        }

        public List<Executable> tokens()
        {
            return tokens;
        }
    }

    /**
     * token1 | token2 | ...
     */
    public static class Pipeline extends Executable
    {
        private final List<Executable> tokens;

        public Pipeline(Token cs, List<Executable> tokens)
        {
            super(cs);
            this.tokens = tokens;
        }

        public List<Executable> tokens()
        {
            return tokens;
        }

    }

    /**
     * ( program )
     */
    public static class Sequence extends Executable
    {
        private final Program program;

        public Sequence(Token cs, Program program)
        {
            super(cs);
            this.program = program;
        }

        public Program program()
        {
            return program;
        }
    }

    /**
     * { program }
     */
    public static class Closure extends Token
    {
        private final Program program;

        public Closure(Token cs, Program program)
        {
            super(cs);
            this.program = program;
        }

        public Program program()
        {
            return program;
        }
    }

    /**
     * [ a b ...]
     * [ k1=v1 k2=v2 ...]
     */
    public static class Array extends Token
    {
        private final List<Token> list;
        private final Map<Token, Token> map;

        public Array(Token cs, List<Token> list, Map<Token, Token> map)
        {
            super(cs);
            assert list != null ^ map != null;
            this.list = list;
            this.map = map;
        }

        public List<Token> list()
        {
            return list;
        }

        public Map<Token, Token> map()
        {
            return map;
        }
    }

    protected final Tokenizer tz;
    protected final LinkedList<String> stack = new LinkedList<>();
    protected final List<Token> tokens = new ArrayList<>();
    protected final List<Statement> statements = new ArrayList<>();

    public Parser(CharSequence line)
    {
        this.tz = new Tokenizer(line);
    }

    public List<Token> tokens() {
        return Collections.unmodifiableList(tokens);
    }

    public List<Statement> statements() {
        statements.sort(Comparator.comparingInt(o -> o.start));
        return Collections.unmodifiableList(statements);
    }

    public Program program()
    {
        List<Executable> tokens = new ArrayList<>();
        List<Executable> pipes = null;
        int start = tz.index - 1;
        while (true)
        {
            Statement ex;
            Token t = next();
            if (t == null)
            {
                if (pipes != null)
                {
                    throw new EOFError(tz.line, tz.column, "unexpected EOT while looking for a statement after |", getMissing("pipe"), "0");
                }
                else
                {
                    return new Program(whole(tokens, start), tokens);
                }
            }
            if (Token.eq("}", t) || Token.eq(")", t) || Token.eq("]", t))
            {
                if (pipes != null)
                {
                    throw new EOFError(t.line, t.column, "unexpected token '" + t + "' while looking for a statement after |", getMissing("pipe"), "0");
                }
                else if (stack.isEmpty())
                {
                    throw new SyntaxError(t.line, t.column, "unexpected token '" + t + "'");
                }
                else
                {
                    push(t);
                    return new Program(whole(tokens, start), tokens);
                }
            }
            else
            {
                push(t);
                ex = statement();
            }
            t = next();
            if (t == null || Token.eq(";", t) || Token.eq("\n", t) || Token.eq("&", t) || Token.eq("&&", t) || Token.eq("||", t))
            {
                if (pipes != null)
                {
                    pipes.add(ex);
                    tokens.add(new Pipeline(whole(pipes, start), pipes));
                    pipes = null;
                }
                else
                {
                    tokens.add(ex);
                }
                if (t == null)
                {
                    return new Program(whole(tokens, start), tokens);
                }
                else {
                    tokens.add(new Operator(t));
                }
            }
            else if (Token.eq("|", t) || Token.eq("|&", t))
            {
                if (pipes == null)
                {
                    pipes = new ArrayList<>();
                }
                pipes.add(ex);
                pipes.add(new Operator(t));
            }
            else
            {
                if (pipes != null)
                {
                    pipes.add(ex);
                    tokens.add(new Pipeline(whole(pipes, start), pipes));
                    pipes = null;
                }
                else
                {
                    tokens.add(ex);
                }
                push(t);
            }
        }
    }

    protected void push(Token t) {
        tz.push(t);
    }

    protected Token next() {
        boolean pushed = tz.pushed != null;
        Token token = tz.next();
        if (!pushed && token != null) {
            tokens.add(token);
        }
        return token;
    }

    public Sequence sequence()
    {
        Token start = start("(", "sequence");
        expectNotNull();
        Program program = program();
        Token end = end(")");
        return new Sequence(whole(start, end), program);
    }

    public Closure closure()
    {
        Token start = start("{", "closure");
        expectNotNull();
        Program program = program();
        Token end = end("}");
        return new Closure(whole(start, end), program);
    }

    private static final Pattern redirNoArg = Pattern.compile("[0-9]?>&[0-9-]|[0-9-]?<&[0-9-]");
    private static final Pattern redirArg = Pattern.compile("[0-9&]?>|[0-9]?>>|[0-9]?<|[0-9]?<>|<<<");
    private static final Pattern redirHereDoc = Pattern.compile("<<-?");

    public Statement statement()
    {
        List<Token> tokens = new ArrayList<>();
        List<Token> redirs = new ArrayList<>();
        boolean needRedirArg = false;
        int start = tz.index;
        while (true)
        {
            Token t = next();
            if (t == null
                    || Token.eq("\n", t)
                    || Token.eq(";", t)
                    || Token.eq("&", t)
                    || Token.eq("&&", t)
                    || Token.eq("||", t)
                    || Token.eq("|", t)
                    || Token.eq("|&", t)
                    || Token.eq("}", t)
                    || Token.eq(")", t)
                    || Token.eq("]", t))
            {
                if (needRedirArg)
                {
                    throw new EOFError(tz.line, tz.column, "Expected file name for redirection", "redir", "foo");
                }
                push(t);
                break;
            }
            if (Token.eq("{", t))
            {
                push(t);
                tokens.add(closure());
            }
            else if (Token.eq("[", t))
            {
                push(t);
                tokens.add(array());
            }
            else if (Token.eq("(", t))
            {
                push(t);
                tokens.add(sequence());
            }
            else if (needRedirArg)
            {
                redirs.add(t);
                needRedirArg = false;
            }
            else if (redirNoArg.matcher(t).matches())
            {
                redirs.add(t);
            }
            else if (redirArg.matcher(t).matches())
            {
                redirs.add(t);
                needRedirArg = true;
            }
            else if (redirHereDoc.matcher(t).matches())
            {
                redirs.add(t);
                redirs.add(tz.readHereDoc(t.charAt(t.length() - 1) == '-'));
            }
            else
            {
                tokens.add(t);
            }
        }
        Statement statement = new Statement(whole(tokens, start), tokens, redirs);
        statements.add(statement);
        return statement;
    }

    public Array array()
    {
        Token start = start("[", "array");
        Boolean isMap = null;
        List<Token> list = new ArrayList<>();
        Map<Token, Token> map = new LinkedHashMap<>();
        while (true)
        {
            Token key = next();
            if (key == null)
            {
                throw new EOFError(tz.line, tz.column, "unexpected EOT", getMissing(), "]");
            }
            if (Token.eq("]", key))
            {
                push(key);
                break;
            }
            if (Token.eq("\n", key))
            {
                continue;
            }
            if (Token.eq("{", key) || Token.eq(";", key) || Token.eq("&", key) || Token.eq("&&", key) || Token.eq("||", key)
                    || Token.eq("|", key) || Token.eq("|&", key) || Token.eq(")", key) || Token.eq("}", key) || Token.eq("=", key))
            {
                throw new SyntaxError(key.line(), key.column(), "unexpected token '" + key + "' while looking for array key");
            }
            if (Token.eq("(", key))
            {
                push(key);
                key = sequence();
            }
            if (Token.eq("[", key))
            {
                push(key);
                key = array();
            }
            if (isMap == null)
            {
                Token n = next();
                if (n == null)
                {
                    throw new EOFError(tz.line, tz.column, "unexpected EOF while looking for array token", getMissing(), "]");
                }
                isMap = Token.eq("=", n);
                push(n);
            }
            if (isMap)
            {
                expect("=");
                Token val = next();
                if (val == null)
                {
                    throw new EOFError(tz.line, tz.column, "unexpected EOF while looking for array value", getMissing(), "0");
                }
                else if (Token.eq(";", val) || Token.eq("&", val) || Token.eq("&&", val) || Token.eq("||", val) || Token.eq("|", val) || Token.eq("|&", val)
                        || Token.eq(")", key) || Token.eq("}", key) || Token.eq("=", key))
                {
                    throw new SyntaxError(key.line(), key.column(), "unexpected token '" + key + "' while looking for array value");
                }
                else if (Token.eq("[", val))
                {
                    push(val);
                    val = array();
                }
                else if (Token.eq("(", val))
                {
                    push(val);
                    val = sequence();
                }
                else if (Token.eq("{", val))
                {
                    push(val);
                    val = closure();
                }
                map.put(key, val);
            }
            else
            {
                list.add(key);
            }
        }
        Token end = end("]");
        if (isMap == null || !isMap)
        {
            return new Array(whole(start, end), list, null);
        }
        else
        {
            return new Array(whole(start, end), null, map);
        }
    }

    protected void expectNotNull()
    {
        Token t = next();
        if (t == null)
        {
            throw new EOFError(tz.line, tz.column,
                    "unexpected EOT",
                    getMissing(), "0");
        }
        push(t);
    }

    private String getMissing() {
        return getMissing(null);
    }

    private String getMissing(String additional) {
        StringBuilder sb = new StringBuilder();
        LinkedList<String> stack = this.stack;
        if (additional != null) {
            stack = new LinkedList<>(stack);
            stack.addLast(additional);
        }
        String last = null;
        int nb = 0;
        for (String cur : stack) {
            if (last == null) {
                last = cur;
                nb = 1;
            } else if (last.equals(cur)) {
                nb++;
            } else {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(last);
                if (nb > 1) {
                    sb.append("(").append(nb).append(")");
                }
                last = cur;
                nb = 1;
            }
        }
        if (sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(last);
        if (nb > 1) {
            sb.append("(").append(nb).append(")");
        }
        return sb.toString();
    }

    protected Token start(String str, String missing) {
        stack.addLast(missing);
        return expect(str);
    }

    protected Token end(String str) {
        Token t = expect(str);
        stack.removeLast();
        return t;
    }

    protected Token expect(String str)
    {
        Token start = next();
        if (start == null)
        {
            throw new EOFError(tz.line, tz.column,
                    "unexpected EOT looking for '" + str + "",
                    getMissing(), str);
        }
        if (!Token.eq(str, start))
        {
            throw new SyntaxError(start.line, start.column, "expected '" + str + "' but got '" + start.toString() + "'");
        }
        return start;
    }

    protected Token whole(List<? extends Token> tokens, int index)
    {
        if (tokens.isEmpty())
        {
            index = Math.min(index, tz.text().length());
            return tz.text().subSequence(index, index);
        }
        Token b = tokens.get(0);
        Token e = tokens.get(tokens.size() - 1);
        return whole(b, e);
    }

    protected Token whole(Token b, Token e)
    {
        return tz.text.subSequence(b.start - tz.text.start, e.start + e.length() - tz.text.start);
    }

}
