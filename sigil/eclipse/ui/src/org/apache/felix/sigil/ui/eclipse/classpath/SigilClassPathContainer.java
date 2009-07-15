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

package org.apache.felix.sigil.ui.eclipse.classpath;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.job.ThreadProgressMonitor;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * @author dave
 *
 */
public class SigilClassPathContainer implements IClasspathContainer {

    private IClasspathEntry[] entries;
    private ISigilProjectModel sigil;

    public SigilClassPathContainer(ISigilProjectModel sigil) {
    	this.sigil = sigil;
	}

	/* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    public IClasspathEntry[] getClasspathEntries() {
        if ( entries == null ) {
        	buildClassPathEntries();
        }
        
        return entries;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
     */
    public String getDescription() {
        return "Bundle Context Classpath";
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
     */
    public int getKind() {
        return K_SYSTEM;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
     */
    public IPath getPath() {
        return new Path( SigilCore.CLASSPATH_CONTAINER_PATH );
    }

    /**
     * @return
     * @throws CoreException 
     * @throws CoreException 
     */
    private void buildClassPathEntries() {
		try {
			IProgressMonitor monitor = ThreadProgressMonitor.getProgressMonitor();
			entries = sigil.findExternalClasspath(monitor).toArray( new IClasspathEntry[0] );
		} catch (CoreException e) {
    		SigilCore.error( "Failed to build classpath entries", e);
		}
		finally {
			if ( entries == null ) {
	    		entries = new IClasspathEntry[] {};
			}
		}
    }
}
