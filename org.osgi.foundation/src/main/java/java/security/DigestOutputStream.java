/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/DigestOutputStream.java,v 1.6 2006/03/14 01:20:27 hargrave Exp $
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

package java.security;
public class DigestOutputStream extends java.io.FilterOutputStream {
	public DigestOutputStream(java.io.OutputStream var0, java.security.MessageDigest var1) { super((java.io.OutputStream) null); }
	public java.security.MessageDigest getMessageDigest() { return null; }
	public void on(boolean var0) { }
	public void setMessageDigest(java.security.MessageDigest var0) { }
	public java.lang.String toString() { return null; }
	public void write(byte[] var0, int var1, int var2) throws java.io.IOException { }
	public void write(int var0) throws java.io.IOException { }
	protected java.security.MessageDigest digest;
}

