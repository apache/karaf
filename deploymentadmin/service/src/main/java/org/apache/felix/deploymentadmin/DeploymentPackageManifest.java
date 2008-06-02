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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * This class represents a manifest file used to describe the contents of a deployment package. It can verify the correctness of a
 * deployment package manifest and can interpret the various manifest entries and headers the OSGi specification defines.
 */
public class DeploymentPackageManifest {

    private final Manifest m_manifest;
    private final Version m_version;

    private final List m_bundleInfos = new ArrayList();
    private final List m_resourceInfos = new ArrayList();
    private final String m_symbolicName;
    private final VersionRange m_fixPackage;

    /**
     * Creates an instance of this class.
     *
     * @param manifest The manifest file to be used as deployment manifest
     * @throws DeploymentException If the specified manifest is not a valid deployment package manifest file.
     */
    public DeploymentPackageManifest(Manifest manifest) throws DeploymentException {
        if ((manifest == null) || (manifest.getMainAttributes() == null)) {
            throw new DeploymentException(DeploymentException.CODE_BAD_HEADER);
        }
        m_manifest = manifest;

        Attributes mainAttributes = m_manifest.getMainAttributes();

        // TODO: verify symbolic name and entry-names for valid format/chars
        m_symbolicName = getNonNullHeader(mainAttributes.getValue(Constants.DEPLOYMENTPACKAGE_SYMBOLICMAME));

        String version = getNonNullHeader(mainAttributes.getValue(Constants.DEPLOYMENTPACKAGE_VERSION));
        try {
            m_version = new Version(version);
        } catch (IllegalArgumentException e) {
            throw new DeploymentException(DeploymentException.CODE_BAD_HEADER);
        }

        String fixPackage = mainAttributes.getValue(Constants.DEPLOYMENTPACKAGE_FIXPACK);
        if (fixPackage != null) {
            try {
                m_fixPackage = VersionRange.parse(fixPackage);
            }
            catch (IllegalArgumentException iae) {
                throw new DeploymentException(DeploymentException.CODE_BAD_HEADER, "Invalid version range for header: " + Constants.DEPLOYMENTPACKAGE_FIXPACK);
            }
        } else {
            m_fixPackage = null;
        }

        Map entries = m_manifest.getEntries();
        for(Iterator i = entries.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            processEntry(key, (Attributes) entries.get(key), (m_fixPackage != null));
        }
    }

    /**
     * Determines the value of a header in the main section of the manifest.
     *
     * @param header Name of the header to retrieve.
     * @return Value of the header or null if the header was not defined.
     */
    public String getHeader(String header) {
        return m_manifest.getMainAttributes().getValue(header);
    }

    /**
     * Determines the version range a fix package can be applied to
     *
     * @return A VersionRange describing the versions the fixpackage applies to, null if the package is not a fix package.
     */
    public VersionRange getFixPackage() {
        return m_fixPackage;
    }

    /**
     * Determines the symbolic name of the deployment package.
     *
     * @return String containing the symbolic name of the deployment package.
     */
    public String getSymbolicName() {
        return m_symbolicName;
    }

    /**
     * Determines the version of the deployment package.
     * @return Version of the deployment package.
     */
    public Version getVersion() {
        return m_version;
    }

    /**
     * Determines which bundle resources are part of the deployment package, this includes customizer bundles.
     *
     * @return A List of <code>BundleInfoImpl</code> objects describing the bundle resources of the deployment package.
     */
    public List getBundleInfos() {
        return m_bundleInfos;
    }

    /**
     * Determines which processed resources are part of the deployment package.
     *
     * @return A list of <code>ResourceInfoImpl</code> objects describing the processed resources of the deployment package.
     */
    public List getResourceInfos() {
        return m_resourceInfos;
    }

    private void processEntry(String key, Attributes attributes, boolean isFixPack) throws DeploymentException {
        if (BundleInfoImpl.isBundleResource(attributes)) {
            BundleInfoImpl bundleInfo = new BundleInfoImpl(key, attributes);
            if (bundleInfo.isMissing() && !isFixPack) {
                throw new DeploymentException(DeploymentException.CODE_BAD_HEADER, "Header '" + Constants.DEPLOYMENTPACKAGE_MISSING + "' for manifest " +
                    "entry '" + key + "' may only be 'true' if " + Constants.DEPLOYMENTPACKAGE_FIXPACK + " manifest header is 'true'");
            }
            m_bundleInfos.add(bundleInfo);
        } else {
            m_resourceInfos.add(new ResourceInfoImpl(key, attributes));
        }
    }

    private String getNonNullHeader(String header) throws DeploymentException {
        if (header == null) {
            throw new DeploymentException(DeploymentException.CODE_MISSING_HEADER);
        } else if(header.trim().equals("")) {
            throw new DeploymentException(DeploymentException.CODE_BAD_HEADER);
        }
        return header;
    }

}
