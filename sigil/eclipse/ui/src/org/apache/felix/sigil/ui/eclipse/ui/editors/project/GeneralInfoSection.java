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


import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.ui.eclipse.ui.form.IFormValueConverter;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilFormEntry;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilFormEntryAdapter;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilPage;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilSection;
import org.apache.felix.sigil.ui.eclipse.ui.util.BackgroundLoadingSelectionDialog;
import org.apache.felix.sigil.ui.eclipse.ui.util.ResourcesDialogHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Version;


/**
 * @author dave
 *
 */
public class GeneralInfoSection extends SigilSection
{

    private String name;
    private String symbolicName;
    private Version version;
    private String description;
    private String provider;
    private String activator;
    private IRequiredBundle fragmentHost;

    private SigilFormEntry nameEntry;
    private SigilFormEntry symbolicNameEntry;
    private SigilFormEntry versionEntry;
    private SigilFormEntry descriptionEntry;
    private SigilFormEntry providerEntry;
    private SigilFormEntry activatorEntry;
    private SigilFormEntry fragmentHostEntry;


    /**
     * @param parent
     * @param toolkit
     * @param style
     * @throws CoreException 
     */
    public GeneralInfoSection( SigilPage page, Composite parent, ISigilProjectModel project ) throws CoreException
    {
        super( page, parent, project );
    }


    protected void createSection( Section section, FormToolkit toolkit )
    {
        setTitle( "General Information" );

        Composite body = createGridBody( 3, false, toolkit );

        Label label = toolkit.createLabel( body, "This section describes general information about this project." );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 3, 1 ) );

        symbolicNameEntry = new SigilFormEntry( body, toolkit, "Symbolic Name" );
        symbolicNameEntry.setFormEntryListener( new SigilFormEntryAdapter()
        {
            @Override
            public void textValueChanged( SigilFormEntry form )
            {
                symbolicName = nullIfEmpty( ( String ) form.getValue() );
                checkDirty();
            }
        } );

        nameEntry = new SigilFormEntry( body, toolkit, "Name" );
        nameEntry.setFormEntryListener( new SigilFormEntryAdapter()
        {
            @Override
            public void textValueChanged( SigilFormEntry form )
            {
                name = nullIfEmpty( ( String ) form.getValue() );
                checkDirty();
            }
        } );

        descriptionEntry = new SigilFormEntry( body, toolkit, "Description" );
        descriptionEntry.setFormEntryListener( new SigilFormEntryAdapter()
        {
            @Override
            public void textValueChanged( SigilFormEntry form )
            {
                description = nullIfEmpty( ( String ) form.getValue() );
                checkDirty();
            }
        } );

        IFormValueConverter converter = new IFormValueConverter()
        {
            public String getLabel( Object value )
            {
                Version v = ( Version ) value;
                return v.toString();
            }


            public Object getValue( String label )
            {
                return VersionTable.getVersion( label );
            }
        };

        versionEntry = new SigilFormEntry( body, toolkit, "Version", null, converter );
        versionEntry.setFormEntryListener( new SigilFormEntryAdapter()
        {
            @Override
            public void textValueChanged( SigilFormEntry form )
            {
                version = ( Version ) form.getValue();
                checkDirty();
            }
        } );

        providerEntry = new SigilFormEntry( body, toolkit, "Provider" );
        providerEntry.setFormEntryListener( new SigilFormEntryAdapter()
        {
            @Override
            public void textValueChanged( SigilFormEntry form )
            {
                provider = nullIfEmpty( ( String ) form.getValue() );
                checkDirty();
            }
        } );

        activatorEntry = new SigilFormEntry( body, toolkit, "Bundle Activator", "Browse...", null );
        activatorEntry.setFormEntryListener( new SigilFormEntryAdapter()
        {
            @Override
            public void textValueChanged( SigilFormEntry form )
            {
                activator = ( String ) form.getValue();
                checkDirty();
            }


            @Override
            public void browseButtonSelected( SigilFormEntry form )
            {
                BackgroundLoadingSelectionDialog<String> dialog = ResourcesDialogHelper.createClassSelectDialog(
                    getShell(), "Add Bundle Activator", getProjectModel(), activator, BundleActivator.class.getName() );

                if ( dialog.open() == Window.OK )
                {
                    form.setValue( dialog.getSelectedElement() );
                }
            }
        } );

        converter = new IFormValueConverter()
        {
            public String getLabel( Object value )
            {
                IRequiredBundle b = ( IRequiredBundle ) value;
                return b == null ? null : b.getSymbolicName() + " " + b.getVersions();
            }


            public Object getValue( String label )
            {
                return null;
            }
        };

        fragmentHostEntry = new SigilFormEntry( body, toolkit, "Fragment Host", "Browse...", converter );
        fragmentHostEntry.setFormEntryListener( new SigilFormEntryAdapter()
        {
            @Override
            public void textValueChanged( SigilFormEntry form )
            {
                fragmentHost = ( IRequiredBundle ) form.getValue();
                checkDirty();
            }


            @Override
            public void browseButtonSelected( SigilFormEntry form )
            {
                NewResourceSelectionDialog<IBundleModelElement> dialog = ResourcesDialogHelper
                    .createRequiredBundleDialog( getSection().getShell(), "Add Required Bundle", getProjectModel(),
                        null, getBundle().getBundleInfo().getRequiredBundles() );

                if ( dialog.open() == Window.OK )
                {
                    IRequiredBundle required = ModelElementFactory.getInstance()
                        .newModelElement( IRequiredBundle.class );
                    required.setSymbolicName( dialog.getSelectedName() );
                    required.setVersions( dialog.getSelectedVersions() );
                    form.setValue( required );
                }
            }
        } );
        fragmentHostEntry.setFreeText( false );
    }


    private static String nullIfEmpty( String value )
    {
        if ( value.trim().length() == 0 )
        {
            return null;
        }
        return value;
    }


    private Shell getShell()
    {
        return getSection().getShell();
    }


    @Override
    public void commit( boolean onSave )
    {
        getBundle().getBundleInfo().setSymbolicName( symbolicName );
        getBundle().getBundleInfo().setName( name );
        getBundle().getBundleInfo().setVersion( version );
        getBundle().getBundleInfo().setDescription( description );
        getBundle().getBundleInfo().setVendor( provider );
        getBundle().getBundleInfo().setFragmentHost( fragmentHost );
        getBundle().getBundleInfo().setActivator( activator );

        super.commit( onSave );
    }


    @Override
    public void refresh()
    {
        symbolicName = getProjectModel().getBundle().getBundleInfo().getSymbolicName();
        name = getProjectModel().getBundle().getBundleInfo().getName();
        description = getProjectModel().getBundle().getBundleInfo().getDescription();
        version = getProjectModel().getBundle().getBundleInfo().getVersion();
        provider = getProjectModel().getBundle().getBundleInfo().getVendor();
        fragmentHost = getProjectModel().getBundle().getBundleInfo().getFragmentHost();
        activator = getProjectModel().getBundle().getBundleInfo().getActivator();

        nameEntry.setValue( name );
        symbolicNameEntry.setValue( symbolicName );
        versionEntry.setValue( version );
        descriptionEntry.setValue( description );
        providerEntry.setValue( provider );
        fragmentHostEntry.setValue( fragmentHost );
        activatorEntry.setValue( activator );

        super.refresh();
    }


    private void checkDirty()
    {
        boolean dirty = different( symbolicName, getProjectModel().getBundle().getBundleInfo().getSymbolicName() )
            || different( name, getProjectModel().getBundle().getBundleInfo().getName() )
            || different( version, getProjectModel().getBundle().getBundleInfo().getVersion() )
            || different( description, getProjectModel().getBundle().getBundleInfo().getDescription() )
            || different( provider, getProjectModel().getBundle().getBundleInfo().getVendor() )
            || different( fragmentHost, getProjectModel().getBundle().getBundleInfo().getFragmentHost() )
            || different( activator, getProjectModel().getBundle().getBundleInfo().getActivator() );

        if ( dirty )
            markDirty();
    }


    private boolean different( Object val1, Object val2 )
    {
        return val1 == null ? val2 != null : !val1.equals( val2 );
    }


    private ISigilBundle getBundle()
    {
        return getProjectModel().getBundle();
    }
}
