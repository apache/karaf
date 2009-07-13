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

package org.cauldron.sigil.obr;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.cauldron.sigil.ui.wizard.repository.RepositoryWizard;
import org.cauldron.sigil.ui.wizard.repository.RepositoryWizardPage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

public class OBRRepositoryWizardPage extends RepositoryWizardPage implements IWizardPage {

	private StringFieldEditor urlEditor;
	private StringFieldEditor cacheEditor;

	protected OBRRepositoryWizardPage(RepositoryWizard parent) {
		super("OSGi Bundle Repository", parent);
	}

	@Override
	public void createFieldEditors() {
		createField( urlEditor = new StringFieldEditor("url", "URL:", getFieldEditorParent()) );
		createField( cacheEditor = new DirectoryFieldEditor("cache", "Cache:", getFieldEditorParent()) );
		addField( new BooleanFieldEditor( "inmemory", "In Memory:", getFieldEditorParent() ));
	}

	private void createField(StringFieldEditor editor) {
		editor.getTextControl(getFieldEditorParent()).addModifyListener( new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPageComplete();
			}
		});
		addField(editor);
	}

	@Override
	protected void checkPageComplete() {
		super.checkPageComplete();
		if ( isPageComplete() && checkURLComplete() ) {
			checkCacheComplete();
		}
	}

	private boolean checkCacheComplete() {
		setPageComplete(cacheEditor.getStringValue().length() > 0);
		
		if ( isPageComplete() ) {
			if ( new File( cacheEditor.getStringValue() ).isDirectory() ) {
				setErrorMessage(null);				
			}
			else {
				setErrorMessage("Invalid cache directory");
				setPageComplete(false);
			}
		}
		
		return isPageComplete();
	}

	private boolean checkURLComplete() {
		setPageComplete(urlEditor.getStringValue().length() > 0);
		
		if ( isPageComplete() ) {
			try {
				new URL(urlEditor.getStringValue());
				setErrorMessage(null);
			}
			catch (MalformedURLException e) {
				if ( !new File(urlEditor.getStringValue()).isFile() ) {
					setErrorMessage("Invalid repository url: " + e.getMessage());
					setPageComplete(false);
				}
			}
		}
		
		return isPageComplete();
	}

	@Override
	public void storeFields() {
		super.storeFields();
		IPath dir = Activator.getDefault().getStateLocation();
		getModel().getPreferences().setValue( "index", dir.append( getModel().getId() + ".obr" ).toOSString() );
	}
	
}
