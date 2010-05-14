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

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.gogo.runtime.Evaluate;
import org.apache.felix.gogo.runtime.Parser;
import org.apache.felix.gogo.runtime.SyntaxError;
import org.apache.felix.gogo.runtime.Token;
import org.apache.felix.gogo.runtime.Tokenizer;
import org.apache.felix.gogo.runtime.Tokenizer.Type;

import junit.framework.TestCase;

public class TestTokenizer extends TestCase
{
    private final Map<String, Object> vars = new HashMap<String, Object>();
    private final Evaluate evaluate;

    public TestTokenizer()
    {
        evaluate = new Evaluate()
        {
            public Object eval(Token t) throws Exception
            {
                throw new UnsupportedOperationException("eval not implemented.");
            }

            public Object get(String key)
            {
                return vars.get(key);
            }

            public Object put(String key, Object value)
            {
                return vars.put(key, value);
            }
        };
    }

    public void testHello() throws Exception
    {
        testHello("hello world\n");
        testHello("hello world\n\n"); // multiple \n reduced to single Token.NL
        testHello("hello world\r\n"); // \r\n -> \n

        // escapes

        testHello("hello \\\nworld\n");
        try
        {
            testHello("hello\\u20world\n");
            fail("bad unicode accepted");
        }
        catch (SyntaxError e)
        {
            // expected
        }

        // whitespace and comments

        testHello(" hello  world    \n ");
        testHello("hello world // comment\n\n");
        testHello("hello world #\\ comment\n\n");
        testHello("// comment\nhello world\n");
        testHello("// comment ?\\ \nhello world\n");
        testHello("hello /*\n * comment\n */ world\n");
    }

    // hello world
    private void testHello(CharSequence text) throws Exception
    {
        Tokenizer t = new Tokenizer(text);
        assertEquals(Type.WORD, t.next());
        assertEquals("hello", t.value().toString());
        assertEquals(Type.WORD, t.next());
        assertEquals("world", t.value().toString());
        assertEquals(Type.NEWLINE, t.next());
        assertEquals(Type.EOT, t.next());
    }

    public void testString() throws Exception
    {
        testString("'single $quote' \"double $quote\"\n");
    }

    // 'single quote' "double quote"
    private void testString(CharSequence text) throws Exception
    {
        Tokenizer t = new Tokenizer(text);
        assertEquals(Type.WORD, t.next());
        assertEquals("'single $quote'", t.value().toString());
        assertEquals(Type.WORD, t.next());
        assertEquals("\"double $quote\"", t.value().toString());
        assertEquals(Type.NEWLINE, t.next());
        assertEquals(Type.EOT, t.next());
    }

    public void testClosure() throws Exception
    {
        testClosure2("x = { echo '}' $args //comment's\n}\n");
        testClosure2("x={ echo '}' $args //comment's\n}\n");
        assertEquals(Type.CLOSURE, token1("{ echo \\{ $args \n}"));
        assertEquals(Type.CLOSURE, token1("{ echo \\} $args \n}"));
    }

    /*
     * x = {echo $args};
     */
    private void testClosure2(CharSequence text) throws Exception
    {
        Tokenizer t = new Tokenizer(text);
        assertEquals(Type.WORD, t.next());
        assertEquals("x", t.value().toString());
        assertEquals(Type.ASSIGN, t.next());
        assertEquals(Type.CLOSURE, t.next());
        assertEquals(" echo '}' $args //comment's\n", t.value().toString());
        assertEquals(Type.NEWLINE, t.next());
        assertEquals(Type.EOT, t.next());
    }

    private Type token1(CharSequence text) throws Exception
    {
        Tokenizer t = new Tokenizer(text);
        Type type = t.next();
        assertEquals(Type.EOT, t.next());
        return type;
    }

    public void testExpand() throws Exception
    {
        final URI home = new URI("/home/derek");
        final File pwd = new File("/tmp");
        final String user = "derek";

        vars.clear();
        vars.put("HOME", home);
        vars.put("PWD", pwd);
        vars.put("USER", user);
        vars.put(user, "Derek Baum");

        // quote removal
        assertEquals("hello", expand("hello"));
        assertEquals("hello", expand("'hello'"));
        assertEquals("\"hello\"", expand("'\"hello\"'"));
        assertEquals("hello", expand("\"hello\""));
        assertEquals("'hello'", expand("\"'hello'\""));

        // escapes
        assertEquals("hello\\w", expand("hello\\\\w"));
        assertEquals("hellow", expand("hello\\w"));
        assertEquals("hello\\w", expand("\"hello\\\\w\""));
        assertEquals("hello\\w", expand("\"hello\\w\""));
        assertEquals("hello\\\\w", expand("'hello\\\\w'"));
        assertEquals("hello", expand("he\\\nllo"));
        assertEquals("he\\llo", expand("'he\\llo'"));
        assertEquals("he'llo", expand("'he'\\''llo'"));
        assertEquals("he\"llo", expand("\"he\\\"llo\""));
        assertEquals("he'llo", expand("he\\'llo"));
        assertEquals("he$llo", expand("\"he\\$llo\""));
        assertEquals("he\\'llo", expand("\"he\\'llo\""));
        assertEquals("hello\\w", expand("\"hello\\w\""));

        // unicode

        // Note: we could use literal Unicode pound '£' instead of \u00a3 in next test.
        // if above is not UK currency symbol, then your locale is not configured for UTF-8.
        // Java on Macs cannot handle UTF-8 unless you explicitly set '-Dfile.encoding=UTF-8'.
        assertEquals("pound\u00a3cent\u00a2", expand("pound\\u00a3cent\\u00a2"));
        assertEquals("euro\\u20ac", expand("'euro\\u20ac'"));
        try
        {
            expand("eot\\u20a");
            fail("EOT in unicode");
        }
        catch (SyntaxError e)
        {
            // expected
        }
        try
        {
            expand("bad\\u20ag");
            fail("bad unicode");
        }
        catch (SyntaxError e)
        {
            // expected
        }

        // simple variable expansion - quoting or concatenation converts result to String
        assertEquals(user, expand("$USER"));
        assertEquals(home, expand("$HOME"));
        assertEquals(home.toString(), expand("$HOME$W"));
        assertEquals(pwd, expand("$PWD"));
        assertEquals("$PWD", expand("'$PWD'"));
        assertEquals("$PWD", expand("\\$PWD"));
        assertEquals(pwd.toString(), expand("\"$PWD\""));
        assertEquals("W" + pwd, expand("W$PWD"));
        assertEquals(pwd + user, expand("$PWD$USER"));

        // variable substitution  ${NAME:-WORD} etc
        assertNull(expand("$JAVA_HOME"));
        assertEquals(user, expand("${USER}"));
        assertEquals(user + "W", expand("${USER}W"));
        assertEquals("java_home", expand("${JAVA_HOME:-java_home}"));
        assertEquals(pwd, expand("${NOTSET:-$PWD}"));
        assertNull(vars.get("JAVA_HOME"));
        assertEquals("java_home", expand("${JAVA_HOME:=java_home}"));
        assertEquals("java_home", vars.get("JAVA_HOME"));
        assertEquals("java_home", expand("$JAVA_HOME"));
        assertEquals("yes", expand("${JAVA_HOME:+yes}"));
        assertNull(expand("${NOTSET:+yes}"));
        assertEquals("", expand("\"${NOTSET:+yes}\""));
        try
        {
            expand("${NOTSET:?}");
            fail("expected 'not set' exception");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        // bad variable names
        assertEquals("$ W", expand("$ W"));
        assertEquals("$ {W}", expand("$ {W}"));
        try
        {
            expand("${W }");
            fail("expected syntax error");
        }
        catch (SyntaxError e)
        {
            // expected
        }

        assertEquals(user, expand("${USER\\\n:?}"));
        assertEquals(user, expand("${US\\u0045R:?}"));

        // bash doesn't supported nested expansions
        // gogo only supports them in the ${} syntax
        assertEquals("Derek Baum", expand("${$USER}"));
        assertEquals("x", expand("${$USR:-x}"));
        assertEquals("$" + user, expand("$$USER"));
    }

    private Object expand(CharSequence word) throws Exception
    {
        return Tokenizer.expand(word, evaluate);
    }

    public void testParser() throws Exception
    {
        new Parser("// comment\n" + "a=\"who's there?\"; ps -ef;\n" + "ls | \n grep y\n").program();
        String p1 = "a=1 \\$b=2 c={closure}\n";
        new Parser(p1).program();
        new Parser(new Token(Type.ARRAY, p1, (short) 0, (short) 0)).program();
    }

}
