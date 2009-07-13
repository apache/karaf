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

package org.cauldron.sigil.ui.preferences.repository;

import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.model.repository.IRepositoryConfiguration;
import org.cauldron.sigil.model.repository.IRepositorySet;
import org.cauldron.sigil.model.repository.RepositorySet;
import org.cauldron.sigil.ui.preferences.ProjectDependentPreferencesPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class RepositoriesPreferencePage extends ProjectDependentPreferencesPage implements
		IWorkbenchPreferencePage {

	private boolean changed;
	private RepositoriesView viewPage;
	private RepositorySetsView setPage;

	public RepositoriesPreferencePage() {
		super( "Repository Preferences" );
	}
	@Override
	protected Control createContents(Composite parent) {
		Control control = initContents( parent );
		return control;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return SigilCore.getDefault().getPreferenceStore();
	}

	protected void changed() {
		changed = true;
		updateApplyButton();
	}
	
	private Control initContents(Composite parent) {
		viewPage = new RepositoriesView(this);
		setPage = new RepositorySetsView(this);
		
		Composite control = new Composite(parent, SWT.NONE);
		
		TabFolder folder = new TabFolder(control, SWT.TOP);
		
		TabItem view = new TabItem(folder, SWT.NONE);
		view.setText("Repositories");
		view.setControl(viewPage.createContents(folder) );
		
		TabItem sets = new TabItem(folder, SWT.NONE);
		sets.setText("Sets");
		sets.setControl(setPage.createContents(folder) );

		control.setLayout(new GridLayout(1, true));
		folder.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return control;
	}

	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doSave() {
		try {
			IRepositoryConfiguration config = SigilCore.getRepositoryConfiguration();
			config.saveRepositories(viewPage.getRepositories());
			config.saveRepositorySets(setPage.getSets());
			IRepositorySet defaultSet = new RepositorySet(setPage.getDefaultRepositories());
			config.setDefaultRepositorySet(defaultSet);
			
			setErrorMessage(null);
			getApplyButton().setEnabled(false);
			changed = false;
		} catch (CoreException e) {
			setErrorMessage("Failed to save repositories:" + e.getStatus().getMessage());
			SigilCore.error("Failed to save repositories", e);
		}
	}

	@Override
	protected boolean isDirty() {
		return changed;
	}	
}
