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

package org.cauldron.sigil.cheatsheets.actions;

import org.apache.felix.sigil.ui.eclipse.ui.wizard.project.SigilProjectWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;

public class OpenEmptySigilProjectWizardAction extends AbstractNewWizardAction implements ICheatSheetAction {

	private String name;
	
	public void run(String[] params, ICheatSheetManager manager) {
		if ( params != null && params.length > 0 )  {
			name = params[0];
		}
		
		run();
	}

	@Override
	protected INewWizard createWizard() throws CoreException {
		SigilProjectWizard wizard = new SigilProjectWizard();
		wizard.setName(name);
		return wizard;
	}
}
