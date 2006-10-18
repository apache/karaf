/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/Properties.java,v 1.6 2006/03/14 01:20:25 hargrave Exp $
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

package java.util;
public class Properties extends java.util.Hashtable {
	public Properties() { }
	public Properties(java.util.Properties var0) { }
	public java.lang.String getProperty(java.lang.String var0) { return null; }
	public java.lang.String getProperty(java.lang.String var0, java.lang.String var1) { return null; }
	public void list(java.io.PrintStream var0) { }
	public void list(java.io.PrintWriter var0) { }
	public void load(java.io.InputStream var0) throws java.io.IOException { }
	public java.util.Enumeration propertyNames() { return null; }
	public void save(java.io.OutputStream var0, java.lang.String var1) { }
	public java.lang.Object setProperty(java.lang.String var0, java.lang.String var1) { return null; }
	public void store(java.io.OutputStream var0, java.lang.String var1) throws java.io.IOException { }
	protected java.util.Properties defaults;
}

