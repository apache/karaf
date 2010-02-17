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
        IBundleModelElement info = null;

        if ( mf != null )
        {
            Attributes attrs = mf.getMainAttributes();
            String name = attrs.getValue( "Bundle-SymbolicName" );
            if ( name == null )
            {
                // framework.jar doesn't have Bundle-SymbolicName!
                name = attrs.getValue( "Bundle-Name" );
            }

            if ( name != null )
            {
                try
                {
                    info = ModelElementFactory.getInstance().newModelElement( IBundleModelElement.class );
                    info.setSymbolicName( name.split( ";" )[0] );
                    info.setVersion( VersionTable.getVersion( attrs.getValue( "Bundle-Version" ) ) );
                    info.setName( attrs.getValue( "Bundle-Name" ) );
                    info.setDescription( attrs.getValue( "Bundle-Description" ) );
                    info.setVendor( attrs.getValue( "Bundle-Vendor" ) );

                    String str = attrs.getValue( "Import-Package" );
                    if ( str != null )
                    {
                        addImports( info, str );
                    }

                    str = attrs.getValue( "Export-Package" );
                    if ( str != null )
                    {
                        addExports( info, str );
                    }

                    str = attrs.getValue( "Require-Bundle" );
                    if ( str != null )
                    {
                        addRequires( info, str );
                    }

                    str = attrs.getValue( "Bundle-Classpath" );

                    if ( str != null )
                    {
                        addClasspath( info, str );
                    }

                    str = attrs.getValue( "Fragment-Host" );
                    if ( str != null )
                    {
                        addHost( info, str );
                    }
                }
                catch ( RuntimeException e )
                {
                    BldCore.error( "Failed to read info from bundle " + name, e );
                    // clear elements as clearly got garbage
                    info = null;
                }
            }
        }

        return info;
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


    private void addClasspath( IBundleModelElement info, String cpStr )
    {
        for ( String cp : cpStr.split( "\\s*,\\s*" ) )
        {
            info.addClasspath( cp );
        }
    }


    private void addExports( IBundleModelElement info, String exportStr ) throws ModelElementFactoryException
    {
        for ( String exp : QuoteUtil.split( exportStr ) )
        {
            try
            {
                String[] parts = exp.split( ";" );
                IPackageExport pe = ModelElementFactory.getInstance().newModelElement( IPackageExport.class );
                pe.setPackageName( parts[0].trim() );

                if ( parts.length > 1 )
                {
                    for ( int i = 1; i < parts.length; i++ )
                    {
                        String check = parts[i];
                        if ( check.toLowerCase().startsWith( "version=" ) )
                        {
                            pe.setVersion( parseVersion( check.substring( "version=".length() ) ) );
                        }
                        else if ( check.toLowerCase().startsWith( "specification-version=" ) )
                        {
                            pe.setVersion( parseVersion( check.substring( "specification-version=".length() ) ) );
                        }
                        else if ( check.toLowerCase().startsWith( "uses:=" ) )
                        {
                            for ( String use : parseUses( check.substring( "uses:=".length() ) ) )
                            {
                                pe.addUse( use );
                            }
                        }
                    }
                }
                info.addExport( pe );
            }
            catch ( RuntimeException e )
            {
                e.printStackTrace();
            }
        }
    }


    private Collection<String> parseUses( String uses )
    {
        if ( uses.startsWith( "\"" ) )
        {
            uses = uses.substring( 1, uses.length() - 2 );
        }

        return Arrays.asList( uses.split( "," ) );
    }


    private Version parseVersion( String val )
    {
        val = val.replaceAll( "\"", "" );
        return VersionTable.getVersion( val );
    }


    private void addImports( IBundleModelElement info, String importStr ) throws ModelElementFactoryException
    {
        for ( String imp : QuoteUtil.split( importStr ) )
        {
            String[] parts = imp.split( ";" );
            IPackageImport pi = ModelElementFactory.getInstance().newModelElement( IPackageImport.class );
            pi.setPackageName( parts[0].trim() );

            if ( parts.length > 1 )
            {
                for ( int i = 1; i < parts.length; i++ )
                {
                    String p = parts[i];
                    if ( p.toLowerCase().startsWith( "version=" ) )
                    {
                        pi.setVersions( VersionRange.parseVersionRange( p.substring( "version=".length() ) ) );
                    }
                    else if ( p.toLowerCase().startsWith( "specification-version=" ) )
                    {
                        pi.setVersions( VersionRange
                            .parseVersionRange( p.substring( "specification-version=".length() ) ) );
                    }
                    else if ( p.toLowerCase().startsWith( "resolution:=" ) )
                    {
                        pi.setOptional( p.toLowerCase().substring( "resolution:=".length() ).equals( "optional" ) );
                    }
                }
            }
            info.addImport( pi );
        }
    }


    private void addRequires( IBundleModelElement info, String reqStr ) throws ModelElementFactoryException
    {
        for ( String imp : QuoteUtil.split( reqStr ) )
        {
            String[] parts = imp.split( ";" );
            IRequiredBundle req = ModelElementFactory.getInstance().newModelElement( IRequiredBundle.class );
            req.setSymbolicName( parts[0] );

            if ( parts.length > 1 )
            {
                if ( parts[1].toLowerCase().startsWith( "version=" ) )
                {
                    req.setVersions( VersionRange.parseVersionRange( parts[1].substring( "version=".length() ) ) );
                }
                else if ( parts[1].toLowerCase().startsWith( "specification-version=" ) )
                {
                    req.setVersions( VersionRange.parseVersionRange( parts[1].substring( "specification-version="
                        .length() ) ) );
                }
            }
            info.addRequiredBundle( req );
        }
    }


    /**
     * @param info
     * @param str
     */
    private void addHost( IBundleModelElement info, String str )
    {
        String[] parts = str.split( ";" );
        IRequiredBundle req = ModelElementFactory.getInstance().newModelElement( IRequiredBundle.class );
        req.setSymbolicName( parts[0].trim() );

        if ( parts.length > 1 )
        {
            String part = parts[1].toLowerCase().trim();
            if ( part.startsWith( "bundle-version=" ) )
            {
                req.setVersions( VersionRange.parseVersionRange( part.substring( "bundle-version=".length() ) ) );
            }
        }
        info.setFragmentHost( req );
    }
}