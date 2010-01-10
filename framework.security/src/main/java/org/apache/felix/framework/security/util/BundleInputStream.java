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
package org.apache.felix.framework.security.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.felix.framework.util.IteratorToEnumeration;
import org.apache.felix.moduleloader.IContent;

/**
 * This class makes a given content available as a inputstream with a jar
 * content. In other words the stream can be used as input to a JarInputStream.
 */
public final class BundleInputStream extends InputStream
{
    private final IContent m_root;
    private final Enumeration m_content;
    private final OutputStreamBuffer m_outputBuffer = new OutputStreamBuffer();

    private ByteArrayInputStream m_buffer = null;
    private JarOutputStream m_output = null;

    public BundleInputStream(IContent root) throws IOException
    {
        m_root = root;

        List entries = new ArrayList();

        int count = 0;
        String manifest = null;
        for (Enumeration e = m_root.getEntries(); e.hasMoreElements();)
        {
            String entry = (String) e.nextElement();
            if (entry.equalsIgnoreCase("META-INF/MANIFEST.MF"))
            {
                if (manifest == null)
                {
                    manifest = entry;
                }
            }
            else if (entry.toUpperCase().startsWith("META-INF/"))
            {
                entries.add(count++, entry);
            }
            else
            {
                entries.add(entry);
            }
        }
        if (manifest == null)
        {
            manifest = "META-INF/MANIFEST.MF";
        }
        m_content = new IteratorToEnumeration(entries.iterator());

        try
        {
            m_output = new JarOutputStream(m_outputBuffer);
            readNext(manifest);
            m_buffer = new ByteArrayInputStream(m_outputBuffer.m_outBuffer
                .toByteArray());

            m_outputBuffer.m_outBuffer = null;
        }
        catch (IOException ex)
        {
            // TODO: figure out what is wrong
            ex.printStackTrace();
            throw ex;
        }
    }

    public int read() throws IOException
    {
        if ((m_output == null) && (m_buffer == null))
        {
            return -1;
        }

        if (m_buffer != null)
        {
            int result = m_buffer.read();

            if (result == -1)
            {
                m_buffer = null;
                return read();
            }

            return result;
        }

        if (m_content.hasMoreElements())
        {
            String current = (String) m_content.nextElement();

            readNext(current);

            if (!m_content.hasMoreElements())
            {
                m_output.close();
                m_output = null;
            }

            m_buffer = new ByteArrayInputStream(m_outputBuffer.m_outBuffer
                .toByteArray());

            m_outputBuffer.m_outBuffer = null;
        }

        return read();
    }

    private void readNext(String path) throws IOException
    {
        m_outputBuffer.m_outBuffer = new ByteArrayOutputStream();

        InputStream in = null;
        try
        {
            in = m_root.getEntryAsStream(path);

            if (in == null)
            {
                throw new IOException("Missing entry");
            }

            JarEntry entry = new JarEntry(path);

            m_output.putNextEntry(entry);

            byte[] buffer = new byte[4 * 1024];

            for (int c = in.read(buffer); c != -1; c = in.read(buffer))
            {
                m_output.write(buffer, 0, c);
            }
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (Exception ex)
                {
                    // Not much we can do
                }
            }
        }

        m_output.closeEntry();

        m_output.flush();
    }

    private static final class OutputStreamBuffer extends OutputStream
    {
        ByteArrayOutputStream m_outBuffer = null;

        public void write(int b)
        {
            m_outBuffer.write(b);
        }

        public void write(byte[] buffer) throws IOException
        {
            m_outBuffer.write(buffer);
        }

        public void write(byte[] buffer, int offset, int length)
        {
            m_outBuffer.write(buffer, offset, length);
        }
    }
}
