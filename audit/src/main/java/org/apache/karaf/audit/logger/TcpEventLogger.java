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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

public class TcpEventLogger implements EventLogger {

    private final String host;
    private final int port;
    private final Charset encoding;
    private final EventLayout layout;
    private BufferedWriter writer;

    public TcpEventLogger(String host, int port, String encoding, EventLayout layout) throws IOException {
        this.host = host;
        this.port = port;
        this.encoding = Charset.forName(encoding);
        this.layout = layout;
    }

    @Override
    public void write(Event event) throws IOException {
        if (writer == null) {
            Socket socket = new Socket(host, port);
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), encoding));
        }
        layout.format(event, writer);
        writer.append("\n");
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    @Override
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
        }
    }
}
