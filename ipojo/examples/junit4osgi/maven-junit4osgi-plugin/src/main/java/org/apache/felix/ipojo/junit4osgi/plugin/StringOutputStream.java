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
package org.apache.felix.ipojo.junit4osgi.plugin;

import java.io.OutputStream;
import java.io.Serializable;

/**
 * Provides an OutputStream to an internal String. Internally converts bytes to
 * a Strings and stores them in an internal StringBuffer.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class StringOutputStream extends OutputStream implements Serializable {

    /**
     * Id.
     */
    private static final long serialVersionUID = -5912060965986156224L;
    
    /**
     * The internal destination StringBuffer.
     */
    protected StringBuffer m_buffer = null;

    /**
     * Creates new StringOutputStream, makes a new internal StringBuffer.
     */
    public StringOutputStream() {
        super();
        m_buffer = new StringBuffer();
    }

    /**
     * Returns the content of the internal StringBuffer as a String, the result
     * of all writing to this OutputStream.
     * 
     * @return returns the content of the internal StringBuffer
     */
    public String toString() {
        return m_buffer.toString();
    }

    /**
     * Sets the internal StringBuffer to null.
     */
    public void close() {
        m_buffer = null;

    }

    /**
     * Writes and appends a byte array to StringOutputStream.
     * 
     * @param b the byte array
     * @param off the byte array starting index
     * @param len the number of bytes from byte array to write to the stream
     */
    public void write(byte[] b, int off, int len) {
        if ((off < 0) || (len < 0) || (off + len) > b.length) {
            throw new IndexOutOfBoundsException(
                    "StringOutputStream.write: Parameters out of bounds.");
        }
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = b[off];
            off++;
        }
        m_buffer.append(toCharArray(bytes));
    }

    /**
     * Writes and appends a single byte to StringOutputStream.
     * 
     * @param b the byte as an int to add
     */
    public void write(int b) {
        m_buffer.append((char) b);
    }
    
    /**
     * Writes and appends a String to StringOutputStream.
     * 
     * @param s the String to add
     */
    public void write(String s) {
        m_buffer.append(s);
    }
    
    /**
     * Converts byte array to char array.
     * @param barr input byte array
     * @return the char array corresponding to the 
     * given byte array
     */
    public static char[] toCharArray(byte[] barr) {
        if (barr == null) {
            return null;
        }
        char[] carr = new char[barr.length];
        for (int i = 0; i < barr.length; i++) {
            carr[i] = (char) barr[i];
        }
        return carr;
    }
}
