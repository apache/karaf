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

package org.apache.felix.sigil.ui.eclipse.ui.editors.project;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class ContentsForm extends SigilPage  {

	public static final String PAGE_ID = "contents";
	
	private ISigilProjectModel project;
	
	public ContentsForm(FormEditor editor, ISigilProjectModel project) {
		super(editor, PAGE_ID, "Contents");
		this.project = project;
	}
	
    @Override
    protected void createFormContent(IManagedForm managedForm) {
        FormToolkit toolkit = managedForm.getToolkit();
        
        ScrolledForm form = managedForm.getForm();
        form.setText( "Contents" );
        
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
        
        Composite top = toolkit.createComposite(body);
        layout = new TableWrapLayout();
        layout.verticalSpacing = 20;
        top.setLayout(layout);
        TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
        data.colspan = 2;
        top.setLayoutData(data);
        
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
	        ClasspathSection classpath = new ClasspathSection( this, top, project );
	        managedForm.addPart( classpath );
	        
	        ResourceBuildSection runtimeBuild = new ResourceBuildSection( this, left, project );
	        managedForm.addPart( runtimeBuild );
	        
	        DownloadSection download = new DownloadSection( this, right, project );
	        managedForm.addPart( download );	        
        }
        catch (CoreException e) {
        	SigilCore.error( "Failed to create contents form", e);
        }
    }
}
