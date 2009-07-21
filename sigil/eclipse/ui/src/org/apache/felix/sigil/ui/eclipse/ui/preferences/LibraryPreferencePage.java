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

package org.apache.felix.sigil.ui.eclipse.ui.preferences;


import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.model.eclipse.ILibrary;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTableProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class LibraryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{

    private TreeSet<ILibrary> libraries;
    private TableViewer libraryView;

    private Table table;
    private Button btnAdd;
    private Button btnEdit;
    private Button btnRemove;


    public void init( IWorkbench workbench )
    {
    }


    @Override
    protected Control createContents( Composite parent )
    {
        Control control = initContents( parent );
        loadPreferences();
        return control;
    }


    @Override
    protected IPreferenceStore doGetPreferenceStore()
    {
        return SigilCore.getDefault().getPreferenceStore();
    }


    @Override
    protected void performDefaults()
    {
        super.performDefaults();
    }


    @Override
    public boolean performOk()
    {
        IPreferenceStore prefs = getPreferenceStore();
        for ( String key : prefs.getString( SigilCore.LIBRARY_KEYS_PREF ).split( "," ) )
        {
            prefs.setToDefault( key );
        }

        StringBuffer keys = new StringBuffer();

        for ( ILibrary lib : libraries )
        {
            throw new IllegalStateException( "XXX-FIXME-XXX" );
        }

        prefs.setValue( SigilCore.LIBRARY_KEYS_PREF, keys.toString() );

        return true;
    }


    private Control initContents( Composite parent )
    {
        Composite control = new Composite( parent, SWT.NONE );
        control.setFont( parent.getFont() );

        GridLayout grid = new GridLayout( 3, false );
        control.setLayout( grid );

        initRepositories( control );

        return control;
    }


    private void initRepositories( Composite composite )
    {
        // Create controls
        new Label( composite, SWT.NONE ).setText( "Libraries:" );
        new Label( composite, SWT.NONE ); // Spacer
        table = new Table( composite, SWT.SINGLE | SWT.BORDER );
        //table.setFont(control.getFont());
        btnAdd = new Button( composite, SWT.PUSH );
        btnAdd.setText( "Add..." );
        //add.setFont(control.getFont());
        btnEdit = new Button( composite, SWT.PUSH );
        btnEdit.setText( "Edit..." );
        //edit.setFont(control.getFont());
        btnRemove = new Button( composite, SWT.PUSH );
        btnRemove.setText( "Remove" );
        //remove.setFont(control.getFont());

        // Table Model
        libraries = new TreeSet<ILibrary>( new Comparator<ILibrary>()
        {
            public int compare( ILibrary l1, ILibrary l2 )
            {
                int c = l1.getName().compareTo( l2.getName() );
                if ( c == 0 )
                {
                    c = l1.getVersion().compareTo( l2.getVersion() );
                }
                return c;
            }
        } );
        libraryView = new TableViewer( table );
        libraryView.setLabelProvider( new LabelProvider()
        {
            public String getText( Object element )
            {
                ILibrary rep = ( ILibrary ) element;
                return rep.getName() + " " + rep.getVersion();
            }
        } );
        libraryView.setContentProvider( new DefaultTableProvider()
        {
            public Object[] getElements( Object inputElement )
            {
                return toArray( inputElement );
            }
        } );
        libraryView.setInput( libraries );

        // Initialize controls
        updateButtonStates();

        // Hookup Listeners
        libraryView.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                updateButtonStates();
            }
        } );
        btnAdd.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleAdd();
            }
        } );
        btnEdit.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleEdit();
            }
        } );
        btnRemove.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleRemove();
            }
        } );

        // Layout
        composite.setLayout( new GridLayout( 2, false ) );
        table.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 4 ) );
        GridDataFactory buttonGD = GridDataFactory.swtDefaults().align( SWT.FILL, SWT.CENTER );
        btnAdd.setLayoutData( buttonGD.create() );
        btnEdit.setLayoutData( buttonGD.create() );
        btnRemove.setLayoutData( buttonGD.create() );
    }


    private void updateButtonStates()
    {
        ISelection sel = libraryView.getSelection();
        btnEdit.setEnabled( !sel.isEmpty() );
        btnRemove.setEnabled( !sel.isEmpty() );
    }


    private void handleAdd()
    {
        LibraryConfigurationDialog d = new LibraryConfigurationDialog( getShell() );
        if ( d.open() == Window.OK )
        {
            libraries.add( d.getLibrary() );
            libraryView.refresh();
        }
    }


    private void handleEdit()
    {
        IStructuredSelection sel = ( IStructuredSelection ) libraryView.getSelection();
        boolean change = false;

        for ( @SuppressWarnings("unchecked")
        Iterator<ILibrary> i = sel.iterator(); i.hasNext(); )
        {
            ILibrary lib = i.next();
            LibraryConfigurationDialog d = new LibraryConfigurationDialog( getShell(), lib );
            if ( d.open() == Window.OK )
            {
                libraries.remove( lib );
                libraries.add( d.getLibrary() );
                change = true;
            }
        }

        if ( change )
        {
            libraryView.refresh();
        }
    }


    private void handleRemove()
    {
        IStructuredSelection sel = ( IStructuredSelection ) libraryView.getSelection();
        for ( @SuppressWarnings("unchecked")
        Iterator<ILibrary> i = sel.iterator(); i.hasNext(); )
        {
            libraries.remove( i );
        }
        libraryView.refresh();
    }


    private void loadPreferences()
    {
        IPreferenceStore prefs = getPreferenceStore();
        String keys = prefs.getString( SigilCore.LIBRARY_KEYS_PREF );
        if ( keys.trim().length() > 0 )
        {
            for ( String key : keys.split( "," ) )
            {
                String libStr = prefs.getString( key );
                // XXX-FIXME-XXX parse library string
                // lib = parse(libstr);
                // libraries.add(lib);
            }
            libraryView.refresh();
        }
    }
}
