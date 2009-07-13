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
import org.cauldron.sigil.model.ModelElementFactory;
import org.cauldron.sigil.model.ModelElementFactoryException;
import org.cauldron.sigil.model.eclipse.IDownloadJar;
import org.cauldron.sigil.model.project.ISigilProjectModel;
import org.cauldron.sigil.ui.form.SigilPage;
import org.cauldron.sigil.ui.util.ModelLabelProvider;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * @author dave
 *
 */
public class DownloadSection extends AbstractResourceSection {

	/**
	 * @param page
	 * @param parent
	 * @param project
	 * @throws CoreException 
	 */
	
	private IDownloadJar dl;
	
	public DownloadSection(SigilPage page, Composite parent, ISigilProjectModel project) throws CoreException {
		super( page, parent, project );
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.ui.editors.project.SigilSection#createSection(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected void createSection(Section section, FormToolkit toolkit) throws CoreException {
		setTitle( "Codebase" );
		
		Composite body = createTableWrapBody(1, toolkit);

        toolkit.createLabel( body, "Specify which resources are included as part of this bundles remote codebase." );
		
		tree = toolkit.createTree( body, SWT.CHECK | SWT.BORDER );
		
		TableWrapData data = new TableWrapData( TableWrapData.FILL_GRAB);
		data.heightHint = 200;
		tree.setLayoutData( data );
		
		viewer = new CheckboxTreeViewer( tree );
		IFolder base = ResourcesPlugin.getWorkspace().getRoot().getFolder(getProjectModel().getJavaModel().getOutputLocation());
		viewer.setContentProvider( new ContainerTreeProvider() );
		viewer.setLabelProvider( new ModelLabelProvider() );
		viewer.addCheckStateListener( this );
		viewer.setInput( base );
		
		dl = getProjectModel().getBundle().getDownloadJar();
		
		startWorkspaceListener(base.getWorkspace());
	}

	@Override
	public void refresh() {
		dl = getProjectModel().getBundle().getDownloadJar();
		super.refresh();
	}

	@Override
	public void commit(boolean onSave) {
		getProjectModel().getBundle().setDownloadJar(dl);			
		super.commit(onSave);
	}

	@Override
	protected void refreshSelections() {
		// zero the state
		if ( dl != null ) {
			for ( IPath path : dl.getEntrys() ) {
				IResource r = findResource( path );
				if ( r != null ) {
					viewer.expandToLevel(r, 0);
					viewer.setChecked( r, true );
					viewer.setGrayed( r, false );
					handleStateChanged(r, true, false, false);
				}
				else {
					SigilCore.error( "Unknown path " + path );
				}
			}
		}
	}	
	
	protected void syncResourceModel(IResource element, boolean checked) {
		try {
			if ( dl == null ) {
				dl = ModelElementFactory.getInstance().newModelElement(IDownloadJar.class);
				getProjectModel().getBundle().setDownloadJar(dl);
			}
			
			if ( checked ) {
				dl.addEntry( element.getProjectRelativePath() );
			}
			else {
				dl.removeEntry( element.getProjectRelativePath() );
			}
			
			markDirty();
		}
		catch (ModelElementFactoryException e) {
			SigilCore.error( "Failed to create model element", e );
		}
	}	
}
