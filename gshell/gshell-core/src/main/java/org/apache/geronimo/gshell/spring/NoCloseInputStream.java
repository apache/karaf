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
package org.apache.geronimo.gshell.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Wrap an InputStream in a complex structure so that it can
 * be reused and closed even when a read() method is blocking.
 *
 * This stream uses a PipedInputStream / PipedOutputStream to
 * decouple the read InputStream.  When the close() method is
 * called, the pipe will be broken, and a single character will
 * be eaten from the reader thread.
 */
public class NoCloseInputStream extends PipedInputStream {

    private final InputStream in;
    private final PipedOutputStream pos;
    private final Thread thread;
    private IOException exception;

    public NoCloseInputStream(InputStream in) throws IOException {
        this.in = in;
        pos = new PipedOutputStream(this);
        thread = new Thread() {
            public void run() {
                doRead();
            }
        };
        thread.start();
    }

    public synchronized int read() throws IOException {
        if (exception != null) {
            throw exception;
        }
        return super.read();
    }

    public synchronized int read(byte b[], int off, int len) throws IOException {
        if (exception != null) {
            throw exception;
        }
        return super.read(b, off, len);
    }

    public void close() throws IOException {
        super.close();
        pos.close();
        thread.interrupt();
    }

    protected void doRead() {
        try {
            int c;
            while ((c = in.read()) != -1) {
                pos.write(c);
                // Need to notify, else there is a 1 sec lag for the
                // echo to be displayed on the terminal.  The notify
                // will unblock the reader thread.
                synchronized (this) {
                    this.notifyAll();
                }
            }
        } catch (IOException e) {
            exception = e;
            try {
                pos.close();
            } catch (Exception e2) {
                // ignore
            }
        }
    }

}
