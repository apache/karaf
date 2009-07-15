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

package org.apache.felix.sigil.ui.eclipse.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class WorkspaceContentProvider implements ITreeContentProvider {
	
	private final boolean includeNonSigil;
	private final boolean includeClosed;

	public WorkspaceContentProvider(boolean includeNonSigil, boolean includeClosed) {
		this.includeNonSigil = includeNonSigil;
		this.includeClosed = includeClosed;
	}

	public Object[] getChildren(Object parentElement) {
		Object[] result = null;
		
		if(parentElement instanceof IWorkspace) {
			IProject[] projects = ((IWorkspace) parentElement).getRoot().getProjects();
			if(includeNonSigil && includeClosed) {
				result = projects;
			} else {
				List<IProject> includedProjects = new ArrayList<IProject>(projects.length);
				for (IProject project : projects) {
					if(!includeClosed && !project.isOpen()) {
						continue;
					}
					
					if(!includeNonSigil) {
						try {
							if(project.getNature(SigilCore.NATURE_ID) == null) {
								continue;
							}
						} catch (CoreException e) {
							continue;
						}
					}
					
					includedProjects.add(project);
				}
				result = includedProjects.toArray(new IProject[includedProjects.size()]);
			}
		} else if(parentElement instanceof IContainer) {
			try {
				IResource[] members = ((IContainer) parentElement).members();
				List<IResource> children = new ArrayList<IResource>(members.length);
				for (int i = 0; i < members.length; i++) {
				    if (members[i].getType() != IResource.FILE) {
				        children.add(members[i]);
				    }
				}
				result = children.toArray(new IResource[children.size()]);
			} catch (CoreException e) {
				// Shouldn't happen
			}
		}
		
		return result;
	}

	public Object getParent(Object element) {
		if(element instanceof IResource) {
			return ((IResource) element).getParent();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return (element instanceof IContainer) && ((IContainer) element).isAccessible();
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
