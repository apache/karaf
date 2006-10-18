/*
 * $Header: /cvshome/build/ee.foundation/src/javax/microedition/io/Datagram.java,v 1.6 2006/03/14 01:20:29 hargrave Exp $
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

package javax.microedition.io;
public abstract interface Datagram extends java.io.DataInput, java.io.DataOutput {
	public abstract java.lang.String getAddress();
	public abstract byte[] getData();
	public abstract int getLength();
	public abstract int getOffset();
	public abstract void reset();
	public abstract void setAddress(javax.microedition.io.Datagram var0);
	public abstract void setAddress(java.lang.String var0) throws java.io.IOException;
	public abstract void setData(byte[] var0, int var1, int var2);
	public abstract void setLength(int var0);
}

