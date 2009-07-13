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

package org.cauldron.sigil.handlers.project;

import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.handlers.IResourceCommandHandler;
import org.cauldron.sigil.model.project.ISigilProjectModel;
import org.cauldron.sigil.ui.SigilUI;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class ConvertProjectCommandHandler implements IResourceCommandHandler {

	public Object execute(IResource[] resources, ExecutionEvent event)
			throws ExecutionException {
		for ( IResource r : resources ) {
			final IProject project = (IProject) r;
			if ( project != null ) {
				WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
					@Override
					protected void execute(IProgressMonitor monitor)
							throws CoreException {
						SigilCore.makeSigilProject(project, monitor);
						IJavaProject java = JavaCore.create(project);
						ISigilProjectModel sigil = SigilCore.create(project);
						IClasspathEntry[] entries = java.getRawClasspath();
						for (int i = 0; i < entries.length; i++) {
							IClasspathEntry entry = entries[i];
							if(entry.getEntryKind()==IClasspathEntry.CPE_SOURCE) {
								String encodedClasspath = sigil.getJavaModel().encodeClasspathEntry( entry );
								sigil.getBundle().addClasspathEntry(encodedClasspath);
							}
						}
						sigil.save(monitor);
					}				
				};
				SigilUI.runWorkspaceOperation(op, null);
			}
		}
		
		return null;
	}

}
