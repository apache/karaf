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

package org.apache.felix.sigil.ui.eclipse.ui.util;


import java.util.ArrayList;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.JavaHelper;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IPackageModelElement;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.ui.eclipse.ui.editors.project.NewPackageExportDialog;
import org.apache.felix.sigil.ui.eclipse.ui.editors.project.NewResourceSelectionDialog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.IJobRunnable;


public class ResourcesDialogHelper
{

    static final int UPDATE_BATCH_SIZE = 100;


    public static BackgroundLoadingSelectionDialog<String> createClassSelectDialog( Shell shell, String title,
        final ISigilProjectModel project, String selected, final String ifaceOrParentClass )
    {
        final BackgroundLoadingSelectionDialog<String> dialog = new BackgroundLoadingSelectionDialog<String>( shell,
            "Class Name", true );

        IJobRunnable job = new IJobRunnable()
        {
            public IStatus run( IProgressMonitor monitor )
            {
                try
                {
                    for ( IJavaElement e : JavaHelper.findTypes( project.getJavaModel(),
                        IJavaElement.PACKAGE_FRAGMENT ) )
                    {
                        IPackageFragment root = ( IPackageFragment ) e;
                        if ( project.isInBundleClasspath( root ) )
                        {
                            for ( IJavaElement e1 : JavaHelper.findTypes( root, IJavaElement.COMPILATION_UNIT,
                                IJavaElement.CLASS_FILE ) )
                            {
                                ITypeRoot typeRoot = ( ITypeRoot ) e1;
                                IType type = ( IType ) JavaHelper.findType( typeRoot, IJavaElement.TYPE );
                                if ( JavaHelper.isAssignableTo( ifaceOrParentClass, type ) )
                                {
                                    dialog.addElement( type.getFullyQualifiedName() );
                                }
                            }
                        }
                    }

                    return Status.OK_STATUS;
                }
                catch ( JavaModelException e )
                {
                    return e.getStatus();
                }
            }

        };

        dialog.addBackgroundJob( "Scanning for activators in project", job );

        return dialog;
    }


    public static NewResourceSelectionDialog<IPackageExport> createImportDialog( Shell shell, String title,
        ISigilProjectModel sigil, final IPackageImport selected, final Collection<IPackageImport> existing )
    {
        final Set<String> existingNames = new HashSet<String>();

        for ( IPackageImport existingImport : existing )
        {
            existingNames.add( existingImport.getPackageName() );
        }

        final NewResourceSelectionDialog<IPackageExport> dialog = new NewResourceSelectionDialog<IPackageExport>(
            shell, "Package Name:", false );

        dialog.setFilter( new IFilter<IPackageModelElement>()
        {
            public boolean select( IPackageModelElement element )
            {
                return !existingNames.contains( element.getPackageName() );
            }
        } );

        dialog.setComparator( new Comparator<IPackageExport>()
        {
            public int compare( IPackageExport o1, IPackageExport o2 )
            {
                return o1.compareTo( o2 );
            }
        } );

        dialog.setDescriptor( new IElementDescriptor<IPackageExport>()
        {
            public String getLabel( IPackageExport element )
            {
                return getName( element ) + " (" + element.getVersion().toString() + ")";
            }


            public String getName( IPackageExport element )
            {
                return element.getPackageName();
            }
        } );

        dialog.setLabelProvider( new WrappedContentProposalLabelProvider<IPackageExport>( dialog.getDescriptor() ) );

        if ( selected != null )
        {
            dialog.setSelectedName( selected.getPackageName() );
            dialog.setVersions( selected.getVersions() );
            dialog.setOptional( selected.isOptional() );
        }

        IJobRunnable job = new ExportedPackageFinder( sigil, dialog );
        dialog.addBackgroundJob( "Scanning for exports in workspace", job );

        return dialog;
    }


    public static NewPackageExportDialog createNewExportDialog( Shell shell, String title,
        final IPackageExport selected, final ISigilProjectModel project, boolean multiSelect )
    {
        IFilter<IJavaElement> selectFilter = new IFilter<IJavaElement>()
        {
            public boolean select( IJavaElement e )
            {
                if ( selected != null && e.getElementName().equals( selected.getPackageName() ) )
                {
                    return true;
                }

                if ( e.getElementName().trim().length() > 0 && isLocal( e ) )
                {
                    for ( IPackageExport p : project.getBundle().getBundleInfo().getExports() )
                    {
                        if ( p.getPackageName().equals( e.getElementName() ) )
                        {
                            return false;
                        }
                    }

                    return true;
                }
                else
                {
                    return false;
                }
            }


            private boolean isLocal( IJavaElement java )
            {
                try
                {
                    switch ( java.getElementType() )
                    {
                        case IJavaElement.PACKAGE_FRAGMENT:
                            IPackageFragment fragment = ( IPackageFragment ) java;
                            return fragment.containsJavaResources();
                        default:
                            throw new IllegalStateException( "Unexpected resource type " + java );
                    }
                }
                catch ( JavaModelException e )
                {
                    SigilCore.error( "Failed to inspect java element ", e );
                    return false;
                }
            }

        };

        final NewPackageExportDialog dialog = new NewPackageExportDialog( shell, multiSelect );
        dialog.setFilter( selectFilter );

        dialog.setProjectVersion( project.getVersion() );
        if ( selected != null )
        {
            dialog.setSelectedName( selected.getPackageName() );
            dialog.setVersion( selected.getRawVersion() );
        }

        IJobRunnable job = new IJobRunnable()
        {
            public IStatus run( IProgressMonitor monitor )
            {
                try
                {
                    ArrayList<IPackageFragment> list = new ArrayList<IPackageFragment>( UPDATE_BATCH_SIZE );
                    for ( IJavaElement e : JavaHelper.findTypes( project.getJavaModel(),
                        IJavaElement.PACKAGE_FRAGMENT ) )
                    {
                        IPackageFragment root = ( IPackageFragment ) e;
                        if ( project.isInBundleClasspath( root ) )
                        {
                            list.add( root );
                            if ( list.size() >= UPDATE_BATCH_SIZE )
                            {
                                dialog.addElements( list );
                                list.clear();
                            }
                        }
                    }
                    if ( !list.isEmpty() )
                    {
                        dialog.addElements( list );
                    }
                    return Status.OK_STATUS;
                }
                catch ( JavaModelException e )
                {
                    return e.getStatus();
                }
            }
        };

        dialog.addBackgroundJob( "Scanning for packages in project", job );

        return dialog;
    }


    public static NewResourceSelectionDialog<IBundleModelElement> createRequiredBundleDialog( Shell shell,
        String title, final ISigilProjectModel sigil, final IRequiredBundle selected,
        final Collection<IRequiredBundle> existing )
    {
        final Set<String> existingNames = new HashSet<String>();
        for ( IRequiredBundle existingBundle : existing )
        {
            existingNames.add( existingBundle.getSymbolicName() );
        }

        final NewResourceSelectionDialog<IBundleModelElement> dialog = new NewResourceSelectionDialog<IBundleModelElement>(
            shell, "Bundle:", false );

        dialog.setDescriptor( new IElementDescriptor<IBundleModelElement>()
        {
            public String getLabel( IBundleModelElement element )
            {
                return getName( element ) + " (" + element.getVersion() + ")";
            }


            public String getName( IBundleModelElement element )
            {
                return element.getSymbolicName();
            }
        } );

        dialog
            .setLabelProvider( new WrappedContentProposalLabelProvider<IBundleModelElement>( dialog.getDescriptor() ) );

        dialog.setFilter( new IFilter<IBundleModelElement>()
        {
            public boolean select( IBundleModelElement element )
            {
                return !existingNames.contains( element.getSymbolicName() );
            }
        } );

        dialog.setComparator( new Comparator<IBundleModelElement>()
        {
            public int compare( IBundleModelElement o1, IBundleModelElement o2 )
            {
                return o1.getSymbolicName().compareTo( o2.getSymbolicName() );
            }
        } );

        if ( selected != null )
        {
            dialog.setSelectedName( selected.getSymbolicName() );
            dialog.setVersions( selected.getVersions() );
            dialog.setOptional( selected.isOptional() );
        }

        IJobRunnable job = new IJobRunnable()
        {
            public IStatus run( final IProgressMonitor monitor )
            {
                final List<IBundleModelElement> bundles = new ArrayList<IBundleModelElement>( UPDATE_BATCH_SIZE );
                final IModelWalker walker = new IModelWalker()
                {
                    //int count = 0;
                    public boolean visit( IModelElement element )
                    {
                        if ( element instanceof IBundleModelElement )
                        {
                            IBundleModelElement b = ( IBundleModelElement ) element;
                            bundles.add( b );

                            if ( bundles.size() >= UPDATE_BATCH_SIZE )
                            {
                                dialog.addElements( bundles );
                                bundles.clear();
                            }
                            // no need to recurse further.
                            return false;
                        }
                        return !monitor.isCanceled();
                    }
                };
                SigilCore.getRepositoryManager( sigil ).visit( walker );
                if ( !bundles.isEmpty() )
                {
                    dialog.addElements( bundles );
                }
                return Status.OK_STATUS;
            }
        };

        dialog.addBackgroundJob( "Scanning for bundles in workspace", job );

        return dialog;
    }
}
