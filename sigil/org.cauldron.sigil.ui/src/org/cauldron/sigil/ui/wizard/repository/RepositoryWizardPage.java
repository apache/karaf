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

package org.cauldron.sigil.ui.wizard.repository;

import java.util.ArrayList;

import org.cauldron.sigil.model.repository.IRepositoryModel;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public abstract class RepositoryWizardPage extends WizardPage {

	private StringFieldEditor nameEditor;
	private ArrayList<FieldEditor> editors = new ArrayList<FieldEditor>();
	private RepositoryWizard wizard;
	
	protected RepositoryWizardPage(String pageName, RepositoryWizard parent) {
		super(pageName);
		setTitle(pageName);
		this.wizard = parent;
	}
	
	public abstract void createFieldEditors();

	public void addField(FieldEditor editor) {
		editors.add( editor );
	}
	
	public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		setControl(control);
		
		if ( getModel().getType().isDynamic() ) {
			nameEditor = new StringFieldEditor("name", "Name:", control);
			nameEditor.setStringValue(getModel().getName());
			nameEditor.getTextControl(getFieldEditorParent()).addModifyListener( new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					checkPageComplete();
				}
			});
		}
		
		createFieldEditors();

		int cols = nameEditor == null ? 0 : nameEditor.getNumberOfControls();
		for ( FieldEditor e : editors ) {
			cols = Math.max(cols, e.getNumberOfControls());
		}
		
		control.setLayout( new GridLayout(cols, false) );
		
		if ( nameEditor != null ) {
			nameEditor.fillIntoGrid(getFieldEditorParent(), cols);
		}

		for ( FieldEditor e : editors ) {
			e.fillIntoGrid(getFieldEditorParent(), cols);
			e.setPreferenceStore(getModel().getPreferences());
			e.load();
		}
		
		checkPageComplete();
	}

	protected void checkPageComplete() {
		if ( nameEditor != null ) {
			setPageComplete(nameEditor.getStringValue().length() > 0);
		}
	}

	public IRepositoryModel getModel() {
		return wizard.getModel();
	}

	protected Composite getFieldEditorParent() {
		return (Composite) getControl();
	}

	public void storeFields() {
		getModel().setName(nameEditor.getStringValue());
		for ( FieldEditor e : editors ) {
			e.store();
		}
	}
	
}
