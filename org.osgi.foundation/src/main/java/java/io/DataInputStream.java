/*
 * $Header: /cvshome/build/ee.foundation/src/java/io/DataInputStream.java,v 1.6 2006/03/14 01:20:24 hargrave Exp $
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
public class DataInputStream extends java.io.FilterInputStream implements java.io.DataInput {
	public DataInputStream(java.io.InputStream var0) { super((java.io.InputStream) null); }
	public final int read(byte[] var0) throws java.io.IOException { return 0; }
	public final int read(byte[] var0, int var1, int var2) throws java.io.IOException { return 0; }
	public final boolean readBoolean() throws java.io.IOException { return false; }
	public final byte readByte() throws java.io.IOException { return 0; }
	public final char readChar() throws java.io.IOException { return 0; }
	public final double readDouble() throws java.io.IOException { return 0.0d; }
	public final float readFloat() throws java.io.IOException { return 0.0f; }
	public final void readFully(byte[] var0) throws java.io.IOException { }
	public final void readFully(byte[] var0, int var1, int var2) throws java.io.IOException { }
	public final int readInt() throws java.io.IOException { return 0; }
	public final java.lang.String readLine() throws java.io.IOException { return null; }
	public final long readLong() throws java.io.IOException { return 0l; }
	public final short readShort() throws java.io.IOException { return 0; }
	public final int readUnsignedByte() throws java.io.IOException { return 0; }
	public final int readUnsignedShort() throws java.io.IOException { return 0; }
	public final java.lang.String readUTF() throws java.io.IOException { return null; }
	public final static java.lang.String readUTF(java.io.DataInput var0) throws java.io.IOException { return null; }
	public final int skipBytes(int var0) throws java.io.IOException { return 0; }
}

