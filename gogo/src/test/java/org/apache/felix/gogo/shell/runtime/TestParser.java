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
package org.apache.felix.gogo.shell.runtime;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import junit.framework.*;

import org.osgi.service.command.*;

public class TestParser extends TestCase {
    int beentheredonethat = 0;

    public void testPipe() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("capture", this);
        c.addCommand("grep", this);
        assertEquals("def", c
                .execute("echo def|grep (d.*)|capture"));
        assertEquals("def", c
                .execute("echo abc; echo def; echo ghi|grep (d.*)|capture"));
        assertEquals("hello world", c.execute("echo hello world|capture"));
        assertEquals("defghi", c
                .execute("echo abc; echo def; echo ghi|grep (def|ghi)|capture"));
    }

    public void testAssignment() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("capture", this);
        c.addCommand("grep", this);
        assertEquals("hello", c.execute("echo hello|capture").toString());
        assertEquals("hello", c.execute("a = <echo hello|capture>").toString());
        assertEquals("a", c.execute("a = a; echo $<echo a>").toString());
        assertEquals("3", c.execute("a=3; echo $a").toString());
        assertEquals("3", c.execute("a = 3; echo $a").toString());
        assertEquals("a", c.execute("a = a; echo $$a").toString());
    }

    public void testComment() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        assertEquals("1", c.execute("echo 1 // hello").toString());

    }

    public void testClosure() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        assertEquals("http://www.aqute.biz?com=2&biz=1", c.execute(
                "['http://www.aqute.biz?com=2&biz=1'] get 0").toString());
        assertEquals("{a=2, b=3}", c.execute("[a=2 b=3]").toString());
        assertEquals("3", c.execute("[a=2 <b>=<3>] get b").toString());
        assertEquals("[3, 4]", c.execute("[1 2 [3 4] 5 6] get 2").toString());
        assertEquals(5, c.execute("[1 2 [3 4] 5 6] size"));

    }

    public void testArray() throws Exception {
        Context c = new Context();
        assertEquals("http://www.aqute.biz?com=2&biz=1", c.execute(
                "['http://www.aqute.biz?com=2&biz=1'] get 0").toString());
        assertEquals("{a=2, b=3}", c.execute("[a=2 b=3]").toString());
        assertEquals("3", c.execute("[a=2 <b>=<3>] get b").toString());
        assertEquals("[3, 4]", c.execute("[1 2 [3 4] 5 6] get 2").toString());
        assertEquals(5, c.execute("[1 2 [3 4] 5 6] size"));

    }

    public void testEscape() {
        Parser parser = new Parser("'a|b;c'");
        CharSequence cs = parser.messy();
        assertEquals("a|b;c", cs.toString());
        assertEquals("a|b;c", new Parser(cs).unescape());
    }


    public void testParentheses() {
        Parser parser = new Parser("(a|b)|(d|f)");
        List<List<List<CharSequence>>> p = parser.program();
        assertEquals("(a|b)", p.get(0).get(0).get(0));

        parser = new Parser("grep (d.*)|grep (d|f)");
        p = parser.program();
        assertEquals("(d.*)", p.get(0).get(0).get(1));
    }

    public void testEcho() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        c.execute("echo peter");
    }

    public void grep(String match) throws IOException {
        Pattern p = Pattern.compile(match);
        BufferedReader rdr = new BufferedReader(
                new InputStreamReader(System.in));
        String s = rdr.readLine();
        while (s != null) {
            if (p.matcher(s).find()) {
                System.out.println(s);
            }
            s = rdr.readLine();
        }
    }

    public String capture() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedReader rdr = new BufferedReader(
                new InputStreamReader(System.in));
        String s = rdr.readLine();
        while (s != null) {
            sw.write(s);
            s = rdr.readLine();
        }
        return sw.toString();
    }

    public void testVars() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);

        assertEquals("", c.execute(
                "echo ${very.likely.that.this.does.not.exist}").toString());
        assertNotNull(c.execute("echo ${java.runtime.name}"));
    }

    public void testFunny() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        assertEquals("a", c.execute("echo a") + "");
        assertEquals("a", c.execute("<echo echo> a") + "");
        assertEquals("a", c.execute("<<echo echo> echo> <echo a>") + "");
    }

    public CharSequence echo(Object args[]) {
        if (args == null)
            return "";
        
        StringBuilder sb = new StringBuilder();
        String del = "";
        for (Object arg : args) {
            sb.append(del);
            if (arg != null) {
                sb.append(arg);
                del = " ";
            }
        }
        return sb;
    }

    public void testContext() throws Exception {
        Context c = new Context();
        c.addCommand("ls", this);
        beentheredonethat = 0;
        c.execute("ls");
        assertEquals(1, beentheredonethat);

        beentheredonethat = 0;
        c.execute("ls 10");
        assertEquals(10, beentheredonethat);

        beentheredonethat = 0;
        c.execute("ls a b c d e f g h i j");
        assertEquals(10, beentheredonethat);

        beentheredonethat = 0;
        Integer result = (Integer) c.execute("ls <ls 5>");
        assertEquals(10, beentheredonethat);
        assertEquals((Integer) 5, result);

    }

    public void ls() {
        beentheredonethat++;
        System.out.println("ls(): Yes!");
    }

    public int ls(int onoff) {
        beentheredonethat += onoff;
        System.out.println("ls(int) " + onoff);
        return onoff;
    }

    public void ls(Object args[]) {
        beentheredonethat = args.length;
        System.out.print("ls(Object[]) [");
        for (Object i : args)
            System.out.print(i + " ");
        System.out.println("]");
    }

    public void testProgram() {
        List<List<List<CharSequence>>> x = new Parser(
                "abc def|ghi jkl;mno pqr|stu vwx").program();
        assertEquals("abc", x.get(0).get(0).get(0));
        assertEquals("def", x.get(0).get(0).get(1));
        assertEquals("ghi", x.get(1).get(0).get(0));
        assertEquals("jkl", x.get(1).get(0).get(1));
        assertEquals("mno", x.get(1).get(1).get(0));
        assertEquals("pqr", x.get(1).get(1).get(1));
        assertEquals("stu", x.get(2).get(0).get(0));
        assertEquals("vwx", x.get(2).get(0).get(1));
    }

    public void testStatements() {
        List<List<CharSequence>> x = new Parser("abc def;ghi jkl;mno pqr")
                .statements();
        assertEquals("abc", x.get(0).get(0));
        assertEquals("def", x.get(0).get(1));
        assertEquals("ghi", x.get(1).get(0));
        assertEquals("jkl", x.get(1).get(1));
        assertEquals("mno", x.get(2).get(0));
        assertEquals("pqr", x.get(2).get(1));
    }

    public void testSimpleValue() {
        List<CharSequence> x = new Parser(
                "abc def.ghi http://www.osgi.org?abc=&x=1 [1,2,3] {{{{{{{xyz}}}}}}} <immediate> {'{{{{{'} {\\}} 'abc{}'")
                .statement();
        assertEquals("abc", x.get(0));
        assertEquals("def.ghi", x.get(1));
        assertEquals("http://www.osgi.org?abc=&x=1", x.get(2));
        assertEquals("[1,2,3]", x.get(3));
        assertEquals("{{{{{{{xyz}}}}}}}", x.get(4));
        assertEquals("<immediate>", x.get(5));
        assertEquals("{'{{{{{'}", x.get(6));
        assertEquals("{\\}}", x.get(7));
        assertEquals("abc{}", x.get(8));
    }

    void each(CommandSession session, Collection<Object> list, Function closure) throws Exception {
        List<Object> args = new ArrayList<Object>();
        args.add(null);
        for (Object x : list) {
            args.set(0, x );
            closure.execute(session, args);
        }
    }

}
