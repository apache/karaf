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

package org.apache.felix.sigil.model.eclipse;


import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.apache.felix.sigil.model.ICompoundModelElement;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IVersionedModelElement;


/**
 * @author dave
 *
 */
public interface ISigilBundle extends ICompoundModelElement, IVersionedModelElement
{
    void synchronize( IProgressMonitor monitor ) throws IOException;


    boolean isSynchronized();


    IBundleModelElement getBundleInfo();


    String getSymbolicName();


    void setBundleInfo( IBundleModelElement bundle );


    void addSourcePath( IPath path );


    void removeSourcePath( IPath path );


    Collection<IPath> getSourcePaths();


    void clearSourcePaths();


    Collection<String> getClasspathEntrys();


    void addClasspathEntry( String encodedClasspath );


    void removeClasspathEntry( String encodedClasspath );


    IPath getLocation();


    void setLocation( IPath location );


    IPath getSourcePathLocation();


    void setSourcePathLocation( IPath location );


    IPath getSourceRootPath();


    void setSourceRootPath( IPath location );


    void setLicencePathLocation( IPath cacheSourceLocation );


    IPath getLicencePathLocation();


    /**
     * get package names included in bundle.
     * Can contain wildcards e.g. org.foo.*
     */
    Collection<String> getPackages();


    /**
     * remove package name from those included in bundle.
     */
    boolean removePackage( String pkg );


    /**
     * add package name to be included in bundle.
     */
    void addPackage( String pkg );


    /**
     * Attempt to find a package export that matches the given name or return null if none specified
     * 
     * @param elementName
     * @return
     */
    IPackageExport findExport( String elementName );


    /**
     * Attempt to find a package import that matches the given name or return null if none specified
     * @param packageName
     * @return
     */
    IPackageImport findImport( String packageName );


    IBundleCapability getBundleCapability();
}