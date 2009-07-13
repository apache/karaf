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

package org.cauldron.sigil.ui.preferences.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.model.repository.IRepositoryModel;
import org.cauldron.sigil.model.repository.IRepositorySet;
import org.cauldron.sigil.model.repository.RepositorySet;
import org.cauldron.sigil.ui.util.DefaultLabelProvider;
import org.cauldron.sigil.ui.util.DefaultTableProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class RepositorySetsView {
	private static final String DEFAULT = "default";

	private final RepositoriesPreferencePage page;

	private ArrayList<RepositoryViewData> sets = new ArrayList<RepositoryViewData>();
	
	private TableViewer setView;

	private RepositoryViewData defaultSet;
	
	public RepositorySetsView(RepositoriesPreferencePage page) {
		this.page = page;
	}

	public Control createContents(Composite parent) {
		// Create Controls
		Composite composite = new Composite(parent, SWT.NONE);
		
		Table table = new Table(composite, SWT.SINGLE | SWT.BORDER);

		// Table Viewer Setup
		setView = new TableViewer(table);

		setView.setContentProvider( new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return toArray(inputElement);
			}
		} );
		
		defaultSet = new RepositoryViewData(DEFAULT, SigilCore.getRepositoryConfiguration().getDefaultRepositorySet().getRepositories());
		
		sets.add( defaultSet );
		
		for( Map.Entry<String, IRepositorySet> e : SigilCore.getRepositoryConfiguration().loadRepositorySets().entrySet() ) {
			IRepositorySet s = e.getValue();
			sets.add( new RepositoryViewData( e.getKey(), s.getRepositories() ) );
		}
		
		setView.setLabelProvider( new DefaultLabelProvider() {
			public Image getImage(Object element) {
				return null;
			}

			public String getText(Object element) {
				RepositoryViewData data = (RepositoryViewData) element;
				return data.getName();
			}
		});
		
		setView.setInput(sets);
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 6));
		
		createButtons(composite);	
				
		return composite;
	}
	
	private void createButtons(final Composite composite) {
		final Button add = new Button(composite, SWT.PUSH);
		add.setText("Add...");
		add.setEnabled(true);
		
		final Button edit = new Button(composite, SWT.PUSH);
		edit.setText("Edit...");
		edit.setEnabled(false);
				
		final Button remove = new Button(composite, SWT.PUSH);
		remove.setText("Remove");
		remove.setEnabled(false);
		// Listeners
		add.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				add(composite);
			}			
		});
		
		edit.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) setView.getSelection();
				edit(composite, sel);
			}			
		});
		
		remove.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) setView.getSelection();
				remove(sel);
			}			
		});

		setView.addSelectionChangedListener( new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				boolean enabled = !event.getSelection().isEmpty();
				if ( enabled ) {
					RepositoryViewData element = (RepositoryViewData) ((IStructuredSelection) event.getSelection()).getFirstElement();
					edit.setEnabled(true);
					remove.setEnabled( element != defaultSet );
				}
				else {
					edit.setEnabled(false);
					remove.setEnabled(false);
				}
			}
		});
		
		add.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		edit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		remove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));		
	}
	
	private void add(Control parent) {
		RepositorySetDialog wizard = new RepositorySetDialog(getShell(parent), getNames());
		if ( wizard.open() == Window.OK ) {
			sets.add( wizard.getData() );
			updated();
		}
	}

	private void edit(Control parent, IStructuredSelection sel) {
		RepositoryViewData data = (RepositoryViewData) sel.getFirstElement();
		RepositorySetDialog wizard = new RepositorySetDialog(getShell(parent), data, data != defaultSet, getNames());
		if ( wizard.open() == Window.OK ) {
			if ( data != defaultSet ) {
				data.setName( wizard.getData().getName() );
			}
			data.setRepositories( wizard.getData().getRepositories() );
			updated();
		}
	}

	private Set<String> getNames() {
		HashSet<String> names = new HashSet<String>();
		
		for ( RepositoryViewData view : sets ) {
			if ( view != defaultSet ) {
				names.add( view.getName() );
			}
		}
		
		return names;
	}

	private Shell getShell(Control parent) {
		return parent.getShell();
	}

	private void remove(IStructuredSelection sel) {
		if ( sets.remove(sel.getFirstElement())) {
			updated();
		}
	}

	private void updated() {
		setView.refresh();
		page.changed();
	}

	public Map<String, IRepositorySet> getSets() {
		HashMap<String, IRepositorySet> ret = new HashMap<String, IRepositorySet>();
		
		for ( RepositoryViewData data : sets ) {
			if ( data != defaultSet ) {
				IRepositorySet set = new RepositorySet(data.getRepositories());
				ret.put( data.getName(), set );
			}
		}
		
		return ret;
	}

	public IRepositoryModel[] getDefaultRepositories() {
		return defaultSet.getRepositories();
	}
}
