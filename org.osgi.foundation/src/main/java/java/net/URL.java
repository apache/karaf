/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/URL.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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

package java.net;
public final class URL implements java.io.Serializable {
	public static void setURLStreamHandlerFactory(java.net.URLStreamHandlerFactory var0) { }
	public URL(java.lang.String var0) throws java.net.MalformedURLException { }
	public URL(java.net.URL var0, java.lang.String var1) throws java.net.MalformedURLException { }
	public URL(java.net.URL var0, java.lang.String var1, java.net.URLStreamHandler var2) throws java.net.MalformedURLException { }
	public URL(java.lang.String var0, java.lang.String var1, java.lang.String var2) throws java.net.MalformedURLException { }
	public URL(java.lang.String var0, java.lang.String var1, int var2, java.lang.String var3) throws java.net.MalformedURLException { }
	public URL(java.lang.String var0, java.lang.String var1, int var2, java.lang.String var3, java.net.URLStreamHandler var4) throws java.net.MalformedURLException { }
	protected void set(java.lang.String var0, java.lang.String var1, int var2, java.lang.String var3, java.lang.String var4) { }
	public boolean equals(java.lang.Object var0) { return false; }
	public boolean sameFile(java.net.URL var0) { return false; }
	public int hashCode() { return 0; }
	public final java.lang.Object getContent() throws java.io.IOException { return null; }
	public final java.lang.Object getContent(java.lang.Class[] var0) throws java.io.IOException { return null; }
	public final java.io.InputStream openStream() throws java.io.IOException { return null; }
	public java.net.URLConnection openConnection() throws java.io.IOException { return null; }
	public java.lang.String toString() { return null; }
	public java.lang.String toExternalForm() { return null; }
	public java.lang.String getFile() { return null; }
	public java.lang.String getHost() { return null; }
	public int getPort() { return 0; }
	public java.lang.String getProtocol() { return null; }
	public java.lang.String getRef() { return null; }
	public java.lang.String getQuery() { return null; }
	public java.lang.String getPath() { return null; }
	public java.lang.String getUserInfo() { return null; }
	public java.lang.String getAuthority() { return null; }
	protected void set(java.lang.String var0, java.lang.String var1, int var2, java.lang.String var3, java.lang.String var4, java.lang.String var5, java.lang.String var6, java.lang.String var7) { }
}

