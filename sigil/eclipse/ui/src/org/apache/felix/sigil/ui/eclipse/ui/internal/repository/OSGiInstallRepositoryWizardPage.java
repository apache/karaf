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

package org.apache.felix.sigil.ui.eclipse.ui.internal.repository;

import java.util.ArrayList;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.install.IOSGiInstall;
import org.apache.felix.sigil.ui.eclipse.ui.wizard.repository.RepositoryWizard;
import org.apache.felix.sigil.ui.eclipse.ui.wizard.repository.RepositoryWizardPage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;

public class OSGiInstallRepositoryWizardPage extends RepositoryWizardPage {

	protected OSGiInstallRepositoryWizardPage(RepositoryWizard parent) {
		super("OSGi Install Repository", parent);
	}

	@Override
	public void createFieldEditors() {
		ArrayList<String[]> installs = new ArrayList<String[]>();
		for ( String id : SigilCore.getInstallManager().getInstallIDs() ) {
			IOSGiInstall i = SigilCore.getInstallManager().findInstall(id);
			installs.add( new String[] { i.getType().getName(), id } );
		}
		String[][] strs = installs.toArray( new String[installs.size()][] );
		
		RadioGroupFieldEditor editor = new RadioGroupFieldEditor(
				"id", 
				"Install", 
				1, strs, getFieldEditorParent() );
		
		addField(editor);
	}

}
