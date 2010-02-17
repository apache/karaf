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

package org.apache.felix.sigil.ui.eclipse.ui.editors.project;


import java.util.Comparator;

import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.ui.eclipse.ui.util.BackgroundLoadingSelectionDialog;
import org.apache.felix.sigil.ui.eclipse.ui.util.IElementDescriptor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Version;


public class NewPackageExportDialog extends BackgroundLoadingSelectionDialog<IPackageFragment>
{

    private static final IElementDescriptor<IPackageFragment> PKG_FRAGMENT_STRINGIFIER = new IElementDescriptor<IPackageFragment>()
    {
        public String getLabel( IPackageFragment element )
        {
            return getName( element );
        }


        public String getName( IPackageFragment element )
        {
            return element.getElementName();
        }
    };

    private static final Comparator<IPackageFragment> PKG_FRAGMENT_COMPARATOR = new Comparator<IPackageFragment>()
    {
        public int compare( IPackageFragment o1, IPackageFragment o2 )
        {
            return o1.getElementName().compareTo( o2.getElementName() );
        }
    };

    private Version version = null;
    private String error = null;
    private Version projectVersion = VersionTable.getVersion( 0, 0, 0 );

    private Button btnInheritBundleVersion;
    private Button btnExplicitVersion;
    private Text txtVersion;


    public NewPackageExportDialog( Shell parentShell, boolean multiSelect )
    {
        super( parentShell, "Package:", multiSelect );
        setDescriptor( PKG_FRAGMENT_STRINGIFIER );
        setComparator( PKG_FRAGMENT_COMPARATOR );
    }


    @Override
    protected Control createDialogArea( Composite parent )
    {
        // Create controls
        Composite container = ( Composite ) super.createDialogArea( parent );
        Composite composite = new Composite( container, SWT.NONE );

        Group grpVersion = new Group( composite, SWT.NONE );
        grpVersion.setText( "Version" );

        btnInheritBundleVersion = new Button( grpVersion, SWT.RADIO );
        btnInheritBundleVersion.setText( "Inherit bundle version" );
        new Label( grpVersion, SWT.NONE ); // Spacer
        btnExplicitVersion = new Button( grpVersion, SWT.RADIO );
        btnExplicitVersion.setText( "Fixed version:" );
        txtVersion = new Text( grpVersion, SWT.BORDER );

        // Initialize
        if ( version == null )
        {
            btnInheritBundleVersion.setSelection( true );
            txtVersion.setEnabled( false );
            txtVersion.setText( projectVersion.toString() );
        }
        else
        {
            btnExplicitVersion.setSelection( true );
            txtVersion.setEnabled( true );
            txtVersion.setText( version.toString() );
        }
        updateButtons();

        // Listeners
        Listener radioAndTextListener = new Listener()
        {
            public void handleEvent( Event event )
            {
                error = null;
                if ( btnInheritBundleVersion.getSelection() )
                {
                    version = null;
                    txtVersion.setEnabled( false );
                }
                else
                {
                    txtVersion.setEnabled( true );
                    try
                    {
                        version = VersionTable.getVersion( txtVersion.getText() );
                    }
                    catch ( IllegalArgumentException e )
                    {
                        error = "Invalid version";
                    }
                }
                setErrorMessage( error );
                updateButtons();
            }
        };
        txtVersion.addListener( SWT.Modify, radioAndTextListener );
        btnInheritBundleVersion.addListener( SWT.Selection, radioAndTextListener );
        btnExplicitVersion.addListener( SWT.Selection, radioAndTextListener );

        // Layout
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        composite.setLayout( new GridLayout( 1, false ) );
        grpVersion.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        grpVersion.setLayout( new GridLayout( 2, false ) );
        txtVersion.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );

        return container;
    }


    @Override
    protected boolean canComplete()
    {
        return super.canComplete() && error == null;
    }


    public void setProjectVersion( Version projectVersion )
    {
        this.projectVersion = projectVersion;
    }


    public void setVersion( Version version )
    {
        this.version = version;
    }


    public Version getVersion()
    {
        return version;
    }
}
