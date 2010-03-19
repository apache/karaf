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
/*
 * $Header: /cvshome/build/org.osgi.service.obr/src/org/osgi/service/obr/RepositoryAdmin.java,v 1.3 2006/03/16 14:56:17 hargrave Exp $
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

import java.net.URL;

import org.osgi.framework.InvalidSyntaxException;

/**
 * Provides centralized access to the distributed repository.
 * 
 * A repository contains a set of <i>resources</i>. A resource contains a
 * number of fixed attributes (name, version, etc) and sets of:
 * <ol>
 * <li>Capabilities - Capabilities provide a named aspect: a bundle, a display,
 * memory, etc.</li>
 * <li>Requirements - A named filter expression. The filter must be satisfied
 * by one or more Capabilities with the given name. These capabilities can come
 * from other resources or from the platform. If multiple resources provide the
 * requested capability, one is selected. (### what algorithm? ###)</li>
 * <li>Requests - Requests are like requirements, except that a request can be
 * fulfilled by 0..n resources. This feature can be used to link to resources
 * that are compatible with the given resource and provide extra functionality.
 * For example, a bundle could request all its known fragments. The UI
 * associated with the repository could list these as optional downloads.</li>
 * 
 * @version $Revision: 1.3 $
 */
public interface RepositoryAdmin
{
    /**
     * Discover any resources that match the given filter.
     * 
     * This is not a detailed search, but a first scan of applicable resources.
     * 
     * ### Checking the capabilities of the filters is not possible because that
     * requires a new construct in the filter.
     * 
     * The filter expression can assert any of the main headers of the resource.
     * The attributes that can be checked are:
     * 
     * <ol>
     * <li>name</li>
     * <li>version (uses filter matching rules)</li>
     * <li>description</li>
     * <li>category</li>
     * <li>copyright</li>
     * <li>license</li>
     * <li>source</li>
     * </ol>
     * 
     * @param filterExpr
     *            A standard OSGi filter
     * @return List of resources matching the filters.
     */
    Resource[] discoverResources(String filterExpr) throws InvalidSyntaxException;

    /**
     * Discover any resources that match the given requirements.
     *
     * @param requirements
     * @return List of resources matching the filter
     */
    Resource[] discoverResources(Requirement[] requirements);

    /**
     * Create a resolver.
     *
     * @return
     */
    Resolver resolver();

    /**
     * Create a resolver on the given repositories.
     *
     * @param repositories the list of repositories to use for the resolution
     * @return
     */
    Resolver resolver(Repository[] repositories);

    /**
     * Add a new repository to the federation.
     *
     * The url must point to a repository XML file.
     *
     * @param repository
     * @return
     * @throws Exception
     */
    Repository addRepository(String repository) throws Exception;

    /**
     * Add a new repository to the federation.
     *
     * The url must point to a repository XML file.
     *
     * @param repository
     * @return
     * @throws Exception
     */
    Repository addRepository(URL repository) throws Exception;

    /**
     * Remove a repository from the federation
     *
     * The url must point to a repository XML file.
     *
     * @param repository
     * @return
     */
    boolean removeRepository(String repository);

    /**
     * List all the repositories.
     * 
     * @return
     */
    Repository[] listRepositories();

    /**
     * Return the repository containing the system bundle
     *
     * @return
     */
    Repository getSystemRepository();

    /**
     * Return the repository containing locally installed resources
     *
     * @return
     */
    Repository getLocalRepository();

    /**
     * Return a helper to perform various operations on the data model
     *
     * @return
     */
    DataModelHelper getHelper();

}