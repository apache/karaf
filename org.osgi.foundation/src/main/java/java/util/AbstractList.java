/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/AbstractList.java,v 1.6 2006/03/14 01:20:25 hargrave Exp $
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
public abstract class AbstractList extends java.util.AbstractCollection implements java.util.List {
	protected AbstractList() { }
	public void add(int var0, java.lang.Object var1) { }
	public boolean add(java.lang.Object var0) { return false; }
	public boolean addAll(int var0, java.util.Collection var1) { return false; }
	public void clear() { }
	public boolean equals(java.lang.Object var0) { return false; }
	public abstract java.lang.Object get(int var0);
	public int hashCode() { return 0; }
	public int indexOf(java.lang.Object var0) { return 0; }
	public java.util.Iterator iterator() { return null; }
	public int lastIndexOf(java.lang.Object var0) { return 0; }
	public java.util.ListIterator listIterator() { return null; }
	public java.util.ListIterator listIterator(int var0) { return null; }
	public java.lang.Object remove(int var0) { return null; }
	protected void removeRange(int var0, int var1) { }
	public java.lang.Object set(int var0, java.lang.Object var1) { return null; }
	public java.util.List subList(int var0, int var1) { return null; }
	protected int modCount;
}

