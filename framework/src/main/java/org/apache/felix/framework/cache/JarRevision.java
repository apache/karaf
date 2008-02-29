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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.IContent;

/**
 * <p>
 * This class implements a bundle archive revision for a standard bundle
 * JAR file. The specified location is the URL of the JAR file. By default,
 * the associated JAR file is copied into the revision's directory on the
 * file system, but it is possible to mark the JAR as 'by reference', which
 * will result in the bundle JAR be used 'in place' and not being copied. In
 * either case, some of the contents may be extracted into the revision
 * directory, such as embedded JAR files and native libraries.
 * </p>
**/
class JarRevision extends BundleRevision
{
    private static final transient String BUNDLE_JAR_FILE = "bundle.jar";

    private File m_bundleFile = null;
    private Map m_header = null;

    public JarRevision(
        Logger logger, File revisionRootDir, String location, boolean byReference)
        throws Exception
    {
        this(logger, revisionRootDir, location, byReference, null);
    }

    public JarRevision(
        Logger logger, File revisionRootDir, String location,
        boolean byReference, InputStream is)
        throws Exception
    {
        super(logger, revisionRootDir, location);

        if (byReference)
        {
            m_bundleFile = new File(location.substring(
                location.indexOf(BundleArchive.FILE_PROTOCOL)
                    + BundleArchive.FILE_PROTOCOL.length()));
        }
        else
        {
            m_bundleFile = new File(getRevisionRootDir(), BUNDLE_JAR_FILE);
        }

        // Save and process the bundle JAR.
        initialize(byReference, is);
    }

    public synchronized Map getManifestHeader() throws Exception
    {
        if (m_header != null)
        {
            return m_header;
        }

        // Get the embedded resource.
        JarFile jarFile = null;

        try
        {
            // Open bundle JAR file.
            jarFile = BundleCache.getSecureAction().openJAR(m_bundleFile);
            // Error if no jar file.
            if (jarFile == null)
            {
                throw new IOException("No JAR file found.");
            }
            // Get manifest.
            Manifest mf = jarFile.getManifest();
            // Create a case insensitive map of manifest attributes.
            m_header = new StringMap(mf.getMainAttributes(), false);
            return m_header;

        }
        finally
        {
            if (jarFile != null) jarFile.close();
        }
    }

    public synchronized IContent getContent() throws Exception
    {
        return new JarContent(getLogger(), this, getRevisionRootDir(), m_bundleFile, true);
    }

    public void dispose() throws Exception
    {
        // Nothing to dispose of, since we don't maintain any state outside
        // of the revision directory, which will be automatically deleted
        // by the parent bundle archive.
    }

    //
    // Private methods.
    //

    private void initialize(boolean byReference, InputStream is)
        throws Exception
    {
        try
        {
            // If the revision directory exists, then we don't
            // need to initialize since it has already been done.
            if (BundleCache.getSecureAction().fileExists(getRevisionRootDir()))
            {
                return;
            }

            // Create revision directory.
            if (!BundleCache.getSecureAction().mkdir(getRevisionRootDir()))
            {
                getLogger().log(
                    Logger.LOG_ERROR,
                    getClass().getName() + ": Unable to create revision directory.");
                throw new IOException("Unable to create archive directory.");
            }

            if (!byReference)
            {
                if (is == null)
                {
                    // Do it the manual way to have a chance to
                    // set request properties such as proxy auth.
                    URL url = new URL(getLocation());
                    URLConnection conn = url.openConnection();

                    // Support for http proxy authentication.
                    String auth = BundleCache.getSecureAction()
                        .getSystemProperty("http.proxyAuth", null);
                    if ((auth != null) && (auth.length() > 0))
                    {
                        if ("http".equals(url.getProtocol()) ||
                            "https".equals(url.getProtocol()))
                        {
                            String base64 = Util.base64Encode(auth);
                            conn.setRequestProperty(
                                "Proxy-Authorization", "Basic " + base64);
                        }
                    }
                    is = BundleCache.getSecureAction().getURLConnectionInputStream(conn);
                }

                // Save the bundle jar file.
                BundleCache.copyStreamToFile(is, m_bundleFile);
            }
        }
        finally
        {
            if (is != null) is.close();
        }
    }
}