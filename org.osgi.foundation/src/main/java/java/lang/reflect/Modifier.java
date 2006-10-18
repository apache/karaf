/*
 * $Header: /cvshome/build/ee.foundation/src/java/lang/reflect/Modifier.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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

package java.lang.reflect;
public class Modifier {
	public Modifier() { }
	public static boolean isAbstract(int var0) { return false; }
	public static boolean isFinal(int var0) { return false; }
	public static boolean isInterface(int var0) { return false; }
	public static boolean isNative(int var0) { return false; }
	public static boolean isPrivate(int var0) { return false; }
	public static boolean isProtected(int var0) { return false; }
	public static boolean isPublic(int var0) { return false; }
	public static boolean isStatic(int var0) { return false; }
	public static boolean isStrict(int var0) { return false; }
	public static boolean isSynchronized(int var0) { return false; }
	public static boolean isTransient(int var0) { return false; }
	public static boolean isVolatile(int var0) { return false; }
	public static java.lang.String toString(int var0) { return null; }
	public final static int PUBLIC = 1;
	public final static int PRIVATE = 2;
	public final static int PROTECTED = 4;
	public final static int STATIC = 8;
	public final static int FINAL = 16;
	public final static int SYNCHRONIZED = 32;
	public final static int VOLATILE = 64;
	public final static int TRANSIENT = 128;
	public final static int NATIVE = 256;
	public final static int INTERFACE = 512;
	public final static int ABSTRACT = 1024;
	public final static int STRICT = 2048;
}

