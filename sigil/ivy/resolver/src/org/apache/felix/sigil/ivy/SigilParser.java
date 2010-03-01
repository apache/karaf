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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.config.BldFactory;
import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.IResolution;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;


public class SigilParser implements ModuleDescriptorParser
{

    private static DelegateParser instance;


    // used by ProjectRepository
    static synchronized DelegateParser instance()
    {
        if ( instance == null )
            throw new IllegalStateException( "SigilParser is not instantiated." );
        
        return instance;
    }


    public SigilParser()
    {
        synchronized(SigilParser.class) {
            if ( instance == null )
            {
                instance = new DelegateParser();
            }
        }
    }


    /**
     * In IvyDE, IvyContext is not available, so we can't find the SigilResolver.
     * This allows us to construct one.
     * @deprecated temporary to support IvyDE
     */
    public void setConfig( String config )
    {
        instance.config = config;
    }


    /**
     * sets delegate parser.
     * If not set, we delegate to the default Ivy parser.
     * @param type name returned by desired parser's getType() method.
     */
    public void setDelegateType( String type )
    {
        for ( ModuleDescriptorParser parser : ModuleDescriptorParserRegistry.getInstance().getParsers() )
        {
            if ( parser.getType().equals( type ) )
            {
                instance.delegate = parser;
                break;
            }
        }

        if ( instance.delegate == null )
        {
            throw new IllegalArgumentException( "Can't find parser delegateType=" + type );
        }
    }


    /**
     * sets default file name the delegate parser accepts.
     * If not set, defaults to "ivy.xml".
     * @param name
     */
    public void setDelegateFile( String name )
    {
        instance.ivyFile = name;
    }


    /**
     * sets the dependencies to keep from the delegate parser.
     * If not set, all existing dependencies are dropped.
     * @param regex pattern matching dependency names to keep.
     */
    public void setKeepDependencies( String regex )
    {
        instance.keepDependencyPattern = Pattern.compile( regex );
    }


    /**
     * reduce level of info logging.
     * @param quiet
     */
    public void setQuiet( String quiet )
    {
        instance.quiet = Boolean.parseBoolean( quiet );
    }


    /**
     * sets the SigilResolver we use.
     * If not set, we use the first SigilResolver we find.
     * @param name
     */
    public void setResolver( String name )
    {
        instance.resolverName = name;
    }


    /**
     * adds override element: <override name="X" pattern="Y" replace="Z"/>. Overrides
     * Ivy variables using a pattern substitution on the resource directory path.
     * 
     * @deprecated
     * This is only needed when a delegate parser expects Ant variables to be set
     * during the ivy:buildlist task (or the ProjectRepository initialisation),
     * which they are not. The delegate parser should really be fixed, as otherwise
     * the ivy:buildlist task won't work without this workaround.
     */
    // e.g. <override name="ant.project.name" pattern=".*/([^/]+)/([^/]+)$" replace="$1-$2"/>
    public void addConfiguredOverride( Map<String, String> var )
    {
        final String name = var.get( "name" );
        final String regex = var.get( "pattern" );
        final String replace = var.get( "replace" );

        if ( name == null || regex == null || replace == null )
            throw new IllegalArgumentException( "override must contain name, pattern and replace attributes." );

        instance.varRegex.put( name, Pattern.compile( regex ) );
        instance.varReplace.put( name, replace );
    }


    // implement ModuleDescriptorParser interface by delegation to singleton instance.

    public boolean accept( Resource res )
    {
        return instance.accept( res );
    }


    public Artifact getMetadataArtifact( ModuleRevisionId mrid, Resource res )
    {
        return instance.getMetadataArtifact( mrid, res );
    }


    public String getType()
    {
        return instance.getType();
    }


    public ModuleDescriptor parseDescriptor( ParserSettings settings, URL xmlURL, boolean validate )
        throws ParseException, IOException
    {
        return instance.parseDescriptor( settings, xmlURL, validate );
    }


    public ModuleDescriptor parseDescriptor( ParserSettings settings, URL xmlURL, Resource res, boolean validate )
        throws ParseException, IOException
    {
        return instance.parseDescriptor( settings, xmlURL, res, validate );
    }


    public void toIvyFile( InputStream source, Resource res, File destFile, ModuleDescriptor md )
        throws ParseException, IOException
    {
        instance.toIvyFile( source, res, destFile, md );
    }

    /**
     * delegating parser.
     * (optionally) removes original dependencies and augments with sigil-determined dependencies.
     */
    static class DelegateParser extends XmlModuleDescriptorParser
    {

        private static final String SIGIL_BUILDLIST = IBldProject.PROJECT_FILE;
        private static final String KEEP_ALL = ".*";

        private IBldResolver resolver;
        private ParserSettings defaultSettings;
        private Map<String, DefaultModuleDescriptor> rawCache = new HashMap<String, DefaultModuleDescriptor>();
        private Map<String, DefaultModuleDescriptor> augmentedCache = new HashMap<String, DefaultModuleDescriptor>();

        private HashMap<String, String> varReplace = new HashMap<String, String>();
        private HashMap<String, Pattern> varRegex = new HashMap<String, Pattern>();

        private ModuleDescriptorParser delegate;
        private String ivyFile = "ivy.xml";
        private String resolverName;
        private Pattern keepDependencyPattern;
        private boolean quiet;
        private String config;


        @Override
        public boolean accept( Resource res )
        {
            boolean accept = ( res instanceof SigilResolver.SigilIvy )
                || res.getName().endsWith( "/" + SIGIL_BUILDLIST )
                || ( ( instance.getSigilURI( res ) != null ) && ( ( instance.delegate != null ? instance.delegate
                    .accept( res ) : false ) || super.accept( res ) ) );
            if ( accept )
                Log.verbose( "accepted: " + res );
            return accept;
        }


        @Override
        public void toIvyFile( InputStream source, Resource res, File destFile, ModuleDescriptor md )
            throws ParseException, IOException
        {
            Log.verbose( "writing resource: " + res + " toIvyFile: " + destFile );
            // source allows us to keep layout and comments,
            // but this doesn't work if source is not ivy.xml.
            // So just write file directly from descriptor.
            try
            {
                XmlModuleDescriptorWriter.write( md, destFile );
            }
            finally
            {
                if ( source != null )
                    source.close();
            }
        }


        @Override
        public ModuleDescriptor parseDescriptor( ParserSettings settings, URL xmlURL, boolean validate )
            throws ParseException, IOException
        {
            return parseDescriptor( settings, xmlURL, new URLResource( xmlURL ), validate );
        }


        @Override
        public ModuleDescriptor parseDescriptor( ParserSettings settings, URL xmlURL, Resource res, boolean validate )
            throws ParseException, IOException
        {
            String cacheKey = xmlURL.toString() + settings.hashCode();
            DefaultModuleDescriptor dmd = augmentedCache.get( cacheKey );

            if ( dmd == null )
            {
                dmd = rawCache.get( cacheKey );
                if ( dmd == null )
                {
                    dmd = delegateParse( settings, xmlURL, res, validate );
                }

                if ( !quiet )
                    Log.info( "augmenting module=" + dmd.getModuleRevisionId().getName() + " ant.project.name="
                        + settings.substitute( "${ant.project.name}" ) );

                addDependenciesToDescriptor( res, dmd );
                augmentedCache.put( cacheKey, dmd );

                Log.verbose( "augmented dependencies: " + Arrays.asList( dmd.getDependencies() ) );
            }

            return dmd;
        }


        // used by ProjectRepository
        ModuleDescriptor parseDescriptor( URL projectURL ) throws ParseException, IOException
        {
            if ( defaultSettings == null )
            {
                Ivy ivy = IvyContext.getContext().peekIvy();
                if ( ivy == null )
                    throw new IllegalStateException( "can't get default settings - no ivy context." );
                defaultSettings = ivy.getSettings();
            }

            URL ivyURL = new URL( projectURL, ivyFile );
            String cacheKey = ivyURL.toString() + defaultSettings.hashCode();
            DefaultModuleDescriptor dmd = rawCache.get( cacheKey );

            if ( dmd == null )
            {
                URLResource res = new URLResource( ivyURL );
                // Note: this doesn't contain the augmented dependencies, which is OK,
                // since the ProjectRepository only needs the id and status.
                dmd = delegateParse( defaultSettings, ivyURL, res, false );
                rawCache.put( cacheKey, dmd );
            }

            return dmd;
        }


        private DefaultModuleDescriptor delegateParse( ParserSettings settings, URL xmlURL, Resource res,
            boolean validate ) throws ParseException, IOException
        {
            String resName = res.getName();

            if ( resName.endsWith( "/" + SIGIL_BUILDLIST ) )
            {
                String name = resName.substring( 0, resName.length() - SIGIL_BUILDLIST.length() );
                res = res.clone( name + ivyFile );
                xmlURL = new URL( xmlURL, ivyFile );
            }

            if ( settings instanceof IvySettings )
            {
                IvySettings ivySettings = ( IvySettings ) settings;
                String dir = new File( res.getName() ).getParent();

                for ( String name : varRegex.keySet() )
                {
                    Pattern regex = varRegex.get( name );
                    String replace = varReplace.get( name );

                    String value = regex.matcher( dir ).replaceAll( replace );

                    Log.debug( "overriding variable " + name + "=" + value );
                    ivySettings.setVariable( name, value );
                }
            }

            ModuleDescriptor md = null;
            if ( delegate == null || !delegate.accept( res ) )
            {
                md = super.parseDescriptor( settings, xmlURL, res, validate );
            }
            else
            {
                md = delegate.parseDescriptor( settings, xmlURL, res, validate );
            }

            return mungeDescriptor( md, res );
        }


        /**
         * clones descriptor and removes dependencies, as descriptor MUST have
         * 'this' as the parser given to its constructor.
         * Only dependency names matched by keepDependencyPattern are kept,
         * as we're going to add our own dependencies later.
         */
        private DefaultModuleDescriptor mungeDescriptor( ModuleDescriptor md, Resource res )
        {

            if ( ( md instanceof DefaultModuleDescriptor ) && ( md.getParser() == this )
                && ( KEEP_ALL.equals( keepDependencyPattern ) ) )
            {
                return ( DefaultModuleDescriptor ) md;
            }

            DefaultModuleDescriptor dmd = new DefaultModuleDescriptor( this, res );

            dmd.setModuleRevisionId( md.getModuleRevisionId() );
            dmd.setPublicationDate( md.getPublicationDate() );

            for ( Configuration c : md.getConfigurations() )
            {
                String conf = c.getName();

                dmd.addConfiguration( c );

                for ( Artifact a : md.getArtifacts( conf ) )
                {
                    dmd.addArtifact( conf, a );
                }
            }

            if ( keepDependencyPattern != null )
            {
                for ( DependencyDescriptor dependency : md.getDependencies() )
                {
                    String name = dependency.getDependencyId().getName();
                    if ( keepDependencyPattern.matcher( name ).matches() )
                    {
                        dmd.addDependency( dependency );
                    }
                }
            }

            return dmd;
        }


        /*
         * find URI to Sigil project file. This assumes that it is in the same
         * directory as the ivy file.
         */
        private URI getSigilURI( Resource res )
        {
            URI uri = null;

            if ( res instanceof URLResource )
            {
                URL url = ( ( URLResource ) res ).getURL();
                uri = URI.create( url.toString() ).resolve( IBldProject.PROJECT_FILE );
                try
                {
                    InputStream stream = uri.toURL().openStream(); // check file
                    // exists
                    stream.close();
                }
                catch ( IOException e )
                {
                    uri = null;
                }
            }
            else if ( res instanceof FileResource )
            {
                uri = ( ( FileResource ) res ).getFile().toURI().resolve( IBldProject.PROJECT_FILE );
                if ( !( new File( uri ).exists() ) )
                    uri = null;
            }

            return uri;
        }


        /*
         * add sigil dependencies to ModuleDescriptor.
         */
        private void addDependenciesToDescriptor( Resource res, DefaultModuleDescriptor md ) throws IOException
        {
            // FIXME: transitive should be configurable
            final boolean transitive = true; // ivy default is true
            final boolean changing = false;
            final boolean force = false;

            URI uri = getSigilURI( res );
            if ( uri == null )
                return;

            IBldProject project;

            project = BldFactory.getProject( uri );

            IBundleModelElement requirements = project.getDependencies();
            Log.verbose( "requirements: " + Arrays.asList( requirements.children() ) );

            // preserve version range for Require-Bundle
            // XXX: synthesise bundle version range corresponding to package version ranges?
            HashMap<String, VersionRange> versions = new HashMap<String, VersionRange>();
            for ( IModelElement child : requirements.children() )
            {
                if ( child instanceof IRequiredBundle )
                {
                    IRequiredBundle bundle = ( IRequiredBundle ) child;
                    versions.put( bundle.getSymbolicName(), bundle.getVersions() );
                }
            }

            IBldResolver resolver = findResolver();
            if ( resolver == null )
            {
                // this can happen in IvyDE, but it retries and is OK next time.
                Log.warn( "failed to find resolver - IvyContext not yet available." );
                return;
            }

            IResolution resolution = resolver.resolve( requirements, false );
            Log.verbose( "resolution: " + resolution.getBundles() );

            ModuleRevisionId masterMrid = md.getModuleRevisionId();
            DefaultDependencyDescriptor dd;
            ModuleRevisionId mrid;

            for ( ISigilBundle bundle : resolution.getBundles() )
            {
                IBundleModelElement info = bundle.getBundleInfo();
                String name = info.getSymbolicName();

                if ( "system bundle".equals(name) )
                {
                    // e.g. SystemProvider with framework=null
                    Log.verbose( "Discarding system bundle" );
                    continue;
                }

                ModuleDescriptor bmd = (ModuleDescriptor) bundle.getMeta().get(ModuleDescriptor.class);
                if ( bmd != null )
                {
                    ModuleRevisionId bmrid = bmd.getModuleRevisionId();
                    String org = bmrid.getOrganisation();
                    if ( org == null )
                        org = masterMrid.getOrganisation();
                    String module = bmrid.getName();
                    String rev = "latest." + bmd.getStatus();

                    mrid = ModuleRevisionId.newInstance( org, module, rev );
                    
                    dd = new SigilDependencyDescriptor( md, mrid, force, changing, transitive );

                    Artifact artifact = (Artifact) bundle.getMeta().get(Artifact.class);
                    if ( artifact != null ) {
                        dd.addDependencyArtifact( mrid.getName(), new DefaultDependencyArtifactDescriptor( dd, artifact.getName(), "jar",
                            "jar", null, null ) );                        
                    }
                }
                else
                {
                    // XXX see FELIX-1395 
                    // The following code has been commented out as it causes
                    // problems with require bundle dependencies
                    // VersionRange version = versions.get( name );
                    // String rev = version != null ? version.toString() : info.getVersion().toString();
                    String rev = info.getVersion().toString();
                    mrid = ModuleRevisionId.newInstance( SigilResolver.ORG_SIGIL, name, rev );
                    dd = new SigilDependencyDescriptor( md, mrid, force, changing, transitive );
                }

                int nDeps = 0;
                boolean foundDefault = false;

                // TODO: make dependency configurations configurable SIGIL-176
                for ( String conf : md.getConfigurationsNames() )
                {
                    if ( conf.equals( "default" ) )
                    {
                        foundDefault = true;
                    }
                    else if ( md.getArtifacts( conf ).length == 0 )
                    {
                        dd.addDependencyConfiguration( conf, conf + "(default)" );
                        nDeps++;
                    }
                }

                if ( nDeps > 0 )
                {
                    if ( foundDefault )
                        dd.addDependencyConfiguration( "default", "default" );
                }
                else
                {
                    dd.addDependencyConfiguration( "*", "*" ); // all configurations
                }

                md.addDependency( dd );
            }

            boolean resolved = true;
            for ( IModelElement child : requirements.children() )
            {
                ISigilBundle provider = resolution.getProvider( child );
                if ( provider == null )
                {
                    resolved = false;
                    // this is parse phase, so only log verbose message.
                    // error is produced during resolution phase.
                    Log.verbose( "WARN: can't resolve: " + child );

                    String name;
                    String version;
                    if ( child instanceof IRequiredBundle )
                    {
                        IRequiredBundle rb = ( IRequiredBundle ) child;
                        name = rb.getSymbolicName();
                        version = rb.getVersions().toString();
                    }
                    else
                    {
                        IPackageImport pi = ( IPackageImport ) child;
                        name = "import!" + pi.getPackageName();
                        version = pi.getVersions().toString();
                    }

                    mrid = ModuleRevisionId.newInstance( "!" + SigilResolver.ORG_SIGIL, name, version );
                    dd = new SigilDependencyDescriptor( md, mrid, force, changing, transitive );
                    dd.addDependencyConfiguration( "*", "*" ); // all
                    // configurations
                    md.addDependency( dd );
                }
            }

            if ( !resolved )
            {
                // Ivy does not show warnings or errors logged from parse phase, until after resolution.
                // but <buildlist> ant task doesn't do resolution, so errors would be silently ignored.
                // Hence, we must use Message.info to ensure this failure is seen.
                if ( !quiet )
                    Log.info( "WARN: resolution failed in: " + masterMrid.getName() + ". Use -v for details." );
            }
        }


        /*
         * find named resolver, or first resolver that implements IBldResolver.
         */
        private IBldResolver findResolver()
        {
            if ( resolver == null )
            {
                Ivy ivy = IvyContext.getContext().peekIvy();
                if ( ivy != null )
                {
                    if ( resolverName != null )
                    {
                        DependencyResolver r = ivy.getSettings().getResolver( resolverName );
                        if ( r == null )
                        {
                            throw new Error( "SigilParser: resolver \"" + resolverName + "\" not defined." );
                        }
                        else if ( r instanceof IBldResolver )
                        {
                            resolver = ( IBldResolver ) r;
                        }
                        else
                        {
                            throw new Error( "SigilParser: resolver \"" + resolverName + "\" is not a SigilResolver." );
                        }
                    }
                    else
                    {
                        for ( Object r : ivy.getSettings().getResolvers() )
                        {
                            if ( r instanceof IBldResolver )
                            {
                                resolver = ( IBldResolver ) r;
                                break;
                            }
                        }
                    }

                    if ( resolver == null )
                    {
                        throw new Error( "SigilParser: can't find SigilResolver. Check ivysettings.xml." );
                    }
                }
                else if ( config != null )
                {
                    Log.warn( "creating duplicate resolver to support IvyDE." );
                    resolver = new SigilResolver();
                    ( ( SigilResolver ) resolver ).setConfig( config );
                }
            }
            return resolver;
        }
    }

}

/*
 * this is only needed so that we can distinguish sigil-added dependencies from
 * existing ones.
 */
class SigilDependencyDescriptor extends DefaultDependencyDescriptor
{
    public SigilDependencyDescriptor( DefaultModuleDescriptor md, ModuleRevisionId mrid, boolean force,
        boolean changing, boolean transitive )
    {
        super( md, mrid, force, changing, transitive );
    }
}