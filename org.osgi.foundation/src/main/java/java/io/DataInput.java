/*
 * $Header: /cvshome/build/ee.foundation/src/java/io/DataInput.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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
public abstract interface DataInput {
	public abstract boolean readBoolean() throws java.io.IOException;
	public abstract byte readByte() throws java.io.IOException;
	public abstract char readChar() throws java.io.IOException;
	public abstract double readDouble() throws java.io.IOException;
	public abstract float readFloat() throws java.io.IOException;
	public abstract void readFully(byte[] var0) throws java.io.IOException;
	public abstract void readFully(byte[] var0, int var1, int var2) throws java.io.IOException;
	public abstract int readInt() throws java.io.IOException;
	public abstract java.lang.String readLine() throws java.io.IOException;
	public abstract long readLong() throws java.io.IOException;
	public abstract short readShort() throws java.io.IOException;
	public abstract int readUnsignedByte() throws java.io.IOException;
	public abstract int readUnsignedShort() throws java.io.IOException;
	public abstract java.lang.String readUTF() throws java.io.IOException;
	public abstract int skipBytes(int var0) throws java.io.IOException;
}

