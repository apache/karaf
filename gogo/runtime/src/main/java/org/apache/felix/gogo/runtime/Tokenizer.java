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


/**
 * Bash-like tokenizer.
 * 
 * Single and double quotes are just like Bash - single quotes escape everything
 * (including backslashes), newlines are allowed in quotes.
 * backslash-newline indicates a line continuation and is removed.
 * 
 * Variable expansion is just like Bash: $NAME or ${NAME[[:][-+=?WORD]},
 * except it can yield any Object. Variables expanded within double-quotes,
 * or adjacent to a String are converted to String.
 * 
 * Unlike bash, indirect variable expansion is supported using ${$NAME}.
 * 
 * Only a single variable assignment is recognized, with '=' being the second token.
 * (Bash allows name1=value1 name2=value2 ... command args)
 * 
 * Comments can only start where white space is allowed:
 * # or // starts a line comment, /* starts a block comment.
 * The following common uses do NOT start comments:
 *    ls http://example.com#anchor
 *    ls $dir/*.java
 * 
 * @see http://wiki.bash-hackers.org/syntax/basicgrammar
 */
public class Tokenizer
{
    public enum Type
    {
        ASSIGN('='), PIPE('|'), SEMICOLON(';'), NEWLINE, ARRAY, CLOSURE, EXECUTION, WORD, EOT;

        private char c;

        Type()
        {
        }

        Type(char c)
        {
            this.c = c;
        }

        @Override
        public String toString()
        {
            return (c == 0 ? super.toString() : "'" + c + "'");
        }
    }

    private static final boolean DEBUG = false;
    private static final char EOT = (char) -1;

    private final CharSequence text;
    private final Evaluate evaluate;
    private final boolean inArray;
    private final boolean inQuote;

    private Type type = Type.NEWLINE;
    private CharSequence value;
    private Token token;

    private short line;
    private short column;
    private char ch;
    private int index;
    private boolean firstWord;

    public Tokenizer(CharSequence text)
    {
        this(text, null, false);
    }

    public Tokenizer(CharSequence text, Evaluate evaluate, boolean inQuote)
    {
        this.text = text;
        this.evaluate = evaluate;
        this.inQuote = inQuote;
        index = 0;
        line = column = 1;

        boolean array = false;

        if (text instanceof Token)
        {
            Token t = (Token) text;
            line = t.line;
            column = t.column;
            array = (Type.ARRAY == t.type);
        }

        inArray = array;
        getch();

        if (DEBUG)
        {
            if (inArray)
                System.err.println("Tokenizer[" + text + "]");
            else
                System.err.println("Tokenizer<" + text + ">");
        }
    }

    public Type type()
    {
        return type;
    }

    public CharSequence value()
    {
        return value;
    }

    public Token token()
    {
        return token;
    }

    public Type next()
    {
        final Type prevType = type;
        token = null;
        value = null;

        short tLine;
        short tColumn;

        while (true)
        {
            skipSpace();
            tLine = line;
            tColumn = column;

            switch (ch)
            {
                case EOT:
                    type = Type.EOT;
                    break;

                case '\n':
                    getch();
                    if (inArray)
                        continue;
                    // only return NEWLINE once and not if not preceded by ; or |
                    switch (prevType)
                    {
                        case PIPE:
                        case SEMICOLON:
                        case NEWLINE:
                            continue;

                        default:
                            type = Type.NEWLINE;
                            break;
                    }
                    break;

                case '{':
                case '(':
                case '[':
                    value = group();
                    getch();
                    break;

                case ';':
                    getch();
                    type = Type.SEMICOLON;
                    break;

                case '|':
                    getch();
                    type = Type.PIPE;
                    break;

                case '=':
                    if (firstWord || inArray)
                    {
                        getch();
                        type = Type.ASSIGN;
                        break;
                    }
                    // fall through
                default:
                    value = word();
                    type = Type.WORD;
            }

            firstWord = (Type.WORD == type && (Type.WORD != prevType && Type.ASSIGN != prevType));
            token = new Token(type, value, tLine, tColumn);

            if (DEBUG)
            {
                System.err.print("<" + type + ">");
                if (Type.EOT == type)
                {
                    System.err.println();
                }
            }

            return type;
        }
    }

    private CharSequence word()
    {
        int start = index - 1;
        int skipCR = 0;

        do
        {
            switch (ch)
            {
                case '\n':
                    if (index >= 2 && text.charAt(index - 2) == '\r')
                        skipCR = 1;
                    // fall through
                case '=':
                    if ((Type.WORD == type || Type.ASSIGN == type) && '=' == ch
                        && !inArray)
                        continue;
                    // fall through
                case ' ':
                case '\t':
                case '|':
                case ';':
                    return text.subSequence(start, index - 1 - skipCR);

                case '{':
                    group();
                    break;

                case '\\':
                    escape();
                    break;

                case '\'':
                case '"':
                    skipQuote();
                    break;
            }
        }
        while (getch() != EOT);

        return text.subSequence(start, index - 1);
    }

    private CharSequence group()
    {
        final char push = ch;
        final char pop;

        switch (ch)
        {
            case '{':
                type = Type.CLOSURE;
                pop = '}';
                break;
            case '(':
                type = Type.EXECUTION;
                pop = ')';
                break;
            case '[':
                type = Type.ARRAY;
                pop = ']';
                break;
            default:
                assert false;
                pop = 0;
        }

        short sLine = line;
        short sCol = column;
        int start = index;
        int depth = 1;

        while (true)
        {
            boolean comment = false;

            switch (ch)
            {
                case '{':
                case '(':
                case '[':
                case '\n':
                    comment = true;
                    break;
            }

            if (getch() == EOT)
            {
                throw new EOFError(sLine, sCol, "unexpected EOT looking for matching '"
                    + pop + "'");
            }

            // don't recognize comments that start within a word
            if (comment || isBlank(ch))
                skipSpace();

            switch (ch)
            {
                case '"':
                case '\'':
                    skipQuote();
                    break;

                case '\\':
                    ch = escape();
                    break;

                default:
                    if (push == ch)
                        depth++;
                    else if (pop == ch && --depth == 0)
                        return text.subSequence(start, index - 1);
            }
        }

    }

    private char escape()
    {
        assert '\\' == ch;

        switch (getch())
        {
            case 'u':
                getch();
                getch();
                getch();
                getch();

                if (EOT == ch)
                {
                    throw new EOFError(line, column, "unexpected EOT in \\u escape");
                }

                String u = text.subSequence(index - 4, index).toString();

                try
                {
                    return (char) Integer.parseInt(u, 16);
                }
                catch (NumberFormatException e)
                {
                    throw new SyntaxError(line, column, "bad unicode escape: \\u" + u);
                }

            case EOT:
                throw new EOFError(line, column, "unexpected EOT in \\ escape");

            case '\n':
                return '\0'; // line continuation

            case '\\':
            case '\'':
            case '"':
            case '$':
                return ch;

            default:
                return ch;
        }
    }

    private void skipQuote()
    {
        assert '\'' == ch || '"' == ch;
        final char quote = ch;
        final short sLine = line;
        final short sCol = column;

        while (getch() != EOT)
        {
            if (quote == ch)
                return;

            if ((quote == '"') && ('\\' == ch))
                escape();
        }

        throw new EOFError(sLine, sCol, "unexpected EOT looking for matching quote: "
            + quote);
    }

    private void skipSpace()
    {
        while (true)
        {
            while (isBlank(ch))
            {
                getch();
            }

            // skip continuation lines, but not other escapes
            if (('\\' == ch) && (peek() == '\n'))
            {
                getch();
                getch();
                continue;
            }

            // skip comments
            if (('/' == ch) || ('#' == ch))
            {
                if (('#' == ch) || (peek() == '/'))
                {
                    while ((getch() != EOT) && ('\n' != ch))
                    {
                    }
                    continue;
                }
                else if ('*' == peek())
                {
                    short sLine = line;
                    short sCol = column;
                    getch();

                    while ((getch() != EOT) && !(('*' == ch) && (peek() == '/')))
                    {
                    }

                    if (EOT == ch)
                    {
                        throw new EOFError(sLine, sCol,
                            "unexpected EOT looking for closing comment: */");
                    }

                    getch();
                    getch();
                    continue;
                }
            }

            break;
        }
    }

    private boolean isBlank(char ch)
    {
        return ' ' == ch || '\t' == ch;
    }

    private boolean isName(char ch)
    {
        return Character.isJavaIdentifierPart(ch) && (ch != '$') || ('.' == ch);
    }

    /**
     * expand variables, quotes and escapes in word.
     * @param vars
     * @return
     */
    public static Object expand(CharSequence word, Evaluate eval)
    {
        return expand(word, eval, false);
    }

    private static Object expand(CharSequence word, Evaluate eval,
        boolean inQuote)
    {
        final String special = "$\\\"'";
        int i = word.length();

        while ((--i >= 0) && (special.indexOf(word.charAt(i)) == -1))
        {
        }

        // shortcut if word doesn't contain any special characters
        if (i < 0)
            return word;

        return new Tokenizer(word, eval, inQuote).expand();
    }

    public Object expand(CharSequence word, short line, short column)
    {
        return expand(new Token(Type.WORD, word, line, column), evaluate, inQuote);
    }

    private Token word(CharSequence value)
    {
        return new Token(Type.WORD, value, line, column);
    }

    private Object expand()
    {
        StringBuilder buf = new StringBuilder();

        while (ch != EOT)
        {
            int start = index;

            switch (ch)
            {
                case '$':
                    Object val = expandVar();

                    if (EOT == ch && buf.length() == 0)
                    {
                        return val;
                    }

                    if (null != val)
                    {
                        buf.append(val);
                    }

                    continue; // expandVar() has already read next char

                case '\\':
                    ch = (inQuote && ("u$\\\n\"".indexOf(peek()) == -1)) ? '\\'
                        : escape();

                    if (ch != '\0') // ignore line continuation
                    {
                        buf.append(ch);
                    }

                    break;

                case '"':
                    Token ww = word(null);
                    skipQuote();
                    ww.value = text.subSequence(start, index - 1);
                    value = ww;
                    Object expand = expand(value, evaluate, true);

                    if (eot() && buf.length() == 0 && value.equals(expand))
                    {
                        return ww.value;
                    }

                    if (null != expand)
                    {
                        buf.append(expand.toString());
                    }
                    break;

                case '\'':
                    if (!inQuote)
                    {
                        skipQuote();
                        value = text.subSequence(start, index - 1);

                        if (eot() && buf.length() == 0)
                        {
                            return value;
                        }

                        buf.append(value);
                        break;
                    }
                    // else fall through
                default:
                    buf.append(ch);
            }

            getch();
        }

        return buf.toString();
    }

    private Object expandVar()
    {
        assert '$' == ch;
        Object val;

        if (getch() != '{')
        {
            int start = index - 1;
            while (isName(ch))
            {
                getch();
            }

            if (index - 1 == start)
            {
                val = "$";
            }
            else
            {
                String name = text.subSequence(start, index - 1).toString();
                val = evaluate.get(name);
            }
        }
        else
        {
            // ${NAME[[:]-+=?]WORD}
            short sLine = line;
            short sCol = column;
            CharSequence group = group();
            char c;
            int i = 0;

            while (i < group.length())
            {
                switch (group.charAt(i))
                {
                    case ':':
                    case '-':
                    case '+':
                    case '=':
                    case '?':
                        break;

                    default:
                        ++i;
                        continue;
                }
                break;
            }

            sCol += i;

            String name = String.valueOf(expand(group.subSequence(0, i), sLine, sCol));

            for (int j = 0; j < name.length(); ++j)
            {
                if (!isName(name.charAt(j)))
                {
                    throw new SyntaxError(sLine, sCol, "bad name: ${" + group + "}");
                }
            }

            val = evaluate.get(name);

            if (i < group.length())
            {
                c = group.charAt(i++);
                if (':' == c)
                {
                    c = (i < group.length() ? group.charAt(i++) : EOT);
                }

                CharSequence word = group.subSequence(i, group.length());

                switch (c)
                {
                    case '-':
                    case '=':
                        if (null == val)
                        {
                            val = expand(word, evaluate, false);
                            if ('=' == c)
                            {
                                evaluate.put(name, val);
                            }
                        }
                        break;

                    case '+':
                        if (null != val)
                        {
                            val = expand(word, evaluate, false);
                        }
                        break;

                    case '?':
                        if (null == val)
                        {
                            val = expand(word, evaluate, false);
                            if (null == val || val.toString().length() == 0)
                            {
                                val = "parameter not set";
                            }
                            throw new IllegalArgumentException(name + ": " + val);
                        }
                        break;

                    default:
                        throw new SyntaxError(sLine, sCol, "bad substitution: ${" + group
                            + "}");
                }
            }
            getch();
        }

        return val;
    }

    /**
     * returns true if getch() will return EOT
     * @return
     */
    private boolean eot()
    {
        return index >= text.length();
    }

    private char getch()
    {
        return ch = getch(false);
    }

    private char peek()
    {
        return getch(true);
    }

    private char getch(boolean peek)
    {
        if (eot())
        {
            if (!peek)
            {
                ++index;
                ch = EOT;
            }
            return EOT;
        }

        int current = index;
        char c = text.charAt(index++);

        if (('\r' == c) && !eot() && (text.charAt(index) == '\n'))
            c = text.charAt(index++);

        if (peek)
        {
            index = current;
        }
        else if ('\n' == c)
        {
            ++line;
            column = 0;
        }
        else
            ++column;

        return c;
    }

}
