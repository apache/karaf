/*
 * $Header: /cvshome/build/org.osgi.service.obr/src/org/osgi/service/obr/Resolver.java,v 1.3 2006/03/16 14:56:17 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2006). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This document is an experimental draft to enable interoperability
// between bundle repositories. There is currently no commitment to 
// turn this draft into an official specification.  
package org.osgi.service.obr;

public interface Resolver
{

    /**
     * Add the following resource to the resolution.
     *
     * The resource will be part of the output and all its requirements
     * will be satisfied.
     *
     * It has the same effect has adding a requirement that will match
     * this resource by symbolicname and version.
     *
     * The current resolution will be lost after adding a resource.
     *
     * @param resource the resource to add
     */
    void add(Resource resource);

    /**
     * Returns the list of resources that have been added to the resolution
     * @return
     */
    Resource[] getAddedResources();

    /**
     * Add the following requirement to the resolution
     *
     * The current resolution will be lost after adding a requirement.
     *
     * @param requirement the requirement to add
     */
    void add(Requirement requirement);

    /**
     * Returns the list of requirements that have been added to the resolution
     * @return
     */
    Requirement[] getAddedRequirements();

    Requirement[] getUnsatisfiedRequirements();

    Resource[] getOptionalResources();

    Requirement[] getReason(Resource resource);

    Resource[] getResources(Requirement requirement);

    Resource[] getRequiredResources();

    /**
     * Start the resolution process and return whether the constraints have
     * been successfully met or not.
     * The resolution can be interrupted by a call to Thread.interrupt() at any
     * time.  The result will be to stop the resolver and throw an InterruptedException.
     *
     * @return <code>true</code> if the resolution has succeeded else <code>false</code>
     * @throws InterruptedResolutionException if the resolution has been interrupted
     */
    boolean resolve();

    void deploy(boolean start);
}