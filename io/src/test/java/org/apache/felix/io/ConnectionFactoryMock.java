/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;

import org.osgi.service.io.ConnectionFactory;

/**
 * {@link ConnectionFactory} implementation for testing purpose. Creates fake connection @see {@link Connection}.
 * 
 * 
 * @version $Rev$ $Date$
 */
public class ConnectionFactoryMock implements ConnectionFactory
{

    /**
     * uri name.
     */
    private String m_name;
    /**
     * access mode.
     */
    private int m_mode;
    /**
     * connection timeout.
     */
    private boolean m_timeout;

    /**
     * @see org.osgi.service.io.ConnectionFactory#createConnection(String, int, boolean)
     */
    public Connection createConnection(String name, int mode, boolean timeout) throws IOException
    {
        this.m_mode = mode;
        this.m_name = name;
        this.m_timeout = timeout;
        return new TestConnection();
    }

    public String getName()
    {
        return m_name;
    }

    public int getMode()
    {
        return m_mode;
    }

    public boolean isTimeout()
    {
        return m_timeout;
    }

    /**
     * Mock implementation of {@link Connection}, {@link InputConnection}, {@link OutputConnection}.
     * 
     */
    private class TestConnection implements Connection, InputConnection, OutputConnection
    {

        /**
         * @see javax.microedition.io.Connection#close()
         */
        public void close() throws IOException
        {

        }

        /**
         * @see javax.microedition.io.InputConnection#openDataInputStream()
         */
        public DataInputStream openDataInputStream() throws IOException
        {
            return new DataInputStream(new ByteArrayInputStream(new byte[]
            {}));
        }

        /**
         * @see javax.microedition.io.InputConnection#openInputStream()
         */
        public InputStream openInputStream() throws IOException
        {
            return new ByteArrayInputStream(new byte[]
            {});
        }

        /**
         * @see javax.microedition.io.OutputConnection#openDataOutputStream()
         */
        public DataOutputStream openDataOutputStream() throws IOException
        {
            return new DataOutputStream(new ByteArrayOutputStream());
        }

        /**
         * @see javax.microedition.io.OutputConnection#openOutputStream()
         */
        public OutputStream openOutputStream() throws IOException
        {
            return new ByteArrayOutputStream();
        }

    }
}