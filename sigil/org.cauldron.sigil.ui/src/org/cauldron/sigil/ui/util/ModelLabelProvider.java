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

package org.cauldron.sigil.ui.util;

import java.util.Set;

import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.model.IModelElement;
import org.cauldron.sigil.model.eclipse.ISigilBundle;
import org.cauldron.sigil.model.osgi.IBundleModelElement;
import org.cauldron.sigil.model.osgi.IPackageExport;
import org.cauldron.sigil.model.osgi.IPackageImport;
import org.cauldron.sigil.model.osgi.IRequiredBundle;
import org.cauldron.sigil.model.repository.IRepositoryModel;
import org.cauldron.sigil.repository.IBundleRepository;
import org.cauldron.sigil.ui.SigilUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Version;

public class ModelLabelProvider extends LabelProvider {
	
	private volatile Set<? extends IModelElement> unresolvedElements = null;
	
	public Image getImage(Object element) {
		boolean unresolved = (unresolvedElements == null) ? false : unresolvedElements.contains(element);
		
		if ( element instanceof ISigilBundle || element instanceof IBundleModelElement) {
			return findBundle();
		} else if(element instanceof IRequiredBundle) {
			boolean optional = ((IRequiredBundle) element).isOptional();
			return findRequiredBundle(optional, unresolved);
		}
		else if ( element instanceof IPackageImport ) {
			boolean optional = ((IPackageImport) element).isOptional();
			return findPackageImport(optional, unresolved);
		}
		else if ( element instanceof IPackageExport ) {
			return findPackageExport();
		}
		else if ( element instanceof IPackageFragmentRoot ) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) element;
			try {
				if ( root.getKind() == IPackageFragmentRoot.K_SOURCE ) {
					return findPackage();
				}
				else {
					return findBundle();
				}
			} catch (JavaModelException e) {
				SigilCore.error( "Failed to inspect package fragment root", e );
			}
		}
		else if ( element instanceof IClasspathEntry ) {
			return findPackage();
		}
		if ( element instanceof IBundleRepository ) {
			IBundleRepository rep = (IBundleRepository) element;
			IRepositoryModel config = SigilCore.getRepositoryConfiguration().findRepository(rep.getId());
			return config.getType().getIcon();
		}
	
		return null;
	}

	public String getText(Object element) {
		if ( element instanceof ISigilBundle ) {
			ISigilBundle bundle = (ISigilBundle) element;
			return bundle.getBundleInfo().getSymbolicName() + " " + bundle.getBundleInfo().getVersion(); 
		}
		if ( element instanceof IBundleModelElement ) {
			IBundleModelElement bundle = (IBundleModelElement) element;
			return bundle.getSymbolicName();
		}
		if ( element instanceof IRequiredBundle ) {
			IRequiredBundle req = (IRequiredBundle) element;
			return req.getSymbolicName() + " " + req.getVersions();
		}
		
		if ( element instanceof IPackageImport ) {
			IPackageImport req = (IPackageImport) element;
			return req.getPackageName() + " " + req.getVersions();
		}
		
		if ( element instanceof IPackageExport ) {
			IPackageExport pe = (IPackageExport) element;
			Version rawVersion = pe.getRawVersion();
			return rawVersion != null ? pe.getPackageName() + " " + rawVersion : pe.getPackageName();
		}
		
		if ( element instanceof IResource ) {
			IResource resource = (IResource) element;
			return resource.getName();
		}
		
		if ( element instanceof IPackageFragment )  {
			IPackageFragment f = (IPackageFragment) element;
			return f.getElementName();
		}
		
		if ( element instanceof IPackageFragmentRoot ) {
			IPackageFragmentRoot f = (IPackageFragmentRoot) element;
			try {
				return f.getUnderlyingResource().getName();
			} catch (JavaModelException e) {
				return "unknown";
			}
		}
		
		if ( element instanceof IClasspathEntry ) {
			IClasspathEntry cp = (IClasspathEntry) element;
			return cp.getPath().toString();
		}
		
		if ( element instanceof IBundleRepository ) {
			IBundleRepository rep = (IBundleRepository) element;
			IRepositoryModel config = SigilCore.getRepositoryConfiguration().findRepository(rep.getId());
			return config.getName();
		}
		
		return element.toString();
	}

	private Image findPackage() {
		return cacheImage( "icons/package_obj.png" ); 
	}

	private Image findPackageImport(boolean optional, boolean unresolved) {
		String path;
		if(optional) {
			path = unresolved ? "icons/package_obj_import_opt_error.png" : "icons/package_obj_import_opt.png";
		} else {
			path = unresolved ? "icons/package_obj_import_error.png" : "icons/package_obj_import.png";
		}
		return cacheImage(path);
	}
	
	private Image findPackageExport() {
		return cacheImage("icons/package_obj_export.png");
	}
	
	private Image findBundle() {
		return cacheImage("icons/jar_obj.png");
	}
	
	private Image findRequiredBundle(boolean optional, boolean unresolved) {
		String path;
		if(optional) {
			path = unresolved ? "icons/required_bundle_opt_error.png" : "icons/required_bundle_opt.png";
		} else {
			path = unresolved ? "icons/required_bundle_error.png" : "icons/jar_obj.png";
		}
		return cacheImage(path);
	}
	
	private static Image cacheImage(String path) {
		return SigilUI.cacheImage(path, ModelLabelProvider.class.getClassLoader());
	}

	public void setUnresolvedElements(Set<? extends IModelElement> elements) {
		this.unresolvedElements = elements;
	}
}
