/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/ServerSocket.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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

package java.net;
public class ServerSocket {
	public ServerSocket(int var0) throws java.io.IOException { }
	public ServerSocket(int var0, int var1) throws java.io.IOException { }
	public ServerSocket(int var0, int var1, java.net.InetAddress var2) throws java.io.IOException { }
	public java.net.Socket accept() throws java.io.IOException { return null; }
	public void close() throws java.io.IOException { }
	public java.net.InetAddress getInetAddress() { return null; }
	public int getLocalPort() { return 0; }
	public int getSoTimeout() throws java.io.IOException { return 0; }
	protected final void implAccept(java.net.Socket var0) throws java.io.IOException { }
	public static void setSocketFactory(java.net.SocketImplFactory var0) throws java.io.IOException { }
	public void setSoTimeout(int var0) throws java.net.SocketException { }
	public java.lang.String toString() { return null; }
}

