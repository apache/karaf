/*
 * $Header: /cvshome/build/ee.foundation/src/javax/microedition/io/DatagramConnection.java,v 1.6 2006/03/14 01:20:29 hargrave Exp $
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
public abstract interface DatagramConnection extends javax.microedition.io.Connection {
	public abstract int getMaximumLength() throws java.io.IOException;
	public abstract int getNominalLength() throws java.io.IOException;
	public abstract javax.microedition.io.Datagram newDatagram(byte[] var0, int var1) throws java.io.IOException;
	public abstract javax.microedition.io.Datagram newDatagram(byte[] var0, int var1, java.lang.String var2) throws java.io.IOException;
	public abstract javax.microedition.io.Datagram newDatagram(int var0) throws java.io.IOException;
	public abstract javax.microedition.io.Datagram newDatagram(int var0, java.lang.String var1) throws java.io.IOException;
	public abstract void receive(javax.microedition.io.Datagram var0) throws java.io.IOException;
	public abstract void send(javax.microedition.io.Datagram var0) throws java.io.IOException;
}

