/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/JarURLConnection.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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
public abstract class JarURLConnection extends java.net.URLConnection {
	protected JarURLConnection(java.net.URL var0) throws java.net.MalformedURLException { super((java.net.URL) null); }
	public java.util.jar.Attributes getAttributes() throws java.io.IOException { return null; }
	public java.security.cert.Certificate[] getCertificates() throws java.io.IOException { return null; }
	public java.lang.String getEntryName() { return null; }
	public java.util.jar.JarEntry getJarEntry() throws java.io.IOException { return null; }
	public java.util.jar.Manifest getManifest() throws java.io.IOException { return null; }
	public abstract java.util.jar.JarFile getJarFile() throws java.io.IOException;
	public java.net.URL getJarFileURL() { return null; }
	public java.util.jar.Attributes getMainAttributes() throws java.io.IOException { return null; }
	protected java.net.URLConnection jarFileURLConnection;
}

