/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/jar/Attributes.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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

package java.util.jar;
public class Attributes implements java.lang.Cloneable, java.util.Map {
	public Attributes() { }
	public Attributes(java.util.jar.Attributes var0) { }
	public Attributes(int var0) { }
	public void clear() { }
	public boolean containsKey(java.lang.Object var0) { return false; }
	public boolean containsValue(java.lang.Object var0) { return false; }
	public java.util.Set entrySet() { return null; }
	public java.lang.Object get(java.lang.Object var0) { return null; }
	public boolean isEmpty() { return false; }
	public java.util.Set keySet() { return null; }
	public java.lang.Object put(java.lang.Object var0, java.lang.Object var1) { return null; }
	public void putAll(java.util.Map var0) { }
	public java.lang.Object remove(java.lang.Object var0) { return null; }
	public int size() { return 0; }
	public java.util.Collection values() { return null; }
	public java.lang.Object clone() { return null; }
	public int hashCode() { return 0; }
	public boolean equals(java.lang.Object var0) { return false; }
	public java.lang.String getValue(java.util.jar.Attributes.Name var0) { return null; }
	public java.lang.String getValue(java.lang.String var0) { return null; }
	public java.lang.String putValue(java.lang.String var0, java.lang.String var1) { return null; }
	protected java.util.Map map;
	public static class Name {
		public Name(java.lang.String var0) { }
		public java.lang.String toString() { return null; }
		public boolean equals(java.lang.Object var0) { return false; }
		public int hashCode() { return 0; }
		public final static java.util.jar.Attributes.Name CLASS_PATH; static { CLASS_PATH = null; }
		public final static java.util.jar.Attributes.Name MANIFEST_VERSION; static { MANIFEST_VERSION = null; }
		public final static java.util.jar.Attributes.Name MAIN_CLASS; static { MAIN_CLASS = null; }
		public final static java.util.jar.Attributes.Name SIGNATURE_VERSION; static { SIGNATURE_VERSION = null; }
		public final static java.util.jar.Attributes.Name CONTENT_TYPE; static { CONTENT_TYPE = null; }
		public final static java.util.jar.Attributes.Name SEALED; static { SEALED = null; }
		public final static java.util.jar.Attributes.Name IMPLEMENTATION_TITLE; static { IMPLEMENTATION_TITLE = null; }
		public final static java.util.jar.Attributes.Name IMPLEMENTATION_VERSION; static { IMPLEMENTATION_VERSION = null; }
		public final static java.util.jar.Attributes.Name IMPLEMENTATION_VENDOR; static { IMPLEMENTATION_VENDOR = null; }
		public final static java.util.jar.Attributes.Name SPECIFICATION_TITLE; static { SPECIFICATION_TITLE = null; }
		public final static java.util.jar.Attributes.Name SPECIFICATION_VERSION; static { SPECIFICATION_VERSION = null; }
		public final static java.util.jar.Attributes.Name SPECIFICATION_VENDOR; static { SPECIFICATION_VENDOR = null; }
		public final static java.util.jar.Attributes.Name EXTENSION_LIST; static { EXTENSION_LIST = null; }
		public final static java.util.jar.Attributes.Name EXTENSION_NAME; static { EXTENSION_NAME = null; }
		public final static java.util.jar.Attributes.Name EXTENSION_INSTALLATION; static { EXTENSION_INSTALLATION = null; }
		public final static java.util.jar.Attributes.Name IMPLEMENTATION_VENDOR_ID; static { IMPLEMENTATION_VENDOR_ID = null; }
		public final static java.util.jar.Attributes.Name IMPLEMENTATION_URL; static { IMPLEMENTATION_URL = null; }
	}
}

