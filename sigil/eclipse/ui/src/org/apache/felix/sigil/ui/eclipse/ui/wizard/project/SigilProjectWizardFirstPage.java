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

package org.apache.felix.sigil.ui.eclipse.ui.wizard.project;


import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.ui.eclipse.ui.views.RepositoryViewPart;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.osgi.framework.Version;


/**
 * @author dave
 *
 */
public class SigilProjectWizardFirstPage extends WizardNewProjectCreationPage
{

    private volatile String description = "";
    private volatile Version version = VersionTable.getVersion( 1, 0, 0 );
    private volatile String vendor = "";
    private volatile String name = "";

    private Text txtDescription;
    private Text txtVersion;
    private Text txtVendor;
    private Text txtName;


    public SigilProjectWizardFirstPage()
    {
        super( "newSigilProjectPage" );
        setTitle( "Sigil Project" );
        setDescription( "Create a new Sigil project" );
        setImageDescriptor( ImageDescriptor.createFromFile( RepositoryViewPart.class, "/icons/logo64x64.gif" ) );
    }


    public boolean isInWorkspace()
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        IPath defaultDefaultLocation = workspace.getRoot().getLocation();

        return defaultDefaultLocation.isPrefixOf( getLocationPath() );
    }


    @Override
    public boolean isPageComplete()
    {
        boolean result = super.isPageComplete();
        return result;
    }


    @Override
    public void createControl( Composite parent )
    {
        FieldDecoration infoDecor = FieldDecorationRegistry.getDefault().getFieldDecoration(
            FieldDecorationRegistry.DEC_INFORMATION );

        // Create controls
        super.createControl( parent );
        Composite control = ( Composite ) getControl();

        Group grpProjectSettings = new Group( control, SWT.NONE );
        grpProjectSettings.setText( "Project Settings" );

        new Label( grpProjectSettings, SWT.NONE ).setText( "Version:" );
        txtVersion = new Text( grpProjectSettings, SWT.BORDER );

        new Label( grpProjectSettings, SWT.NONE ).setText( "Name:" );
        txtName = new Text( grpProjectSettings, SWT.BORDER );

        ControlDecoration txtNameDecor = new ControlDecoration( txtName, SWT.LEFT | SWT.CENTER );
        txtNameDecor.setImage( infoDecor.getImage() );
        txtNameDecor.setDescriptionText( "Defines a human-readable name for the bundle" );

        new Label( grpProjectSettings, SWT.NONE ).setText( "Description:" );
        txtDescription = new Text( grpProjectSettings, SWT.BORDER );

        ControlDecoration txtDescDecor = new ControlDecoration( txtDescription, SWT.LEFT | SWT.CENTER );
        txtDescDecor.setImage( infoDecor.getImage() );
        txtDescDecor.setDescriptionText( "Defines a short human-readable description for the bundle" );

        new Label( grpProjectSettings, SWT.NONE ).setText( "Provider:" );
        txtVendor = new Text( grpProjectSettings, SWT.BORDER );

        ControlDecoration txtVendorDecor = new ControlDecoration( txtVendor, SWT.LEFT | SWT.CENTER );
        txtVendorDecor.setImage( infoDecor.getImage() );
        txtVendorDecor.setDescriptionText( "The name of the company, organisation or individual providing the bundle" );

        // Set values
        txtDescription.setText( description );
        txtVersion.setText( version.toString() );
        txtVendor.setText( vendor );
        txtName.setText( name );

        // Add listeners
        ModifyListener txtModListener = new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                description = txtDescription.getText();
                vendor = txtVendor.getText();
                name = txtName.getText();

                validateSettings();
            }
        };
        txtDescription.addModifyListener( txtModListener );
        txtVersion.addModifyListener( txtModListener );
        txtVendor.addModifyListener( txtModListener );
        txtName.addModifyListener( txtModListener );

        // Layout
        control.setLayout( new GridLayout() );
        grpProjectSettings.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        grpProjectSettings.setLayout( new GridLayout( 2, false ) );
        txtDescription.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        txtVersion.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        txtVendor.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        txtName.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
    }


    private void validateSettings()
    {
        try
        {
            version = VersionTable.getVersion( txtVersion.getText() );
        }
        catch ( IllegalArgumentException e )
        {
            version = null;
            setErrorMessage( "Invalid version" );
            setPageComplete( false );
            return;
        }

        setErrorMessage( null );
        setPageComplete( true );
    }


    public Version getVersion()
    {
        return version;
    }


    public String getVendor()
    {
        return vendor;
    }


    public String getDescription()
    {
        return description;
    }


    public String getName()
    {
        return name;
    }
}
