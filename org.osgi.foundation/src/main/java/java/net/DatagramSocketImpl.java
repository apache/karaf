/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/DatagramSocketImpl.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
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
public abstract class DatagramSocketImpl implements java.net.SocketOptions {
	public DatagramSocketImpl() { }
	protected abstract void bind(int var0, java.net.InetAddress var1) throws java.net.SocketException;
	protected abstract void close();
	protected abstract void create() throws java.net.SocketException;
	protected java.io.FileDescriptor getFileDescriptor() { return null; }
	protected int getLocalPort() { return 0; }
	public abstract java.lang.Object getOption(int var0) throws java.net.SocketException;
	protected abstract int getTimeToLive() throws java.io.IOException;
	protected abstract void join(java.net.InetAddress var0) throws java.io.IOException;
	protected abstract void leave(java.net.InetAddress var0) throws java.io.IOException;
	protected abstract int peek(java.net.InetAddress var0) throws java.io.IOException;
	protected abstract void receive(java.net.DatagramPacket var0) throws java.io.IOException;
	protected abstract void send(java.net.DatagramPacket var0) throws java.io.IOException;
	public abstract void setOption(int var0, java.lang.Object var1) throws java.net.SocketException;
	protected abstract void setTimeToLive(int var0) throws java.io.IOException;
	protected java.io.FileDescriptor fd;
	protected int localPort;
}

