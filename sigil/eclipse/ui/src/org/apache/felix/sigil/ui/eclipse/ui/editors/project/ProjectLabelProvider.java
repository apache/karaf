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

import java.io.InputStream;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultLabelProvider;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Widget;

public class ProjectLabelProvider extends DefaultLabelProvider implements IBaseLabelProvider {

	private final Widget parent;
	private final ImageRegistry registry;
	private final IDependencyChecker checker;
	
	public ProjectLabelProvider(Widget parent, ImageRegistry registry ) {
		this(parent, registry, null);
	}
	
	public ProjectLabelProvider(Widget parent, ImageRegistry registry, IDependencyChecker checker) {
		this.parent = parent;
		this.registry = registry;
		this.checker = checker;
	}

	public Image getImage(Object element) {
		Image result = null;
		if ( element instanceof ISigilBundle || element instanceof IRequiredBundle || element instanceof IBundleModelElement) {
			result = findBundle();
		}
		else if (element instanceof IPackageImport) {
			if(checker != null) {
				result = checker.isSatisfied((IPackageImport) element) ? findPackage() : findPackageError();
			} else {
				result = findPackage();
			}
		}
		else if (element instanceof IPackageExport) {
			result = findPackage();
		}
		else if ( element instanceof IPackageFragmentRoot ) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) element;
			try {
				if ( root.getKind() == IPackageFragmentRoot.K_SOURCE ) {
					result = findPackage();
				}
				else {
					result = findBundle();
				}
			} catch (JavaModelException e) {
				SigilCore.error( "Failed to inspect package fragment root", e );
			}
		}
		else if ( element instanceof IClasspathEntry ) {
			result = findPackage();
		}
	
		return result;
	}

	public String getText(Object element) {
		if ( element instanceof ISigilBundle ) {
			ISigilBundle bundle = (ISigilBundle) element;
			return bundle.getBundleInfo().getSymbolicName(); 
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
			return pe.getPackageName() + " " + pe.getVersion();
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
		
		return element.toString();
	}

	private Image findPackage() {
		Image image = registry.get( "package" );
		
		if ( image == null ) {
			image = loadImage( "icons/package_obj.png" ); 
			registry.put( "package", image);
		}
		
		return image; 
	}
	
	private Image findPackageError() {
		Image image = registry.get("package_error");
		if(image == null) {
			image = loadImage("icons/package_obj_error.gif");
			registry.put("package_error", image);
		}
		return image;
	}

	private Image findBundle() {
		Image image = registry.get( "bundle" );
		
		if ( image == null ) {
			image = loadImage( "icons/jar_obj.png" ); 
			registry.put( "bundle", image);
		}
		
		return image; 
	}
		
	private Image loadImage(String resource) {
		InputStream in = ProjectLabelProvider.class.getClassLoader().getResourceAsStream( resource );
		ImageData data = new ImageData( in );
		return new Image( parent.getDisplay(), data );
	}

}
