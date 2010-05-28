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
package org.apache.felix.framework.resolver;

import java.net.URL;
import java.util.Enumeration;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Requirement;

public interface Wire
{
    /**
     * Returns the importing module.
     * @return The importing module.
    **/
    public Module getImporter();
    /**
     * Returns the associated requirement from the importing module that
     * resulted in the creation of this wire.
     * @return
    **/
    public Requirement getRequirement();
    /**
     * Returns the exporting module.
     * @return The exporting module.
    **/
    public Module getExporter();
    /**
     * Returns the associated capability from the exporting module that
     * satisfies the requirement of the importing module.
     * @return
    **/
    public Capability getCapability();
    /**
     * Returns whether or not the wire has a given package name. For some
     * wires, such as ones for Require-Bundle, there may be many packages.
     * This method is necessary since the set of packages attained by wires
     * restrict which packages can be dynamically imported (i.e., you cannot
     * dynamically import a package that is already attainable from an
     * existing wire).
     * @return <tt>true</tt> if the package name is attainable from this wire,
     *         <tt>false</tt> otherwise.
    **/
    public boolean hasPackage(String pkgName);
    /**
     * Requests a class from the exporting module. If the class is found, then
     * it is returned. If the class is not found, then this method may or may
     * not throw an exception depending on the wire type (e.g., for an
     * imported package or a required bundle). Throwing an exception indicates
     * that the search should be aborted, while returning a <tt>null</tt>
     * indicates that the search should continue.
     * @return The class if found or <tt>null</tt> if not found and the search
     *         should continue.
     * @throws java.lang.ClassNotFoundException If the class was not found and
     *         the search should be aborted.
    **/
    public Class getClass(String name) throws ClassNotFoundException;
    /**
     * Requests a resource from the exporting module. If the resource is found,
     * then an URL is returned. If the resource is not found, then this method may
     * or may not throw an exception depending on the wire type (e.g., for an
     * imported package or a required bundle). Throwing an exception indicates
     * that the search should be aborted, while returning a <tt>null</tt>
     * indicates that the search should continue.
     * @return An URL to the resource if found or <tt>null</tt> if not found
     *         and the search should continue.
     * @throws ResourceNotFoundException If the resource was not found and
     *         the search should be aborted.
    **/
    public URL getResource(String name) throws ResourceNotFoundException;
    /**
     * Requests resources from the exporting module. If the resources are found,
     * then an enumeration of URLs is returned. If the resources are not found,
     * then this method may or may not throw an exception depending on the wire
     * type (e.g., for an imported package or a required bundle). Throwing an
     * exception indicates that the search should be aborted, while returning a
     * <tt>null</tt> indicates that the search should continue.
     * @return An enumeration of URLs for the resource if found or <tt>null</tt>
     *         if not found and the search should continue.
     * @throws ResourceNotFoundException If the resource was not found and
     *         the search should be aborted.
    **/
    public Enumeration getResources(String name) throws ResourceNotFoundException;
}