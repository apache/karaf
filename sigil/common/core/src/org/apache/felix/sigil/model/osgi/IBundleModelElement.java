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

package org.apache.felix.sigil.model.osgi;


import java.net.URI;
import java.util.Collection;
import java.util.Set;

import org.apache.felix.sigil.model.ICompoundModelElement;
import org.apache.felix.sigil.model.INamedModelElement;
import org.apache.felix.sigil.model.eclipse.ILibraryImport;
import org.osgi.framework.Version;


public interface IBundleModelElement extends INamedModelElement, ICompoundModelElement, IVersionedModelElement
{

    String getActivator();


    void setActivator( String activator );


    String getCategory();


    void setCategory( String category );


    String getContactAddress();


    void setContactAddress( String contactAddress );


    String getCopyright();


    void setCopyright( String copyright );


    URI getDocURI();


    void setDocURI( URI docURI );


    Collection<IPackageExport> getExports();


    void addExport( IPackageExport packageExport );


    void removeExport( IPackageExport packageExport );


    Collection<IPackageImport> getImports();


    void addImport( IPackageImport packageImport );


    void removeImport( IPackageImport packageImport );


    Collection<IRequiredBundle> getRequiredBundles();


    void addRequiredBundle( IRequiredBundle bundle );


    void removeRequiredBundle( IRequiredBundle bundle );


    void addLibraryImport( ILibraryImport library );


    void removeLibraryImport( ILibraryImport library );


    Set<ILibraryImport> getLibraryImports();


    URI getLicenseURI();


    void setLicenseURI( URI licenseURI );


    URI getSourceLocation();


    void setSourceLocation( URI sourceLocation );


    String getSymbolicName();


    void setSymbolicName( String symbolicName );


    URI getUpdateLocation();


    void setUpdateLocation( URI updateLocation );


    String getVendor();


    void setVendor( String vendor );


    Version getVersion();


    void setVersion( Version version );


    void setDescription( String elementText );


    String getDescription();


    Collection<String> getClasspaths();


    void addClasspath( String path );


    void removeClasspath( String path );


    void setFragmentHost( IRequiredBundle fragmentHost );


    IRequiredBundle getFragmentHost();
}