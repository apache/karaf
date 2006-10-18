/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/MulticastSocket.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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
public class MulticastSocket extends java.net.DatagramSocket {
	public MulticastSocket() throws java.io.IOException { }
	public MulticastSocket(int var0) throws java.io.IOException { }
	public java.net.InetAddress getInterface() throws java.net.SocketException { return null; }
	public int getTimeToLive() throws java.io.IOException { return 0; }
	public void joinGroup(java.net.InetAddress var0) throws java.io.IOException { }
	public void leaveGroup(java.net.InetAddress var0) throws java.io.IOException { }
	public void send(java.net.DatagramPacket var0, byte var1) throws java.io.IOException { }
	public void setInterface(java.net.InetAddress var0) throws java.net.SocketException { }
	public void setTimeToLive(int var0) throws java.io.IOException { }
}

