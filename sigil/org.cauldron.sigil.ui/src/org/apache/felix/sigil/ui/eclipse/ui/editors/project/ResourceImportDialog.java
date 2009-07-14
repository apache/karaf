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

import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.common.VersionRange;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class ResourceImportDialog extends ResourceSelectDialog implements VersionsChangeListener {

	private VersionRangeComponent versions;
	private VersionRange range;
	
	public ResourceImportDialog(Shell parentShell, String title, String label, IContentProvider content, ViewerFilter filter, Object scope ) {
		super(parentShell, content, filter, scope, title, label, true);
	}

	public VersionRange getVersions() {
		return range;
	}

	@Override
	protected void createCustom(Composite body) {
		versions = new VersionRangeComponent(body, SWT.BORDER );
		versions.addVersionChangeListener(this);
		versions.setVersions(range);
		
		GridData data = new GridData( SWT.LEFT, SWT.TOP, true, true );
		data.horizontalSpan = 2;
		data.widthHint = 300;
		data.heightHint = 200;
		versions.setLayoutData(data);
	}

	@Override
	protected void selectionChanged(SelectionChangedEvent event) {
		if ( event.getSelection().isEmpty() ) {
			versions.setEnabled(false);
		}
		else {
			versions.setEnabled(true);
		}
	}

	public void versionsChanged(VersionRange range) {
		this.range = range; 
		if ( range == null ) {
			setErrorMessage( "Invalid version" );
		}
		else {
			setErrorMessage( null );
		}
	}

	public void setVersions(VersionRange range) {
		this.range = range;
	}

	public IPackageImport getImport() {
		IPackageImport packageImport = ModelElementFactory.getInstance().newModelElement( IPackageImport.class );
		packageImport.setPackageName( (String) getSelected()[0] );
		packageImport.setVersions( getVersions() );
		return packageImport;
	}
}
