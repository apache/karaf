/*
 * $Header: /cvshome/build/ee.foundation/src/java/io/StreamTokenizer.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
 *
 * (C) Copyright 2001 Sun Microsystems, Inc.
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.io;
public class StreamTokenizer {
	public StreamTokenizer(java.io.Reader var0) { }
	public void commentChar(int var0) { }
	public void eolIsSignificant(boolean var0) { }
	public int lineno() { return 0; }
	public void lowerCaseMode(boolean var0) { }
	public int nextToken() throws java.io.IOException { return 0; }
	public void ordinaryChar(int var0) { }
	public void ordinaryChars(int var0, int var1) { }
	public void parseNumbers() { }
	public void pushBack() { }
	public void quoteChar(int var0) { }
	public void resetSyntax() { }
	public void slashSlashComments(boolean var0) { }
	public void slashStarComments(boolean var0) { }
	public java.lang.String toString() { return null; }
	public void whitespaceChars(int var0, int var1) { }
	public void wordChars(int var0, int var1) { }
	public double nval;
	public java.lang.String sval;
	public final static int TT_EOF = -1;
	public final static int TT_EOL = 10;
	public final static int TT_NUMBER = -2;
	public final static int TT_WORD = -3;
	public int ttype;
}

