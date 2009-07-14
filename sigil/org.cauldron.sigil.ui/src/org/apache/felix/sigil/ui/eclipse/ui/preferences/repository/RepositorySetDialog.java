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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultLabelProvider;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTableProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class RepositorySetDialog extends TitleAreaDialog {
	
	private CheckboxTableViewer viewer;
	private Text nameTxt;
	private Button upBtn;
	private Button downBtn;
	private final String setName;
	private List<IRepositoryModel> repositories;
	private final boolean nameEditable;
	private final Set<String> set;
	
	private String newName;
	
	public RepositorySetDialog(Shell shell, Set<String> set) {
		this(shell, null, true, set);
	}
	
	public RepositorySetDialog(Shell parent, RepositoryViewData data, boolean nameEditable, Set<String> set) {
		super(parent);
		this.set = set;
		this.setName = data == null ? "" : data.getName();
		this.repositories = data == null ? new ArrayList<IRepositoryModel>() : new ArrayList<IRepositoryModel>(Arrays.asList(data.getRepositories()));
		this.nameEditable = nameEditable;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		createControl(area);
		return area;
	}

	public void createControl(Composite parent) {
		// controls
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayoutData( new GridData(GridData.FILL_BOTH) );
		
		if ( nameEditable ) {
			new Label( body, SWT.NONE ).setText( "Name" );
			
			nameTxt = new Text( body, SWT.BORDER );
			
			nameTxt.setText(setName);
			
			nameTxt.addKeyListener( new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					checkComplete();
				}
			});
		}
		
		Composite table = new Composite(body, SWT.NONE);
		table.setLayout( new GridLayout(2, false ) );
		createTable( table );
		
		// layout
		body.setLayout( new GridLayout( 2, false ) );
		if ( nameEditable ) {
			nameTxt.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false) );
		}
		table.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1) );
	}
	
	public RepositoryViewData getData() {
		String name = nameEditable ? newName : setName;
		IRepositoryModel[] reps = repositories.toArray( new IRepositoryModel[repositories.size()]);
		return new RepositoryViewData( name, reps );
	}

	private void checkComplete() {
		if ( nameEditable ) {
			String name = nameTxt.getText();
			if ( !name.equals( setName ) && set.contains( name ) ) {
				setErrorMessage("Set " + name + " already exists" );
				Button b = getButton(IDialogConstants.OK_ID);
				b.setEnabled(false);
			}
		}		
		setErrorMessage(null);
		Button b = getButton(IDialogConstants.OK_ID);
		b.setEnabled(true);
	}


	@Override
	protected void okPressed() {
		if ( nameEditable ) {
			newName = nameTxt.getText();
		}
		repositories = getRepositories();
		super.okPressed();
	}

	private void createTable(Composite body) {
		createViewer(body);
		
		Composite btns = new Composite(body, SWT.NONE);
		btns.setLayout( new GridLayout( 1, true ) );
		
		createButtons(btns);
		
		// layout
		viewer.getTable().setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
		btns.setLayoutData( new GridData( SWT.RIGHT, SWT.TOP, false, false ) );
	}

	private void createButtons(Composite parent) {
		upBtn = new Button(parent, SWT.PUSH);
		upBtn.setText( "Up" );
		upBtn.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				up();
			}
		});
		
		downBtn = new Button(parent, SWT.PUSH);
		downBtn.setText( "Down" );
		downBtn.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				down();
			}
		});
		
		setUpDownEnabled(false);
	}

	private void up() {
		IRepositoryModel model = (IRepositoryModel) ((StructuredSelection) viewer.getSelection()).getFirstElement();
		int i = repositories.indexOf(model);
		if ( i > 0 ) {
			repositories.remove( i );
			repositories.add( i - 1, model );
			viewer.refresh();
		}
	}

	private void down() {
		IRepositoryModel model = (IRepositoryModel) ((StructuredSelection) viewer.getSelection()).getFirstElement();
		int i = repositories.indexOf(model);
		if ( i < repositories.size() - 1 ) {
			repositories.remove( i );
			repositories.add( i + 1, model );
			viewer.refresh();
		}
	}

	private void createViewer(Composite parent) {
		viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		
		viewer.addSelectionChangedListener( new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setUpDownEnabled( !viewer.getSelection().isEmpty() ); 
			}
		});
		
		viewer.setContentProvider( new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return toArray(inputElement);
			}
		});
		
		viewer.setLabelProvider( new DefaultLabelProvider() {
			public Image getImage(Object element) {
				return null;
			}

			public String getText(Object element) {
				IRepositoryModel m = (IRepositoryModel) element;
				return m.getName();
			}			
		});
		
		viewer.setInput( repositories );
		
		for ( IRepositoryModel m : repositories ) {
			viewer.setChecked(m, true);
		}
		
		List<IRepositoryModel> allRepositories = SigilCore.getRepositoryConfiguration().loadRepositories();
		
		for ( IRepositoryModel m : allRepositories ) {
			if ( !repositories.contains(m) ) {
				repositories.add(m);
			}
		}
		
		viewer.refresh();		
	}

	private void setUpDownEnabled(boolean enabled) {
		upBtn.setEnabled(enabled);
		downBtn.setEnabled(enabled);
	}

	private List<IRepositoryModel> getRepositories() {
		ArrayList<IRepositoryModel> reps = new ArrayList<IRepositoryModel>();
		
		for ( IRepositoryModel m : repositories ) {
			if ( viewer.getChecked(m) ) {
				reps.add( m );
			}
		}
		
		return reps;
	}
}
