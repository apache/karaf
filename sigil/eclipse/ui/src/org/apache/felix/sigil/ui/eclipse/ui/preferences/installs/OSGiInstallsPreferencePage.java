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

package org.apache.felix.sigil.ui.eclipse.ui.preferences.installs;

import java.util.HashMap;
import java.util.UUID;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.install.IOSGiInstallType;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.apache.felix.sigil.ui.eclipse.ui.preferences.ProjectDependentPreferencesPage;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTableProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class OSGiInstallsPreferencePage extends ProjectDependentPreferencesPage implements
		IWorkbenchPreferencePage {

	private class Install {
		private String id;
		private String location;
		private IOSGiInstallType type;
		
		private Install(String id, String location) {
			this.id = id;
			this.location = location;
		}

		private IOSGiInstallType getType() {
			if ( type == null ) {
				type = SigilCore.getInstallManager().findInstallType(location);
			}
			return type;
		}
	}

	private HashMap<String, Install> installs = new HashMap<String, Install>();
	private CheckboxTableViewer viewer;
	private boolean changed;
	
	public OSGiInstallsPreferencePage() {
		super("OSGi Installs");
	}
	
	public void init(IWorkbench workbench) {
	}


	@Override
	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		
		buildComponents(control);
		
		load();
		
		checkValid();
		
		return control;
	}

	@Override
	protected boolean isDirty() {
		return changed;
	}

	
	private void buildComponents(Composite control) {
		new Label(control, SWT.NONE).setText("Installs:");
		new Label(control, SWT.NONE); // padding
		
		Table table = new Table(control, SWT.CHECK | SWT.SINGLE | SWT.BORDER);
		
		Button add = new Button(control, SWT.PUSH);
		add.setText("Add");
		add.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}			
		});
		
		final Button remove = new Button(control, SWT.PUSH);
		remove.setEnabled(false);
		remove.setText("Remove");
		remove.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remove();
			}			
		});
		
		// viewers
		viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider( new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return toArray(inputElement);
			}			
		});
				
		viewer.setLabelProvider( new LabelProvider() {
			@Override
			public String getText(Object element) {
				Install i = (Install) element;
				IOSGiInstallType type = i.getType();
				if ( type == null ) {
					return "<invalid> [" + i.location + "]";
				}
				else {
					return type.getName() + " " + type.getVersion() + " [" + i.location + "]";
				}
			}

			@Override
			public Image getImage(Object element) {
				Install i = (Install) element;
				IOSGiInstallType type = i.getType();
				
				if (type == null) {
					return null;
				} else {
				return type.getIcon();
				}
			}			
		});
		
		viewer.addSelectionChangedListener( new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				boolean enabled = !event.getSelection().isEmpty();
				remove.setEnabled(enabled );
			}
		});
		
		viewer.addCheckStateListener( new ICheckStateListener () {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if ( event.getChecked() ) {
					changed = true;
				}
				viewer.setCheckedElements( new Object[] { event.getElement() } );
			}
		});
		
		viewer.setInput(installs.values());
		
		// layout
		control.setLayout( new GridLayout(2, false) );
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		add.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		remove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));		
	}

	private void load() {
		String pref = getPreferenceStore().getString(SigilCore.OSGI_INSTALLS);
		if ( pref != null && pref.length() > 0 ) {
			for ( String id : pref.split(",") ) {
				String loc = getPreferenceStore().getString( SigilCore.OSGI_INSTALL_PREFIX + id );
				installs.put( id, new Install( id, loc ) );
			}
		}
		
		viewer.refresh();
		
		if ( !installs.isEmpty() ) {
			String defId = getPreferenceStore().getString( SigilCore.OSGI_DEFAULT_INSTALL_ID );
			if ( defId == null || defId.trim().length() == 0 ) {
				viewer.setCheckedElements( new Object[] { installs.values().iterator().next() } );
			}
			else {
				viewer.setCheckedElements( new Object[] { installs.get( defId ) } );
			}
		}
	}
	
	protected void doSave() {
		// zero out old configs
		String pref = getPreferenceStore().getString(SigilCore.OSGI_INSTALLS);
		if ( pref != null && pref.length() > 0 ) {
			for ( String id : pref.split(",") ) {
				getPreferenceStore().setToDefault( SigilCore.OSGI_INSTALL_PREFIX + id );
			}
		}
		
		// store new configs
		if ( installs.isEmpty() ) {
			getPreferenceStore().setToDefault(SigilCore.OSGI_INSTALLS);
			getPreferenceStore().setToDefault(SigilCore.OSGI_DEFAULT_INSTALL_ID);
		}
		else {
			StringBuffer buf = new StringBuffer();
			for (Install i : installs.values() ) {
				if ( buf.length() > 0 ) {
					buf.append(",");
				}
				buf.append( i.id );
				getPreferenceStore().setValue( SigilCore.OSGI_INSTALL_PREFIX + i.id, i.location );
			}
			
			getPreferenceStore().setValue( SigilCore.OSGI_INSTALLS, buf.toString() );
			Install def = (Install) viewer.getCheckedElements()[0];
			getPreferenceStore().setValue(SigilCore.OSGI_DEFAULT_INSTALL_ID, def.id);
		}
		changed = false;
	}

	private boolean isOK() {
		return installs.isEmpty() || viewer.getCheckedElements().length > 0;
	}
	
	private void add() {
		Shell shell = SigilUI.getActiveWorkbenchShell();
		DirectoryDialog dialog = new DirectoryDialog(shell);
		String dir = dialog.open();
		if ( dir != null ) {
			Install install = new Install( UUID.randomUUID().toString(), dir );
			if ( install.getType() == null ) {
				MessageDialog.openError(shell, "Error", "Invalid OSGi install directory" );
			}
			else {
				boolean empty = installs.isEmpty();
				
				installs.put( install.id, install );
				viewer.refresh();
				
				if ( empty ) {
					viewer.setCheckedElements( new Object[] { install } );
				}
				
				checkValid();
				changed = true;
			}
		}
	}

	private void checkValid() {
		if ( isOK() ) {
			setErrorMessage(null);
			setValid(true);
		}
		else {
			setErrorMessage("Missing default OSGi install");
			setValid(false);
		}
	}

	private void remove() {
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
		Install i = (Install) sel.getFirstElement();
		boolean def = viewer.getChecked(i);
		installs.remove(i.id);
		viewer.refresh();
		if ( def && installs.size() > 0 ) {
			viewer.setCheckedElements( new Object[] { installs.values().iterator().next() } );
		}
		checkValid();
		changed = true;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return SigilCore.getDefault().getPreferenceStore();
	}
}
