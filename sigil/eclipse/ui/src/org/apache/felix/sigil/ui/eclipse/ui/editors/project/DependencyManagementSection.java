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


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionRangeBoundingRule;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilPage;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilSection;
import org.apache.felix.sigil.ui.eclipse.ui.util.AccumulatorAdapter;
import org.apache.felix.sigil.ui.eclipse.ui.util.ExportedPackageFinder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;


public class DependencyManagementSection extends SigilSection
{

    private Hyperlink hypConvertRBtoIP;


    public DependencyManagementSection( SigilPage page, Composite parent, ISigilProjectModel project )
        throws CoreException
    {
        super( page, parent, project );
    }


    protected void createSection( Section section, FormToolkit toolkit ) throws CoreException
    {
        setTitle( "Dependency Management" );

        Composite body = createGridBody( 1, false, toolkit );

        hypConvertRBtoIP = toolkit.createHyperlink( body, "Convert Required Bundles to Imported Packages", SWT.NONE );
        hypConvertRBtoIP.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );

        hypConvertRBtoIP.addHyperlinkListener( new HyperlinkAdapter()
        {
            public void linkActivated( HyperlinkEvent e )
            {
                run();
            }
        } );
    }


    protected void run()
    {
        final Map<String, IPackageExport> exports = new HashMap<String, IPackageExport>();
        final Set<String> imports = new HashSet<String>();

        // Find all exports
        final ExportedPackageFinder exportFinder = new ExportedPackageFinder( getProjectModel(),
            new AccumulatorAdapter<IPackageExport>()
            {
                public void addElements( Collection<? extends IPackageExport> elements )
                {
                    for ( IPackageExport export : elements )
                    {
                        exports.put( export.getPackageName(), export );
                    }
                }
            } );
        Job findExportsJob = new Job( "Find exports" )
        {
            protected IStatus run( IProgressMonitor monitor )
            {
                return exportFinder.run( monitor );
            }
        };
        findExportsJob.setUser( true );
        findExportsJob.schedule();

        // Find imports from Java source
        Job findImportsJob = new Job( "Find imports" )
        {
            protected IStatus run( IProgressMonitor monitor )
            {
                IJavaProject javaProject = getProjectModel().getJavaModel();
                try
                {
                    IPackageFragment[] packages = javaProject.getPackageFragments();
                    for ( IPackageFragment pkg : packages )
                    {
                        ICompilationUnit[] compilationUnits = pkg.getCompilationUnits();
                        for ( ICompilationUnit compilationUnit : compilationUnits )
                        {
                            IImportDeclaration[] importDecls = compilationUnit.getImports();
                            for ( IImportDeclaration importDecl : importDecls )
                            {
                                imports.add( getPackageName( importDecl ) );
                            }
                        }
                    }
                    return Status.OK_STATUS;
                }
                catch ( JavaModelException e )
                {
                    return new Status( IStatus.ERROR, SigilUI.PLUGIN_ID, 0, "Error finding imports", e );
                }
            }
        };
        findImportsJob.setUser( true );
        findImportsJob.schedule();

        // Wait for both jobs to complete
        try
        {
            findImportsJob.join();
            findExportsJob.join();
        }
        catch ( InterruptedException e )
        {
            // Aborted, just do nothing
            return;
        }

        // Get the version rules
        IPreferenceStore prefStore = SigilCore.getDefault().getPreferenceStore();
        VersionRangeBoundingRule lowerBoundRule = VersionRangeBoundingRule.valueOf( prefStore
            .getString( SigilCore.DEFAULT_VERSION_LOWER_BOUND ) );
        VersionRangeBoundingRule upperBoundRule = VersionRangeBoundingRule.valueOf( prefStore
            .getString( SigilCore.DEFAULT_VERSION_UPPER_BOUND ) );

        // Get the existing imports for the bundle
        IBundleModelElement bundleInfo = getProjectModel().getBundle().getBundleInfo();
        Collection<IPackageImport> existingImports = bundleInfo.getImports();
        Map<String, IPackageImport> existingImportsMap = new HashMap<String, IPackageImport>();
        for ( IPackageImport existingImport : existingImports )
        {
            existingImportsMap.put( existingImport.getPackageName(), existingImport );
        }

        // Add imports to the bundle
        ModelElementFactory elementFactory = ModelElementFactory.getInstance();
        int count = 0;
        for ( String pkgImport : imports )
        {
            IPackageExport export = exports.get( pkgImport );
            if ( export != null && !existingImportsMap.containsKey( pkgImport ) )
            {
                VersionRange versionRange = VersionRange.newInstance( export.getVersion(), lowerBoundRule,
                    upperBoundRule );
                IPackageImport newImport = elementFactory.newModelElement( IPackageImport.class );
                newImport.setPackageName( pkgImport );
                newImport.setVersions( versionRange );
                newImport.setOptional( false );

                bundleInfo.addImport( newImport );
                count++;
            }
        }

        // Remove required bundles
        Collection<IRequiredBundle> requiredBundles = bundleInfo.getRequiredBundles();
        int requiredBundlesSize = requiredBundles.size();
        for ( IRequiredBundle requiredBundle : requiredBundles )
        {
            bundleInfo.removeRequiredBundle( requiredBundle );
        }

        // Update the editor
        if ( count + requiredBundlesSize > 0 )
        {
            IFormPart[] parts = getPage().getManagedForm().getParts();
            for ( IFormPart formPart : parts )
            {
                formPart.refresh();
                ( ( AbstractFormPart ) formPart ).markDirty();
            }
        }

        MessageDialog.openInformation( getManagedForm().getForm().getShell(), "Dependency Management", "Removed "
            + requiredBundlesSize + " required bundle(s) and added " + count + " imported package(s)." );
    }


    private static String getPackageName( IImportDeclaration decl )
    {
        String name = decl.getElementName();
        int lastDot = name.lastIndexOf( '.' );
        return name.substring( 0, lastDot );
    }
}
