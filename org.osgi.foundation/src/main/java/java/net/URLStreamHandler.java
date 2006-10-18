/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/URLStreamHandler.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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
public abstract class URLStreamHandler {
	public URLStreamHandler() { }
	protected abstract java.net.URLConnection openConnection(java.net.URL var0) throws java.io.IOException;
	protected void parseURL(java.net.URL var0, java.lang.String var1, int var2, int var3) { }
	protected void setURL(java.net.URL var0, java.lang.String var1, java.lang.String var2, int var3, java.lang.String var4, java.lang.String var5, java.lang.String var6, java.lang.String var7, java.lang.String var8) { }
	protected java.lang.String toExternalForm(java.net.URL var0) { return null; }
	protected boolean equals(java.net.URL var0, java.net.URL var1) { return false; }
	protected int getDefaultPort() { return 0; }
	protected java.net.InetAddress getHostAddress(java.net.URL var0) { return null; }
	protected int hashCode(java.net.URL var0) { return 0; }
	protected boolean hostsEqual(java.net.URL var0, java.net.URL var1) { return false; }
	protected boolean sameFile(java.net.URL var0, java.net.URL var1) { return false; }
}

