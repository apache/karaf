/*
 * $Header: /cvshome/build/ee.foundation/src/java/io/RandomAccessFile.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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
public class RandomAccessFile implements java.io.DataInput, java.io.DataOutput {
	public RandomAccessFile(java.io.File var0, java.lang.String var1) throws java.io.FileNotFoundException { }
	public RandomAccessFile(java.lang.String var0, java.lang.String var1) throws java.io.FileNotFoundException { }
	public void close() throws java.io.IOException { }
	public final java.io.FileDescriptor getFD() throws java.io.IOException { return null; }
	public long getFilePointer() throws java.io.IOException { return 0l; }
	public long length() throws java.io.IOException { return 0l; }
	public int read() throws java.io.IOException { return 0; }
	public int read(byte[] var0) throws java.io.IOException { return 0; }
	public int read(byte[] var0, int var1, int var2) throws java.io.IOException { return 0; }
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
	public void seek(long var0) throws java.io.IOException { }
	public void setLength(long var0) throws java.io.IOException { }
	public int skipBytes(int var0) throws java.io.IOException { return 0; }
	public void write(byte[] var0) throws java.io.IOException { }
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
}

