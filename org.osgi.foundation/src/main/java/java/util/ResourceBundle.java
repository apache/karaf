/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/ResourceBundle.java,v 1.6 2006/03/14 01:20:26 hargrave Exp $
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
public abstract class ResourceBundle {
	public ResourceBundle() { }
	public final static java.util.ResourceBundle getBundle(java.lang.String var0) throws java.util.MissingResourceException { return null; }
	public final static java.util.ResourceBundle getBundle(java.lang.String var0, java.util.Locale var1) { return null; }
	public static java.util.ResourceBundle getBundle(java.lang.String var0, java.util.Locale var1, java.lang.ClassLoader var2) throws java.util.MissingResourceException { return null; }
	public abstract java.util.Enumeration getKeys();
	public java.util.Locale getLocale() { return null; }
	public final java.lang.Object getObject(java.lang.String var0) throws java.util.MissingResourceException { return null; }
	public final java.lang.String getString(java.lang.String var0) throws java.util.MissingResourceException { return null; }
	public final java.lang.String[] getStringArray(java.lang.String var0) throws java.util.MissingResourceException { return null; }
	protected abstract java.lang.Object handleGetObject(java.lang.String var0) throws java.util.MissingResourceException;
	protected void setParent(java.util.ResourceBundle var0) { }
	protected java.util.ResourceBundle parent;
}

