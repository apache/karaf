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


import java.util.HashSet;
import java.util.Set;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.ui.eclipse.ui.util.IValidationListener;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Version;


public class VersionRangeComponent extends Composite
{
    private VersionRange versions = VersionRange.ANY_VERSION;

    private Button specificButton;

    private Text specificText;
    private Button rangeButton;
    private Text minimumText;
    private Text maximumText;
    private Button minInclusiveButton;
    private Button maxInclusiveButton;

    private Set<VersionsChangeListener> listeners = new HashSet<VersionsChangeListener>();
    private Set<IValidationListener> validationListeners = new HashSet<IValidationListener>();


    public VersionRangeComponent( Composite parent, int style )
    {
        super( parent, style );
        createComponents( this );
    }


    public void addVersionChangeListener( VersionsChangeListener listener )
    {
        synchronized ( listeners )
        {
            listeners.add( listener );
        }
    }


    public void removeVersionChangeListener( VersionsChangeListener listener )
    {
        synchronized ( listeners )
        {
            listeners.remove( listener );
        }
    }


    public void addValidationListener( IValidationListener listener )
    {
        validationListeners.add( listener );
    }


    public void removeValidationListener( IValidationListener listener )
    {
        validationListeners.remove( listener );
    }


    @Override
    public void setEnabled( boolean enabled )
    {
        super.setEnabled( enabled );
        specificButton.setEnabled( enabled );
        rangeButton.setEnabled( enabled );
        if ( enabled )
        {
            specificButton.setSelection( versions.isPointVersion() );
            setSpecific();
        }
        else
        {
            minimumText.setEnabled( enabled );
            maximumText.setEnabled( enabled );
            minInclusiveButton.setEnabled( enabled );
            maxInclusiveButton.setEnabled( enabled );
        }
    }


    public VersionRange getVersions()
    {
        return versions;
    }


    public void setVersions( VersionRange versions )
    {
        this.versions = versions == null ? VersionRange.ANY_VERSION : versions;
        updateFields();
    }


    private void updateFields()
    {
        if ( versions.isPointVersion() )
        {
            specificButton.setSelection( true );
            specificText.setText( versions.getCeiling() == VersionRange.INFINITE_VERSION ? "*" : versions.getFloor()
                .toString() );
        }
        else
        {
            rangeButton.setSelection( true );
            minimumText.setText( versions.getFloor().toString() );
            minInclusiveButton.setSelection( !versions.isOpenFloor() );
            maximumText.setText( versions.getCeiling() == VersionRange.INFINITE_VERSION ? "*" : versions.getCeiling()
                .toString() );
            maxInclusiveButton.setSelection( !versions.isOpenCeiling() );
        }

        setSpecific();
    }


    private void createComponents( Composite body )
    {
        setLayout( new GridLayout( 3, false ) );

        specificButton = new Button( body, SWT.RADIO );
        specificButton.setText( "Specific:" );
        specificButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                setSpecific();
            }
        } );

        new Label( body, SWT.NONE ).setText( "Version:" );

        specificText = new Text( body, SWT.BORDER );
        specificText.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyReleased( KeyEvent e )
            {
                setVersions();
            }
        } );

        rangeButton = new Button( body, SWT.RADIO );
        rangeButton.setText( "Range:" );

        new Label( body, SWT.NONE ).setText( "Minimum:" );

        minimumText = new Text( body, SWT.BORDER );
        minimumText.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyReleased( KeyEvent e )
            {
                setVersions();
            }
        } );

        minInclusiveButton = new Button( body, SWT.CHECK );
        minInclusiveButton.setText( "inclusive" );
        minInclusiveButton.setSelection( true );
        minInclusiveButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                setVersions();
            }
        } );

        new Label( body, SWT.NONE ).setText( "Maximum:" );
        maximumText = new Text( body, SWT.BORDER );
        maximumText.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyReleased( KeyEvent e )
            {
                setVersions();
            }
        } );

        maxInclusiveButton = new Button( body, SWT.CHECK );
        maxInclusiveButton.setText( "inclusive" );
        maxInclusiveButton.setSelection( false );
        maxInclusiveButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                setVersions();
            }
        } );

        // Layout
        specificButton.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false, 3, 1 ) );
        specificText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false, 2, 1 ) );
        rangeButton.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false, 3, 1 ) );
        minimumText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        maximumText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );

        updateFields();
    }


    private void setVersions()
    {
        try
        {
            if ( specificButton.getSelection() )
            {
                if ( "*".equals( specificText.getText() ) )
                {
                    versions = VersionRange.ANY_VERSION;
                }
                else if ( specificText.getText().trim().length() == 0 )
                {
                    versions = null;
                }
                else
                {
                    Version v = VersionTable.getVersion( specificText.getText().trim() );
                    versions = new VersionRange( false, v, v, false );
                }
            }
            else
            {
                Version min = VersionTable.getVersion( minimumText.getText() );
                Version max = "*".equals( maximumText.getText() ) ? VersionRange.INFINITE_VERSION : VersionTable.getVersion( maximumText.getText() );
                versions = new VersionRange( !minInclusiveButton.getSelection(), min, max, !maxInclusiveButton
                    .getSelection() );
            }
            fireValidationMessage( null, IMessageProvider.NONE );
        }
        catch ( IllegalArgumentException e )
        {
            versions = null;
            fireValidationMessage( "Invalid version", IMessageProvider.ERROR );
        }

        fireVersionChange();
    }


    private void fireVersionChange()
    {
        synchronized ( listeners )
        {
            for ( VersionsChangeListener l : listeners )
            {
                l.versionsChanged( versions );
            }
        }
    }


    private void fireValidationMessage( String message, int level )
    {
        for ( IValidationListener validationListener : validationListeners )
        {
            validationListener.validationMessage( message, level );
        }
    }


    private void setSpecific()
    {
        boolean specific = specificButton.getSelection();
        specificButton.setSelection( specific );
        specificText.setEnabled( specific );
        minimumText.setEnabled( !specific );
        maximumText.setEnabled( !specific );
        minInclusiveButton.setEnabled( !specific );
        maxInclusiveButton.setEnabled( !specific );
        setVersions();
    }
}
