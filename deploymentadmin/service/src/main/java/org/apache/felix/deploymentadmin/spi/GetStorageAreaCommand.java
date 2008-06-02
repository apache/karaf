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
package org.apache.felix.deploymentadmin.spi;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


import org.osgi.framework.Bundle;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.log.LogService;

/**
 * Command that determines the storage area's of all bundles in the source deployment
 * package of a deployment session.
 */
public class GetStorageAreaCommand extends Command {

    private final Map m_storageAreas = new HashMap();

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        DeploymentPackage target = session.getTargetDeploymentPackage();
        BundleInfo[] infos = target.getBundleInfos();
        for (int i = 0; i < infos.length; i++) {
            if (isCancelled()) {
                throw new DeploymentException(DeploymentException.CODE_CANCELLED);
            }
            Bundle bundle = target.getBundle(infos[i].getSymbolicName());
            if (bundle != null) {
                try {
                    File root = session.getDataFile(bundle);
                    m_storageAreas.put(bundle.getSymbolicName(), root);
                }
                catch (IllegalStateException ise) {
                    session.getLog().log(LogService.LOG_WARNING, "Could not get reference to storage area of bundle '" + bundle.getSymbolicName() +"'");
                }
            }
        }
    }

    /**
     * Determines the storage area's of all bundles in the source deployment package of
     * a deployment session.
     *
     * @return <code>Map</code> with <code>File</code> object references to the storage area's, they bundle symbolic name is used as a key in the <code>Map</code>.
     */
    public Map getStorageAreas() {
        return m_storageAreas;
    }

}
