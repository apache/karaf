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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IPackageModelElement;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilPage;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTableProvider;
import org.apache.felix.sigil.ui.eclipse.ui.util.ResourcesDialogHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;


public class ImportPackagesSection extends BundleDependencySection
{

    public ImportPackagesSection( SigilPage page, Composite parent, ISigilProjectModel project,
        Set<IModelElement> unresolvedPackages ) throws CoreException
    {
        super( page, parent, project, unresolvedPackages );
    }


    @Override
    protected String getTitle()
    {
        return "Import Packages";
    }


    @Override
    protected Label createLabel( Composite parent, FormToolkit toolkit )
    {
        return toolkit.createLabel( parent, "Specify which packages this bundle imports from other bundles." );
    }


    @Override
    protected IContentProvider getContentProvider()
    {
        return new DefaultTableProvider()
        {
            public Object[] getElements( Object inputElement )
            {
                ArrayList<IPackageImport> imports = new ArrayList<IPackageImport>( getBundle().getBundleInfo()
                    .getImports() );
                Collections.sort( imports, new Comparator<IPackageImport>()
                {
                    public int compare( IPackageImport o1, IPackageImport o2 )
                    {
                        return o1.getPackageName().compareTo( o2.getPackageName() );
                    }
                } );
                return imports.toArray();
            }
        };
    }


    protected ISigilBundle getBundle()
    {
        return getProjectModel().getBundle();
    }


    @Override
    protected void handleAdd()
    {
        NewResourceSelectionDialog<? extends IPackageModelElement> dialog = ResourcesDialogHelper.createImportDialog(
            getSection().getShell(), "Add Imported Package", getProjectModel(), null, getBundle().getBundleInfo()
                .getImports() );

        if ( dialog.open() == Window.OK )
        {
            IPackageImport pi = ModelElementFactory.getInstance().newModelElement( IPackageImport.class );
            pi.setPackageName( dialog.getSelectedName() );
            pi.setVersions( dialog.getSelectedVersions() );
            pi.setOptional( dialog.isOptional() );

            getBundle().getBundleInfo().addImport( pi );
            refresh();
            markDirty();
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    protected void handleRemoved()
    {
        IStructuredSelection selection = ( IStructuredSelection ) getSelection();

        if ( !selection.isEmpty() )
        {
            for ( Iterator<IPackageImport> i = selection.iterator(); i.hasNext(); )
            {
                getBundle().getBundleInfo().removeImport( i.next() );
            }

            refresh();
            markDirty();
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    protected void handleEdit()
    {
        IStructuredSelection selection = ( IStructuredSelection ) getSelection();

        boolean changed = false;

        if ( !selection.isEmpty() )
        {
            for ( Iterator<IPackageImport> i = selection.iterator(); i.hasNext(); )
            {
                IPackageImport packageImport = i.next();
                NewResourceSelectionDialog<? extends IPackageModelElement> dialog = ResourcesDialogHelper
                    .createImportDialog( getSection().getShell(), "Edit Imported Package", getProjectModel(),
                        packageImport, getBundle().getBundleInfo().getImports() );
                if ( dialog.open() == Window.OK )
                {
                    changed = true;
                    IPackageImport newImport = ModelElementFactory.getInstance().newModelElement( IPackageImport.class );
                    newImport.setPackageName( dialog.getSelectedName() );
                    newImport.setVersions( dialog.getSelectedVersions() );
                    newImport.setOptional( dialog.isOptional() );

                    getBundle().getBundleInfo().removeImport( packageImport );
                    getBundle().getBundleInfo().addImport( newImport );
                }
            }
        }

        if ( changed )
        {
            refresh();
            markDirty();
        }
    }
}
