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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.cauldron.sigil.model.eclipse.ISigilBundle;
import org.cauldron.sigil.model.project.ISigilProjectModel;
import org.cauldron.sigil.model.util.JavaHelper;
import org.cauldron.sigil.ui.form.SigilPage;
import org.cauldron.sigil.ui.form.SigilSection;
import org.cauldron.sigil.ui.util.BackgroundLoadingSelectionDialog;
import org.cauldron.sigil.ui.util.DefaultTableProvider;
import org.cauldron.sigil.ui.util.IFilter;
import org.cauldron.sigil.ui.util.ModelLabelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class ClasspathSection extends SigilSection {

	private ProjectTableViewer viewer;
	private final Comparator<IClasspathEntry> CLASSPATH_COMPARATOR = new Comparator<IClasspathEntry>() {
		public int compare(IClasspathEntry o1, IClasspathEntry o2) {
			return compareClasspaths(o1, o2);
		}
	};

	public ClasspathSection(SigilPage page, Composite parent, ISigilProjectModel project) throws CoreException {
		super( page, parent, project );
	}
	
	@Override
	protected void createSection(Section section, FormToolkit toolkit) {
		setTitle( "Classpath");
		
		Composite body = createGridBody(2, false, toolkit);
		
        Label label = toolkit.createLabel( body, "Specify the internal classpath of this bundle." );
        label.setLayoutData( new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1 ) );
		
		Table bundleTable = toolkit.createTable(body, SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.BORDER);
		GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableLayoutData.heightHint = 150;
        bundleTable.setLayoutData(tableLayoutData);
        
        createButtons(body, toolkit);
        createViewer( bundleTable );
	}

	private void createButtons(Composite body, FormToolkit toolkit) {
        Composite buttons = toolkit.createComposite(body);
        TableWrapLayout layout = new TableWrapLayout();
        layout.numColumns = 1;
        layout.topMargin = 0;
        layout.leftMargin = 0;
        layout.rightMargin = 0;
        layout.bottomMargin = 0;
        buttons.setLayout( layout );
        
        Button add = toolkit.createButton(buttons, "Add", SWT.NULL);
        add.setLayoutData( new TableWrapData( TableWrapData.FILL ) );
        add.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}        	
        } );
        
        Button remove = toolkit.createButton(buttons, "Remove", SWT.NULL);
        remove.setLayoutData( new TableWrapData( TableWrapData.FILL ) );
        remove.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoved();
			}
        } );
	}

	private void createViewer(Table bundleTable) {
        viewer = new ProjectTableViewer(bundleTable);
        viewer.setContentProvider( new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				ArrayList<IClasspathEntry> cp = new ArrayList<IClasspathEntry>();
				for (IClasspathEntry cpe : JavaHelper.findClasspathEntries(getBundle())) {
					cp.add(cpe);
				}
				
				Collections.sort(cp, new Comparator<IClasspathEntry>() {
					public int compare(IClasspathEntry o1, IClasspathEntry o2) {
						return o1.toString().compareTo(o2.toString());
					}
				});
				return cp.toArray();
			}
        });    
        viewer.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				return index((IClasspathEntry) element);
			}
        });
	}
	
	protected ISigilBundle getBundle() {
		return getProjectModel().getBundle();
	}

	private void handleAdd() {
		try {
			BackgroundLoadingSelectionDialog<IClasspathEntry> dialog = new BackgroundLoadingSelectionDialog<IClasspathEntry>(getSection().getShell(), "Classpath Entry:", true);
			
			dialog.setDescriptor(new IElementDescriptor<IClasspathEntry>() {
				public String getName(IClasspathEntry element) {
					return element.getPath().toString();
				}
			
				public String getLabel(IClasspathEntry element) {
					return getName(element);
				}
			});
			
			dialog.setLabelProvider(new ModelLabelProvider());
			
			dialog.setFilter(new IFilter<IClasspathEntry>() {
				public boolean select(IClasspathEntry cp) {
					switch ( cp.getEntryKind() ) {
					case IClasspathEntry.CPE_LIBRARY:
					case IClasspathEntry.CPE_VARIABLE:
					case IClasspathEntry.CPE_SOURCE:
						return !getBundle().getClasspathEntrys().contains(encode(cp));
					default:
						return false;
					}
				}
			});
			
			dialog.setComparator(CLASSPATH_COMPARATOR);
			
			IClasspathEntry[] classpath = getProjectModel().getJavaModel().getRawClasspath();
			dialog.addElements(Arrays.asList(classpath));
			if ( dialog.open() == Window.OK ) {
				List<IClasspathEntry> selectedElements = dialog.getSelectedElements();
				
				Object[] added = selectedElements.toArray();
				for (IClasspathEntry entry : selectedElements) {
					getBundle().addClasspathEntry(encode(entry));
				}
				viewer.add(added);
				viewer.refresh();
				markDirty();
			}
		} catch (JavaModelException e) {
			ErrorDialog.openError(getSection().getShell(), "Error", null, e.getStatus());
		}
	}

	private int compareClasspaths(IClasspathEntry o1, IClasspathEntry o2) {
		if ( o1.getEntryKind() == o2.getEntryKind() ) {
			ModelLabelProvider mlp = viewer.getLabelProvider();
			return mlp.getText(o1).compareTo(mlp.getText(o2));
		}
		else {
			int i1 = index(o1);
			int i2 = index(o2);
			
			if ( i1 < i2 ) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	private static int index(IClasspathEntry o1) {
		switch ( o1.getEntryKind() ) {
		case IClasspathEntry.CPE_SOURCE: return 0;
		case IClasspathEntry.CPE_PROJECT: return 1;
		case IClasspathEntry.CPE_LIBRARY: return 2;
		case IClasspathEntry.CPE_VARIABLE: return 3;
		case IClasspathEntry.CPE_CONTAINER: return 4;
		default: throw new IllegalStateException( "Unknown classpath entry type " + o1);
		}
	}

	private String encode(IClasspathEntry cp) {
		return getProjectModel().getJavaModel().encodeClasspathEntry(cp).trim();
	}

	@SuppressWarnings("unchecked")
	private void handleRemoved() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

		if ( !selection.isEmpty() ) {
			for ( Iterator<IClasspathEntry> i = selection.iterator(); i.hasNext(); ) {			
				getBundle().removeClasspathEntry(getProjectModel().getJavaModel().encodeClasspathEntry(i.next()).trim());
			}		
			viewer.remove(selection.toArray());
			markDirty();
		}
	}
}
