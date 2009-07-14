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

package org.apache.felix.sigil.ui.eclipse.ui.preferences.repository;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTableProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

public class RepositoryTypeSelectionPage extends WizardSelectionPage implements IWizardPage {

	private static final String TITLE = "Select Repository";
	
	private TableViewer repositoryView;
	private IRepositoryModel repositoryElement;
	
	public RepositoryTypeSelectionPage() {
		super(TITLE);
		setTitle(TITLE);
	}

	public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		
		// components
		new Label(control, SWT.NONE).setText("Repositories" );
		Table table = new Table(control, SWT.SINGLE | SWT.BORDER);
		
		// layout
		control.setLayout( new GridLayout(1, true) );
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// view
		repositoryView = new TableViewer(table);
		repositoryView.setLabelProvider( new LabelProvider() {
			@Override
			public String getText(Object element) {
				IRepositoryType rep = (IRepositoryType) element;
				return rep.getType();
			}

			@Override
			public Image getImage(Object element) {
				IRepositoryType rep = (IRepositoryType) element;
				return rep.getIcon();
			}			
		});
		
		repositoryView.setContentProvider( new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return toArray(inputElement);
			}
		} );
		
		repositoryView.addSelectionChangedListener( new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if ( !event.getSelection().isEmpty() ) {
					IStructuredSelection sel = (IStructuredSelection) event.getSelection();
					IRepositoryType type = (IRepositoryType) sel.getFirstElement();
					repositoryElement = SigilCore.getRepositoryConfiguration().newRepositoryElement(type);
					selectWizardNode(new RepositoryWizardNode(repositoryElement));
				}
			}
		});
		
		ArrayList<IRepositoryType> descriptors = new ArrayList<IRepositoryType>(SigilCore.getRepositoryConfiguration().loadRepositoryTypes());
		
		for ( Iterator<IRepositoryType> i = descriptors.iterator(); i.hasNext(); ) {
			if ( !i.next().isDynamic() ) {
				i.remove();
			}
		}
		
		repositoryView.setInput( descriptors );
		
		setControl(control);
	}
	
	public void selectWizardNode(RepositoryWizardNode node) {
		setSelectedNode(node);
	}
	
	public RepositoryWizardNode getSelectedWizardNode() {
		return (RepositoryWizardNode) getSelectedNode();
	}

	public IRepositoryModel getRepository() {		
		return repositoryElement;
	}

}
