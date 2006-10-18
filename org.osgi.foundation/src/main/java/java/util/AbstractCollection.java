/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/AbstractCollection.java,v 1.6 2006/03/14 01:20:25 hargrave Exp $
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
public abstract class AbstractCollection implements java.util.Collection {
	protected AbstractCollection() { }
	public boolean add(java.lang.Object var0) { return false; }
	public boolean addAll(java.util.Collection var0) { return false; }
	public void clear() { }
	public boolean contains(java.lang.Object var0) { return false; }
	public boolean containsAll(java.util.Collection var0) { return false; }
	public boolean isEmpty() { return false; }
	public abstract java.util.Iterator iterator();
	public boolean remove(java.lang.Object var0) { return false; }
	public boolean removeAll(java.util.Collection var0) { return false; }
	public boolean retainAll(java.util.Collection var0) { return false; }
	public abstract int size();
	public java.lang.Object[] toArray() { return null; }
	public java.lang.Object[] toArray(java.lang.Object[] var0) { return null; }
	public java.lang.String toString() { return null; }
}

