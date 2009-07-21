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

package org.apache.felix.sigil.ui.eclipse.ui.preferences.project;


import java.util.concurrent.Callable;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTableProvider;
import org.apache.felix.sigil.ui.eclipse.ui.util.ProjectUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;


public class ProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage
{

    private boolean projectSpecific;
    private ComboViewer setView;
    private Composite settings;
    private Button projectSpecificBtn;


    @Override
    protected Control createContents( Composite parent )
    {
        final Composite control = new Composite( parent, SWT.NONE );

        projectSpecificBtn = new Button( control, SWT.CHECK );
        projectSpecificBtn.setText( "Enable project specific settings" );
        projectSpecificBtn.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                setProjectSpecific( !projectSpecific );
            }
        } );

        Label link = new Label( control, SWT.UNDERLINE_SINGLE );
        link.addMouseListener( new MouseAdapter()
        {
            @Override
            public void mouseDown( MouseEvent e )
            {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn( null,
                    SigilCore.REPOSITORIES_PREFERENCES_ID, null, null );
                dialog.open();
            }
        } );

        link.setText( "Configure workspace settings" );

        settings = new Composite( control, SWT.BORDER );
        settings.setLayout( new GridLayout( 1, false ) );
        createSettings( settings );

        setFonts( control );

        // layout
        control.setLayout( new GridLayout( 2, false ) );
        projectSpecificBtn.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        settings.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 2, 1 ) );

        // load settings
        String currentSet = getCurrentSet();

        if ( currentSet == null )
        {
            setProjectSpecific( false );
        }
        else
        {
            setView.setSelection( new StructuredSelection( currentSet ) );
            setProjectSpecific( true );
        }

        return control;
    }


    private void setFonts( Composite control )
    {
        Composite p = control.getParent();
        for ( Control c : control.getChildren() )
        {
            c.setFont( p.getFont() );
            if ( c instanceof Composite )
            {
                setFonts( ( Composite ) c );
            }
        }
    }


    private void setProjectSpecific( boolean projectSpecific )
    {
        if ( this.projectSpecific != projectSpecific )
        {
            this.projectSpecific = projectSpecific;
            settings.setEnabled( projectSpecific );
            for ( Control c : settings.getChildren() )
            {
                c.setEnabled( projectSpecific );
            }
            projectSpecificBtn.setSelection( projectSpecific );
        }
    }


    private void createSettings( Composite parent )
    {
        Composite control = new Composite( parent, SWT.NONE );

        new Label( control, SWT.NONE ).setText( "Repository Set:" );
        Combo combo = new Combo( control, SWT.SINGLE );

        setView = new ComboViewer( combo );
        setView.setContentProvider( new DefaultTableProvider()
        {
            public Object[] getElements( Object inputElement )
            {
                return toArray( inputElement );
            }
        } );

        setView.setInput( SigilCore.getRepositoryConfiguration().loadRepositorySets().keySet() );

        // layout
        control.setLayout( new GridLayout( 2, false ) );
    }


    private String getCurrentSet()
    {
        try
        {
            IProject p = ( IProject ) getElement().getAdapter( IProject.class );
            ISigilProjectModel model = SigilCore.create( p );
            return model.getPreferences().get( SigilCore.REPOSITORY_SET, null );
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Failed to read repository set", e );
            return null;
        }
    }


    @Override
    public boolean okToLeave()
    {
        if ( projectSpecific )
        {
            if ( setView.getSelection().isEmpty() )
            {
                setErrorMessage( "Must select a repository set" );
                return false;
            }
        }
        setErrorMessage( null );
        return true;
    }


    @Override
    public boolean performOk()
    {
        return ProjectUtils.runTaskWithRebuildCheck( new Callable<Boolean>()
        {
            public Boolean call() throws CoreException, BackingStoreException
            {
                String set = null;
                if ( projectSpecific )
                {
                    set = ( String ) ( ( IStructuredSelection ) setView.getSelection() ).getFirstElement();
                }

                IProject p = ( IProject ) getElement().getAdapter( IProject.class );
                ISigilProjectModel model = SigilCore.create( p );
                model.getPreferences().put( SigilCore.REPOSITORY_SET, set );
                model.getPreferences().flush();
                return true;
            }

        }, getShell() );
    }

}
