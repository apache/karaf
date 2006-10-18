/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/URLConnection.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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
public abstract class URLConnection {
	protected URLConnection(java.net.URL var0) { }
	public abstract void connect() throws java.io.IOException;
	public boolean getAllowUserInteraction() { return false; }
	public java.lang.Object getContent() throws java.io.IOException { return null; }
	public java.lang.Object getContent(java.lang.Class[] var0) throws java.io.IOException { return null; }
	public java.lang.String getContentEncoding() { return null; }
	public int getContentLength() { return 0; }
	public java.lang.String getContentType() { return null; }
	public long getDate() { return 0l; }
	public static boolean getDefaultAllowUserInteraction() { return false; }
	public boolean getDefaultUseCaches() { return false; }
	public boolean getDoInput() { return false; }
	public boolean getDoOutput() { return false; }
	public long getExpiration() { return 0l; }
	public static java.net.FileNameMap getFileNameMap() { return null; }
	public java.lang.String getHeaderField(int var0) { return null; }
	public java.lang.String getHeaderField(java.lang.String var0) { return null; }
	public long getHeaderFieldDate(java.lang.String var0, long var1) { return 0l; }
	public int getHeaderFieldInt(java.lang.String var0, int var1) { return 0; }
	public java.lang.String getHeaderFieldKey(int var0) { return null; }
	public long getIfModifiedSince() { return 0l; }
	public java.io.InputStream getInputStream() throws java.io.IOException { return null; }
	public long getLastModified() { return 0l; }
	public java.io.OutputStream getOutputStream() throws java.io.IOException { return null; }
	public java.security.Permission getPermission() throws java.io.IOException { return null; }
	public java.lang.String getRequestProperty(java.lang.String var0) { return null; }
	public java.net.URL getURL() { return null; }
	public boolean getUseCaches() { return false; }
	protected static java.lang.String guessContentTypeFromName(java.lang.String var0) { return null; }
	public static java.lang.String guessContentTypeFromStream(java.io.InputStream var0) throws java.io.IOException { return null; }
	public void setAllowUserInteraction(boolean var0) { }
	public static void setContentHandlerFactory(java.net.ContentHandlerFactory var0) { }
	public static void setDefaultAllowUserInteraction(boolean var0) { }
	public void setDefaultUseCaches(boolean var0) { }
	public void setDoInput(boolean var0) { }
	public void setDoOutput(boolean var0) { }
	public static void setFileNameMap(java.net.FileNameMap var0) { }
	public void setIfModifiedSince(long var0) { }
	public void setRequestProperty(java.lang.String var0, java.lang.String var1) { }
	public void setUseCaches(boolean var0) { }
	public java.lang.String toString() { return null; }
	protected java.net.URL url;
	protected long ifModifiedSince;
	protected boolean useCaches;
	protected boolean connected;
	protected boolean doOutput;
	protected boolean doInput;
	protected boolean allowUserInteraction;
}

