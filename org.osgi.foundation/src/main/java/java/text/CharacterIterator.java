/*
 * $Header: /cvshome/build/ee.foundation/src/java/text/CharacterIterator.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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

package java.text;
public abstract interface CharacterIterator extends java.lang.Cloneable {
	public abstract java.lang.Object clone();
	public abstract char current();
	public abstract char first();
	public abstract int getBeginIndex();
	public abstract int getEndIndex();
	public abstract int getIndex();
	public abstract char last();
	public abstract char next();
	public abstract char previous();
	public abstract char setIndex(int var0);
	public final static char DONE = 65535;
}

