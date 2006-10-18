/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/URLClassLoader.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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
public class URLClassLoader extends java.security.SecureClassLoader {
	public URLClassLoader(java.net.URL[] var0) { }
	public URLClassLoader(java.net.URL[] var0, java.lang.ClassLoader var1) { }
	protected void addURL(java.net.URL var0) { }
	public java.util.Enumeration findResources(java.lang.String var0) throws java.io.IOException { return null; }
	protected java.security.PermissionCollection getPermissions(java.security.CodeSource var0) { return null; }
	public java.net.URL[] getURLs() { return null; }
	public static java.net.URLClassLoader newInstance(java.net.URL[] var0) { return null; }
	public static java.net.URLClassLoader newInstance(java.net.URL[] var0, java.lang.ClassLoader var1) { return null; }
	public URLClassLoader(java.net.URL[] var0, java.lang.ClassLoader var1, java.net.URLStreamHandlerFactory var2) { }
	protected java.lang.Class findClass(java.lang.String var0) throws java.lang.ClassNotFoundException { return null; }
	public java.net.URL findResource(java.lang.String var0) { return null; }
	protected java.lang.Package definePackage(java.lang.String var0, java.util.jar.Manifest var1, java.net.URL var2) throws java.lang.IllegalArgumentException { return null; }
}

