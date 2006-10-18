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
import java.net.URL;
import java.net.URLConnection;
import java.security.PrivilegedActionException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.*;
import org.apache.felix.moduleloader.*;

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
    private static final transient String EMBEDDED_DIRECTORY = "embedded";
    private static final transient String LIBRARY_DIRECTORY = "lib";

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

    public IContent getContent() throws Exception
    {
        return new JarContent(m_bundleFile);
    }

    public synchronized IContent[] getContentPath() throws Exception
    {
        // Creating the content path entails examining the bundle's
        // class path to determine whether the bundle JAR file itself
        // is on the bundle's class path and then creating content
        // objects for everything on the class path.

        File embedDir = new File(getRevisionRootDir(), EMBEDDED_DIRECTORY);

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
        JarFile bundleJar = null;
        try
        {
            bundleJar = BundleCache.getSecureAction().openJAR(m_bundleFile);
            IContent self = new JarContent(m_bundleFile);
            List contentList = new ArrayList();
            for (int i = 0; i < classPathStrings.length; i++)
            {
                if (classPathStrings[i].equals(FelixConstants.CLASS_PATH_DOT))
                {
                    contentList.add(self);
                }
                else
                {
                    // Determine if the class path entry is a file or directory
                    // in the bundle JAR file.
                    ZipEntry entry = bundleJar.getEntry(classPathStrings[i]);
                    if ((entry != null) && entry.isDirectory())
                    {
                        contentList.add(new ContentDirectoryContent(self, classPathStrings[i]));
                    }
                    else
                    {
                        // Ignore any entries that do not exist per the spec.
                        File extractedJar = new File(embedDir, classPathStrings[i]);
                        if (BundleCache.getSecureAction().fileExists(extractedJar))
                        {
                            contentList.add(new JarContent(extractedJar));
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
        finally
        {
            if (bundleJar != null) bundleJar.close();
        }
    }

// TODO: This will need to consider security.
    public synchronized String findLibrary(String libName) throws Exception
    {
        // Get bundle lib directory.
        File libDir = new File(getRevisionRootDir(), LIBRARY_DIRECTORY);
        // Get lib file.
        File libFile = new File(libDir, File.separatorChar + libName);
        // Make sure that the library's parent directory exists;
        // it may be in a sub-directory.
        libDir = libFile.getParentFile();
        if (!BundleCache.getSecureAction().fileExists(libDir))
        {
            if (!BundleCache.getSecureAction().mkdirs(libDir))
            {
                throw new IOException("Unable to create library directory.");
            }
        }
        // Extract the library from the JAR file if it does not
        // already exist.
        if (!BundleCache.getSecureAction().fileExists(libFile))
        {
            JarFile bundleJar = null;
            InputStream is = null;

            try
            {
                bundleJar = BundleCache.getSecureAction().openJAR(m_bundleFile);
                ZipEntry ze = bundleJar.getEntry(libName);
                if (ze == null)
                {
                    throw new IOException("No JAR entry: " + libName);
                }
                is = new BufferedInputStream(
                    bundleJar.getInputStream(ze), BundleCache.BUFSIZE);
                if (is == null)
                {
                    throw new IOException("No input stream: " + libName);
                }

                // Create the file.
                BundleCache.copyStreamToFile(is, libFile);
            }
            finally
            {
                if (bundleJar != null) bundleJar.close();
                if (is != null) is.close();
            }
        }

        return BundleCache.getSecureAction().getAbsolutePath(libFile);
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

            preprocessBundleJar();
        }
        finally
        {
            if (is != null) is.close();
        }
    }

    /**
     * This method pre-processes a bundle JAR file making it ready
     * for use. This entails extracting all embedded JAR files and
     * all native libraries.
     * @throws java.lang.Exception if any error occurs while processing JAR file.
    **/
    private void preprocessBundleJar() throws Exception
    {
        //
        // Create special directories so that we can avoid checking
        // for their existence all the time.
        //

        File embedDir = new File(getRevisionRootDir(), EMBEDDED_DIRECTORY);
        if (!BundleCache.getSecureAction().fileExists(embedDir))
        {
            if (!BundleCache.getSecureAction().mkdir(embedDir))
            {
                throw new IOException("Could not create embedded JAR directory.");
            }
        }

        File libDir = new File(getRevisionRootDir(), LIBRARY_DIRECTORY);
        if (!BundleCache.getSecureAction().fileExists(libDir))
        {
            if (!BundleCache.getSecureAction().mkdir(libDir))
            {
                throw new IOException("Unable to create native library directory.");
            }
        }

        //
        // This block extracts all embedded JAR files.
        //

        try
        {
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

            for (int i = 0; i < classPathStrings.length; i++)
            {
                if (!classPathStrings[i].equals(FelixConstants.CLASS_PATH_DOT))
                {
                    extractEmbeddedJar(classPathStrings[i]);
                }
            }

        }
        catch (PrivilegedActionException ex)
        {
            throw ((PrivilegedActionException) ex).getException();
        }
    }

    /**
     * This method extracts an embedded JAR file from the bundle's
     * JAR file.
     * @param id the identifier of the bundle that owns the embedded JAR file.
     * @param jarPath the path to the embedded JAR file inside the bundle JAR file.
    **/
    private void extractEmbeddedJar(String jarPath)
        throws Exception
    {
        // Remove leading slash if present.
        jarPath = (jarPath.length() > 0) && (jarPath.charAt(0) == '/')
            ? jarPath.substring(1) : jarPath;

        // If JAR is already extracted, then don't re-extract it...
        File jarFile = new File(
            getRevisionRootDir(), EMBEDDED_DIRECTORY + File.separatorChar + jarPath);

        if (!BundleCache.getSecureAction().fileExists(jarFile))
        {
            JarFile bundleJar = null;
            InputStream is = null;
            try
            {
                // Make sure class path entry is a JAR file.
                bundleJar = BundleCache.getSecureAction().openJAR(m_bundleFile);
                ZipEntry ze = bundleJar.getEntry(jarPath);
                if (ze == null)
                {
// TODO: FRAMEWORK - Per the spec, this should fire a FrameworkEvent.INFO event;
//       need to create an "Eventer" class like "Logger" perhaps.
                    getLogger().log(Logger.LOG_INFO, "Class path entry not found: " + jarPath);
                    return;
                }
                // If the zip entry is a directory, then ignore it since
                // we don't need to extact it; otherwise, it points to an
                // embedded JAR file, so extract it.
                else if (!ze.isDirectory())
                {
                    // Make sure that the embedded JAR's parent directory exists;
                    // it may be in a sub-directory.
                    File jarDir = jarFile.getParentFile();
                    if (!BundleCache.getSecureAction().fileExists(jarDir))
                    {
                        if (!BundleCache.getSecureAction().mkdirs(jarDir))
                        {
                            throw new IOException("Unable to create embedded JAR directory.");
                        }
                    }

                    // Extract embedded JAR into its directory.
                    is = new BufferedInputStream(bundleJar.getInputStream(ze), BundleCache.BUFSIZE);
                    if (is == null)
                    {
                        throw new IOException("No input stream: " + jarPath);
                    }
                    // Copy the file.
                    BundleCache.copyStreamToFile(is, jarFile);
                }
            }
            finally
            {
                if (bundleJar != null) bundleJar.close();
                if (is != null) is.close();
            }
        }
    }

    protected X509Certificate[] getRevisionCertificates() throws Exception
    {
        return getCertificatesForJar(BundleCache.getSecureAction().openJAR(m_bundleFile, true));
    }
}
