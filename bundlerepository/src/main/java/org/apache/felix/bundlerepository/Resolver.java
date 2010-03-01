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
package org.apache.felix.bundlerepository;

public interface Resolver
{

    int NO_OPTIONAL_RESOURCES =    0x0001;
    int NO_LOCAL_RESOURCES =       0x0002;
    int NO_SYSTEM_BUNDLE =         0x0004;
    int DO_NOT_PREFER_LOCAL =      0x0008;
    int START =                    0x0010;

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

    /**
     * Add a global capability.
     *
     * A global capability is one capability provided by the environment
     * but not reflected in local resources.
     *
     * @param capability the new global capability
     */
    void addGlobalCapability(Capability capability);

    /**
     * Returns the list of global capabilities
     * @return
     */
    Capability[] getGlobalCapabilities();

    /**
     * Add a global requirement.
     *
     * A global requirement is a requirement that must be satisfied by all
     * resources.  Such requirements are usually built using an
     *    IF x then Y
     * which can be expressed using the following logical expression
     *    !X OR (X AND Y)
     * which can be translated to the following filter
     *    (|(!(x))(&(x)(y))
     *
     * @param requirement
     */
    void addGlobalRequirement(Requirement requirement);

    /**
     * Returns a list of global requirements
     * @return
     */
    Requirement[] getGlobalRequirements();

   /**
     * Start the resolution process and return whether the constraints have
     * been successfully met or not.
     * The resolution can be interrupted by a call to Thread.interrupt() at any
     * time.  The result will be to stop the resolver and throw an InterruptedException.
     *
     * @return <code>true</code> if the resolution has succeeded else <code>false</code>
     * @throws InterruptedResolutionException if the resolution has been interrupted
     */
    boolean resolve() throws InterruptedResolutionException;

    /**
     * Start the resolution process with the following flags.
     * @param flags resolution flags
     * @return <code>true</code> if the resolution has succeeded else <code>false</code>
     * @throws InterruptedResolutionException if the resolution has been interrupted
     */
    boolean resolve(int flags) throws InterruptedResolutionException;

    Requirement[] getUnsatisfiedRequirements();

    Resource[] getOptionalResources();

    Requirement[] getReason(Resource resource);

    Resource[] getResources(Requirement requirement);

    Resource[] getRequiredResources();

    void deploy(boolean start);

    void deploy(int flags);
}