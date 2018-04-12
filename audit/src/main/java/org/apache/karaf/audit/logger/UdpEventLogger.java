/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.audit.logger;

import org.apache.karaf.audit.Event;
import org.apache.karaf.audit.EventLayout;
import org.apache.karaf.audit.EventLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class UdpEventLogger implements EventLogger {

    private final InetAddress host;
    private final int port;
    private final CharsetEncoder encoder;
    private final EventLayout layout;
    private final DatagramSocket dgram;

    private ByteBuffer bb = ByteBuffer.allocate(1024);

    public UdpEventLogger(String host, int port, String encoding, EventLayout layout) throws SocketException, UnknownHostException {
        this.layout = layout;
        this.host = InetAddress.getByName(host);
        this.port = port;
        this.encoder = Charset.forName(encoding).newEncoder();
        this.dgram = new DatagramSocket();
    }

    @Override
    public void write(Event event) throws IOException {
        CharBuffer cb = layout.format(event);
        int cap = (int) (cb.remaining() * encoder.averageBytesPerChar());
        ByteBuffer bb;
        if (this.bb.capacity() > cap) {
            bb = this.bb;
        } else {
            bb = ByteBuffer.allocate(cap);
        }
        encoder.reset();
        encoder.encode(cb, bb, true);
        if (cb.remaining() > 0) {
            bb = ByteBuffer.allocate(bb.capacity() * 2);
            cb.position(0);
            encoder.reset();
            encoder.encode(cb, bb, true);
        }

        dgram.send(new DatagramPacket(bb.array(), 0, bb.position(), host, port));
        bb.position(0);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        dgram.close();
    }

}
