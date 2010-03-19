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
 * $Header: /cvshome/build/org.osgi.service.obr/src/org/osgi/service/obr/Resource.java,v 1.5 2006/03/16 14:56:17 hargrave Exp $
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

import java.util.Map;

import org.osgi.framework.Version;

/**
 * A resource is an abstraction of a downloadable thing, like a bundle.
 * 
 * Resources have capabilities and requirements. All a resource's requirements
 * must be satisfied before it can be installed.
 */
public interface Resource
{
    final String LICENSE_URI = "license";

    final String DESCRIPTION = "description";

    final String DOCUMENTATION_URI = "documentation";

    final String COPYRIGHT = "copyright";

    final String SOURCE_URI = "source";

    final String JAVADOC_URI = "javadoc";

    final String SYMBOLIC_NAME = "symbolicname";

    final String PRESENTATION_NAME = "presentationname";

    final String ID = "id";

    final String VERSION = "version";

    final String URI = "uri";

    final String SIZE = "size";

    final String CATEGORY = "category";

    final String MANIFEST_VERSION = "manifestversion";

    /**
     * Get all resource properties
     * @return
     */
    Map getProperties();

    /**
     * Shortcut for {{getProperties().get(ID)}}
     * @return
     */
    String getId();

    /**
     * Shortcut for {{getProperties().get(SYMBOLIC_NAME)}}
     * @return
     */
    String getSymbolicName();

    /**
     * Shortcut for {{getProperties().get(VERSION)}}
     * @return
     */
    Version getVersion();

    /**
     * Shortcut for {{getProperties().get(PRESENTATION_NAME)}}
     * @return
     */
    String getPresentationName();

    /**
     * Shortcut for {{getProperties().get(URI)}}
     * @return
     */
    String getURI();

    /**
     * Shortcut for {{getProperties().get(SIZE)}}
     * @return
     */
    Long getSize();

    /**
     * Retrieve this resource categories
     * @return
     */
    String[] getCategories();

    /**
     * Retrieve the capabilities
     * @return
     */
    Capability[] getCapabilities();

    /**
     * Retrieve the requirements
     *
     * @return
     */
    Requirement[] getRequirements();

    /**
     * Returns whether this resource is a local one or not.
     *
     * Local resources are already available in the OSGi framework and thus will be
     * preferred over other resources.
     */
    boolean isLocal();

}