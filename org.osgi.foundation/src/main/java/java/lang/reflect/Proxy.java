/*
 * $Header: /cvshome/build/ee.foundation/src/java/lang/reflect/Proxy.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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
public class Proxy implements java.io.Serializable {
	protected Proxy(java.lang.reflect.InvocationHandler var0) { }
	public static java.lang.Class getProxyClass(java.lang.ClassLoader var0, java.lang.Class[] var1) throws java.lang.IllegalArgumentException { return null; }
	public static java.lang.Object newProxyInstance(java.lang.ClassLoader var0, java.lang.Class[] var1, java.lang.reflect.InvocationHandler var2) throws java.lang.IllegalArgumentException { return null; }
	public static boolean isProxyClass(java.lang.Class var0) { return false; }
	public static java.lang.reflect.InvocationHandler getInvocationHandler(java.lang.Object var0) throws java.lang.IllegalArgumentException { return null; }
	protected java.lang.reflect.InvocationHandler h;
}

