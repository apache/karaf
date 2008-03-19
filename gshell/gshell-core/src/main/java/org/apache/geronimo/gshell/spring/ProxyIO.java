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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.apache.geronimo.gshell.command.IO;

/**
 * An IO implementation that delegates the Input and Output
 * stream to another IO stored in a ThreadLocal.
 * The reason for this class is that Spring AOP can not proxy
 * fields and GShell always access the in, out and err fields
 * directly, hence the need to wrap these using delegates. 
 */
public class ProxyIO extends IO {

    private static final ThreadLocal<IO> TLS_IO = new ThreadLocal<IO>();

    public ProxyIO() {
        super(new ProxyInputStream() {
            protected InputStream getIn() {
                return TLS_IO.get().inputStream;
            }
        }, new ProxyOutputStream() {
            protected OutputStream getOut() {
                return TLS_IO.get().outputStream;
            }
        }, new ProxyOutputStream() {
            protected OutputStream getOut() {
                return TLS_IO.get().errorStream;
            }
        });
    }

    public static void setIO(IO io) {
        TLS_IO.set(io);
    }

    public static IO getIO() {
        return TLS_IO.get();
    }

    protected static abstract class ProxyInputStream extends InputStream {
        public int read() throws IOException {
            return getIn().read();
        }
        public int read(byte b[]) throws IOException {
            return read(b, 0, b.length);
        }
        public int read(byte b[], int off, int len) throws IOException {
            return getIn().read(b, off, len);
        }
        public long skip(long n) throws IOException {
            return getIn().skip(n);
        }
        public int available() throws IOException {
            return getIn().available();
        }
        public void close() throws IOException {
            getIn().close();
        }
        public synchronized void mark(int readlimit) {
            getIn().mark(readlimit);
        }
        public synchronized void reset() throws IOException {
            getIn().reset();
        }
        public boolean markSupported() {
            return getIn().markSupported();
        }
        protected abstract InputStream getIn();
    }

    protected static abstract class ProxyOutputStream extends OutputStream {
        public void write(int b) throws IOException {
            getOut().write(b);
        }
        public void write(byte b[]) throws IOException {
            write(b, 0, b.length);
        }
        public void write(byte b[], int off, int len) throws IOException {
            if ((off | len | (b.length - (len + off)) | (off + len)) < 0)
                throw new IndexOutOfBoundsException();
            for (int i = 0 ; i < len ; i++) {
                write(b[off + i]);
            }
        }
        public void flush() throws IOException {
            getOut().flush();
        }
        public void close() throws IOException {
            try {
                flush();
            } catch (IOException ignored) {
            }
            getOut().close();
        }
        protected abstract OutputStream getOut();
    }
}
