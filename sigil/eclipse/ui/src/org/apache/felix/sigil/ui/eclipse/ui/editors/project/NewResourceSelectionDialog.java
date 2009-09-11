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


import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionRangeBoundingRule;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.model.osgi.IVersionedModelElement;
import org.apache.felix.sigil.ui.eclipse.ui.util.BackgroundLoadingSelectionDialog;
import org.apache.felix.sigil.ui.eclipse.ui.util.IValidationListener;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Version;


public class NewResourceSelectionDialog<E extends IVersionedModelElement> extends BackgroundLoadingSelectionDialog<E>
{

    private VersionRangeComponent pnlVersionRange;
    private boolean optionalEnabled = true;
    private Button btnOptional;

    private VersionRange selectedVersions = null;
    private boolean optional = false;


    public NewResourceSelectionDialog( Shell parentShell, String selectionLabel, boolean multi )
    {
        super( parentShell, selectionLabel, multi );
    }


    public void setOptionalEnabled( boolean enabled )
    {
        optionalEnabled = enabled;
    }


    @Override
    protected Control createDialogArea( Composite parent )
    {
        // Create controls
        Composite container = ( Composite ) super.createDialogArea( parent );
        Composite composite = new Composite( container, SWT.NONE );

        if ( optionalEnabled )
        {
            new Label( composite, SWT.NONE ); //Spacer
            btnOptional = new Button( composite, SWT.CHECK );
            btnOptional.setText( "Optional" );
        }

        Label lblVersionRange = new Label( composite, SWT.NONE );
        lblVersionRange.setText( "Version Range:" );
        Group group = new Group( composite, SWT.BORDER );

        pnlVersionRange = new VersionRangeComponent( group, SWT.NONE );

        // Initialize
        if ( selectedVersions != null )
        {
            pnlVersionRange.setVersions( selectedVersions );
        }

        if ( optionalEnabled )
        {
            btnOptional.setSelection( optional );
            updateButtons();
        }

        // Hookup Listeners
        pnlVersionRange.addVersionChangeListener( new VersionsChangeListener()
        {
            public void versionsChanged( VersionRange range )
            {
                selectedVersions = range;
                updateButtons();
            }
        } );
        pnlVersionRange.addValidationListener( new IValidationListener()
        {
            public void validationMessage( String message, int level )
            {
                setMessage( message, level );
                updateButtons();
            }
        } );

        if ( optionalEnabled )
        {
            btnOptional.addSelectionListener( new SelectionAdapter()
            {
                public void widgetSelected( SelectionEvent e )
                {
                    optional = btnOptional.getSelection();
                }
            } );
        }

        // Layout
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        GridLayout layout = new GridLayout( 2, false );
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 10;
        composite.setLayout( layout );

        lblVersionRange.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false ) );
        group.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        group.setLayout( new FillLayout() );

        return container;
    }


    @Override
    protected void elementSelected( E selection )
    {
        if ( selection != null )
        {
            IPreferenceStore store = SigilCore.getDefault().getPreferenceStore();
            VersionRangeBoundingRule lowerBoundRule = VersionRangeBoundingRule.valueOf( store
                .getString( SigilCore.DEFAULT_VERSION_LOWER_BOUND ) );
            VersionRangeBoundingRule upperBoundRule = VersionRangeBoundingRule.valueOf( store
                .getString( SigilCore.DEFAULT_VERSION_UPPER_BOUND ) );

            Version version = selection.getVersion();
            selectedVersions = VersionRange.newInstance( version, lowerBoundRule, upperBoundRule );
            pnlVersionRange.setVersions( selectedVersions );
        }
    }


    @Override
    protected synchronized boolean canComplete()
    {
        return super.canComplete() && selectedVersions != null;
    }


    public VersionRange getSelectedVersions()
    {
        return selectedVersions;
    }


    public void setVersions( VersionRange versions )
    {
        selectedVersions = versions;
        if ( pnlVersionRange != null && !pnlVersionRange.isDisposed() )
        {
            pnlVersionRange.setVersions( versions );
            updateButtons();
        }
    }


    public boolean isOptional()
    {
        return optional;
    }


    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }
}
