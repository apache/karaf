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
package org.apache.felix.framework.cache;

import java.io.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.*;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.*;
import org.apache.felix.moduleloader.*;

/**
 * <p>
 * This class implements a bundle archive revision for exploded bundle
 * JAR files. It uses the specified location directory "in-place" to
 * execute the bundle and does not copy the bundle content at all.
 * </p>
**/
class DirectoryRevision extends BundleRevision
{
    private static final transient String BUNDLE_JAR_FILE = "bundle.jar";

    private File m_refDir = null;
    private Map m_header = null;

    public DirectoryRevision(
        Logger logger, File revisionRootDir, String location) throws Exception
    {
        super(logger, revisionRootDir, location);
        m_refDir = new File(location.substring(
            location.indexOf(BundleArchive.FILE_PROTOCOL)
                + BundleArchive.FILE_PROTOCOL.length()));

        // If the revision directory exists, then we don't
        // need to initialize since it has already been done.
        if (BundleCache.getSecureAction().fileExists(getRevisionRootDir()))
        {
            return;
        }

        // Create revision directory, we only need this to store the
        // revision location, since nothing else needs to be extracted
        // since we are referencing a read directory already.
        if (!BundleCache.getSecureAction().mkdir(getRevisionRootDir()))
        {
            getLogger().log(
                Logger.LOG_ERROR,
                getClass().getName() + ": Unable to create revision directory.");
            throw new IOException("Unable to create archive directory.");
        }
    }

    public synchronized Map getManifestHeader()
        throws Exception
    {
        if (m_header != null)
        {
            return m_header;
        }

        // Read the header file from the reference directory.
        InputStream is = null;

        try
        {
            // Open manifest file.
            is = BundleCache.getSecureAction()
                .getFileInputStream(new File(m_refDir, "META-INF/MANIFEST.MF"));
            // Error if no jar file.
            if (is == null)
            {
                throw new IOException("No manifest file found.");
            }

            // Get manifest.
            Manifest mf = new Manifest(is);
            // Create a case insensitive map of manifest attributes.
            m_header = new StringMap(mf.getMainAttributes(), false);
            return m_header;
        }
        finally
        {
            if (is != null) is.close();
        }
    }

    public IContent getContent() throws Exception
    {
        return new DirectoryContent(m_refDir);
    }

    public synchronized IContent[] getContentPath() throws Exception
    {
        // Creating the content path entails examining the bundle's
        // class path to determine whether the bundle JAR file itself
        // is on the bundle's class path and then creating content
        // objects for everything on the class path.

        // Get the bundle's manifest header.
        Map map = getManifestHeader();

        // Find class path meta-data.
        String classPath = (map == null)
            ? null : (String) map.get(FelixConstants.BUNDLE_CLASSPATH);

        // Parse the class path into strings.
        String[] classPathStrings = Util.parseDelimitedString(
            classPath, FelixConstants.CLASS_PATH_SEPARATOR);

        if (classPathStrings == null)
        {
            classPathStrings = new String[0];
        }

        // Create the bundles class path.
        IContent self = new DirectoryContent(m_refDir);
        List contentList = new ArrayList();
        for (int i = 0; i < classPathStrings.length; i++)
        {
            if (classPathStrings[i].equals(FelixConstants.CLASS_PATH_DOT))
            {
                contentList.add(self);
            }
            else
            {
                // Determine if the class path entry is a file or directory.
                File file = new File(m_refDir, classPathStrings[i]);
                if (BundleCache.getSecureAction().isFileDirectory(file))
                {
                    contentList.add(new DirectoryContent(file));
                }
                else
                {
                    // Ignore any entries that do not exist per the spec.
                    if (BundleCache.getSecureAction().fileExists(file))
                    {
                        contentList.add(new JarContent(file));
                    }
                }
            }
        }

        // If there is nothing on the class path, then include
        // "." by default, as per the spec.
        if (contentList.size() == 0)
        {
            contentList.add(self);
        }

        return (IContent[]) contentList.toArray(new IContent[contentList.size()]);
    }

// TODO: This will need to consider security.
    public String findLibrary(String libName) throws Exception
    {
        return BundleCache.getSecureAction().getAbsolutePath(new File(m_refDir, libName));
    }

    public void dispose() throws Exception
    {
        // Nothing to dispose of, since we don't maintain any state outside
        // of the revision directory, which will be automatically deleted
        // by the parent bundle archive.
    }

    protected X509Certificate[] getRevisionCertificates() throws Exception
    {
        File tmp = new File(getRevisionRootDir(), BUNDLE_JAR_FILE);

        if (BundleCache.getSecureAction().fileExists(tmp))
        {
            BundleCache.getSecureAction().deleteFile(tmp);
        }

        try
        {
            BundleCache.copyStreamToFile(new RevisionToInputStream(m_refDir),
                tmp);

            JarFile bundle = BundleCache.getSecureAction().openJAR(tmp);

            return getCertificatesForJar(bundle);
        }
        finally
        {
            try
            {
                if (BundleCache.getSecureAction().fileExists(tmp))
                {
                    BundleCache.getSecureAction().deleteFile(tmp);
                }
            }
            catch (Exception e)
            {
                // Not much we can do
            }
        }
    }

    private class RevisionToInputStream extends InputStream
    {
        class OutputStreamBuffer extends OutputStream
        {
            ByteArrayOutputStream outBuffer = null;

            public void write(int b)
            {
                outBuffer.write(b);
            }
        }

        private File m_revisionDir = null;
        private File[] m_content = null;
        private File m_manifest = null;
        private ByteArrayInputStream m_buffer = null;
        private int m_current = 0;
        private OutputStreamBuffer m_outputBuffer = new OutputStreamBuffer();
        private JarOutputStream m_output = null;

        RevisionToInputStream(File revisionDir) throws IOException
        {
            m_revisionDir = revisionDir;

            m_outputBuffer.outBuffer = new ByteArrayOutputStream();

            m_manifest = new File(m_revisionDir, "META-INF/MANIFEST.MF");

            m_output = new JarOutputStream(m_outputBuffer);

            readNext(m_manifest, false);

            m_content = listFilesRecursive(revisionDir);
        }

        private File[] listFilesRecursive(File dir)
        {
            File[] children = BundleCache.getSecureAction().listDirectory(dir);
            File[] combined = children;
            for (int i = 0; i < children.length; i++)
            {
                if (BundleCache.getSecureAction().isFileDirectory(children[i]))
                {
                    File[] grandchildren = listFilesRecursive(children[i]);
                    if (grandchildren.length > 0)
                    {
                        File[] tmp = new File[combined.length + grandchildren.length];
                        System.arraycopy(combined, 0, tmp, 0, combined.length);
                        System.arraycopy(grandchildren, 0, tmp, combined.length, grandchildren.length);
                        combined = tmp;
                    }
                }
            }
            return combined;
        }

        private boolean readNext(File file, boolean close) throws IOException
        {
            if (BundleCache.getSecureAction().isFileDirectory(file))
            {
                return false;
            }

            m_outputBuffer.outBuffer = new ByteArrayOutputStream();

            InputStream in = null;
            try
            {
                in = BundleCache.getSecureAction().getFileInputStream(file);

                JarEntry entry = new JarEntry(
                    file.getPath().substring(m_revisionDir.getPath().length() + 1));


                m_output.putNextEntry(entry);

                int c = -1;

                while ((c = in.read()) != -1)
                {
                    m_output.write(c);
                }
            }
            finally
            {
                if (in != null)
                {
                    in.close();
                }
            }

            m_output.closeEntry();

            m_output.flush();

            if (close)
            {
                m_output.close();
                m_output = null;
            }

            m_buffer = new ByteArrayInputStream(m_outputBuffer.outBuffer.toByteArray());

            m_outputBuffer.outBuffer = null;

            return true;
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
                else
                {
                    return result;
                }
            }

            while ((m_current < m_content.length) &&
                (m_content[m_current].equals(m_manifest) ||
                !readNext(m_content[m_current], (m_current + 1) == m_content.length)))
            {
                m_current++;
            }

            m_current++;

            return read();
        }
    }
}
