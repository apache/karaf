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
package org.apache.felix.bundleplugin;


import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import aQute.lib.osgi.Analyzer;
import aQute.libg.header.OSGiHeader;


/**
 * Add BND directives to embed selected dependencies inside a bundle
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class DependencyEmbedder extends AbstractDependencyFilter
{
    public static final String EMBED_DEPENDENCY = "Embed-Dependency";
    public static final String EMBED_DIRECTORY = "Embed-Directory";
    public static final String EMBED_STRIP_GROUP = "Embed-StripGroup";
    public static final String EMBED_STRIP_VERSION = "Embed-StripVersion";
    public static final String EMBED_TRANSITIVE = "Embed-Transitive";

    private static final String MAVEN_DEPENDENCIES = "{maven-dependencies}";

    private String m_embedDirectory;
    private String m_embedStripGroup;
    private String m_embedStripVersion;

    /**
     * Maven logger.
     */
    private final Log m_log;

    /**
     * Inlined paths.
     */
    private final Collection m_inlinedPaths;

    /**
     * Embedded artifacts.
     */
    private final Collection m_embeddedArtifacts;


    public DependencyEmbedder( Log log, Collection dependencyArtifacts )
    {
        super( dependencyArtifacts );

        m_log = log;

        m_inlinedPaths = new LinkedHashSet();
        m_embeddedArtifacts = new LinkedHashSet();
    }


    public void processHeaders( Analyzer analyzer ) throws MojoExecutionException
    {
        StringBuffer includeResource = new StringBuffer();
        StringBuffer bundleClassPath = new StringBuffer();

        m_inlinedPaths.clear();
        m_embeddedArtifacts.clear();

        String embedDependencyHeader = analyzer.getProperty( EMBED_DEPENDENCY );
        if ( null != embedDependencyHeader && embedDependencyHeader.length() > 0 )
        {
            m_embedDirectory = analyzer.getProperty( EMBED_DIRECTORY );
            m_embedStripGroup = analyzer.getProperty( EMBED_STRIP_GROUP, "true" );
            m_embedStripVersion = analyzer.getProperty( EMBED_STRIP_VERSION );

            Map embedInstructions = OSGiHeader.parseHeader( embedDependencyHeader );
            processInstructions( embedInstructions );

            for ( Iterator i = m_inlinedPaths.iterator(); i.hasNext(); )
            {
                inlineDependency( ( String ) i.next(), includeResource );
            }
            for ( Iterator i = m_embeddedArtifacts.iterator(); i.hasNext(); )
            {
                embedDependency( ( Artifact ) i.next(), includeResource, bundleClassPath );
            }
        }

        if ( bundleClassPath.length() > 0 )
        {
            // set explicit default before merging dependency classpath
            if ( analyzer.getProperty( Analyzer.BUNDLE_CLASSPATH ) == null )
            {
                analyzer.setProperty( Analyzer.BUNDLE_CLASSPATH, "." );
            }
        }

        appendDependencies( analyzer, Analyzer.INCLUDE_RESOURCE, includeResource.toString() );
        appendDependencies( analyzer, Analyzer.BUNDLE_CLASSPATH, bundleClassPath.toString() );
    }


    @Override
    protected void processDependencies( String tag, String inline, Collection dependencies )
    {
        if ( dependencies.isEmpty() )
        {
            m_log.warn( EMBED_DEPENDENCY + ": clause \"" + tag + "\" did not match any dependencies" );
        }

        if ( null == inline || "false".equalsIgnoreCase( inline ) )
        {
            m_embeddedArtifacts.addAll( dependencies );
        }
        else
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                addInlinedPaths( ( Artifact ) i.next(), inline, m_inlinedPaths );
            }
        }
    }


    private static void addInlinedPaths( Artifact dependency, String inline, Collection inlinedPaths )
    {
        File path = dependency.getFile();
        if ( null != path && path.exists() )
        {
            if ( "true".equalsIgnoreCase( inline ) || inline.length() == 0 )
            {
                inlinedPaths.add( path.getPath() );
            }
            else
            {
                String[] filters = inline.split( "\\|" );
                for ( int i = 0; i < filters.length; i++ )
                {
                    if ( filters[i].length() > 0 )
                    {
                        inlinedPaths.add( path + "!/" + filters[i] );
                    }
                }
            }
        }
    }


    private void embedDependency( Artifact dependency, StringBuffer includeResource, StringBuffer bundleClassPath )
    {
        File sourceFile = dependency.getFile();
        if ( null != sourceFile && sourceFile.exists() )
        {
            String embedDirectory = m_embedDirectory;
            if ( "".equals( embedDirectory ) || ".".equals( embedDirectory ) )
            {
                embedDirectory = null;
            }

            if ( false == Boolean.valueOf( m_embedStripGroup ).booleanValue() )
            {
                embedDirectory = new File( embedDirectory, dependency.getGroupId() ).getPath();
            }

            File targetFile;
            if ( Boolean.valueOf( m_embedStripVersion ).booleanValue() )
            {
                String extension = dependency.getArtifactHandler().getExtension();
                if ( extension != null )
                {
                    targetFile = new File( embedDirectory, dependency.getArtifactId() + "." + extension );
                }
                else
                {
                    targetFile = new File( embedDirectory, dependency.getArtifactId() );
                }
            }
            else
            {
                targetFile = new File( embedDirectory, sourceFile.getName() );
            }

            String targetFilePath = targetFile.getPath();

            // replace windows backslash with a slash
            if ( File.separatorChar != '/' )
            {
                targetFilePath = targetFilePath.replace( File.separatorChar, '/' );
            }

            if ( includeResource.length() > 0 )
            {
                includeResource.append( ',' );
            }

            includeResource.append( targetFilePath );
            includeResource.append( '=' );
            includeResource.append( sourceFile );

            if ( bundleClassPath.length() > 0 )
            {
                bundleClassPath.append( ',' );
            }

            bundleClassPath.append( targetFilePath );
        }
    }


    private static void inlineDependency( String path, StringBuffer includeResource )
    {
        if ( includeResource.length() > 0 )
        {
            includeResource.append( ',' );
        }

        includeResource.append( '@' );
        includeResource.append( path );
    }


    public Collection getInlinedPaths()
    {
        return m_inlinedPaths;
    }


    public Collection getEmbeddedArtifacts()
    {
        return m_embeddedArtifacts;
    }


    private static void appendDependencies( Analyzer analyzer, String directiveName, String mavenDependencies )
    {
        /*
         * similar algorithm to {maven-resources} but default behaviour here is to append rather than override
         */
        final String instruction = analyzer.getProperty( directiveName );
        if ( instruction != null && instruction.length() > 0 )
        {
            if ( instruction.indexOf( MAVEN_DEPENDENCIES ) >= 0 )
            {
                // if there are no embeddded dependencies, we do a special treatment and replace
                // every occurance of MAVEN_DEPENDENCIES and a following comma with an empty string
                if ( mavenDependencies.length() == 0 )
                {
                    String cleanInstruction = BundlePlugin.removeTagFromInstruction( instruction, MAVEN_DEPENDENCIES );
                    analyzer.setProperty( directiveName, cleanInstruction );
                }
                else
                {
                    String mergedInstruction = StringUtils.replace( instruction, MAVEN_DEPENDENCIES, mavenDependencies );
                    analyzer.setProperty( directiveName, mergedInstruction );
                }
            }
            else if ( mavenDependencies.length() > 0 )
            {
                if ( Analyzer.INCLUDE_RESOURCE.equalsIgnoreCase( directiveName ) )
                {
                    // dependencies should be prepended so they can be overwritten by local resources
                    analyzer.setProperty( directiveName, mavenDependencies + ',' + instruction );
                }
                else
                // Analyzer.BUNDLE_CLASSPATH
                {
                    // for the classpath we want dependencies to be appended after local entries
                    analyzer.setProperty( directiveName, instruction + ',' + mavenDependencies );
                }
            }
            // otherwise leave instruction unchanged
        }
        else if ( mavenDependencies.length() > 0 )
        {
            analyzer.setProperty( directiveName, mavenDependencies );
        }
        // otherwise leave instruction unchanged
    }
}
