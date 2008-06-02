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

import java.util.jar.Attributes;

import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * This class represents the meta data of a processed resource as used by the Deployment Admin.
 */
public class ResourceInfoImpl extends AbstractInfo {

    private String m_resourceProcessor;

    /**
     * Create an instance of this class.
     *
     * @param path String containing the path / resource-id of the processed resource.
     * @param attributes Attributes containing the meta data of the resource.
     * @throws DeploymentException If the specified attributes do not describe a processed resource.
     */
    public ResourceInfoImpl(String path, Attributes attributes) throws DeploymentException {
        super(path, attributes);
        m_resourceProcessor = attributes.getValue(Constants.RESOURCE_PROCESSOR);
    }

    /**
     * Determines the resource processor for this processed resource.
     *
     * @return String containing the PID of the resource processor that should handle this processed resource.
     */
    public String getResourceProcessor() {
        return m_resourceProcessor;
    }
}
