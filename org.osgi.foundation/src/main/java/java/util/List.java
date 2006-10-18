/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/List.java,v 1.6 2006/03/14 01:20:25 hargrave Exp $
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
public abstract interface List extends java.util.Collection {
	public abstract void add(int var0, java.lang.Object var1);
	public abstract boolean add(java.lang.Object var0);
	public abstract boolean addAll(int var0, java.util.Collection var1);
	public abstract boolean addAll(java.util.Collection var0);
	public abstract void clear();
	public abstract boolean contains(java.lang.Object var0);
	public abstract boolean containsAll(java.util.Collection var0);
	public abstract boolean equals(java.lang.Object var0);
	public abstract java.lang.Object get(int var0);
	public abstract int hashCode();
	public abstract int indexOf(java.lang.Object var0);
	public abstract boolean isEmpty();
	public abstract java.util.Iterator iterator();
	public abstract int lastIndexOf(java.lang.Object var0);
	public abstract java.util.ListIterator listIterator();
	public abstract java.util.ListIterator listIterator(int var0);
	public abstract java.lang.Object remove(int var0);
	public abstract boolean remove(java.lang.Object var0);
	public abstract boolean removeAll(java.util.Collection var0);
	public abstract boolean retainAll(java.util.Collection var0);
	public abstract java.lang.Object set(int var0, java.lang.Object var1);
	public abstract int size();
	public abstract java.util.List subList(int var0, int var1);
	public abstract java.lang.Object[] toArray();
	public abstract java.lang.Object[] toArray(java.lang.Object[] var0);
}

