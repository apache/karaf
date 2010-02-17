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

import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ILibrary;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTableProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Version;


public class LibraryConfigurationDialog extends TitleAreaDialog
{

    private static final Comparator<IPackageImport> COMPARATOR = new Comparator<IPackageImport>()
    {
        public int compare( IPackageImport o1, IPackageImport o2 )
        {
            return o1.getPackageName().compareTo( o2.getPackageName() );
        }
    };

    private String name;
    private Version version;
    private TreeSet<IPackageImport> packageImports = new TreeSet<IPackageImport>( COMPARATOR );

    private boolean editOnly;

    private TableViewer viewer;
    private Text txtName;
    private Text txtVersion;


    public LibraryConfigurationDialog( Shell parentShell )
    {
        super( parentShell );
        name = "";
        version = Version.emptyVersion;
    }


    public LibraryConfigurationDialog( Shell parentShell, ILibrary lib )
    {
        super( parentShell );
        editOnly = true;
        name = lib.getName();
        version = lib.getVersion();
        packageImports.addAll( lib.getImports() );
    }


    @Override
    protected Control createDialogArea( Composite par )
    {
        setTitle( "Add Library" );
        Composite container = ( Composite ) super.createDialogArea( par );

        Composite topPanel = new Composite( container, SWT.NONE );

        new Label( topPanel, SWT.NONE ).setText( "Name" );

        txtName = new Text( topPanel, SWT.BORDER );
        txtName.setEditable( !editOnly );
        if ( name != null )
            txtName.setText( name );

        new Label( topPanel, SWT.NONE ).setText( "Version" );

        txtVersion = new Text( topPanel, SWT.BORDER );
        txtVersion.setText( version.toString() );
        txtVersion.setEditable( !editOnly );

        Composite bottomPanel = new Composite( container, SWT.NONE );

        Table table = new Table( bottomPanel, SWT.BORDER );
        table.setSize( new Point( 300, 200 ) );

        Button add = new Button( bottomPanel, SWT.PUSH );
        add.setText( "Add..." );

        final Button edit = new Button( bottomPanel, SWT.PUSH );
        edit.setText( "Edit..." );
        edit.setEnabled( false );

        final Button remove = new Button( bottomPanel, SWT.PUSH );
        remove.setText( "Remove" );
        remove.setEnabled( false );

        updateState();

        // Hookup Listeners
        txtName.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                updateState();
            }
        } );
        txtVersion.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                updateState();
            }
        } );
        add.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleAdd();
            }
        } );
        edit.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleEdit();
            }
        } );
        remove.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleRemove();
            }
        } );

        // Layout
        topPanel.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        topPanel.setLayout( new GridLayout( 2, false ) );
        txtName.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        txtVersion.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );

        bottomPanel.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        bottomPanel.setLayout( new GridLayout( 2, false ) );
        table.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 4 ) );
        add.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );
        edit.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );
        remove.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );

        // Table Viewer
        viewer = new TableViewer( table );
        viewer.setLabelProvider( new LabelProvider()
        {
            @Override
            public String getText( Object element )
            {
                IPackageImport pi = ( IPackageImport ) element;
                return pi.getPackageName() + " " + pi.getVersions();
            }
        } );
        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                edit.setEnabled( !event.getSelection().isEmpty() );
                remove.setEnabled( !event.getSelection().isEmpty() );
            }
        } );
        viewer.setContentProvider( new DefaultTableProvider()
        {
            public Object[] getElements( Object inputElement )
            {
                return toArray( inputElement );
            }
        } );

        viewer.setInput( packageImports );
        return container;
    }


    private void updateState()
    {
        String error = null;
        String warning = null;

        name = txtName.getText();

        try
        {
            version = VersionTable.getVersion( txtVersion.getText() );
            if ( version.getQualifier().indexOf( '_' ) > -1 )
            {
                warning = "The use of underscores in a version qualifier is discouraged.";
            }
        }
        catch ( IllegalArgumentException e )
        {
            version = null;
            error = "Invalid version format";
        }

        Button okButton = getButton( IDialogConstants.OK_ID );
        if ( okButton != null && !okButton.isDisposed() )
            okButton.setEnabled( allowOkay() );

        setErrorMessage( error );
        setMessage( warning, IMessageProvider.WARNING );
    }


    private boolean allowOkay()
    {
        return name != null && name.length() > 0 && version != null;
    }


    @Override
    protected Button createButton( Composite parent, int id, String label, boolean defaultButton )
    {
        Button button = super.createButton( parent, id, label, defaultButton );
        if ( id == IDialogConstants.OK_ID )
        {
            button.setEnabled( allowOkay() );
        }
        return button;
    }


    private void handleAdd()
    {
        /*NewResourceSelectionDialog<? extends IPackageModelElement> dialog = ResourcesDialogHelper.createImportDialog(getShell(), "Add Imported Package", null, packageImports);
        if ( dialog.open() == Window.OK ) {
        	IPackageImport pi = ModelElementFactory.getInstance().newModelElement(IPackageImport.class);
        	pi.setPackageName(dialog.getSelectedName());
        	pi.setVersions(dialog.getSelectedVersions());
        	pi.setOptional(dialog.isOptional());
        	
        	packageImports.add(pi);
        	viewer.refresh();
        }*/
    }


    private void handleEdit()
    {
        /*IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        
        boolean changed = false;
        
        if ( !selection.isEmpty() ) {
        	for ( Iterator<IPackageImport> i = selection.iterator(); i.hasNext(); ) {	
        		IPackageImport packageImport = i.next();
        		NewResourceSelectionDialog<? extends IPackageModelElement> dialog = ResourcesDialogHelper.createImportDialog( getShell(), "Edit Imported Package", packageImport, packageImports );
        		dialog.setVersions( packageImport.getVersions() );
        		dialog.setOptional(packageImport.isOptional());
        		if ( dialog.open() == Window.OK ) {
        			changed = true;
        			String packageName = dialog.getSelectedName();
        			VersionRange versionRange = dialog.getSelectedVersions();
        			
        			IPackageImport newImport = ModelElementFactory.getInstance().newModelElement(IPackageImport.class);
        			newImport.setPackageName(packageName);
        			newImport.setVersions(versionRange);
        			newImport.setOptional(dialog.isOptional());
        			
        			packageImports.remove(packageImport);
        			packageImports.add(newImport);
        		}
        	}
        }
        
        if ( changed ) {
        	viewer.refresh();
        } */
    }


    private void handleRemove()
    {
        IStructuredSelection selection = ( IStructuredSelection ) viewer.getSelection();

        if ( !selection.isEmpty() )
        {
            for ( Iterator<IPackageImport> i = selection.iterator(); i.hasNext(); )
            {
                packageImports.remove( i.next() );
            }

            viewer.refresh();
        }
    }


    public ILibrary getLibrary()
    {
        ILibrary library = ModelElementFactory.getInstance().newModelElement( ILibrary.class );

        library.setName( name );
        library.setVersion( version );

        for ( IPackageImport pi : packageImports )
        {
            library.addImport( pi );
        }

        return library;
    }

}
