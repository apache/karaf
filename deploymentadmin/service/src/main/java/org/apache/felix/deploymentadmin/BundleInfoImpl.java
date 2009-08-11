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

import java.util.StringTokenizer;
import java.util.jar.Attributes;

import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * Implementation of the <code>BundleInfo</code> interface as defined by the OSGi mobile specification.
 */
public class BundleInfoImpl extends AbstractInfo implements BundleInfo {

    private final Version m_version;
    private final String m_symbolicName;
    private final boolean m_customizer;

    /**
     * Creates an instance of this class.
     *
     * @param path The path / resource-id of the bundle resource.
     * @param attributes Set of attributes describing the bundle resource.
     * @throws DeploymentException If the specified attributes do not describe a valid bundle.
     */
    public BundleInfoImpl(String path, Attributes attributes) throws DeploymentException {
        super(path, attributes);

        String bundleSymbolicName = attributes.getValue(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME);
        if (bundleSymbolicName == null) {
            throw new DeploymentException(DeploymentException.CODE_MISSING_HEADER, "Missing '" + org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME + "' header for manifest entry '" + getPath() + "'");
        } else if (bundleSymbolicName.trim().equals("")) {
            throw new DeploymentException(DeploymentException.CODE_BAD_HEADER, "Invalid '" + org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME + "' header for manifest entry '" + getPath() + "'");
        } else {
            m_symbolicName = parseSymbolicName(bundleSymbolicName);
        }

        String version = attributes.getValue(org.osgi.framework.Constants.BUNDLE_VERSION);
        if (version == null || version == "") {
            throw new DeploymentException(DeploymentException.CODE_BAD_HEADER, "Invalid '" + org.osgi.framework.Constants.BUNDLE_VERSION + "' header for manifest entry '" + getPath() + "'");
        }
        try {
            m_version = Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            throw new DeploymentException(DeploymentException.CODE_BAD_HEADER, "Invalid '" + org.osgi.framework.Constants.BUNDLE_VERSION + "' header for manifest entry '" + getPath() + "'");
        }

        m_customizer = parseBooleanHeader(attributes, Constants.DEPLOYMENTPACKAGE_CUSTOMIZER);
    }
    
    /**
     * Strips parameters from the bundle symbolic name such as "foo;singleton:=true".
     * 
     * @param name full name as found in the manifest of the deployment package
     * @return name without parameters
     */
    private String parseSymbolicName(String name) {
        // note that we don't explicitly check if there are tokens, because that
        // check has already been made before we are invoked here
        StringTokenizer st = new StringTokenizer(name, ";");
        return st.nextToken();
    }

    public String getSymbolicName() {
        return m_symbolicName;
    }

    public Version getVersion() {
        return m_version;
    }

    /**
     * Determine whether this bundle resource is a customizer bundle.
     *
     * @return True if the bundle is a customizer bundle, false otherwise.
     */
    public boolean isCustomizer() {
        return m_customizer;
    }

    /**
     * Verify if the specified attributes describe a bundle resource.
     *
     * @param attributes Attributes describing the resource
     * @return true if the attributes describe a bundle resource, false otherwise
     */
    public static boolean isBundleResource(Attributes attributes) {
        return (attributes.getValue(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) != null);
    }

}
