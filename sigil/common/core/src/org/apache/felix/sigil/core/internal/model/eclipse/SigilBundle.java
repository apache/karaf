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

package org.apache.felix.sigil.core.internal.model.eclipse;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.JarFile;

import org.apache.felix.sigil.core.BldCore;
import org.apache.felix.sigil.core.util.ManifestUtil;
import org.apache.felix.sigil.model.AbstractCompoundModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.framework.Version;


/**
 * @author dave
 *
 */
public class SigilBundle extends AbstractCompoundModelElement implements ISigilBundle
{

    private static final long serialVersionUID = 1L;

    private IBundleModelElement bundle;
    private IPath[] sourcePaths;
    private String[] classpath;
    private String[] packages;
    private IPath location;

    private IPath sourcePathLocation;
    private IPath licencePathLocation;
    private IPath sourceRootPath;

    public SigilBundle()
    {
        super( "Sigil Bundle" );
        sourcePaths = new IPath[0];
        classpath = new String[0];
        packages = new String[0];
    }


    public void synchronize( IProgressMonitor monitor ) throws IOException
    {
        SubMonitor progress = SubMonitor.convert( monitor, 100 );
        progress.subTask( "Synchronizing " + bundle.getSymbolicName() + " binary" );
        sync( location, bundle.getUpdateLocation(), progress.newChild( 45 ) );
        
        if ( bundle.getSourceLocation() != null ) {
            try
            {
                progress.subTask( "Synchronizing " + bundle.getSymbolicName() + " source" );
                sync( sourcePathLocation, bundle.getSourceLocation(), progress.newChild( 45 ) );
            }
            catch ( IOException e )
            {
                BldCore.error( "Failed to download source for " + bundle.getSymbolicName() + " " + bundle.getVersion(), e
                    .getCause() );
            }
        }
        
        if ( bundle.getLicenseURI() != null ) {
            try
            {
                progress.subTask( "Synchronizing " + bundle.getSymbolicName() + " licence" );
                sync( licencePathLocation, bundle.getLicenseURI(), progress.newChild( 10 ) );
            }
            catch ( IOException e )
            {
                BldCore.error( "Failed to download licence for " + bundle.getSymbolicName() + " " + bundle.getVersion(), e
                    .getCause() );
            }
        }
        
        updateManifest(location);
    }


    private void updateManifest(IPath location) throws IOException
    {
        if ( location != null ) {
            JarFile f = new JarFile(location.toFile());
            try {
                setBundleInfo(ManifestUtil.buildBundleModelElement(f.getManifest()));
            }
            finally {
                f.close();
            }
        }
    }


    public boolean isSynchronized()
    {
        return location == null || location.toFile().exists();
    }


    private static void sync( IPath local, URI remote, IProgressMonitor monitor ) throws IOException
    {
        try
        {
            if ( remote != null )
            {
                if ( local != null && !local.toFile().exists() )
                {
                    URL url = remote.toURL();
                    URLConnection connection = url.openConnection();
                    int contentLength = connection.getContentLength();

                    monitor.beginTask( "Downloading from " + url.getHost(), contentLength );

                    InputStream in = null;
                    OutputStream out = null;
                    try
                    {
                        URLConnection conn = url.openConnection();
                        if ( conn instanceof HttpURLConnection )
                        {
                            HttpURLConnection http = ( HttpURLConnection ) conn;
                            http.setConnectTimeout( 10000 );
                            http.setReadTimeout( 5000 );
                        }
                        in = conn.getInputStream();
                        File f = local.toFile();
                        f.getParentFile().mkdirs();
                        out = new FileOutputStream( f );
                        stream( in, out, monitor );
                    }
                    finally
                    {
                        if ( in != null )
                        {
                            in.close();
                        }
                        if ( out != null )
                        {
                            out.close();
                        }
                        monitor.done();
                    }
                }
            }
        }
        catch ( IOException e )
        {
            local.toFile().delete();
            throw e;
        }
    }


    private static void stream( InputStream in, OutputStream out, IProgressMonitor monitor ) throws IOException
    {
        byte[] b = new byte[1024];
        for ( ;; )
        {
            if ( monitor.isCanceled() )
            {
                throw new InterruptedIOException( "User canceled download" );
            }
            int r = in.read( b );
            if ( r == -1 )
                break;
            out.write( b, 0, r );
            monitor.worked( r );
        }

        out.flush();
    }


    public IBundleModelElement getBundleInfo()
    {
        return bundle;
    }


    public void setBundleInfo( IBundleModelElement bundle )
    {
        if ( bundle == null )
        {
            if ( this.bundle != null )
            {
                this.bundle.setParent( null );
            }
        }
        else
        {
            bundle.setParent( this );
        }
        this.bundle = bundle;
    }


    public void addSourcePath( IPath path )
    {
        ArrayList<IPath> tmp = new ArrayList<IPath>(getSourcePaths());
        tmp.add(path);
        sourcePaths = tmp.toArray( new IPath[tmp.size()] );
    }


    public void removeSourcePath( IPath path )
    {
        ArrayList<IPath> tmp = new ArrayList<IPath>(getSourcePaths());
        if ( tmp.remove(path) ) {
            sourcePaths = tmp.toArray( new IPath[tmp.size()] );
        }
    }


    public Collection<IPath> getSourcePaths()
    {
        return Arrays.asList(sourcePaths);
    }


    public void clearSourcePaths()
    {
        sourcePaths = new IPath[0];
    }


    public void addClasspathEntry( String encodedClasspath )
    {
        ArrayList<String> tmp = new ArrayList<String>(getClasspathEntrys());
        tmp.add(encodedClasspath.trim());
        classpath = tmp.toArray( new String[tmp.size()] );
    }


    public Collection<String> getClasspathEntrys()
    {
        return Arrays.asList(classpath);
    }


    public void removeClasspathEntry( String encodedClasspath )
    {
        ArrayList<String> tmp = new ArrayList<String>(getClasspathEntrys());
        if ( tmp.remove( encodedClasspath.trim() ) ) {
            classpath = tmp.toArray( new String[tmp.size()] );
        }
    }


    public IPath getLocation()
    {
        return location;
    }


    public void setLocation( IPath location )
    {
        this.location = location;
    }


    public IPath getSourcePathLocation()
    {
        return sourcePathLocation;
    }


    public IPath getSourceRootPath()
    {
        return sourceRootPath;
    }


    public void setSourcePathLocation( IPath location )
    {
        this.sourcePathLocation = location;
    }


    public void setSourceRootPath( IPath location )
    {
        this.sourceRootPath = location;
    }


    @Override
    public String toString()
    {
        return "SigilBundle["
            + ( getBundleInfo() == null ? null : ( getBundleInfo().getSymbolicName() + ":" + getBundleInfo()
                .getVersion() ) ) + "]";
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null )
            return false;
        if ( obj == this )
            return true;

        if ( obj instanceof SigilBundle )
        {
            return obj.toString().equals( toString() );
        }

        return false;
    }


    @Override
    public int hashCode()
    {
        return 31 * toString().hashCode();
    }


    public IPath getLicencePathLocation()
    {
        return licencePathLocation;
    }


    public void setLicencePathLocation( IPath licencePathLocation )
    {
        this.licencePathLocation = licencePathLocation;
    }

    public String getElementName()
    {
        return bundle.getSymbolicName();
    }


    public Version getVersion()
    {
        return bundle.getVersion();
    }


    public void setVersion( Version version )
    {
        this.bundle.setVersion( version );
    }


    public String getSymbolicName()
    {
        return bundle.getSymbolicName();
    }


    public Collection<String> getPackages()
    {
        return Arrays.asList(packages);
    }


    public void addPackage( String pkg )
    {
        ArrayList<String> tmp = new ArrayList<String>(getClasspathEntrys());
        tmp.add(pkg);
        packages = tmp.toArray( new String[tmp.size()] );
    }


    public boolean removePackage( String pkg )
    {
        ArrayList<String> tmp = new ArrayList<String>(getClasspathEntrys());
        if ( tmp.remove(pkg) ) {
            packages = tmp.toArray( new String[tmp.size()] );
            return true;
        }
        else {
            return false;
        }
    }


    public IPackageExport findExport( String packageName )
    {
        for ( IPackageExport e : bundle.getExports() )
        {
            if ( packageName.equals( e.getPackageName() ) )
            {
                return e;
            }
        }
        return null;
    }


    public IPackageImport findImport( String packageName )
    {
        for ( IPackageImport i : bundle.getImports() )
        {
            if ( packageName.equals( i.getPackageName() ) )
            {
                return i;
            }
        }
        return null;
    }
}
