/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/Provider.java,v 1.6 2006/03/14 01:20:27 hargrave Exp $
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
public abstract class Provider extends java.util.Properties {
	protected Provider(java.lang.String var0, double var1, java.lang.String var2) { }
	public void clear() { }
	public java.util.Set entrySet() { return null; }
	public java.lang.String getInfo() { return null; }
	public java.lang.String getName() { return null; }
	public double getVersion() { return 0.0d; }
	public java.util.Set keySet() { return null; }
	public void load(java.io.InputStream var0) throws java.io.IOException { }
	public java.lang.Object put(java.lang.Object var0, java.lang.Object var1) { return null; }
	public void putAll(java.util.Map var0) { }
	public java.lang.Object remove(java.lang.Object var0) { return null; }
	public java.lang.String toString() { return null; }
	public java.util.Collection values() { return null; }
}

