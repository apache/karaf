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

package org.apache.felix.karaf.shell.wrapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

//
// Based on Apache Ant 1.6.5
//

/**
 * Copies standard output and error of children streams to standard output and error of the parent.
 *
 * @version $Rev: 705608 $ $Date: 2008-10-17 15:28:45 +0200 (Fri, 17 Oct 2008) $
 */
public class PumpStreamHandler
{
    private InputStream in;

    private OutputStream out;

    private OutputStream err;

    private Thread outputThread;

    private Thread errorThread;

    private StreamPumper inputPump;

    //
    // NOTE: May want to use a ThreadPool here, 3 threads per/pair seems kinda expensive :-(
    //

    public PumpStreamHandler(final InputStream in, final OutputStream out, final OutputStream err) {
        assert in != null;
        assert out != null;
        assert err != null;

        this.in = in;
        this.out = out;
        this.err = err;
    }

    public PumpStreamHandler(final OutputStream out, final OutputStream err) {
        this(null, out, err);
    }

    public PumpStreamHandler(final OutputStream outAndErr) {
        this(outAndErr, outAndErr);
    }

    /**
     * Set the input stream from which to read the standard output of the child.
     */
    public void setChildOutputStream(final InputStream in) {
        assert in != null;

        createChildOutputPump(in, out);
    }

    /**
     * Set the input stream from which to read the standard error of the child.
     */
    public void setChildErrorStream(final InputStream in) {
        assert in != null;

        if (err != null) {
            createChildErrorPump(in, err);
        }
    }

    /**
     * Set the output stream by means of which input can be sent to the child.
     */
    public void setChildInputStream(final OutputStream out) {
        assert out != null;

        if (in != null) {
            inputPump = createInputPump(in, out, true);
        }
        else {
            try {
                out.close();
            } catch (IOException e) { }
        }
    }

    /**
     * Attach to a child streams from the given process.
     *
     * @param p     The process to attach to.
     */
    public void attach(final Process p) {
        assert p != null;

        setChildInputStream(p.getOutputStream());
        setChildOutputStream(p.getInputStream());
        setChildErrorStream(p.getErrorStream());
    }
    /**
     * Start pumping the streams.
     */
    public void start() {
        if (outputThread != null) {
            outputThread.start();
        }

        if (errorThread != null) {
            errorThread.start();
        }

        if (inputPump != null) {
            Thread inputThread = new Thread(inputPump);
            inputThread.setDaemon(true);
            inputThread.start();
        }
    }

    /**
     * Stop pumping the streams.
     */
    public void stop() {
        if (outputThread != null) {
            try {
                outputThread.join();
            }
            catch (InterruptedException e) {
                // ignore
            }
        }

        if (errorThread != null) {
            try {
                errorThread.join();
            }
            catch (InterruptedException e) {
                // ignore
            }
        }

        if (inputPump != null) {
            inputPump.stop();
        }

        try {
            err.flush();
        } catch (IOException e) { }
        try {
            out.flush();
        } catch (IOException e) { }
    }

    /**
     * Create the pump to handle child output.
     */
    protected void createChildOutputPump(final InputStream in, final OutputStream out) {
        assert in != null;
        assert out != null;

        outputThread = createPump(in, out);
    }

    /**
     * Create the pump to handle error output.
     */
    protected void createChildErrorPump(final InputStream in, final OutputStream out) {
        assert in != null;
        assert out != null;

        errorThread = createPump(in, out);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given output stream.
     */
    protected Thread createPump(final InputStream in, final OutputStream out) {
        assert in != null;
        assert out != null;

        return createPump(in, out, false);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     *
     * @param in                    The input stream to copy from.
     * @param out                   The output stream to copy to.
     * @param closeWhenExhausted    If true close the inputstream.
     * @return                      A thread object that does the pumping.
     */
    protected Thread createPump(final InputStream in, final OutputStream out, final boolean closeWhenExhausted) {
        assert in != null;
        assert out != null;

        final Thread result = new Thread(new StreamPumper(in, out, closeWhenExhausted));
        result.setDaemon(true);
        return result;
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream. Used for standard input.
     */
    protected StreamPumper createInputPump(final InputStream in, final OutputStream out, final boolean closeWhenExhausted) {
        assert in != null;
        assert out != null;

        StreamPumper pumper = new StreamPumper(in, out, closeWhenExhausted);
        pumper.setAutoflush(true);
        return pumper;
    }
}