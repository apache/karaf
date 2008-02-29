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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.cache.DirectoryContent;
import org.apache.felix.moduleloader.IContent;
import org.apache.felix.framework.cache.JarContent;

/**
 * <p>
 * This class implements a bundle archive revision for exploded bundle
 * JAR files. It uses the specified location directory "in-place" to
 * execute the bundle and does not copy the bundle content at all.
 * </p>
**/
class DirectoryRevision extends BundleRevision
{
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

    public synchronized IContent getContent() throws Exception
    {
        return new DirectoryContent(getLogger(), this, getRevisionRootDir(), m_refDir);
    }

    public void dispose() throws Exception
    {
        // Nothing to dispose of, since we don't maintain any state outside
        // of the revision directory, which will be automatically deleted
        // by the parent bundle archive.
    }
}