/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.util.process;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Copies all data from an input stream to an output stream.
 */
public class StreamPumper implements Runnable {

    private InputStream in;

    private OutputStream out;

    private volatile boolean finish = false;

    private volatile boolean finished = false;

    private boolean closeWhenExhausted;

    private boolean nonBlocking;

    private boolean autoflush;

    private Throwable exception;

    private int bufferSize = 128;

    private boolean started;

    private Thread thread;

    /**
     * Create a new stream pumper.
     *
     * @param in The input stream to read data from.
     * @param out The output stream to write data to.
     * @param closeWhenExhausted If true, the output stream will be closed when the input is exhausted.
     */
    public StreamPumper(final InputStream in, final OutputStream out, final boolean closeWhenExhausted) {
        assert in != null;
        assert out != null;

        this.in = in;
        this.out = out;
        this.closeWhenExhausted = closeWhenExhausted;
    }

    /**
     * Create a new stream pumper.
     *
     * @param in The input stream to read data from.
     * @param out The output stream to write data to.
     */
    public StreamPumper(final InputStream in, final OutputStream out) {
        this(in, out, false);
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }

    /**
     * Set whether data should be flushed through to the output stream.
     *
     * @param autoflush If true, push through data; if false, let it be buffered.
     */
    public void setAutoflush(boolean autoflush) {
        this.autoflush = autoflush;
    }

    /**
     * Set whether data should be read in a non blocking way.
     *
     * @param nonBlocking If true, data will be read in a non blocking mode.
     */
    public void setNonBlocking(boolean nonBlocking) {
        this.nonBlocking = nonBlocking;
    }

    /**
     * Copy data from the input stream to the output stream.
     *
     * Terminate as soon as the input stream is closed or an error occurs.
     */
    public void run() {
        synchronized (this) {
            started = true;
            thread = Thread.currentThread();
        }

        final byte[] buf = new byte[bufferSize];

        int length = 0;
        try {
            while (true) {
                if (nonBlocking) {
                    while (in.available() > 0) {
                        length = in.read(buf);
                        if (length > 0) {
                            out.write(buf, 0, length);
                            if (autoflush) {
                                out.flush();
                            }
                        } else {
                            break;
                        }
                    }
                    Thread.sleep(50); // Pause to avoid tight loop if external proc is too slow
                } else {
                    do {
                        length = in.read(buf);
                        if (length > 0) {
                            out.write(buf, 0, length);
                            if (autoflush) {
                                out.flush();
                            }
                        }
                    } while (length > 0);
                }
                boolean finish;
                synchronized (this) {
                    finish = this.finish || length < 0;
                }
                if (finish) {
                    break;
                }
            }
        }
        catch (Throwable t) {
            synchronized (this) {
                exception = t;
            }
        }
        finally {
            try {
                out.flush();
            } catch (IOException e) { }
            if (closeWhenExhausted) {
                try {
                    out.close();
                } catch (IOException e) { }
            }
            synchronized (this) {
                finished = true;
                notifyAll();
            }
        }
    }

    /**
     * Tell whether the end of the stream has been reached.
     *
     * @return true if the stream has been exhausted.
     */
    public synchronized boolean isFinished() {
        return finished;
    }

    /**
     * This method blocks until the stream pumper finishes.
     *
     * @see #isFinished()
     * @throws InterruptedException if the stream pumper has been interrupted.
     */
    public synchronized void waitFor() throws InterruptedException {
        while (!isFinished()) {
            wait();
        }
    }

    /**
     * Set the size in bytes of the read buffer.
     *
     * @param bufferSize the buffer size to use.
     */
    public synchronized void setBufferSize(final int bufferSize) {
        if (started) {
            throw new IllegalStateException("Cannot set buffer size on a running StreamPumper");
        }

        this.bufferSize = bufferSize;
    }

    /**
     * Get the size in bytes of the read buffer.
     *
     * @return The size of the read buffer.
     */
    public synchronized int getBufferSize() {
        return bufferSize;
    }

    /**
     * Get the exception encountered, if any.
     *
     * @return The Exception encountered; or null if there was none.
     */
    public synchronized Throwable getException() {
        return exception;
    }

    /**
     * Stop the pumper as soon as possible.
     *
     * Note that it may continue to block on the input stream
     * but it will really stop the thread as soon as it gets EOF
     * or any byte, and it will be marked as finished.
     */
    public synchronized void stop() {
        finish = true;
        if (nonBlocking && thread != null && !finished) {
            thread.interrupt();
        }
        notifyAll();
    }
    
    public InputStream getInputStream() {
        return this.in;
    }
}
