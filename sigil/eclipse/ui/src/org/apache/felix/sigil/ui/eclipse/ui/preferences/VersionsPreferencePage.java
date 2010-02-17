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


import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionRangeBoundingRule;
import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Version;


public class VersionsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{

    private static final Version SAMPLE_VERSION = VersionTable.getVersion( 1, 2, 3, "qualifier" );

    private IWorkbench workbench;

    private Button btnLowerBoundExact;
    private Button btnLowerBoundMicro;
    private Button btnLowerBoundMinor;
    private Button btnLowerBoundMajor;
    private Button btnLowerBoundAny;

    private Button btnUpperBoundExact;
    private Button btnUpperBoundMicro;
    private Button btnUpperBoundMinor;
    private Button btnUpperBoundMajor;
    private Button btnUpperBoundAny;

    private Text txtSampleVersion;
    private Label lblCalculatedRange;
    private Text txtMatchVersion;
    private Label lblMatchResult;

    private VersionRangeBoundingRule lowerBoundRule;
    private VersionRangeBoundingRule upperBoundRule;

    private Version sampleVersion = SAMPLE_VERSION;
    private Version matchVersion = null;


    public VersionsPreferencePage()
    {
        super();
        setDescription( "Specify the Lower and Upper bounds for a default version range calculated from a point version, e.g. \"1.2.3.qualifier\"" );
    }


    @Override
    protected Control createContents( Composite parent )
    {
        // Create controls
        Composite composite = new Composite( parent, SWT.NONE );

        Group grpLowerBound = new Group( composite, SWT.NONE );
        grpLowerBound.setText( "Lower Bound" );
        btnLowerBoundExact = new Button( grpLowerBound, SWT.RADIO );
        btnLowerBoundExact.setText( "Exact e.g. [1.2.3.qualifer, ...)" );
        btnLowerBoundMicro = new Button( grpLowerBound, SWT.RADIO );
        btnLowerBoundMicro.setText( "Micro e.g. [1.2.3, ...)" );
        btnLowerBoundMinor = new Button( grpLowerBound, SWT.RADIO );
        btnLowerBoundMinor.setText( "Minor e.g. [1.2, ...)" );
        btnLowerBoundMajor = new Button( grpLowerBound, SWT.RADIO );
        btnLowerBoundMajor.setText( "Major e.g. [1, ...)" );
        btnLowerBoundAny = new Button( grpLowerBound, SWT.RADIO );
        btnLowerBoundAny.setText( "Any e.g. [0, ...)" );

        Group grpUpperBound = new Group( composite, SWT.NONE );
        grpUpperBound.setText( "Upper Bound" );

        btnUpperBoundExact = new Button( grpUpperBound, SWT.RADIO );
        btnUpperBoundExact.setText( "Exact e.g. [..., 1.2.3.qualifer]" );
        btnUpperBoundMicro = new Button( grpUpperBound, SWT.RADIO );
        btnUpperBoundMicro.setText( "Micro e.g. [..., 1.2.4)" );
        btnUpperBoundMinor = new Button( grpUpperBound, SWT.RADIO );
        btnUpperBoundMinor.setText( "Minor e.g. [..., 1.3)" );
        btnUpperBoundMajor = new Button( grpUpperBound, SWT.RADIO );
        btnUpperBoundMajor.setText( "Major e.g. [..., 2)" );
        btnUpperBoundAny = new Button( grpUpperBound, SWT.RADIO );
        btnUpperBoundAny.setText( "Any e.g. [..., \u221e)" );

        Group grpRangeTest = new Group( composite, SWT.NONE );
        grpRangeTest.setText( "Range Test" );
        new Label( grpRangeTest, SWT.NONE ).setText( "Sample Input Version: " );
        txtSampleVersion = new Text( grpRangeTest, SWT.BORDER );
        new Label( grpRangeTest, SWT.NONE ).setText( "Calculated Version Range: " );
        lblCalculatedRange = new Label( grpRangeTest, SWT.NONE );

        new Label( grpRangeTest, SWT.NONE ).setText( "Test: " );
        txtMatchVersion = new Text( grpRangeTest, SWT.BORDER );
        new Label( grpRangeTest, SWT.NONE ).setText( "Result: " );
        lblMatchResult = new Label( grpRangeTest, SWT.NONE );

        // Initialize controls
        loadPreferences( false );
        updateRadioButtons();

        txtSampleVersion.setText( sampleVersion.toString() );
        updateCalculatedRange();

        // Add listeners
        SelectionListener buttonListener = new SelectionListener()
        {
            public void widgetSelected( SelectionEvent e )
            {
                readRadioButtons();
                updateCalculatedRange();
            }


            public void widgetDefaultSelected( SelectionEvent e )
            {
                readRadioButtons();
                updateCalculatedRange();
            }
        };
        btnLowerBoundAny.addSelectionListener( buttonListener );
        btnLowerBoundMajor.addSelectionListener( buttonListener );
        btnLowerBoundMinor.addSelectionListener( buttonListener );
        btnLowerBoundMicro.addSelectionListener( buttonListener );
        btnLowerBoundExact.addSelectionListener( buttonListener );

        btnUpperBoundAny.addSelectionListener( buttonListener );
        btnUpperBoundMajor.addSelectionListener( buttonListener );
        btnUpperBoundMinor.addSelectionListener( buttonListener );
        btnUpperBoundMicro.addSelectionListener( buttonListener );
        btnUpperBoundExact.addSelectionListener( buttonListener );

        txtSampleVersion.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                try
                {
                    sampleVersion = VersionTable.getVersion( txtSampleVersion.getText() );
                }
                catch ( IllegalArgumentException x )
                {
                    sampleVersion = null;
                }
                updateCalculatedRange();
            }
        } );
        txtMatchVersion.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                try
                {
                    matchVersion = VersionTable.getVersion( txtMatchVersion.getText() );
                }
                catch ( IllegalArgumentException x )
                {
                    matchVersion = null;
                }
                updateCalculatedRange();
            }
        } );

        // Layout
        GridLayout layout = new GridLayout( 1, false );
        layout.verticalSpacing = 20;
        composite.setLayout( layout );

        grpLowerBound.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        grpLowerBound.setLayout( new GridLayout( 1, false ) );

        grpUpperBound.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        grpUpperBound.setLayout( new GridLayout( 1, false ) );

        grpRangeTest.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, true ) );
        grpRangeTest.setLayout( new GridLayout( 2, false ) );

        txtSampleVersion.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        lblCalculatedRange.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        txtMatchVersion.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        lblMatchResult.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );

        return composite;
    }


    private void loadPreferences( boolean useDefaults )
    {
        IPreferenceStore prefs = getPreferenceStore();
        String lowerBoundStr;
        if ( useDefaults )
        {
            lowerBoundStr = prefs.getDefaultString( SigilCore.DEFAULT_VERSION_LOWER_BOUND );
        }
        else
        {
            lowerBoundStr = prefs.getString( SigilCore.DEFAULT_VERSION_LOWER_BOUND );
        }

        String upperBoundStr;
        if ( useDefaults )
        {
            upperBoundStr = prefs.getDefaultString( SigilCore.DEFAULT_VERSION_UPPER_BOUND );
        }
        else
        {
            upperBoundStr = prefs.getString( SigilCore.DEFAULT_VERSION_UPPER_BOUND );
        }

        lowerBoundRule = VersionRangeBoundingRule.valueOf( lowerBoundStr );
        upperBoundRule = VersionRangeBoundingRule.valueOf( upperBoundStr );
    }


    private void updateRadioButtons()
    {
        switch ( lowerBoundRule )
        {
            case Exact:
                btnLowerBoundExact.setSelection( true );
                btnLowerBoundMicro.setSelection( false );
                btnLowerBoundMinor.setSelection( false );
                btnLowerBoundMajor.setSelection( false );
                btnLowerBoundAny.setSelection( false );
                break;
            case Micro:
                btnLowerBoundExact.setSelection( false );
                btnLowerBoundMicro.setSelection( true );
                btnLowerBoundMinor.setSelection( false );
                btnLowerBoundMajor.setSelection( false );
                btnLowerBoundAny.setSelection( false );
                break;
            case Minor:
                btnLowerBoundExact.setSelection( false );
                btnLowerBoundMicro.setSelection( false );
                btnLowerBoundMinor.setSelection( true );
                btnLowerBoundMajor.setSelection( false );
                btnLowerBoundAny.setSelection( false );
                break;
            case Major:
                btnLowerBoundExact.setSelection( false );
                btnLowerBoundMicro.setSelection( false );
                btnLowerBoundMinor.setSelection( false );
                btnLowerBoundMajor.setSelection( true );
                btnLowerBoundAny.setSelection( false );
                break;
            case Any:
                btnLowerBoundExact.setSelection( false );
                btnLowerBoundMicro.setSelection( false );
                btnLowerBoundMinor.setSelection( false );
                btnLowerBoundMajor.setSelection( false );
                btnLowerBoundAny.setSelection( true );
                break;
        }

        switch ( upperBoundRule )
        {
            case Exact:
                btnUpperBoundExact.setSelection( true );
                btnUpperBoundMicro.setSelection( false );
                btnUpperBoundMinor.setSelection( false );
                btnUpperBoundMajor.setSelection( false );
                btnUpperBoundAny.setSelection( false );
                break;
            case Micro:
                btnUpperBoundExact.setSelection( false );
                btnUpperBoundMicro.setSelection( true );
                btnUpperBoundMinor.setSelection( false );
                btnUpperBoundMajor.setSelection( false );
                btnUpperBoundAny.setSelection( false );
                break;
            case Minor:
                btnUpperBoundExact.setSelection( false );
                btnUpperBoundMicro.setSelection( false );
                btnUpperBoundMinor.setSelection( true );
                btnUpperBoundMajor.setSelection( false );
                btnUpperBoundAny.setSelection( false );
                break;
            case Major:
                btnUpperBoundExact.setSelection( false );
                btnUpperBoundMicro.setSelection( false );
                btnUpperBoundMinor.setSelection( false );
                btnUpperBoundMajor.setSelection( true );
                btnUpperBoundAny.setSelection( false );
                break;
            case Any:
                btnUpperBoundExact.setSelection( false );
                btnUpperBoundMicro.setSelection( false );
                btnUpperBoundMinor.setSelection( false );
                btnUpperBoundMajor.setSelection( false );
                btnUpperBoundAny.setSelection( true );
        }
    }


    private void readRadioButtons()
    {
        if ( btnLowerBoundExact.getSelection() )
        {
            lowerBoundRule = VersionRangeBoundingRule.Exact;
        }
        else if ( btnLowerBoundMicro.getSelection() )
        {
            lowerBoundRule = VersionRangeBoundingRule.Micro;
        }
        else if ( btnLowerBoundMinor.getSelection() )
        {
            lowerBoundRule = VersionRangeBoundingRule.Minor;
        }
        else if ( btnLowerBoundMajor.getSelection() )
        {
            lowerBoundRule = VersionRangeBoundingRule.Major;
        }
        else if ( btnLowerBoundAny.getSelection() )
        {
            lowerBoundRule = VersionRangeBoundingRule.Any;
        }

        if ( btnUpperBoundExact.getSelection() )
        {
            upperBoundRule = VersionRangeBoundingRule.Exact;
        }
        else if ( btnUpperBoundMicro.getSelection() )
        {
            upperBoundRule = VersionRangeBoundingRule.Micro;
        }
        else if ( btnUpperBoundMinor.getSelection() )
        {
            upperBoundRule = VersionRangeBoundingRule.Minor;
        }
        else if ( btnUpperBoundMajor.getSelection() )
        {
            upperBoundRule = VersionRangeBoundingRule.Major;
        }
        else if ( btnUpperBoundAny.getSelection() )
        {
            upperBoundRule = VersionRangeBoundingRule.Any;
        }
    }


    private void updateCalculatedRange()
    {
        VersionRange range;
        String rangeStr;
        String matchResult;

        if ( sampleVersion == null )
        {
            range = null;
            rangeStr = "";
        }
        else
        {
            range = VersionRange.newInstance( sampleVersion, lowerBoundRule, upperBoundRule );
            rangeStr = range.toString();
        }
        lblCalculatedRange.setText( rangeStr );

        if ( matchVersion == null || range == null )
        {
            matchResult = "";
        }
        else
        {
            matchResult = range.contains( matchVersion ) ? "MATCH!" : "No Match";
        }
        lblMatchResult.setText( matchResult );
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
        getPreferenceStore().setValue( SigilCore.DEFAULT_VERSION_LOWER_BOUND, lowerBoundRule.name() );
        getPreferenceStore().setValue( SigilCore.DEFAULT_VERSION_UPPER_BOUND, upperBoundRule.name() );

        return true;
    }


    @Override
    protected void performDefaults()
    {
        super.performDefaults();
        loadPreferences( true );
        updateRadioButtons();
        updateCalculatedRange();
    }

}
