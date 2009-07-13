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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * @author dave
 *
 */
public class OverviewForm extends SigilPage {
    public static final String PAGE_ID = "overview";
    private ISigilProjectModel sigil;
    
    public OverviewForm(SigilProjectEditorPart editor, ISigilProjectModel sigil) {
        super(editor, PAGE_ID, "Overview");
        this.sigil = sigil;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        FormToolkit toolkit = managedForm.getToolkit();
        
        ScrolledForm form = managedForm.getForm();
        form.setText( "Overview" );
        
        Composite body = form.getBody();        
        TableWrapLayout layout = new TableWrapLayout();
        layout.bottomMargin = 10;
        layout.topMargin = 5;
        layout.leftMargin = 10;
        layout.rightMargin = 10;
        layout.numColumns = 2;
        layout.horizontalSpacing = 10;
        body.setLayout(layout);
        body.setLayoutData(new TableWrapData(TableWrapData.FILL));
        
        Composite left = toolkit.createComposite(body);
        layout = new TableWrapLayout();
        layout.verticalSpacing = 20;
        left.setLayout(layout);
        left.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
        
        Composite right = toolkit.createComposite(body);
        layout = new TableWrapLayout();
        layout.verticalSpacing = 20;
        right.setLayout(layout);
        right.setLayoutData( new TableWrapData( TableWrapData.FILL_GRAB) );
        
        try {
	        GeneralInfoSection general = new GeneralInfoSection(this, left, sigil);
	        managedForm.addPart( general );
	        
	        ContentSummarySection content = new ContentSummarySection( this, right, sigil);
	        managedForm.addPart( content );
	        
	        // XXX-FELIX
	        // commented out due to removal of runtime newton integration
	        // potential to bring back in medium term...
	        //TestingSection testing = new TestingSection(this, right, newton);
	        //managedForm.addPart(testing);
	        
	        ToolsSection tools = new ToolsSection(this, right, sigil);
	        managedForm.addPart(tools);
        }
        catch (CoreException e) {
        	SigilCore.error( "Failed to create overview form", e );
        }
    }
}
