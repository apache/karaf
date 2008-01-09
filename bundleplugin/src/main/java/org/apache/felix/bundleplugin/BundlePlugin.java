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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import org.apache.maven.archiver.ManifestSection;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringInputStream;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.EmbeddedResource;
import aQute.lib.osgi.FileResource;
import aQute.lib.osgi.Jar;

/**
 * Create an OSGi bundle from Maven project
 *
 * @goal bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description build an OSGi bundle jar
 */
public class BundlePlugin extends AbstractMojo {

    /**
     * Directory where the manifest will be written
     *
     * @parameter expression="${manifestLocation}" default-value="${project.build.outputDirectory}/META-INF"
     */
    protected File manifestLocation;

    /**
     * When true, unpack the bundle contents to the outputDirectory
     *
     * @parameter expression="${unpackBundle}"
     */
    protected boolean unpackBundle;

    /**
     * When true, exclude project dependencies from the classpath passed to BND
     *
     * @parameter expression="${excludeDependencies}"
     */
    protected boolean excludeDependencies;

    /**
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Project types which this plugin supports.
     *
     * @parameter
     */
    private List supportedProjectTypes = Arrays.asList(new String[]{"jar","bundle"});

    /**
     * The directory for the generated bundles.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The directory for the pom
     *
     * @parameter expression="${basedir}"
     * @required
     */
    private File   baseDir;

    /**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String   buildDirectory;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The name of the generated JAR file.
     *
     * @parameter
     */
    private Map    instructions = new HashMap();

    /**
     * @component
     */
    private Maven2OsgiConverter maven2OsgiConverter;

    private static final String MAVEN_RESOURCES = "{maven-resources}";
    private static final String MAVEN_RESOURCES_REGEX = "\\{maven-resources\\}";
    private static final String[] EMPTY_STRING_ARRAY = {};
    private static final String[] DEFAULT_INCLUDES = {"**/**"};


    protected Maven2OsgiConverter getMaven2OsgiConverter()
    {
        return this.maven2OsgiConverter;
    }

    void setMaven2OsgiConverter(Maven2OsgiConverter maven2OsgiConverter)
    {
        this.maven2OsgiConverter = maven2OsgiConverter;
    }

    protected MavenProject getProject()
    {
        return this.project;
    }

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException
    {
        Properties properties = new Properties();

        // ignore project types not supported, useful when the plugin is configured in the parent pom
        if (!this.supportedProjectTypes.contains(this.getProject().getArtifact().getType()))
        {
            this.getLog().debug("Ignoring project " + this.getProject().getArtifact() + " : type " + this.getProject().getArtifact().getType() +
                       " is not supported by bundle plugin, supported types are " + this.supportedProjectTypes );
            return;
        }

        this.execute(this.project, this.instructions, properties);
    }

    protected void execute(MavenProject project, Map instructions, Properties properties)
    throws MojoExecutionException
    {
        try
        {
            this.execute(project, instructions, properties, this.getClasspath(project));
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException("Error calculating classpath for project " + project, e);
        }
    }

    /* transform directives from their XML form to the expected BND syntax (eg. _include becomes -include) */
    protected Map transformDirectives(Map instructions)
    {
        Map transformedInstructions = new HashMap();
        for (Iterator i = instructions.entrySet().iterator(); i.hasNext();)
        {
            Map.Entry e = (Map.Entry)i.next();

            String key = (String)e.getKey();
            if (key.startsWith("_"))
            {
                key = "-"+key.substring(1);
            }

            String value = (String)e.getValue();
            if (null == value)
            {
                value = "";
            }
            else
            {
                value = value.replaceAll("[\r\n]", "");
            }

            transformedInstructions.put(key, value);
        }
        return transformedInstructions;
    }

    protected void execute(MavenProject project, Map instructions, Properties properties, Jar[] classpath)
    throws MojoExecutionException
    {
        try
        {
            File jarFile = new File(this.getBuildDirectory(), this.getBundleName(project));

            properties.putAll(this.getDefaultProperties(project));

            String bsn = project.getGroupId() + "." + project.getArtifactId();
            if (!instructions.containsKey(Analyzer.PRIVATE_PACKAGE))
            {
                 properties.put(Analyzer.EXPORT_PACKAGE, bsn + ".*");
            }

            properties.putAll(this.transformDirectives(instructions));

            // pass maven resource paths onto BND analyzer
            final String mavenResourcePaths = this.getMavenResourcePaths(project);
            final String includeResource = (String)properties.get(Analyzer.INCLUDE_RESOURCE);
            if (includeResource != null)
            {
                if (includeResource.indexOf(MAVEN_RESOURCES) >= 0)
                {
                    // if there is no maven resource path, we do a special treatment and replace
                    // every occurance of MAVEN_RESOURCES and a following comma with an empty string
                    if ( mavenResourcePaths.length() == 0 )
                    {
                        String cleanedResource = removeMavenResourcesTag( includeResource );
                        if ( cleanedResource.length() > 0 )
                        {
                            properties.put(Analyzer.INCLUDE_RESOURCE, cleanedResource);
                        }
                        else
                        {
                            properties.remove(Analyzer.INCLUDE_RESOURCE);
                        }
                    }
                    else
                    {
                        String combinedResource = includeResource.replaceAll(MAVEN_RESOURCES_REGEX, mavenResourcePaths);
                        properties.put(Analyzer.INCLUDE_RESOURCE, combinedResource);
                    }
                }
                else if ( mavenResourcePaths.length() > 0 )
                {
                    this.getLog().warn(Analyzer.INCLUDE_RESOURCE + ": overriding " + mavenResourcePaths + " with " +
                        includeResource + " (add " + MAVEN_RESOURCES + " if you want to include the maven resources)");
                }
            }
            else if (mavenResourcePaths.length() > 0 )
            {
                properties.put(Analyzer.INCLUDE_RESOURCE, mavenResourcePaths);
            }

            Builder builder = new Builder();
            builder.setBase(project.getBasedir());
            builder.setProperties(properties);
            builder.setClasspath(classpath);

            Collection embeddableArtifacts = getEmbeddableArtifacts(project, properties);
            if (embeddableArtifacts.size() > 0)
            {
                // add BND instructions to embed selected dependencies
                new DependencyEmbedder(embeddableArtifacts).processHeaders(properties);
            }

            builder.build();
            Jar jar = builder.getJar();
            this.doMavenMetadata(project, jar);
            builder.setJar(jar);

            List errors = builder.getErrors();
            List warnings = builder.getWarnings();

            for (Iterator w = warnings.iterator(); w.hasNext();)
            {
                String msg = (String) w.next();
                this.getLog().warn("Warning building bundle " + project.getArtifact() + " : " + msg);
            }
            for (Iterator e = errors.iterator(); e.hasNext();)
            {
                 String msg = (String) e.next();
                 this.getLog().error("Error building bundle " + project.getArtifact() + " : " + msg);
            }

            if (errors.size() > 0)
            {
                String failok = properties.getProperty( "-failok" );
                if (null == failok || "false".equalsIgnoreCase( failok ))
                {
                    jarFile.delete();

                    throw new MojoFailureException("Error(s) found in bundle configuration");
                }
            }

            try
            {
                /*
                 * Grab customized manifest entries from the maven-jar-plugin configuration
                 */
                MavenArchiveConfiguration archiveConfig = JarPluginConfiguration.getArchiveConfiguration( project );
                String mavenManifestText = new MavenArchiver().getManifest( project, archiveConfig ).toString();
                Manifest mavenManifest = new Manifest();

                // First grab the external manifest file (if specified)
                File externalManifestFile = archiveConfig.getManifestFile();
                if ( null != externalManifestFile && externalManifestFile.exists() )
                {
                    InputStream mis = new FileInputStream( externalManifestFile );
                    mavenManifest.read( mis );
                    mis.close();
                }

                // Then apply the customized entries from the jar plugin
                mavenManifest.read( new StringInputStream( mavenManifestText ) );

                if ( !archiveConfig.isManifestSectionsEmpty() )
                {
                    /*
                     * Add customized manifest sections (for some reason MavenArchiver doesn't do this for us)
                     */
                    List sections = archiveConfig.getManifestSections();
                    for ( Iterator i = sections.iterator(); i.hasNext(); )
                    {
                        ManifestSection section = (ManifestSection) i.next();
                        Attributes attributes = new Attributes();

                        if ( !section.isManifestEntriesEmpty() )
                        {
                            Map entries = section.getManifestEntries();
                            for ( Iterator j = entries.entrySet().iterator(); j.hasNext(); )
                            {
                                Map.Entry entry = (Map.Entry) j.next();
                                attributes.putValue( (String)entry.getKey(), (String)entry.getValue() );
                            }
                        }

                        mavenManifest.getEntries().put( section.getName(), attributes );
                    }
                }

                /*
                 * Overlay generated bundle manifest with customized entries
                 */
                Manifest bundleManifest = jar.getManifest();
                bundleManifest.getMainAttributes().putAll( mavenManifest.getMainAttributes() );
                bundleManifest.getMainAttributes().putValue( "Created-By", "Apache Maven Bundle Plugin" );
                bundleManifest.getEntries().putAll( mavenManifest.getEntries() );
                jar.setManifest( bundleManifest );
            }
            catch (Exception e)
            {
                getLog().warn( "Unable to merge Maven manifest: " + e.getLocalizedMessage() );
            }
            
            jarFile.getParentFile().mkdirs();
            builder.getJar().write(jarFile);
            Artifact bundleArtifact = project.getArtifact();
            bundleArtifact.setFile(jarFile);

            if (unpackBundle)
            {
                File outputDir = this.getOutputDirectory();
                if (null == outputDir)
                {
                    outputDir = new File( this.getBuildDirectory(), "classes" );
                }

                try
                {
                    /*
                     * this directory must exist before unpacking, otherwise the plexus
                     * unarchiver decides to use the current working directory instead!
                     */
                    if (!outputDir.exists())
                    {
                        outputDir.mkdirs();
                    }

                    UnArchiver unArchiver = archiverManager.getUnArchiver( "jar" );
                    unArchiver.setDestDirectory( outputDir );
                    unArchiver.setSourceFile( jarFile );
                    unArchiver.extract();
                }
                catch ( Exception e )
                {
                    getLog().error( "Problem unpacking " + jarFile + " to " + outputDir, e );
                }
            }

            if (manifestLocation != null)
            {
                File outputFile = new File( manifestLocation, "MANIFEST.MF" );

                try
                {
                    Manifest manifest = builder.getJar().getManifest();
                    ManifestPlugin.writeManifest( manifest, outputFile );
                }
                catch ( IOException e )
                {
                    getLog().error( "Error trying to write Manifest to file " + outputFile, e );
                }
            }

            // workaround for MNG-1682: force maven to install artifact using the "jar" handler
            bundleArtifact.setArtifactHandler( artifactHandlerManager.getArtifactHandler( "jar" ) );
        }
        catch (MojoFailureException e)
        {
            getLog().error( e.getLocalizedMessage() );
            throw new MojoExecutionException( "Error(s) found in bundle configuration", e );
        }
        catch (Exception e)
        {
            getLog().error( "An internal error occurred", e );
            throw new MojoExecutionException( "Internal error in maven-bundle-plugin", e );
        }
    }

    private String removeMavenResourcesTag( String includeResource )
    {
        StringBuffer buf = new StringBuffer();

        String[] clauses = includeResource.split(",");
        for (int i = 0; i < clauses.length; i++)
        {
            String clause = clauses[i].trim();
            if (!MAVEN_RESOURCES.equals(clause))
            {
                if (buf.length() > 0)
                {
                    buf.append(',');
                }
                buf.append(clause);
            }
        }

        return buf.toString();
    }

    private Map getProperies(Model projectModel, String prefix, Object model)
    {
        Map properties = new HashMap();
        Method methods[] = Model.class.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++)
        {
            String name = methods[i].getName();
            if ( name.startsWith("get") )
            {
                try
                {
                    Object v = methods[i].invoke(projectModel, null );
                    if ( v != null )
                    {
                        name = prefix + Character.toLowerCase(name.charAt(3)) + name.substring(4);
                        if ( v.getClass().isArray() )
                            properties.put( name, Arrays.asList((Object[])v).toString() );
                        else
                            properties.put( name, v );

                    }
                }
                catch (Exception e)
                {
                    // too bad
                }
           }
        }
        return properties;
    }

    private StringBuffer printLicenses(List licenses)
    {
        if (licenses == null || licenses.size() == 0)
            return null;
        StringBuffer sb = new StringBuffer();
        String del = "";
        for (Iterator i = licenses.iterator(); i.hasNext();)
        {
            License l = (License) i.next();
            String url = l.getUrl();
            if (url == null) continue;
            sb.append(del);
            sb.append(url);
            del = ", ";
        }
        if (sb.length() == 0) return null;
        return sb;
    }

    /**
     * @param jar
     * @throws IOException
     */
    private void doMavenMetadata(MavenProject project, Jar jar) throws IOException {
        String path = "META-INF/maven/" + project.getGroupId() + "/"
              + project.getArtifactId();
        File pomFile = new File(this.baseDir, "pom.xml");
        jar.putResource(path + "/pom.xml", new FileResource(pomFile));

        Properties p = new Properties();
        p.put("version", project.getVersion());
        p.put("groupId", project.getGroupId());
        p.put("artifactId", project.getArtifactId());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.store(out, "Generated by org.apache.felix.plugin.bundle");
        jar.putResource(path + "/pom.properties", new EmbeddedResource(out
           .toByteArray(), System.currentTimeMillis()));
    }

    /**
     * @return
     * @throws ZipException
     * @throws IOException
     */
    protected Jar[] getClasspath(MavenProject project) throws ZipException, IOException
    {
        List list = new ArrayList();

        if (this.getOutputDirectory() != null && this.getOutputDirectory().exists())
        {
            list.add(new Jar(".", this.getOutputDirectory()));
        }

        final Set artifacts;
        if (excludeDependencies)
        {
            artifacts = Collections.EMPTY_SET;
        }
        else
        {
            artifacts = project.getArtifacts();
        }

        for (Iterator it = artifacts.iterator(); it.hasNext();)
        {
            Artifact artifact = (Artifact) it.next();
            if (artifact.getArtifactHandler().isAddedToClasspath())
            {
                if (Artifact.SCOPE_COMPILE.equals(artifact.getScope())
                    || Artifact.SCOPE_SYSTEM.equals(artifact.getScope())
                    || Artifact.SCOPE_PROVIDED.equals(artifact.getScope()))
                {
                    File file = this.getFile(artifact);
                    if (file == null)
                    {
                        getLog().warn( "File is not available for artifact " + artifact + " in project " + project.getArtifact() );
                        continue;
                    }
                    Jar jar = new Jar(artifact.getArtifactId(), file);
                    list.add(jar);
                }
            }
        }
        Jar[] cp = new Jar[list.size()];
        list.toArray(cp);
        return cp;
    }

    /**
     * Get the file for an Artifact
     *
     * @param artifact
     */
    protected File getFile(Artifact artifact)
    {
        return artifact.getFile();
    }

    private void header(Properties properties, String key, Object value)
    {
        if (value == null)
            return;

        if (value instanceof Collection && ((Collection) value).isEmpty())
            return;

        properties.put(key, value.toString().replaceAll("[\r\n]", ""));
    }

    /**
     * Convert a Maven version into an OSGi compliant version
     *
     * @param version Maven version
     * @return the OSGi version
     */
    protected String convertVersionToOsgi(String version)
    {
        return this.getMaven2OsgiConverter().getVersion( version );
    }

    /**
     * TODO this should return getMaven2Osgi().getBundleFileName( project.getArtifact() )
     */
    protected String getBundleName(MavenProject project)
    {
        return project.getBuild().getFinalName() + ".jar";
    }

    protected String getBuildDirectory()
    {
        return this.buildDirectory;
    }

    void setBuildDirectory(String buildirectory)
    {
        this.buildDirectory = buildirectory;
    }

    protected Properties getDefaultProperties(MavenProject project)
    {
        Properties properties = new Properties();

        String bsn;
        try
        {
            bsn = maven2OsgiConverter.getBundleSymbolicName( project.getArtifact() );
        }
        catch (Exception e)
        {
            bsn = project.getGroupId() + "." + project.getArtifactId();
        }

        // Setup defaults
        properties.put(Analyzer.BUNDLE_SYMBOLICNAME, bsn);
        properties.put(Analyzer.IMPORT_PACKAGE, "*");
        properties.put(Analyzer.BUNDLE_VERSION, project.getVersion());

        // remove the verbose Include-Resource entry from generated manifest
        properties.put(Analyzer.REMOVE_HEADERS, Analyzer.INCLUDE_RESOURCE);

        this.header(properties, Analyzer.BUNDLE_DESCRIPTION, project
           .getDescription());
        StringBuffer licenseText = this.printLicenses(project.getLicenses());
        if (licenseText != null) {
            this.header(properties, Analyzer.BUNDLE_LICENSE, licenseText);
        }
        this.header(properties, Analyzer.BUNDLE_NAME, project.getName());

        if (project.getOrganization() != null)
        {
            this.header(properties, Analyzer.BUNDLE_VENDOR, project
                .getOrganization().getName());
            if (project.getOrganization().getUrl() != null)
            {
                this.header(properties, Analyzer.BUNDLE_DOCURL, project
                      .getOrganization().getUrl());
            }
        }

        properties.putAll(project.getProperties());
        properties.putAll(project.getModel().getProperties());
        properties.putAll( this.getProperies(project.getModel(), "project.build.", project.getBuild()));
        properties.putAll( this.getProperies(project.getModel(), "pom.", project.getModel()));
        properties.putAll( this.getProperies(project.getModel(), "project.", project));
        properties.put("project.baseDir", this.baseDir );
        properties.put("project.build.directory", this.getBuildDirectory() );
        properties.put("project.build.outputdirectory", this.getOutputDirectory() );

        return properties;
    }

    void setBasedir(File basedir)
    {
        this.baseDir = basedir;
    }

    File getOutputDirectory()
    {
        return this.outputDirectory;
    }

    void setOutputDirectory(File outputDirectory)
    {
        this.outputDirectory = outputDirectory;
    }

    String getMavenResourcePaths(MavenProject project)
    {
        final String basePath = project.getBasedir().getAbsolutePath();

        StringBuffer resourcePaths = new StringBuffer();
        for (Iterator i = project.getResources().iterator(); i.hasNext();)
        {
            org.apache.maven.model.Resource resource = (org.apache.maven.model.Resource)i.next();

            final String sourcePath = resource.getDirectory();
            final String targetPath = resource.getTargetPath();

            // ignore empty or non-local resources
            if (new File(sourcePath).exists() && ((targetPath == null) || (targetPath.indexOf("..") < 0)))
            {
                DirectoryScanner scanner = new DirectoryScanner();

                scanner.setBasedir( resource.getDirectory() );
                if ( resource.getIncludes() != null && !resource.getIncludes().isEmpty() )
                {
                    scanner.setIncludes( (String[]) resource.getIncludes().toArray( EMPTY_STRING_ARRAY ) );
                }
                else
                {
                    scanner.setIncludes( DEFAULT_INCLUDES );
                }

                if ( resource.getExcludes() != null && !resource.getExcludes().isEmpty() )
                {
                    scanner.setExcludes( (String[]) resource.getExcludes().toArray( EMPTY_STRING_ARRAY ) );
                }

                scanner.addDefaultExcludes();
                scanner.scan();

                List includedFiles = Arrays.asList( scanner.getIncludedFiles() );

                for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
                {
                    String name = (String) j.next();
                    String path = sourcePath + '/' + name;

                    // make relative to project
                    if (path.startsWith(basePath))
                    {
                        if ( path.length() == basePath.length() )
                        {
                            path = ".";
                        }
                        else
                        {
                            path = path.substring( basePath.length() + 1 );
                        }
                    }

                    // replace windows backslash with a slash
                    // this is a workaround for a problem with bnd 0.0.189
                    if ( File.separatorChar != '/' )
                    {
                        name = name.replace(File.separatorChar, '/');
                        path = path.replace(File.separatorChar, '/');
                    }

                    // copy to correct place
                    path = name + '=' + path;
                    if (targetPath != null)
                    {
                        path = targetPath + '/' + path;
                    }

                    if (resourcePaths.length() > 0)
                    {
                        resourcePaths.append(',');
                    }

                    if (resource.isFiltering())
                    {
                        resourcePaths.append('{');
                        resourcePaths.append(path);
                        resourcePaths.append('}');
                    }
                    else
                    {
                        resourcePaths.append(path);
                    }
                }                
            }
        }

        return resourcePaths.toString();
    }

    Collection getEmbeddableArtifacts(MavenProject project, Properties properties)
    {
        String embedTransitive = properties.getProperty(DependencyEmbedder.EMBED_TRANSITIVE);
        if (Boolean.valueOf(embedTransitive).booleanValue())
        {
            // includes transitive dependencies
            return project.getArtifacts();
        }
        else
        {
            // only includes direct dependencies
            return project.getDependencyArtifacts();
        }
    }
}
