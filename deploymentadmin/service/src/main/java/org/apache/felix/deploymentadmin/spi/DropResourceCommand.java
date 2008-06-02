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

import org.apache.felix.deploymentadmin.AbstractDeploymentPackage;
import org.apache.felix.deploymentadmin.ResourceInfoImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.osgi.service.log.LogService;

/**
 * Command that drops resources.
 */
public class DropResourceCommand extends Command {

    private final CommitResourceCommand m_commitCommand;

    /**
     * Creates an instance of this command. The commit command is used to make sure
     * the resource processors used to drop resources will be committed at a later stage in the process.
     *
     * @param commitCommand The commit command that will be executed at a later stage in the process.
     */
    public DropResourceCommand(CommitResourceCommand commitCommand) {
        m_commitCommand = commitCommand;
        addRollback(m_commitCommand);
    }

    public void execute(DeploymentSessionImpl session) {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();
        LogService log = session.getLog();

        ResourceInfoImpl[] orderedTargetResources = target.getOrderedResourceInfos();
        for (int i = orderedTargetResources.length - 1; i >= 0; i--) {
            ResourceInfoImpl resourceInfo = orderedTargetResources[i];
            String path = resourceInfo.getPath();
            if (source.getResourceInfoByPath(path) == null) {
                ServiceReference ref = target.getResourceProcessor(path);
                if (ref != null) {
                    ResourceProcessor resourceProcessor = (ResourceProcessor) context.getService(ref);
                    if (resourceProcessor != null) {
                        try {
                            if (m_commitCommand.addResourceProcessor(resourceProcessor)) {
                            	resourceProcessor.begin(session);
                            }
                            resourceProcessor.dropped(path);
                        }
                        catch (ResourceProcessorException e) {
                            log.log(LogService.LOG_WARNING, "Not allowed to drop resource '" + path + "'", e);
                        }
                    }
                }
            }
        }
    }
}
