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

package org.apache.felix.sigil.ui.eclipse.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class RefreshRepositoryAction extends DisplayAction {
	private final IRepositoryModel[] model;

	public RefreshRepositoryAction(IRepositoryModel... model) {
		super( "Refresh repository");
		this.model = model;
	}
	
	@Override
	public void run() {
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

			@Override
			protected void execute(IProgressMonitor monitor)
					throws CoreException, InvocationTargetException,
					InterruptedException {
				boolean changed = false;
				
				for ( IBundleRepository b : SigilCore.getGlobalRepositoryManager().getRepositories() ) {
					for ( IRepositoryModel m : model ) {
						if ( b.getId().equals( m.getId() ) ) {
							b.refresh();
							changed = true;
						}
					}
				}
				
				if ( changed ) {
					List<ISigilProjectModel> projects = SigilCore.getRoot().getProjects();
					SubMonitor sub = SubMonitor.convert(monitor, projects.size() * 10);
					for ( ISigilProjectModel p : projects ) {
						p.resetClasspath(sub.newChild(10));
					}
				}
			}
		};
		
		SigilUI.runWorkspaceOperation(op, null);
	}
}
