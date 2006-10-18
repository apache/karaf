/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/zip/GZIPOutputStream.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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

package java.util.zip;
public class GZIPOutputStream extends java.util.zip.DeflaterOutputStream {
	public GZIPOutputStream(java.io.OutputStream var0) throws java.io.IOException { super((java.io.OutputStream) null, (java.util.zip.Deflater) null, 0); }
	public GZIPOutputStream(java.io.OutputStream var0, int var1) throws java.io.IOException { super((java.io.OutputStream) null, (java.util.zip.Deflater) null, 0); }
	public void finish() throws java.io.IOException { }
	public void close() throws java.io.IOException { }
	public void write(byte[] var0, int var1, int var2) throws java.io.IOException { }
	protected java.util.zip.CRC32 crc;
}

