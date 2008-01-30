/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.deploymentadmin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This extension of the <code>InputStream</code> writes every byte that is read to an
 * <code>OutputStream</code> of choice. The outputstream is closed automatically when
 * the end of the inputstream is reached.
 */
public class OutputtingInputStream extends InputStream {

    private final InputStream m_inputStream;
    private final OutputStream m_outputStream;

    /**
     * Creates an instance of the <code>OutputtingInputStream</code>.
     *
     * @param inputStream The inputstream from which bytes will be read.
     * @param outputStream The outputstream to which every byte that is read should be outputted.
     */
    public OutputtingInputStream(InputStream inputStream, OutputStream outputStream) {
        super();
        m_inputStream = inputStream;
        m_outputStream = outputStream;
    }

    public int read() throws IOException {
        int i = m_inputStream.read();
        if (i != -1) {
            m_outputStream.write(i);
        }
        return i;
    }

    public int read(byte[] buffer) throws IOException {
        int i = m_inputStream.read(buffer);
        if (i != -1) {
            m_outputStream.write(buffer, 0, i);
        }
        return i;
    }

    public int read(byte[] buffer, int off, int len) throws IOException {
        int i = m_inputStream.read(buffer, off, len);
        if (i != -1) {
            m_outputStream.write(buffer, off, i);
        }
        return i;
    }

    public void close() throws IOException {
        try {
            m_inputStream.close();
        }
        finally {
            try {
                m_outputStream.close();
            }
            catch (Exception e) {
                // not much we can do
            }
        }
    }

}
