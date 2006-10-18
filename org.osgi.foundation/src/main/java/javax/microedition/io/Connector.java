/*
 * $Header: /cvshome/build/ee.foundation/src/javax/microedition/io/Connector.java,v 1.6 2006/03/14 01:20:29 hargrave Exp $
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

package javax.microedition.io;
public class Connector {
	public static javax.microedition.io.Connection open(java.lang.String var0) throws java.io.IOException { return null; }
	public static javax.microedition.io.Connection open(java.lang.String var0, int var1) throws java.io.IOException { return null; }
	public static javax.microedition.io.Connection open(java.lang.String var0, int var1, boolean var2) throws java.io.IOException { return null; }
	public static java.io.DataInputStream openDataInputStream(java.lang.String var0) throws java.io.IOException { return null; }
	public static java.io.DataOutputStream openDataOutputStream(java.lang.String var0) throws java.io.IOException { return null; }
	public static java.io.InputStream openInputStream(java.lang.String var0) throws java.io.IOException { return null; }
	public static java.io.OutputStream openOutputStream(java.lang.String var0) throws java.io.IOException { return null; }
	public final static int READ = 1;
	public final static int WRITE = 2;
	public final static int READ_WRITE = 3;
	private Connector() { } /* generated constructor to prevent compiler adding default public constructor */
}

