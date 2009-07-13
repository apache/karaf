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

package org.cauldron.sigil.ui.editors.project;

import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.model.project.ISigilProjectModel;
import org.cauldron.sigil.ui.form.SigilPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class ExportsForm extends SigilPage  {

	public static final String PAGE_ID = "exports";
	
	private ISigilProjectModel project;
	
	public ExportsForm(FormEditor editor, ISigilProjectModel project) {
		super(editor, PAGE_ID, "Exports");
		this.project = project;
	}
	
    @Override
    protected void createFormContent(IManagedForm managedForm) {
        ScrolledForm form = managedForm.getForm();
        form.setText( "Exports" );
        
        Composite body = form.getBody();        
        TableWrapLayout layout = new TableWrapLayout();
        layout.bottomMargin = 10;
        layout.topMargin = 5;
        layout.leftMargin = 10;
        layout.rightMargin = 10;
        layout.numColumns = 1;
        layout.horizontalSpacing = 10;
        layout.verticalSpacing = 20;
        body.setLayout(layout);
        body.setLayoutData(new TableWrapData(TableWrapData.FILL));
                       
        try {
	        ExportPackagesSection exportPackages = new ExportPackagesSection( this, body, project );
	        managedForm.addPart( exportPackages );
        }
        catch (CoreException e) {
        	SigilCore.error( "Failed to create contents form", e);
        }
    }
}
