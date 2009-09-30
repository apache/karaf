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

package org.apache.felix.sigil.ivy;


import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.config.BldFactory;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.IResolution;
import org.apache.felix.sigil.repository.ResolutionException;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.BasicResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.FileUtil;
import org.eclipse.core.runtime.IPath;


/**
 * This resolver is able to work with Sigil repositories.
 * It does not allow publishing.
 */
public class SigilResolver extends BasicResolver implements IBldResolver
{
    /** the sigil-injected dependency organisation name */
    static final String ORG_SIGIL = "sigil";

    private IBldResolver resolver;
    private String config;
    private boolean extractBCP = false;
    private Map<String, Resource> resourcesCache = new HashMap<String, Resource>();


    /**
     * no-args constructor required by Ivy.
     * 
     * XXX: It doesn't currently seem to matter that Ivy instantiates this many times,
     * since SigilParser caches it, and the ProjectRepositoryProvider static cache
     * prevents it being re-loaded unnecessarily.
     * 
     * If this should become a problem, then we will need to delegate to a singleton instance,
     * as we have done in SigilParser.
     */
    public SigilResolver()
    {
    }


    public void setConfig( String config )
    {
        this.config = config;
    }


    /**
     * if true, resolver extracts any jars embedded in Bundle-ClassPath.
     */
    public void setExtractBCP( String extract )
    {
        this.extractBCP = Boolean.parseBoolean( extract );
    }


    private IBldResolver getResolver()
    {
        if ( resolver == null )
        {
            if ( config == null )
            {
                throw new Error( "SigilResolver: not configured. Specify config=\"PATH\" in ivysettings.xml." );
            }
            try
            {
                URI uri;
                File file = new File( config );

                if ( file.isAbsolute() )
                {
                    uri = file.toURI();
                }
                else
                {
                    URI cwd = new File( "." ).toURI();
                    uri = cwd.resolve( config );
                }

                Map<String, Properties> repositories = BldFactory.getConfig( uri ).getRepositoryConfig();
                resolver = new BldResolver( repositories );
            }
            catch ( IOException e )
            {
                throw new Error( "SigilResolver: failed to configure: " + e );
            }
        }
        return resolver;
    }


    public IResolution resolveOrFail( IModelElement element, boolean transitive ) throws ResolutionException
    {
        return getResolver().resolveOrFail( element, transitive );
    }


    public IResolution resolve( IModelElement element, boolean transitive )
    {
        return getResolver().resolve( element, transitive );
    }


    public String getTypeName()
    {
        return "sigil";
    }


    /*
     * synthesize an ivy descriptor for a Sigil dependency. called after Sigil
     * resolution, so descriptor already contains resolved version.
     */
    public ResolvedResource findIvyFileRef( DependencyDescriptor dd, ResolveData data )
    {
        ResolvedResource ref = null;

        ModuleRevisionId id = dd.getDependencyRevisionId();

        if ( !id.getOrganisation().equals( ORG_SIGIL ) )
        {
            return null;
        }

        ISigilBundle bundle = resolve( id );
        if ( bundle == null )
        {
            Log.error( "Failed to find bundle for module " + id );
            return null;
        }

        String symbolicName = id.getName();
        String version = bundle.getVersion().toString();

        Resource res = new SigilIvy( extractBCP ? bundle : null, symbolicName, version );
        ref = new ResolvedResource( res, version );

        Log.debug( format( "findIvyFileRef: dd=%s => ref=%s", dd, ref ) );
        return ref;
    }


    @Override
    protected ResolvedResource findArtifactRef( Artifact artifact, Date date )
    {
        String name = artifact.getName();
        ModuleRevisionId id = artifact.getModuleRevisionId();

        if ( !id.getOrganisation().equals( ORG_SIGIL ) )
        {
            return null;
        }

        ISigilBundle bundle = resolve( id );
        if ( bundle == null )
        {
            return null;
        }

        IBundleModelElement info = bundle.getBundleInfo();
        URI uri = info.getUpdateLocation();
        Resource res = null;

        try
        {
            URL url = ( uri != null ) ? uri.toURL() : bundle.getLocation().toFile().toURL();
            if ( name.contains( "!" ) )
            {
                String[] split = name.split( "!" );
                url = new URL( "jar:" + url + "!/" + split[1] + "." + artifact.getExt() );
            }
            res = new URLResource( url );
        }
        catch ( MalformedURLException e )
        {
            System.out.println( "Oops! " + e );
        }

        String version = bundle.getVersion().toString();
        ResolvedResource ref = new ResolvedResource( res, version );

        Log.debug( format( "findArtifactRef: artifact=%s, date=%s => ref=%s", artifact, date, ref ) );
        return ref;
    }


    private ISigilBundle resolve( ModuleRevisionId id )
    {
        String revision = id.getRevision();
        String range = revision;

        if ( revision.indexOf( ',' ) < 0 )
        {
            // SigilParser has already resolved the revision from the import
            // version range.
            // We now need to locate the same bundle to get its download URL.
            // We must use an OSGi version range to specify the exact version,
            // otherwise it will resolve to "specified version or above".
            range = "[" + revision + "," + revision + "]";
        }

        IRequiredBundle bundle = ModelElementFactory.getInstance().newModelElement(IRequiredBundle.class);
        bundle.setSymbolicName( id.getName() );
        bundle.setVersions( VersionRange.parseVersionRange( range ) );

        Log.verbose( "searching for " + bundle );

        try
        {
            IResolution resolution = resolveOrFail( bundle, false );
            ISigilBundle[] bundles = resolution.getBundles().toArray( new ISigilBundle[0] );
            if ( bundles.length == 1 )
            {
                return bundles[0];
            }
        }
        catch ( ResolutionException e )
        {
            Log.warn( e.getMessage() );
            return null;
        }
        return null;
    }


    /*
     * Implement BasicResolver abstract methods
     */

    @Override
    protected long get( Resource res, File dest ) throws IOException
    {
        FileUtil.copy( res.openStream(), dest, null );
        long len = res.getContentLength();
        Log.debug( format( "get(%s, %s) = %d", res, dest, len ) );
        return len;
    }


    @Override
    public Resource getResource( String source ) throws IOException
    {
        source = encode( source );
        Resource res = resourcesCache.get( source );
        if ( res == null )
        {
            res = new URLResource( new URL( source ) );
            resourcesCache.put( source, res );
        }
        Log.debug( format( "SIGIL: getResource(%s) = %d", source, res ) );
        return res;
    }


    private static String encode( String source )
    {
        return source.trim().replaceAll( " ", "%20" );
    }


    @SuppressWarnings("unchecked")
    @Override
    protected Collection findNames( Map tokenValues, String token )
    {
        throw new Error( "SigilResolver: findNames not supported." );
    }


    public void publish( Artifact artifact, File src, boolean overwrite ) throws IOException
    {
        throw new Error( "SigilResolver: publish not supported." );
    }


    public void beginPublishTransaction( ModuleRevisionId module, boolean overwrite ) throws IOException
    {
        // stop publish attempts being silently ignored.
        throw new Error( "SigilResolver: publish not supported." );
    }

    /*
     * Synthesize a virtual ivy file for a Sigil dependency.
     */
    static class SigilIvy implements Resource
    {
        private StringBuilder content = new StringBuilder();
        private String name;
        private boolean exists = true;


        private SigilIvy()
        {
        }


        public SigilIvy( ISigilBundle bundle, String module, String rev )
        {
            this.name = "sigil!" + module + "#" + rev;

            String org = ORG_SIGIL;
            // FIXME: make status and pub configurable
            String status = "release";
            String pub = "20080912162859";

            content.append( "<ivy-module version=\"1.0\">\n" );

            content.append( format(
                "<info organisation=\"%s\" module=\"%s\" revision=\"%s\" status=\"%s\" publication=\"%s\"/>\n", org,
                module, rev, status, pub ) );

            String bcp = readBundleClassPath( bundle );
            if ( bcp != null )
            {
                content.append( "<publications>\n" );
                for ( String j : bcp.split( ",\\s*" ) )
                {
                    if ( j.equals( "." ) )
                    {
                        content.append( "<artifact/>\n" );
                    }
                    else if ( j.endsWith( ".jar" ) && bundleContains( bundle, j ) )
                    {
                        j = j.substring( 0, j.length() - 4 );
                        content.append( format( "<artifact name=\"%s!%s\"/>\n", module, j ) );
                    }
                }
                content.append( "</publications>\n" );
            }

            // TODO: add dependencies?
            // <dependencies>
            // <dependency org="org.apache" name="log4j" rev="1.2.12"
            // revConstraint="[1.2,1.3)"/>
            // </dependencies>

            content.append( "</ivy-module>\n" );
        }


        private boolean bundleContains( ISigilBundle bundle, String j )
        {
            InputStream is = null;
            try
            {
                URL url = getURL( bundle );
                is = url.openStream();
                JarInputStream js = new JarInputStream( is, false );
                JarEntry entry;
                while ( ( entry = js.getNextJarEntry() ) != null )
                {
                    if ( j.equals( entry.getName() ) )
                    {
                        return true;
                    }
                }
            }
            catch ( IOException e )
            {
            }
            finally
            {
                if ( is != null )
                {
                    try
                    {
                        is.close();
                    }
                    catch ( IOException e2 )
                    {
                    }
                }
            }
            return false;
        }


        private String readBundleClassPath( ISigilBundle bundle )
        {
            if ( bundle == null )
                return null;

            InputStream is = null;
            try
            {
                URL url = getURL( bundle );
                is = url.openStream();
                JarInputStream js = new JarInputStream( is, false );
                Manifest m = js.getManifest();
                if ( m != null )
                    return ( String ) m.getMainAttributes().getValue( "Bundle-ClassPath" );
            }
            catch ( IOException e )
            {
            }
            finally
            {
                if ( is != null )
                {
                    try
                    {
                        is.close();
                    }
                    catch ( IOException e2 )
                    {
                    }
                }
            }

            return null;
        }


        private URL getURL( ISigilBundle bundle ) throws MalformedURLException
        {
            URI uri = bundle.getBundleInfo().getUpdateLocation();
            if ( uri != null ) {
                return uri.toURL();
            }
            else {
                IPath path = bundle.getLocation();
                if ( path == null ) {
                    throw new NullPointerException( "Missing location for " + bundle.getSymbolicName() );
                }
                return path.toFile().toURI().toURL();
            }
        }


        public String toString()
        {
            return "SigilIvy[" + name + "]";
        }


        // clone is used to read checksum files
        // so clone(getName() + ".sha1").exists() should be false
        public Resource clone( String cloneName )
        {
            Log.debug( "SigilIvy: clone: " + cloneName );
            SigilIvy clone = new SigilIvy();
            clone.name = cloneName;
            clone.exists = false;
            return clone;
        }


        public boolean exists()
        {
            return exists;
        }


        public long getContentLength()
        {
            return content.length();
        }


        public long getLastModified()
        {
            // TODO Auto-generated method stub
            Log.debug( "NOT IMPLEMENTED: getLastModified" );
            return 0;
        }


        public String getName()
        {
            return name;
        }


        public boolean isLocal()
        {
            return false;
        }


        @SuppressWarnings("deprecation")
        public InputStream openStream() throws IOException
        {
            return new java.io.StringBufferInputStream( content.toString() );
        }
    }
}
