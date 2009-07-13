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

import java.util.Iterator;
import java.util.List;

import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.model.ModelElementFactory;
import org.cauldron.sigil.model.ModelElementFactoryException;
import org.cauldron.sigil.model.common.VersionRange;
import org.cauldron.sigil.model.common.VersionRangeBoundingRule;
import org.cauldron.sigil.model.eclipse.ISigilBundle;
import org.cauldron.sigil.model.osgi.IPackageExport;
import org.cauldron.sigil.model.osgi.IPackageImport;
import org.cauldron.sigil.model.project.ISigilProjectModel;
import org.cauldron.sigil.ui.form.SigilPage;
import org.cauldron.sigil.ui.preferences.OptionalPrompt;
import org.cauldron.sigil.ui.util.DefaultTableProvider;
import org.cauldron.sigil.ui.util.ResourcesDialogHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.framework.Version;

public class ExportPackagesSection extends BundleDependencySection {

	public ExportPackagesSection(SigilPage page, Composite parent, ISigilProjectModel project) throws CoreException {
		super( page, parent, project );
	}
		
	@Override
	protected String getTitle() {
		return "Export Packages";
	}

	@Override
	protected Label createLabel(Composite parent, FormToolkit toolkit) {
		return toolkit.createLabel( parent, "Specify which packages this bundle shares with other bundles." );
	}

	@Override
	protected IContentProvider getContentProvider() {
        return new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return getBundle().getBundleInfo().getExports().toArray();
			}
	    };
	}

	@Override
	protected void handleAdd() {
		NewPackageExportDialog dialog = ResourcesDialogHelper.createNewExportDialog(getSection().getShell(), "Add Exported Package", null, getProjectModel(), true);

		if ( dialog.open() == Window.OK ) {
			try {
				// Add selected exports
				boolean exportsAdded = false;
				
				List<IPackageFragment> newPkgFragments = dialog.getSelectedElements();
				for (IPackageFragment pkgFragment : newPkgFragments) {
					IPackageExport pkgExport = ModelElementFactory.getInstance().newModelElement(IPackageExport.class);
					pkgExport.setPackageName(pkgFragment.getElementName());
					pkgExport.setVersion(dialog.getVersion());
					getBundle().getBundleInfo().addExport(pkgExport);
					
					exportsAdded = true;
				}

				// Add corresponding imports (maybe)
				boolean importsAdded = false;
				
				IPreferenceStore store = SigilCore.getDefault().getPreferenceStore();
				boolean shouldAddImports = OptionalPrompt.optionallyPrompt(store, SigilCore.PREFERENCES_ADD_IMPORT_FOR_EXPORT, "Add Exports", "Should corresponding imports be added?", getSection().getShell());
				if(shouldAddImports) {
					for (IPackageFragment pkgFragment : newPkgFragments) {
						IPackageImport pkgImport = ModelElementFactory.getInstance().newModelElement(IPackageImport.class);
						pkgImport.setPackageName(pkgFragment.getElementName());
						VersionRangeBoundingRule lowerBound = VersionRangeBoundingRule.valueOf(store.getString(SigilCore.DEFAULT_VERSION_LOWER_BOUND));
						VersionRangeBoundingRule upperBound = VersionRangeBoundingRule.valueOf(store.getString(SigilCore.DEFAULT_VERSION_UPPER_BOUND));
						Version version = dialog.getVersion();
						if(version == null) {
							version = getBundle().getVersion();
						}
						VersionRange versionRange = VersionRange.newInstance(version, lowerBound, upperBound);
						pkgImport.setVersions(versionRange);
						
						getBundle().getBundleInfo().addImport(pkgImport);
						
						importsAdded = true;
					}
				}
				
				if(importsAdded) {
					((SigilProjectEditorPart) getPage().getEditor()).refreshAllPages();
					markDirty();
				} else if(exportsAdded) {
					refresh();
					markDirty();
				}
			}
			catch (ModelElementFactoryException e) {
				SigilCore.error( "Failed to buiild model element for package export", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleEdit() {
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		
		boolean changed = false;
		
		if ( !selection.isEmpty() ) {
			for ( Iterator<IPackageExport> i = selection.iterator(); i.hasNext(); ) {	
				IPackageExport packageExport = i.next();
				NewPackageExportDialog dialog = ResourcesDialogHelper.createNewExportDialog(getSection().getShell(), "Edit Imported Package", packageExport, getProjectModel(), false);
				if ( dialog.open() == Window.OK ) {
					changed = true;
					IPackageFragment pkgFragment = dialog.getSelectedElement();
					packageExport.setPackageName(pkgFragment.getElementName());
					packageExport.setVersion(dialog.getVersion());
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
			for ( Iterator<IPackageExport> i = selection.iterator(); i.hasNext(); ) {			
				getBundle().getBundleInfo().removeExport( i.next() );
			}		
			
			refresh();
			markDirty();
		}
	}
		
	private ISigilBundle getBundle() {
		return getProjectModel().getBundle();
	}

}
