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

package org.apache.felix.sigil.repository;


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.core.BldCore;
import org.apache.felix.sigil.core.licence.ILicenseManager;
import org.apache.felix.sigil.core.licence.ILicensePolicy;
import org.apache.felix.sigil.core.util.ManifestUtil;
import org.apache.felix.sigil.core.util.QuoteUtil;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.ModelElementFactoryException;
import org.apache.felix.sigil.model.eclipse.ILibrary;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.osgi.framework.Version;


public abstract class AbstractBundleRepository implements IBundleRepository
{

    private final String id;
    private final HashSet<IBundleRepositoryListener> listeners = new HashSet<IBundleRepositoryListener>();


    public AbstractBundleRepository( String id )
    {
        this.id = id;
    }


    public abstract void accept( IRepositoryVisitor visitor, int options );


    public void addBundleRepositoryListener( IBundleRepositoryListener listener )
    {
        synchronized ( listeners )
        {
            listeners.add( listener );
        }
    }


    public void removeBundleRepositoryListener( IBundleRepositoryListener listener )
    {
        synchronized ( listeners )
        {
            listeners.remove( listener );
        }
    }


    protected void notifyChange()
    {
        for ( IBundleRepositoryListener l : listeners )
        {
            l.notifyChange( this );
        }
    }


    public String getId()
    {
        return id;
    }


    public void accept( IRepositoryVisitor visitor )
    {
        accept( visitor, 0 );
    }


    public void writeOBR( OutputStream out ) throws IOException
    {
        throw new UnsupportedOperationException();
    }


    public Collection<ISigilBundle> findProviders( final ILibrary library, int options )
    {
        final ArrayList<ISigilBundle> found = new ArrayList<ISigilBundle>();

        final ILicensePolicy policy = findPolicy( library );

        IRepositoryVisitor visitor = new IRepositoryVisitor()
        {
            public boolean visit( ISigilBundle bundle )
            {
                if ( policy.accept( bundle ) )
                {
                    IBundleModelElement info = bundle.getBundleInfo();
                    for ( IPackageImport pi : library.getImports() )
                    {
                        for ( IPackageExport e : info.getExports() )
                        {
                            if ( pi.getPackageName().equals( e.getPackageName() )
                                && pi.getVersions().contains( e.getVersion() ) )
                            {
                                found.add( bundle );
                                break;
                            }
                        }
                    }
                }
                return true;
            }
        };

        accept( visitor, options );

        return found;
    }


    public Collection<ISigilBundle> findAllProviders( final IRequiredBundle req, int options )
    {
        final ArrayList<ISigilBundle> found = new ArrayList<ISigilBundle>();

        final ILicensePolicy policy = findPolicy( req );

        IRepositoryVisitor visitor = new IRepositoryVisitor()
        {
            public boolean visit( ISigilBundle bundle )
            {
                if ( policy.accept( bundle ) )
                {
                    IBundleModelElement info = bundle.getBundleInfo();
                    if ( req.getSymbolicName().equals( info.getSymbolicName() )
                        && req.getVersions().contains( info.getVersion() ) )
                    {
                        found.add( bundle );
                    }
                }
                return true;
            }
        };

        accept( visitor, options );

        return found;
    }


    public Collection<ISigilBundle> findAllProviders( final IPackageImport pi, int options )
    {
        final ArrayList<ISigilBundle> found = new ArrayList<ISigilBundle>();

        final ILicensePolicy policy = findPolicy( pi );

        IRepositoryVisitor visitor = new IRepositoryVisitor()
        {

            public boolean visit( ISigilBundle bundle )
            {
                if ( policy.accept( bundle ) )
                {
                    IBundleModelElement info = bundle.getBundleInfo();
                    if ( info != null )
                    {
                        for ( IPackageExport e : info.getExports() )
                        {
                            if ( pi.getPackageName().equals( e.getPackageName() ) )
                            {
                                if ( pi.getVersions().contains( e.getVersion() ) )
                                {
                                    found.add( bundle );
                                    break;
                                }
                            }
                        }
                    }
                }
                return true;
            }

        };

        accept( visitor, options );

        return found;
    }


    public ISigilBundle findProvider( final IPackageImport pi, int options )
    {
        final ArrayList<ISigilBundle> found = new ArrayList<ISigilBundle>();

        final ILicensePolicy policy = findPolicy( pi );

        IRepositoryVisitor visitor = new IRepositoryVisitor()
        {
            public boolean visit( ISigilBundle bundle )
            {
                if ( policy.accept( bundle ) )
                {
                    IBundleModelElement info = bundle.getBundleInfo();
                    for ( IPackageExport e : info.getExports() )
                    {
                        if ( pi.getPackageName().equals( e.getPackageName() )
                            && pi.getVersions().contains( e.getVersion() ) )
                        {
                            found.add( bundle );
                            return false;
                        }
                    }
                }
                return true;
            }

        };

        accept( visitor, options );

        return found.isEmpty() ? null : found.iterator().next();
    }


    public ISigilBundle findProvider( final IRequiredBundle req, int options )
    {
        final ArrayList<ISigilBundle> found = new ArrayList<ISigilBundle>();

        final ILicensePolicy policy = findPolicy( req );

        IRepositoryVisitor visitor = new IRepositoryVisitor()
        {

            public boolean visit( ISigilBundle bundle )
            {
                if ( policy.accept( bundle ) )
                {
                    IBundleModelElement info = bundle.getBundleInfo();
                    if ( req.getSymbolicName().equals( info.getSymbolicName() )
                        && req.getVersions().contains( info.getVersion() ) )
                    {
                        found.add( bundle );
                        return false;
                    }
                }
                return true;
            }

        };

        accept( visitor, options );

        return found.isEmpty() ? null : found.iterator().next();
    }


    public IBundleModelElement buildBundleModelElement( Manifest mf )
    {
        return ManifestUtil.buildBundleModelElement(mf);
    }


    protected ILicensePolicy findPolicy( IModelElement elem )
    {
        ILicenseManager man = BldCore.getLicenseManager();

        /*		ISigilProjectModel p = elem.getAncestor(ISigilProjectModel.class);
        		
        		ILicensePolicy policy = null;
        		
        		if ( p != null ) {
        			policy = man.getPolicy(p);
        		}
        		else {
        			policy = man.getDefaultPolicy();
        		}
        		
        		return policy; */

        return man.getDefaultPolicy();
    }
}