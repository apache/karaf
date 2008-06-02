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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;

import org.osgi.framework.BundleContext;
import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * Implementation of a <code>DeploymentPackage</code> that is persisted on disk.
 */
class FileDeploymentPackage extends AbstractDeploymentPackage {

    private final List m_index;
    private final File m_contentsDir;

    /**
     * Creates a new instance of a deployment package stored on disk.
     *
     * @param index Reference to the index file that contains the order in which all the resources of this deployment package were received
     * @param packageDir Reference to the directory in which the index and package contents are stored.
     * @param bundleContext The bundle context
     * @throws DeploymentException Thrown if the disk contents do not resemble a valid deployment package.
     * @throws IOException Thrown if there was a problem reading the resources from disk.
     */
    public FileDeploymentPackage(File index, File packageDir, BundleContext bundleContext) throws DeploymentException, IOException {
        this(ExplodingOutputtingInputStream.readIndex(index), packageDir, bundleContext);
    }

    private FileDeploymentPackage(List index, File packageDir, BundleContext bundleContext) throws DeploymentException, IOException {
        super(new Manifest(new GZIPInputStream(new FileInputStream(new File(packageDir, (String) index.remove(0))))), bundleContext);
        m_index = index;
        m_contentsDir = packageDir;
    }

    public BundleInfoImpl[] getOrderedBundleInfos() {
        List result = new ArrayList();
        for(Iterator i = m_index.iterator(); i.hasNext();) {
            AbstractInfo bundleInfo = getBundleInfoByPath((String) i.next());
            if (bundleInfo != null) {
                result.add(bundleInfo);
            }
        }
        return (BundleInfoImpl[]) result.toArray(new BundleInfoImpl[result.size()]);
    }

    public InputStream getBundleStream(String symbolicName) throws IOException {
        BundleInfoImpl bundleInfo = getBundleInfoByName(symbolicName);
        if (bundleInfo != null) {
            return new GZIPInputStream(new FileInputStream(new File(m_contentsDir, bundleInfo.getPath())));
        }
        return null;
    }

    public ResourceInfoImpl[] getOrderedResourceInfos() {
        List result = new ArrayList();
        for(Iterator i = m_index.iterator(); i.hasNext();) {
            AbstractInfo resourceInfo = getResourceInfoByPath((String) i.next());
            if (resourceInfo != null) {
                result.add(resourceInfo);
            }
        }
        return (ResourceInfoImpl[]) result.toArray(new ResourceInfoImpl[result.size()]);
    }

    public InputStream getCurrentEntryStream() {
        throw new UnsupportedOperationException("Not implemented for file-based deployment package");
    }

    public AbstractInfo getNextEntry() throws IOException {
        throw new UnsupportedOperationException("Not implemented for file-based deployment package");
    }

}