/*
 * $Header: /cvshome/build/ee.foundation/src/java/io/DataOutputStream.java,v 1.6 2006/03/14 01:20:24 hargrave Exp $
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
public class DataOutputStream extends java.io.FilterOutputStream implements java.io.DataOutput {
	public DataOutputStream(java.io.OutputStream var0) { super((java.io.OutputStream) null); }
	public void flush() throws java.io.IOException { }
	public final int size() { return 0; }
	public void write(byte[] var0, int var1, int var2) throws java.io.IOException { }
	public void write(int var0) throws java.io.IOException { }
	public final void writeBoolean(boolean var0) throws java.io.IOException { }
	public final void writeByte(int var0) throws java.io.IOException { }
	public final void writeBytes(java.lang.String var0) throws java.io.IOException { }
	public final void writeChar(int var0) throws java.io.IOException { }
	public final void writeChars(java.lang.String var0) throws java.io.IOException { }
	public final void writeDouble(double var0) throws java.io.IOException { }
	public final void writeFloat(float var0) throws java.io.IOException { }
	public final void writeInt(int var0) throws java.io.IOException { }
	public final void writeLong(long var0) throws java.io.IOException { }
	public final void writeShort(int var0) throws java.io.IOException { }
	public final void writeUTF(java.lang.String var0) throws java.io.IOException { }
	protected int written;
}

