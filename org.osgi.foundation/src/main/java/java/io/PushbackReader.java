/*
 * $Header: /cvshome/build/ee.foundation/src/java/io/PushbackReader.java,v 1.6 2006/03/14 01:20:24 hargrave Exp $
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
public class PushbackReader extends java.io.FilterReader {
	public PushbackReader(java.io.Reader var0) { super((java.io.Reader) null); }
	public PushbackReader(java.io.Reader var0, int var1) { super((java.io.Reader) null); }
	public void close() throws java.io.IOException { }
	public void mark(int var0) throws java.io.IOException { }
	public boolean markSupported() { return false; }
	public int read() throws java.io.IOException { return 0; }
	public int read(char[] var0, int var1, int var2) throws java.io.IOException { return 0; }
	public boolean ready() throws java.io.IOException { return false; }
	public void reset() throws java.io.IOException { }
	public void unread(char[] var0) throws java.io.IOException { }
	public void unread(char[] var0, int var1, int var2) throws java.io.IOException { }
	public void unread(int var0) throws java.io.IOException { }
}

