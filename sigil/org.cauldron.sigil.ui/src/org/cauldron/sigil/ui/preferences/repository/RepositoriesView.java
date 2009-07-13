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
import java.util.Iterator;
import java.util.List;

import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.actions.RefreshRepositoryAction;
import org.cauldron.sigil.model.repository.IRepositoryModel;
import org.cauldron.sigil.ui.util.DefaultTableProvider;
import org.cauldron.sigil.ui.wizard.repository.RepositoryWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
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

public class RepositoriesView {
	private final RepositoriesPreferencePage page;

	private List<IRepositoryModel> repositories;
	
	private TableViewer repositoryView;

	public RepositoriesView(RepositoriesPreferencePage page) {
		this.page = page;
	}

	public Control createContents(Composite parent) {
		// Create Controls
		Composite composite = new Composite(parent, SWT.NONE);
		
		Table table = new Table(composite, SWT.MULTI | SWT.BORDER);

		// Table Viewer Setup
		repositoryView = new TableViewer(table);
		repositoryView.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				IRepositoryModel rep = (IRepositoryModel) element;
				return rep.getName();
			}

			@Override
			public Image getImage(Object element) {
				IRepositoryModel rep = (IRepositoryModel) element;				
				return rep.getType().getIcon();
			}
		});

		repositoryView.setContentProvider( new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return toArray(inputElement);
			}
		} );
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 6));
		
		createButtons(composite, repositoryView);	
		
		repositories = SigilCore.getRepositoryConfiguration().loadRepositories();
		repositoryView.setInput(repositories);		
		
		return composite;
	}
	
	private void createButtons(final Composite composite, final TableViewer repositoryView) {
		final Button add = new Button(composite, SWT.PUSH);
		add.setText("Add...");
		add.setEnabled(true);
		
		final Button edit = new Button(composite, SWT.PUSH);
		edit.setText("Edit...");
		edit.setEnabled(false);
				
		final Button remove = new Button(composite, SWT.PUSH);
		remove.setText("Remove");
		remove.setEnabled(false);
		
		final Button refresh = new Button(composite, SWT.PUSH);
		refresh.setText("Refresh");
		refresh.setEnabled( false );
		
		// Listeners
		add.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				add(composite);
			}			
		});
		
		edit.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) repositoryView.getSelection();
				edit(composite, sel);
			}			
		});
		
		remove.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) repositoryView.getSelection();
				remove(sel);
			}			
		});
		
		refresh.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) repositoryView.getSelection();
				refresh(composite, sel);
			}			
		});
		
		repositoryView.addSelectionChangedListener( new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				boolean selected = !event.getSelection().isEmpty();
				if ( selected ) {
					refresh.setEnabled(true);
					
					IStructuredSelection sel = (IStructuredSelection) event.getSelection();
					
					checkEditEnabled(edit, sel);
					checkRemoveEnabled(remove, sel);
				}
				else {
					refresh.setEnabled(false);
					edit.setEnabled(false);
					remove.setEnabled(false);
				}
			}
		});
		
		add.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		edit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		remove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}
	
	@SuppressWarnings("unchecked")
	private void checkRemoveEnabled(Button button, IStructuredSelection sel) {
		boolean alldynamic = true;
		for ( Iterator i = sel.iterator(); i.hasNext(); ) {
			IRepositoryModel model = (IRepositoryModel) i.next();
			if ( !model.getType().isDynamic() ) {
				alldynamic = false;
				break;
			}
		}
		button.setEnabled(alldynamic);
	}

	private void checkEditEnabled(Button edit, IStructuredSelection sel) {
		if ( sel.size() == 1 ) {
			IRepositoryModel element = (IRepositoryModel) sel.getFirstElement();
			if ( WizardHelper.hasWizard( element.getType()) ) {
				edit.setEnabled(true);						
			}
			else {
				edit.setEnabled(false);
			}
		}
		else {
			edit.setEnabled(false);
		}
	}

	@SuppressWarnings("unchecked")
	protected void refresh(Control parent, IStructuredSelection sel) {
		ArrayList<IRepositoryModel> models = new ArrayList<IRepositoryModel>(sel.size());
		
		for ( Iterator i = sel.iterator(); i.hasNext(); ) {
			IRepositoryModel model = (IRepositoryModel) i.next();
			models.add( model );
		}
		
		new RefreshRepositoryAction(models.toArray(new IRepositoryModel[models.size()])).run();
	}

	private void add(Control parent) {
		NewRepositoryWizard wizard = new NewRepositoryWizard();
		WizardDialog dialog = new WizardDialog(getShell(parent), wizard);
		if ( dialog.open() == Window.OK ) {
			repositories.add(wizard.getRepository());
			updated();
		}
	}

	private void edit(Control parent, IStructuredSelection sel) {
		IRepositoryModel model = (IRepositoryModel) sel.getFirstElement();
		try {
			RepositoryWizard wizard = WizardHelper.loadWizard(model.getType());
			wizard.init(model);
			WizardDialog dialog = new WizardDialog(getShell(parent), wizard);
			if ( dialog.open() == Window.OK ) {
				updated();
			}
		}
		catch (CoreException e) {
			SigilCore.error( "Failed to load wizard", e);
			MessageDialog.openError(getShell(parent), "Error", "Failed to load wizard:" + e.getStatus().getMessage() );
		}
	}

	private Shell getShell(Control parent) {
		return parent.getShell();
	}

	@SuppressWarnings("unchecked")
	private void remove(IStructuredSelection sel) {
		boolean change = false;
		for ( Iterator i = sel.iterator(); i.hasNext(); ) {
			change = repositories.remove(i.next());
		}
		
		if ( change ) {
			updated();
		}
	}

	private void updated() {
		repositoryView.refresh();
		page.changed();
	}

	public List<IRepositoryModel> getRepositories() {
		return repositories;
	}	
}
