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
package org.apache.felix.framework.util.ldap;

import java.io.*;
import java.util.*;

public class Driver {

    public static void main(String[] argv)
    {
    Mapper mapper = new DriverMapper();

    if(argv== null || argv.length == 0) {
        System.err.println("usage: Driver <ldap spec file>");
        return;
    }
    LdapLexer lexer = new LdapLexer();
    FileReader fr = null;
    char[] line = null;
    Evaluator engine = new Evaluator();

    Parser parser = new Parser();
//	parser.setDebug(System.out);

    try {
        File spec = new File(argv[0]);
        fr = new FileReader(spec);

        // The basic operation of the driver is:
        // 1. read a line from the file
        // 2. parse that line
        // 3. print the resulting program
        // 4. repeat 1 until eof

        for(;;) {
        line = getLine(fr);
        if(line == null) break;
        System.out.println("Driver: filter: "+new String(line));
        CharArrayReader car = new CharArrayReader(line);
        lexer.setReader(car);
        parser.reset(lexer);
        boolean status = false;
        try {
            status = parser.start();
            if(!status) {
            System.err.println("parse failed");
            printErrorLocation(line,lexer.charno());
            }
        } catch (ParseException pe) {
            System.err.println(pe.toString());
            printErrorLocation(line,lexer.charno());
        }
        if(status) {
            try {
            engine.reset(parser.getProgram());
//            System.out.println("Driver: program: "+engine.toString());
            System.out.println("Driver: program: "+engine.toStringInfix());
            System.out.println("Eval = " + engine.evaluate(mapper));
            } catch (EvaluationException ee) {
            System.err.print("Driver: ");
            printEvaluationStack(engine.getOperands());
            System.err.println(ee.toString());
            }
        }
        }
    } catch (Exception e) {
        System.err.println(e.toString());
        printErrorLocation(line,lexer.charno());
        e.printStackTrace();
    }
    }

    // Get a line of input at a time and return a char[] buffer
    // containing the line

    static char[] getLine(Reader reader) throws IOException
    {
    StringBuffer buf = new StringBuffer();
    for(;;) {
        int c = reader.read();
        if(c == '\r') continue;
        if(c < 0) {
        if(buf.length() == 0) return null; // no more lines
        break;
        }
        if(c == '\n') break;
        buf.append((char)c);
    }

    char[] cbuf = new char[buf.length()];
    buf.getChars(0,buf.length(),cbuf,0);
    return cbuf;
    }


    static void printErrorLocation(char[] line, int charno)
    {
    System.err.print("|");
    if(line != null) System.err.print(new String(line));
    System.err.println("|");
    for(int i=0;i<charno;i++) System.err.print(" ");
    System.err.println("^");
    }

    // Report the final contents of the evaluation stack
    static void printEvaluationStack(Stack stack)
    {
    System.err.print("Stack:");
    // recast operands as Vector to make interior access easier
    Vector operands = stack;
    int len = operands.size();
    for(int i=0;i<len;i++) System.err.print(" "+operands.elementAt(i));
    System.err.println();
    }

}

class DriverMapper implements Mapper {

    Hashtable hash = new Hashtable();

    public DriverMapper()
    {
        hash.put("cn","Babs Jensen");
        hash.put("objectClass","Person");
        hash.put("sn","Jensen");
        hash.put("o","university of michigan");
        hash.put("foo","bar");
    }

    public Object lookup(String key)
    {
        return hash.get(key);
    }
}
