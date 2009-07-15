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


import java.util.Iterator;
import java.util.Set;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.ModelElementFactoryException;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilPage;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTableProvider;
import org.apache.felix.sigil.ui.eclipse.ui.util.ResourcesDialogHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class RequiresBundleSection extends BundleDependencySection {
	
	public RequiresBundleSection(SigilPage page, Composite parent, ISigilProjectModel project, Set<IModelElement> unresolvedElements) throws CoreException {
		super( page, parent, project, unresolvedElements );
	}
	
	@Override
	protected String getTitle() {
		return "Requires Bundles";
	}
	
	@Override
	protected Label createLabel(Composite parent, FormToolkit toolkit) {
		return toolkit.createLabel( parent, "Specify which bundles this bundle depends on." );
	}
	
	@Override
	protected IContentProvider getContentProvider() {
		return new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return getBundle().getBundleInfo().getRequiredBundles().toArray();
			}
		};
	}

	protected ISigilBundle getBundle() {
		return getProjectModel().getBundle();
	}

	@Override
	protected void handleAdd() {
		try {
			NewResourceSelectionDialog<IBundleModelElement> dialog = ResourcesDialogHelper.createRequiredBundleDialog( getSection().getShell(), "Add Required Bundle", getProjectModel(), null, getBundle().getBundleInfo().getRequiredBundles() );
			
			if (dialog.open() == Window.OK) {
				IRequiredBundle required = ModelElementFactory.getInstance().newModelElement( IRequiredBundle.class );
				required.setSymbolicName(dialog.getSelectedName());
				required.setVersions(dialog.getSelectedVersions());
				required.setOptional(dialog.isOptional());
				
				getBundle().getBundleInfo().addRequiredBundle(required);
				refresh();
				markDirty();
			}
		}
		catch (ModelElementFactoryException e) {
			SigilCore.error( "Failed to build required bundle", e );
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleEdit() {
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		
		boolean changed = false;
		
		if ( !selection.isEmpty() ) {
			for ( Iterator<IRequiredBundle> i = selection.iterator(); i.hasNext(); ) {	
				IRequiredBundle requiredBundle = i.next();
				NewResourceSelectionDialog<IBundleModelElement> dialog = ResourcesDialogHelper.createRequiredBundleDialog(getSection().getShell(), "Edit Imported Package", getProjectModel(), requiredBundle, getBundle().getBundleInfo().getRequiredBundles() );
				if ( dialog.open() == Window.OK ) {
					changed = true;
					requiredBundle.setSymbolicName(dialog.getSelectedName());
					requiredBundle.setVersions(dialog.getSelectedVersions());
					requiredBundle.setOptional(dialog.isOptional());
				}
			}					
		}
		
		if ( changed ) {
			refresh();
			markDirty();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void handleRemoved() {
		IStructuredSelection selection = (IStructuredSelection) getSelection();

		if ( !selection.isEmpty() ) {
			for ( Iterator<IRequiredBundle> i = selection.iterator(); i.hasNext(); ) {			
				getBundle().getBundleInfo().removeRequiredBundle( i.next() );
			}		
			
			refresh();
			markDirty();
		}
	}
}
