/*
 * $Header: /cvshome/build/ee.foundation/src/java/text/CollationElementIterator.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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
public final class CollationElementIterator {
	public void reset() { }
	public int next() { return 0; }
	public int previous() { return 0; }
	public final static int primaryOrder(int var0) { return 0; }
	public final static short secondaryOrder(int var0) { return 0; }
	public final static short tertiaryOrder(int var0) { return 0; }
	public void setOffset(int var0) { }
	public int getOffset() { return 0; }
	public int getMaxExpansion(int var0) { return 0; }
	public void setText(java.lang.String var0) { }
	public void setText(java.text.CharacterIterator var0) { }
	public final static int NULLORDER = -1;
	private CollationElementIterator() { } /* generated constructor to prevent compiler adding default public constructor */
}

