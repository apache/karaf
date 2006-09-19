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

import java.io.IOException;
import java.io.Reader;

public class LdapLexer {

    static final int EOF = -1;
    static final int NOCHAR = 0; // signal no peeked char; different from EOF

    Reader reader = null;

    int nextChar = NOCHAR; // last peeked character

    public LdapLexer() {}

    public LdapLexer(Reader r)
    {
    setReader(r);
    charno = 1;
    }

    public void setReader(Reader r)
    {
    reader = r;
    }

    /*
    The procedures get(),peek(),skipwhitespace(),getnw(), and peeknw()
    provide the essential LdapLexer interface.
    */

    public int get() throws IOException // any next char
    {
    if(nextChar == NOCHAR) return readChar();
    int c = nextChar;
    nextChar = NOCHAR;
    return c;
    }

    public int peek() throws IOException
    {
    if(nextChar == NOCHAR) {
        nextChar = readChar();
    }
    return nextChar;
    }

    void skipwhitespace() throws IOException
    {
        while (Character.isWhitespace((char) peek())) get();
    }

    public int getnw() throws IOException // next non-whitespace char
    {					   // (note: not essential but useful)
    skipwhitespace();
    return get();
    }

    public int peeknw() throws IOException // next non-whitespace char
    {					   // (note: not essential but useful)
    skipwhitespace();
    return peek();
    }

    // Following is for error reporting

    // Pass all character reads thru this so we can track char count

    int charno; // 1-based

    public int charno() {return charno;}

    int readChar() throws IOException
    {
    charno++;
    return reader.read();
    }

}
