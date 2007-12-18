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
package org.apache.felix.moduleloader;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

import org.apache.felix.framework.util.SecureAction;

public class JarContent implements IContent
{
    private static final int BUFSIZE = 4096;

    private File m_file = null;
    private JarFileX m_jarFile = null;
    private boolean m_opened = false;
    
    private static SecureAction m_secureAction = new SecureAction();

    public JarContent(File file)
    {
        m_file = file;
    }

    protected void finalize()
    {
        if (m_jarFile != null)
        {
            try
            {
                m_jarFile.close();
            }
            catch (IOException ex)
            {
                // Not much we can do, so ignore it.
            }
        }
    }

    public void open()
    {
        m_opened = true;
    }

    public synchronized void close()
    {
        try
        {
            if (m_jarFile != null)
            {
                m_jarFile.close();
            }
        }
        catch (Exception ex)
        {
            System.err.println("JarContent: " + ex);
        }

        m_jarFile = null;
        m_opened = false;
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return false;
            }
        }

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            return ze != null;
        }
        catch (Exception ex)
        {
            return false;
        }
        finally
        {
        }
    }

    public synchronized byte[] getEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return null;
            }
        }

        // Get the embedded resource.
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            if (ze == null)
            {
                return null;
            }
            is = m_jarFile.getInputStream(ze);
            if (is == null)
            {
                return null;
            }
            baos = new ByteArrayOutputStream(BUFSIZE);
            byte[] buf = new byte[BUFSIZE];
            int n = 0;
            while ((n = is.read(buf, 0, buf.length)) >= 0)
            {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();

        }
        catch (Exception ex)
        {
            return null;
        }
        finally
        {
            try
            {
                if (baos != null) baos.close();
            }
            catch (Exception ex)
            {
            }
            try
            {
                if (is != null) is.close();
            }
            catch (Exception ex)
            {
            }
        }
    }

    public synchronized InputStream getEntryAsStream(String name)
        throws IllegalStateException, IOException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return null;
            }
        }

        // Get the embedded resource.
        InputStream is = null;

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            if (ze == null)
            {
                return null;
            }
            is = m_jarFile.getInputStream(ze);
            if (is == null)
            {
                return null;
            }
        }
        catch (Exception ex)
        {
            return null;
        }

        return is;
    }

    public synchronized Enumeration getEntries()
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return null;
            }
        }

        // Wrap entries enumeration to filter non-matching entries.
        Enumeration e = new EntriesEnumeration(m_jarFile.entries());

        // Spec says to return null if there are no entries.
        return (e.hasMoreElements()) ? e : null;
    }

    private void openJarFile() throws IOException
    {
        if (m_jarFile == null)
        {
            m_jarFile = m_secureAction.openJAR(m_file);
        }
    }

    public String toString()
    {
        return "JAR " + m_file.getPath();
    }

    public File getFile()
    {
        return m_file;
    }

    private static class EntriesEnumeration implements Enumeration
    {
        private Enumeration m_enumeration = null;

        public EntriesEnumeration(Enumeration enumeration)
        {
            m_enumeration = enumeration;
        }

        public boolean hasMoreElements()
        {
            return m_enumeration.hasMoreElements();
        }

        public Object nextElement()
        {
            return ((ZipEntry) m_enumeration.nextElement()).getName();
        }
    }
}