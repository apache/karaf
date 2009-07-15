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

package org.apache.felix.sigil.ui.eclipse.ui.preferences.repository;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.ui.eclipse.ui.wizard.repository.RepositoryWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.swt.graphics.Point;

public class RepositoryWizardNode implements IWizardNode {

	private IRepositoryModel repository;
	
	private RepositoryWizard wizard;
	
	public RepositoryWizardNode(IRepositoryModel repository) {
		this.repository = repository;
	}

	public void dispose() {
		if ( wizard != null ) {
			wizard.dispose();
			wizard = null;
		}
	}

	public Point getExtent() {
		return new Point(-1, -1);
	}

	public IWizard getWizard() {
		if ( wizard == null ) {
			try {
				wizard = WizardHelper.loadWizard(repository.getType());
				wizard.init( repository );
			} catch (CoreException e) {
				SigilCore.error( "Failed to create wizard for " + repository.getType(), e);
			}
		}
		return wizard;
	}
	
	public IRepositoryModel getRepository() {
		return repository;
	}

	public boolean isContentCreated() {
		return wizard != null;
	}

}
