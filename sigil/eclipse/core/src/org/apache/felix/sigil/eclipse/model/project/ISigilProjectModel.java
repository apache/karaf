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

package org.apache.felix.sigil.eclipse.model.project;


import java.util.Collection;


import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.core.BldCore;
import org.apache.felix.sigil.model.ICompoundModelElement;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Version;
import org.osgi.service.prefs.Preferences;


/**
 * Represents a sigil project. To get a reference to a ISigilProjectModel you can use the 
 * helper method {@link BldCore#create(IProject)}.
 * 
 * @author dave
 *
 */
public interface ISigilProjectModel extends ICompoundModelElement
{

    /**
     * @return
     */
    IProject getProject();


    // shortcut to getProject().getName()
    String getName();


    Preferences getPreferences();


    /**
     * 
     * @param monitor
     *            The progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call done() on the
     *            given monitor. Accepts null, indicating that no progress
     *            should be reported and that the operation cannot be cancelled
     * @throws CoreException
     */
    void save(IProgressMonitor monitor) throws CoreException;
    
    /**
     * Save the project and optionally rebuildDependencies
     * @param monitor
     * @param rebuildDependencies
     * @throws CoreException
     */
    void save( IProgressMonitor monitor, boolean rebuildDependencies ) throws CoreException;
    
    void rebuildDependencies(IProgressMonitor monitor) throws CoreException;


    /**
     * @return
     */
    Version getVersion();


    String getSymbolicName();


    ISigilBundle getBundle();


    void setBundle( ISigilBundle bundle );


    /**
     * @return
     */
    IJavaProject getJavaModel();


    Collection<ISigilProjectModel> findDependentProjects( IProgressMonitor monitor );


    void resetClasspath( IProgressMonitor monitor ) throws CoreException;


    IPath findBundleLocation() throws CoreException;


    IModelElement findImport( String packageName, IProgressMonitor monitor );


    boolean isInClasspath( String packageName, IProgressMonitor monitor ) throws CoreException;


    boolean isInClasspath( ISigilBundle bundle );


    boolean isInBundleClasspath( IPackageFragment root ) throws JavaModelException;


    IPath findOutputLocation() throws CoreException;


    IBldProject getBldProject() throws CoreException;


    Collection<IClasspathEntry> findExternalClasspath( IProgressMonitor monitor ) throws CoreException;
}
