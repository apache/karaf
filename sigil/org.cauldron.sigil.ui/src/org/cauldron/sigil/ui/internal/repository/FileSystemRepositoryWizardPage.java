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

package org.cauldron.sigil.ui.internal.repository;

import java.io.File;

import org.cauldron.sigil.ui.wizard.repository.RepositoryWizard;
import org.cauldron.sigil.ui.wizard.repository.RepositoryWizardPage;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

public class FileSystemRepositoryWizardPage extends RepositoryWizardPage
		implements IWizardPage {

	private DirectoryFieldEditor dirEditor;

	protected FileSystemRepositoryWizardPage(RepositoryWizard parent) {
		super("File System Repository", parent);
	}

	@Override
	public void createFieldEditors() {
		dirEditor = new DirectoryFieldEditor("dir", "Directory:", getFieldEditorParent() );
		dirEditor.getTextControl( getFieldEditorParent() ).addModifyListener( new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPageComplete();
			} 
		} );
		
		addField( dirEditor );
		addField(new BooleanFieldEditor("recurse", "Recurse:", getFieldEditorParent() ) );
	}

	@Override
	protected void checkPageComplete() {
		super.checkPageComplete();
		if ( isPageComplete() ) {
			setPageComplete(dirEditor.getStringValue().length() > 0);
			if ( isPageComplete() ) {
				if ( new File( dirEditor.getStringValue()).isDirectory() ) {
					setPageComplete(true);
					setErrorMessage(null);
				}
				else {
					setPageComplete(false);
					setErrorMessage("Invalid directory");
				}
			}
		}
	}
}
