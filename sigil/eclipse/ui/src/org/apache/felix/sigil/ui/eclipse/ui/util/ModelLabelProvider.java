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


import java.util.Set;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Version;


public class ModelLabelProvider extends LabelProvider
{
    private volatile Set<? extends IModelElement> unresolvedElements = null;


    public Image getImage( Object element )
    {
        boolean unresolved = ( unresolvedElements == null ) ? false : unresolvedElements.contains( element );

        if ( element instanceof ISigilBundle || element instanceof IBundleModelElement )
        {
            return findBundle();
        }
        else if ( element instanceof IRequiredBundle )
        {
            boolean optional = ( ( IRequiredBundle ) element ).isOptional();
            return findRequiredBundle( optional, unresolved );
        }
        else if ( element instanceof IPackageImport )
        {
            boolean optional = ( ( IPackageImport ) element ).isOptional();
            return findPackageImport( optional, unresolved );
        }
        else if ( element instanceof IPackageExport )
        {
            return findPackageExport();
        }
        else if ( element instanceof IPackageFragmentRoot )
        {
            IPackageFragmentRoot root = ( IPackageFragmentRoot ) element;
            try
            {
                if ( root.getKind() == IPackageFragmentRoot.K_SOURCE )
                {
                    return findPackage();
                }
                else
                {
                    return findBundle();
                }
            }
            catch ( JavaModelException e )
            {
                SigilCore.error( "Failed to inspect package fragment root", e );
            }
        }
        else if ( element instanceof IClasspathEntry )
        {
            return findPackage();
        }
        if ( element instanceof IBundleRepository )
        {
            IBundleRepository rep = ( IBundleRepository ) element;
            IRepositoryModel config = SigilCore.getRepositoryConfiguration().findRepository( rep.getId() );
            return config.getType().getIcon();
        }

        return null;
    }


    public String getText( Object element )
    {
        if ( element instanceof ISigilBundle )
        {
            ISigilBundle bundle = ( ISigilBundle ) element;
            return bundle.getBundleInfo().getSymbolicName() + " " + bundle.getBundleInfo().getVersion();
        }
        if ( element instanceof IBundleModelElement )
        {
            IBundleModelElement bundle = ( IBundleModelElement ) element;
            return bundle.getSymbolicName();
        }
        if ( element instanceof IRequiredBundle )
        {
            IRequiredBundle req = ( IRequiredBundle ) element;
            return req.getSymbolicName() + " " + req.getVersions();
        }

        if ( element instanceof IPackageImport )
        {
            IPackageImport req = ( IPackageImport ) element;
            return req.getPackageName() + " " + req.getVersions();
        }

        if ( element instanceof IPackageExport )
        {
            IPackageExport pe = ( IPackageExport ) element;
            Version rawVersion = pe.getRawVersion();
            return rawVersion != null ? pe.getPackageName() + " " + rawVersion : pe.getPackageName();
        }

        if ( element instanceof IResource )
        {
            IResource resource = ( IResource ) element;
            return resource.getName();
        }

        if ( element instanceof IPackageFragment )
        {
            IPackageFragment f = ( IPackageFragment ) element;
            return f.getElementName();
        }

        if ( element instanceof IPackageFragmentRoot )
        {
            IPackageFragmentRoot f = ( IPackageFragmentRoot ) element;
            try
            {
                return f.getUnderlyingResource().getName();
            }
            catch ( JavaModelException e )
            {
                return "unknown";
            }
        }

        if ( element instanceof IClasspathEntry )
        {
            IClasspathEntry cp = ( IClasspathEntry ) element;
            return cp.getPath().toString();
        }

        if ( element instanceof IBundleRepository )
        {
            IBundleRepository rep = ( IBundleRepository ) element;
            IRepositoryModel config = SigilCore.getRepositoryConfiguration().findRepository( rep.getId() );
            return config.getName();
        }

        return element.toString();
    }


    private Image findPackage()
    {
        return cacheImage( "icons/package.gif" );
    }


    private Image findPackageImport( boolean optional, boolean unresolved )
    {
        String path;
        if ( optional )
        {
            path = unresolved ? "icons/import-package-optional-error.gif" : "icons/import-package-optional.gif";
        }
        else
        {
            path = unresolved ? "icons/import-package-error.gif" : "icons/import-package.gif";
        }
        return cacheImage( path );
    }


    private Image findPackageExport()
    {
        return cacheImage( "icons/export-package.gif" );
    }


    private Image findBundle()
    {
        return cacheImage( "icons/bundle.gif" );
    }


    private Image findRequiredBundle( boolean optional, boolean unresolved )
    {
        String path;
        if ( optional )
        {
            path = unresolved ? "icons/require-bundle-optional-error.gif" : "icons/require-bundle-optional.gif";
        }
        else
        {
            path = unresolved ? "icons/require-bundle-error.gif" : "icons/require-bundle.gif";
        }
        return cacheImage( path );
    }


    private static Image cacheImage( String path )
    {
        return SigilUI.cacheImage( path, ModelLabelProvider.class.getClassLoader() );
    }


    public void setUnresolvedElements( Set<? extends IModelElement> elements )
    {
        this.unresolvedElements = elements;
    }
}
