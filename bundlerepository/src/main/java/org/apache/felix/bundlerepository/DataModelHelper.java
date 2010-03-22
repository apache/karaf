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
 * $Header: /cvshome/build/org.osgi.service.obr/src/org/osgi/service/obr/Requirement.java,v 1.4 2006/03/16 14:56:17 hargrave Exp $
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.jar.Attributes;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

public interface DataModelHelper {

    /**
     * Create a simple requirement to be used for selection
     * @param name
     * @param filter
     * @return
     * @throws org.osgi.framework.InvalidSyntaxException
     */
    Requirement requirement(String name, String filter);

    /**
     * Create an extender filter supporting the SUBSET, SUPERSET and other extensions
     *
     * @param filter the string filter
     * @return
     * @throws org.osgi.framework.InvalidSyntaxException
     */
    Filter filter(String filter) throws InvalidSyntaxException;

    /**
     * Create a repository from the specified URL.
     *
     * @param repository
     * @return
     * @throws Exception
     */
    Repository repository(URL repository) throws Exception;

    /**
     * Create a repository for the given set of resources.
     * Such repositories can be used to create a resolver
     * that would resolve on a subset of available resources
     * instead of all of them.
     *
     * @param resources an array of resources
     * @return a repository containing the given resources
     */
    Repository repository(Resource[] resources);

    /**
     * Create a capability
     *
     * @param name name of this capability
     * @param properties the properties
     * @return a new capability with the specified name and properties
     */
    Capability capability(String name, Map properties);

    /**
     * Create a resource corresponding to the given bundle.
     *
     * @param bundle the bundle
     * @return the corresponding resource
     */
    Resource createResource(Bundle bundle);

    /**
     * Create a resource for the bundle located at the
     * given location.
     *
     * @param bundleUrl the location of the bundle
     * @return the corresponding resource
     * @throws IOException
     */
    Resource createResource(URL bundleUrl) throws IOException;

    /**
     * Create a resource corresponding to the given manifest
     * entries.
     *
     * @param attributes the manifest headers
     * @return the corresponding resource
     */
    Resource createResource(Attributes attributes);

    //===========================
    //==   XML serialization   ==
    //===========================

    Repository readRepository(String xml) throws Exception;

    Repository readRepository(Reader reader) throws Exception;

    Resource readResource(String xml) throws Exception;

    Resource readResource(Reader reader) throws Exception;

    Capability readCapability(String xml) throws Exception;

    Capability readCapability(Reader reader) throws Exception;

    Requirement readRequirement(String xml) throws Exception;

    Requirement readRequirement(Reader reader) throws Exception;

    Property readProperty(String xml) throws Exception;

    Property readProperty(Reader reader) throws Exception;

    String writeRepository(Repository repository);

    void writeRepository(Repository repository, Writer writer) throws IOException;

    String writeResource(Resource resource);

    void writeResource(Resource resource, Writer writer) throws IOException;

    String writeCapability(Capability capability);

    void writeCapability(Capability capability, Writer writer) throws IOException;

    String writeRequirement(Requirement requirement);

    void writeRequirement(Requirement requirement, Writer writer) throws IOException;

    String writeProperty(Property property);

    void writeProperty(Property property, Writer writer) throws IOException;

}
