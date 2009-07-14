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

package org.apache.felix.sigil.ui.eclipse.ui.form;

import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.parts.IFormEntryListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

@SuppressWarnings("restriction")
public abstract class SigilSection extends SectionPart implements IFormEntryListener, IPartSelectionListener {

	private SigilPage page;
	private ISigilProjectModel project;
	
	public SigilSection(SigilPage page, Composite parent, ISigilProjectModel project) throws CoreException {
		super(parent, page.getManagedForm().getToolkit(), ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED );
		this.project = project;
		this.page = page;
		createSection( getSection(), page.getManagedForm().getToolkit() );		
	}

	public ISigilProjectModel getProjectModel() {
		return project;
	}
	
	public SigilPage getPage() {
		return page;
	}
	
	public void setExpanded( boolean expanded ) {
		getSection().setExpanded(expanded);
	}
	
	protected abstract void createSection(Section section,FormToolkit toolkit ) throws CoreException;
	
	protected void setTitle( String title ) {
			Section section = getSection();
	        section.setText( title );
			section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
			TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
			section.setLayoutData(data);				
	}
	
	protected void setMarker(String type, String message, int priority, int severity) throws CoreException {
		IFileEditorInput file = (IFileEditorInput) getPage().getEditor().getEditorInput();
		IMarker marker = file.getFile().createMarker(type);
		marker.setAttribute( IMarker.MESSAGE, message );
		marker.setAttribute(IMarker.PRIORITY, priority);
		marker.setAttribute( IMarker.SEVERITY, severity );
	}
	
	protected void clearMarkers() throws CoreException {
		IFileEditorInput file = (IFileEditorInput) getPage().getEditor().getEditorInput();
		file.getFile().deleteMarkers(null, true, IResource.DEPTH_INFINITE );
	}
	
	protected Composite createTableWrapBody( int columns, FormToolkit toolkit ) {
		Section section = getSection();
        Composite client = toolkit.createComposite(section);
        
        TableWrapLayout layout = new TableWrapLayout();
        layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
        layout.numColumns = columns;
        client.setLayout(layout);
        client.setLayoutData( new TableWrapData( TableWrapData.FILL_GRAB) );
		
        section.setClient(client);
        
        return client;
	}
	
	protected Composite createGridBody( int columns, boolean columnsSameWidth, FormToolkit toolkit ) {
		Section section = getSection();
        Composite client = toolkit.createComposite(section);
        
		GridLayout layout = new GridLayout();
		
		layout.makeColumnsEqualWidth = columnsSameWidth;
		layout.numColumns = columns;
        client.setLayout(layout);
        
        client.setLayoutData( new TableWrapData( TableWrapData.FILL_GRAB) );
		
        section.setClient(client);
        
        return client;
	}
	
	public void browseButtonSelected(FormEntry entry) {
	}

	public void focusGained(FormEntry entry) {
	}

	public void selectionChanged(FormEntry entry) {
	}

	public void textDirty(FormEntry entry) {
	}

	public void textValueChanged(FormEntry entry) {
	}

	public void linkActivated(HyperlinkEvent e) {
	}

	public void linkEntered(HyperlinkEvent e) {
	}

	public void linkExited(HyperlinkEvent e) {
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
	}
}
