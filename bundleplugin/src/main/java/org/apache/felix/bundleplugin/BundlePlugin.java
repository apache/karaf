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
 
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.ZipException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.*;
import org.apache.maven.plugin.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;

import aQute.lib.osgi.*;
 
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

 protected Maven2OsgiConverter getMaven2OsgiConverter() {
  return maven2OsgiConverter;
 }

 void setMaven2OsgiConverter(Maven2OsgiConverter maven2OsgiConverter) {
  this.maven2OsgiConverter = maven2OsgiConverter;
 }

 protected MavenProject getProject() {
  return project;
 }

 public void execute() throws MojoExecutionException {
  Properties properties = new Properties();

  /* ignore project types not supported, useful when the plugin is configured in the parent pom */
  if (!supportedProjectTypes.contains(getProject().getArtifact().getType())) {
    getLog().debug("Ignoring project " + getProject().getArtifact() + " : type " + getProject().getArtifact().getType() +
                   " is not supported by bundle plugin, supported types are " + supportedProjectTypes );
    return;
  }
  
  execute(project, instructions, properties);
 }

 protected void execute(MavenProject project, Map instructions, Properties properties) throws MojoExecutionException {
  try {
    execute(project, instructions, properties, getClasspath(project));
  }
  catch ( IOException e ) {
    throw new MojoExecutionException("Error calculating classpath for project " + project, e);
  }
 }

 /* transform directives from their XML form to the expected BND syntax (eg. _include becomes -include) */
 protected Map transformDirectives(Map instructions) {
  Map transformedInstructions = new HashMap();
  for (Iterator i = instructions.entrySet().iterator(); i.hasNext();) {
    Map.Entry e = (Map.Entry)i.next();

    String key = (String)e.getKey();
    if (key.startsWith("_")) {
      key = "-"+key.substring(1);
    }

    String value = (String)e.getValue();
    if (null == value) {
      value = "";
    }

    transformedInstructions.put(key, value);
  }
  return transformedInstructions;
 }

 protected void execute(MavenProject project, Map instructions, Properties properties, Jar[] classpath) throws MojoExecutionException {
  try {
   File jarFile = new File(getBuildDirectory(), getBundleName(project));

   properties.putAll(getDefaultProperties(project));
   
   String bsn = project.getGroupId() + "." + project.getArtifactId();
   if (!instructions.containsKey(Analyzer.PRIVATE_PACKAGE)) {
     properties.put(Analyzer.EXPORT_PACKAGE, bsn + ".*");
   }

   properties.putAll(transformDirectives(instructions));
 
   // pass maven resource paths onto BND analyzer
   String mavenResourcePaths = getMavenResourcePaths();
   if (mavenResourcePaths.length() > 0) {
     final String includeResource = (String)properties.get(Analyzer.INCLUDE_RESOURCE);
     if (includeResource != null) {
       getLog().warn(Analyzer.INCLUDE_RESOURCE + ": overriding " + mavenResourcePaths + " with " + includeResource );
     } else {
       properties.put(Analyzer.INCLUDE_RESOURCE, mavenResourcePaths);
     }
   }
  
   Builder builder = new Builder();
   builder.setBase(baseDir);
   builder.setProperties(properties);
   builder.setClasspath(classpath);
 
   builder.build();
   Jar jar = builder.getJar();
   doMavenMetadata(project, jar);
   builder.setJar(jar);
 
   List errors = builder.getErrors();
   List warnings = builder.getWarnings();
 
   if (errors.size() > 0) {
    jarFile.delete();
    for (Iterator e = errors.iterator(); e.hasNext();) {
     String msg = (String) e.next();
     getLog().error("Error building bundle " + project.getArtifact() + " : " + msg);
    }
    throw new MojoFailureException("Found errors, see log");
   }
   else {
    jarFile.getParentFile().mkdirs();
    builder.getJar().write(jarFile);
    project.getArtifact().setFile(jarFile);
   }
   for (Iterator w = warnings.iterator(); w.hasNext();) {
    String msg = (String) w.next();
    getLog().warn("Warning building bundle " + project.getArtifact() + " : " + msg);
   }
 
  }
  catch (Exception e) {
   e.printStackTrace();
   throw new MojoExecutionException("Unknown error occurred", e);
  }
 }
 
 private Map getProperies(Model projectModel, String prefix, Object model) {
  Map properties = new HashMap();
  Method methods[] = Model.class.getDeclaredMethods();
  for (int i = 0; i < methods.length; i++) {
   String name = methods[i].getName();
   if ( name.startsWith("get") ) {
    try {
     Object v = methods[i].invoke(projectModel, null );
     if ( v != null ) {
      name = prefix + Character.toLowerCase(name.charAt(3)) + name.substring(4);
      if ( v.getClass().isArray() )
       properties.put( name, Arrays.asList((Object[])v).toString() );
      else
       properties.put( name, v );
       
     } 
    }
    catch (Exception e) {
     // too bad
    }
   }
  }
  return properties;
 }
 
 private StringBuffer printLicenses(List licenses) {
  if (licenses == null || licenses.size() == 0)
   return null;
  StringBuffer sb = new StringBuffer();
  String del = "";
  for (Iterator i = licenses.iterator(); i.hasNext();) {
   License l = (License) i.next();
   String url = l.getUrl();
   sb.append(del);
   sb.append(url);
   del = ", ";
  }
  return sb;
 }
 
 /**
  * @param jar
  * @throws IOException
  */
 private void doMavenMetadata(MavenProject project, Jar jar) throws IOException {
  String path = "META-INF/maven/" + project.getGroupId() + "/"
    + project.getArtifactId();
  File pomFile = new File(baseDir, "pom.xml");
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
 protected Jar[] getClasspath(MavenProject project) throws ZipException, IOException {
  List list = new ArrayList();
  
  if (getOutputDirectory() != null && getOutputDirectory().exists()) {
    list.add(new Jar(".", getOutputDirectory()));
  }
 
  Set artifacts = project.getDependencyArtifacts();
  for (Iterator it = artifacts.iterator(); it.hasNext();) {
   Artifact artifact = (Artifact) it.next();
   if (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) 
     || Artifact.SCOPE_SYSTEM.equals(artifact.getScope()) 
     || Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
    File file = getFile(artifact);
    if (file == null) {
        throw new RuntimeException("File is not available for artifact " + artifact + " in project " + project.getArtifact());
    }
    Jar jar = new Jar(artifact.getArtifactId(), file);
    list.add(jar);
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
 protected File getFile(Artifact artifact) {
  return artifact.getFile();
 }

 private void header(Properties properties, String key, Object value) {
  if (value == null)
   return;
 
  if (value instanceof Collection && ((Collection) value).isEmpty())
   return;
 
  properties.put(key, value.toString());
 }

 /**
  * Convert a Maven version into an OSGi compliant version
  * 
  * @param version Maven version
  * @return the OSGi version
  */
 protected String convertVersionToOsgi(String version)
 {
     return getMaven2OsgiConverter().getVersion( version );
 }

 /**
  * TODO this should return getMaven2Osgi().getBundleFileName( project.getArtifact() )  
  */
 protected String getBundleName(MavenProject project) {
  return project.getBuild().getFinalName() + ".jar";
 }

 public String getBuildDirectory() {
  return buildDirectory;
 }

 void setBuildDirectory(String buildirectory) {
  this.buildDirectory = buildirectory;    
 }

 /**
  * Get a list of packages inside a Jar
  * 
  * @param jar
  * @return list of package names
  */
 public List getPackages(Jar jar) {
  List packages = new ArrayList();
  for (Iterator p = jar.getDirectories().entrySet().iterator(); p.hasNext();) {
    Map.Entry directory = (Map.Entry) p.next();
    String path = (String) directory.getKey();

    String pack = path.replace('/', '.');
    packages.add(pack);
  }
  return packages;
 }

 protected Properties getDefaultProperties(MavenProject project) {
     Properties properties = new Properties();
     // Setup defaults
     String bsn = project.getGroupId() + "." + project.getArtifactId();
     properties.put(Analyzer.BUNDLE_SYMBOLICNAME, bsn);
     properties.put(Analyzer.IMPORT_PACKAGE, "*");

     String version = getMaven2OsgiConverter().getVersion( project.getVersion() );
     
     properties.put(Analyzer.BUNDLE_VERSION, version);
     header(properties, Analyzer.BUNDLE_DESCRIPTION, project
       .getDescription());
     header(properties, Analyzer.BUNDLE_LICENSE, printLicenses(project
       .getLicenses()));
     header(properties, Analyzer.BUNDLE_NAME, project.getName());
     
     if (project.getOrganization() != null) {
       header(properties, Analyzer.BUNDLE_VENDOR, project
         .getOrganization().getName());
       if (project.getOrganization().getUrl() != null) {
         header(properties, Analyzer.BUNDLE_DOCURL, project
           .getOrganization().getUrl());
       }
     }

     properties.putAll(project.getProperties());
     properties.putAll(project.getModel().getProperties());
     properties.putAll( getProperies(project.getModel(), "project.build.", project.getBuild()));
     properties.putAll( getProperies(project.getModel(), "pom.", project.getModel()));
     properties.putAll( getProperies(project.getModel(), "project.", project));
     properties.put("project.baseDir", baseDir );
     properties.put("project.build.directory", getBuildDirectory() );
     properties.put("project.build.outputdirectory", getOutputDirectory() );
     
     return properties;
 }
 
 void setBasedir(File basedir){
     this.baseDir = basedir;
 }

 File getOutputDirectory(){
     return this.outputDirectory;
 }

 void setOutputDirectory(File outputDirectory){
     this.outputDirectory = outputDirectory;
 }

 String getMavenResourcePaths()
 {
     final String basePath = baseDir.getAbsolutePath();

     StringBuffer resourcePaths = new StringBuffer();
     for (Iterator i = project.getResources().iterator(); i.hasNext();) {
         org.apache.maven.model.Resource resource = (org.apache.maven.model.Resource)i.next();

         final String sourcePath = resource.getDirectory();
         final String targetPath = resource.getTargetPath();

         // ignore empty or non-local resources
         if (new File(sourcePath).exists() && ((targetPath == null) || (targetPath.indexOf("..") < 0))) {
             String path = sourcePath;

             // make relative to basedir
             if (path.startsWith(basePath)) {
                 path = path.substring(basePath.length() + 1);
             }

             if (targetPath != null) {
                 path = targetPath + '=' + path;
             }

             if (resourcePaths.length() > 0) {
                 resourcePaths.append(',');
             }

             if (resource.isFiltering()) {
               resourcePaths.append('{');
               resourcePaths.append(path);
               resourcePaths.append('}');
             } else {
               resourcePaths.append(path);
             }
         }
     }

     return resourcePaths.toString();
 }
}
