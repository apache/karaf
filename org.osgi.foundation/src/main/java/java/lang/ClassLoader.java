/*
 * $Header: /cvshome/build/ee.foundation/src/java/lang/ClassLoader.java,v 1.6 2006/03/14 01:20:24 hargrave Exp $
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

package java.lang;
public abstract class ClassLoader {
	protected ClassLoader() { }
	protected ClassLoader(java.lang.ClassLoader var0) { }
	protected final java.lang.Class defineClass(java.lang.String var0, byte[] var1, int var2, int var3) throws java.lang.ClassFormatError { return null; }
	protected final java.lang.Class defineClass(java.lang.String var0, byte[] var1, int var2, int var3, java.security.ProtectionDomain var4) throws java.lang.ClassFormatError { return null; }
	protected java.lang.Class findClass(java.lang.String var0) throws java.lang.ClassNotFoundException { return null; }
	protected final java.lang.Class findLoadedClass(java.lang.String var0) { return null; }
	protected final java.lang.Class findSystemClass(java.lang.String var0) throws java.lang.ClassNotFoundException { return null; }
	public final java.lang.ClassLoader getParent() { return null; }
	public java.net.URL getResource(java.lang.String var0) { return null; }
	public final java.util.Enumeration getResources(java.lang.String var0) throws java.io.IOException { return null; }
	public java.io.InputStream getResourceAsStream(java.lang.String var0) { return null; }
	public static java.lang.ClassLoader getSystemClassLoader() { return null; }
	public static java.net.URL getSystemResource(java.lang.String var0) { return null; }
	public static java.util.Enumeration getSystemResources(java.lang.String var0) throws java.io.IOException { return null; }
	public static java.io.InputStream getSystemResourceAsStream(java.lang.String var0) { return null; }
	public java.lang.Class loadClass(java.lang.String var0) throws java.lang.ClassNotFoundException { return null; }
	protected java.lang.Class loadClass(java.lang.String var0, boolean var1) throws java.lang.ClassNotFoundException { return null; }
	protected final void resolveClass(java.lang.Class var0) { }
	protected java.net.URL findResource(java.lang.String var0) { return null; }
	protected java.util.Enumeration findResources(java.lang.String var0) throws java.io.IOException { return null; }
	protected java.lang.String findLibrary(java.lang.String var0) { return null; }
	protected java.lang.Package getPackage(java.lang.String var0) { return null; }
	protected java.lang.Package[] getPackages() { return null; }
	protected java.lang.Package definePackage(java.lang.String var0, java.lang.String var1, java.lang.String var2, java.lang.String var3, java.lang.String var4, java.lang.String var5, java.lang.String var6, java.net.URL var7) throws java.lang.IllegalArgumentException { return null; }
	protected final void setSigners(java.lang.Class var0, java.lang.Object[] var1) { }
}

