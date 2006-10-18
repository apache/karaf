/*
 * $Header: /cvshome/build/ee.foundation/src/java/text/AttributedCharacterIterator.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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
public abstract interface AttributedCharacterIterator extends java.text.CharacterIterator {
	public abstract java.util.Set getAllAttributeKeys();
	public abstract java.lang.Object getAttribute(java.text.AttributedCharacterIterator.Attribute var0);
	public abstract java.util.Map getAttributes();
	public abstract int getRunLimit();
	public abstract int getRunLimit(java.text.AttributedCharacterIterator.Attribute var0);
	public abstract int getRunLimit(java.util.Set var0);
	public abstract int getRunStart();
	public abstract int getRunStart(java.text.AttributedCharacterIterator.Attribute var0);
	public abstract int getRunStart(java.util.Set var0);
	public static class Attribute implements java.io.Serializable {
		protected Attribute(java.lang.String var0) { }
		public final boolean equals(java.lang.Object var0) { return false; }
		protected java.lang.String getName() { return null; }
		public final int hashCode() { return 0; }
		protected java.lang.Object readResolve() throws java.io.InvalidObjectException { return null; }
		public java.lang.String toString() { return null; }
		public final static java.text.AttributedCharacterIterator.Attribute INPUT_METHOD_SEGMENT; static { INPUT_METHOD_SEGMENT = null; }
		public final static java.text.AttributedCharacterIterator.Attribute LANGUAGE; static { LANGUAGE = null; }
		public final static java.text.AttributedCharacterIterator.Attribute READING; static { READING = null; }
	}
}

