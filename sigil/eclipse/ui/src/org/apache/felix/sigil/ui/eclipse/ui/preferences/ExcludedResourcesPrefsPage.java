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


import java.util.ArrayList;
import java.util.Iterator;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.preferences.PrefsUtils;
import org.eclipse.core.internal.preferences.PrefsMessages;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class ExcludedResourcesPrefsPage extends PreferencePage implements IWorkbenchPreferencePage
{

    private TableViewer viewer;
    private IWorkbench workbench;
    private ArrayList<String> resources;


    public ExcludedResourcesPrefsPage()
    {
        super();
        setDescription( "Specify resources that should not be offered for inclusion in a generated bundle." );
    }


    @Override
    protected Control createContents( Composite parent )
    {
        // Create controls
        Composite composite = new Composite( parent, SWT.NONE );
        Table table = new Table( composite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION );

        Button btnAdd = new Button( composite, SWT.PUSH );
        btnAdd.setText( "Add" );

        final Button btnRemove = new Button( composite, SWT.PUSH );
        btnRemove.setText( "Remove" );
        btnRemove.setEnabled( false );

        // Create viewer
        viewer = new TableViewer( table );
        viewer.setContentProvider( new ArrayContentProvider() );

        // Load data
        loadPreferences( false );
        viewer.setInput( resources );

        // Listeners
        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                btnRemove.setEnabled( !viewer.getSelection().isEmpty() );
            }
        } );

        btnRemove.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                IStructuredSelection selection = ( IStructuredSelection ) viewer.getSelection();
                Object[] deleted = selection.toArray();
                for ( Object delete : deleted )
                {
                    resources.remove( delete );
                }
                viewer.remove( deleted );
            }
        } );

        btnAdd.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                InputDialog dialog = new InputDialog( getShell(), "Add Resource", "Enter resource name", "",
                    new IInputValidator()
                    {
                        public String isValid( String newText )
                        {
                            String error = null;
                            if ( newText == null || newText.length() == 0 )
                            {
                                error = "Name must not be empty.";
                            }
                            else if ( resources.contains( newText ) )
                            {
                                error = "Specified resource name is already on the list.";
                            }
                            return error;
                        }
                    } );

                if ( dialog.open() == Window.OK )
                {
                    String value = dialog.getValue();
                    resources.add( value );
                    viewer.add( value );
                }
            }
        } );

        // Layout
        composite.setLayout( new GridLayout( 2, false ) );
        table.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 3 ) );
        btnAdd.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );
        btnRemove.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );

        return composite;
    }


    private void loadPreferences( boolean useDefaults )
    {
        String resourcesListStr = useDefaults ? getPreferenceStore().getDefaultString(
            SigilCore.DEFAULT_EXCLUDED_RESOURCES ) : getPreferenceStore().getString(
            SigilCore.DEFAULT_EXCLUDED_RESOURCES );
        String[] resourcesArray = PrefsUtils.stringToArray( resourcesListStr );

        resources = new ArrayList<String>( resourcesArray.length );
        for ( String resource : resourcesArray )
        {
            resources.add( resource );
        }
    }


    public void init( IWorkbench workbench )
    {
        this.workbench = workbench;
    }


    @Override
    protected IPreferenceStore doGetPreferenceStore()
    {
        return SigilCore.getDefault().getPreferenceStore();
    }


    @Override
    public boolean performOk()
    {
        String resourcesStr = PrefsUtils.arrayToString( resources.toArray( new String[resources.size()] ) );
        getPreferenceStore().setValue( SigilCore.DEFAULT_EXCLUDED_RESOURCES, resourcesStr );
        return true;
    }


    @Override
    protected void performDefaults()
    {
        super.performDefaults();
        loadPreferences( true );
        viewer.setInput( resources );
    }

}
