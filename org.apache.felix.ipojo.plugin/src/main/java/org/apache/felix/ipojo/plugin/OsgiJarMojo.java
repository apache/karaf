/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.plugin;

//import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.ipojo.manipulation.Manipulator;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
//import org.apache.felix.ipojo.parser.KXml2SAXParser;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
//import org.apache.felix.ipojo.parser.XMLGenericMetadataParser;
import org.apache.felix.ipojo.parser.XMLMetadataParser;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
//import org.xmlpull.v1.XmlPullParserException;

/**
 * Package an OSGi jar "bundle."
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Apache Felix Project</a>
 * @version $Rev$, $Date$
 * @goal ipojo-bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description build an OSGi bundle jar
 */
public class OsgiJarMojo extends AbstractMojo {
	private static final String[]		EMPTY_STRING_ARRAY		= {};

	int									bundleManifestVersion	= 1;

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject				project;

	/**
	 * The directory for the generated JAR.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private String						buildDirectory;

	/**
	 * The directory containing generated classes.
	 * 
	 * @parameter expression="${project.build.outputDirectory}"
	 * @required
	 * @readonly
	 */
	private File						outputDirectory;

	/**
	 * The name of the generated JAR file.
	 * 
	 * @parameter alias="jarName" expression="${project.build.finalName}"
	 * @required
	 */
	private String						jarName;

	/**
	 * The Jar archiver.
	 * 
	 * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
	 * @required
	 */
	private JarArchiver					jarArchiver;

	/**
	 * The maven archive configuration to use.
	 */
	private MavenArchiveConfiguration	archiveConfig			= new MavenArchiveConfiguration();

	/**
	 * The comma separated list of tokens to include in the JAR. Default is
	 * '**'.
	 * 
	 * @parameter alias="includes"
	 */
	private String						jarSourceIncludes		= "**";

	/**
	 * The comma separated list of tokens to exclude from the JAR.
	 * 
	 * @parameter alias="excludes"
	 */
	private String						jarSourceExcludes;

	/**
	 * @parameter
	 */
	private String						manifestFile;

	/**
	 * @parameter expression="${org.apache.felix.ipojo.tools.maven.plugin.OsgiManifest}"
	 */
	private OsgiManifest				osgiManifest;
	
	private String[][] namespaces; 

	/**
	 * Execute this Mojo
	 * 
	 * @throws MojoExecutionException
	 */
	public void execute() throws MojoExecutionException {
		File jarFile = new File(buildDirectory, jarName + ".jar");

		try {
			performPackaging(jarFile);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error assembling JAR bundle", e);
		}
	}

	/**
	 * Generates the JAR bundle file.
	 * 
	 * @param jarFile the target JAR file
	 * @throws IOException
	 * @throws ArchiverException
	 * @throws ManifestException
	 * @throws DependencyResolutionRequiredException
	 */
	private void performPackaging(File jarFile) throws IOException,
			ArchiverException, ManifestException,
			DependencyResolutionRequiredException, MojoExecutionException {

		verifyDeclaredBundleManifestVersion();

		getLog().info("Generating JAR bundle " + jarFile.getAbsolutePath());

		MavenArchiver archiver = new MavenArchiver();

		archiver.setArchiver(jarArchiver);
		archiver.setOutputFile(jarFile);

		addManifestFile();
		addManifestEntries();

		// Add the JARs that were specified in the POM
		// as "not" provided
		addEmbeddedJars();
		addBundleVersion();
		
		// Insert iPOJO Manipulation
		iPojoManipulation();

		jarArchiver.addDirectory(outputDirectory, getIncludes(), getExcludes());

		// Parse the output directory as if it was a JAR file.
		// This creates special entries for classes, packageinfo
		// and embedded JAR files (which are parsed as well).
		Jar mainJar = new Jar(null, jarName, outputDirectory);

		// Calculate the Bundle Classpath from the embedded
		// JAR files. We hardcode the bcp as ., <embedded jars>
		// TODO we add all the found JARs to the Bcp, maybe we
		// should look if they are needed by traversing the imports.
		List bundleClassPath = getBundleClassPath(mainJar);
		bundleClassPath.add(0, ".");
		createBundleClasspathHeader(bundleClassPath);

		// Calculate the exports (contained) and imports (referred)
		// The bundleClassPath contains the JARs in the right order
		Set contained = new HashSet(); // package name
		Set referred = new HashSet(); // package name
		Map uses = new HashMap(); // package name => Set of package name

		// Iterate over the bundle class path and calculate the contained
		// and referred packages as well as the uses.
		for (Iterator i = bundleClassPath.iterator(); i.hasNext();) {
			String path = (String) i.next();
			Jar jar = path.equals(".") ? mainJar : (Jar) mainJar.resources
					.get(path);
			analyzeJar(jar, contained, referred, uses);
		}

		referred.removeAll(contained);

		Map exports = parseHeader(osgiManifest.getExportPackage());
		Map imports = parseHeader(osgiManifest.getImportPackage());
		Map dynamicImports = parseHeader(osgiManifest.getDynamicImportPackage());

		if (dynamicImports != null) {
			// Remove any dynamic imports from the referred set.
			referred = new HashSet(referred);
			referred.removeAll(dynamicImports.keySet());
		}

		if (exports != null) {
			verifyDeclaredExports(exports, contained);
			createExportHeader(exports, uses);
		}

		// If the POM file contains an import declaration,
		// we verify its validity. Otherwise, we generate the
		// import package header from the referred. Exports
		// are added to automatically imports for R4 bundles.
		if (imports == null) {
			createImportHeader(referred, exports == null ? new HashSet()
					: exports.keySet());
		}
		else {
			verifyDeclaredImports(referred, imports);
		}

		//verifyBundleActivator(mainJar); // Replace it by setBundleActivator
		
		
		archiver.createArchive(project, archiveConfig);
		project.getArtifact().setFile(jarFile);
	}
	
	private Element[] parseXMLMetadata(String path) throws MojoExecutionException {
		File metadata = new File(outputDirectory+path);
		URL url;
		Element[] components = null;
		try {
			url = metadata.toURI().toURL();
			if (url == null) { 
				getLog().error("No metadata at : " + outputDirectory+path);
				throw new MojoExecutionException("[iPOJO] No metadata at : " + outputDirectory+path); 
			}
			
			InputStream stream = url.openStream();

//	        //Open the file and parse :
//			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
//		    XMLGenericMetadataParser handler = new XMLGenericMetadataParser();
//		    KXml2SAXParser parser;
//			parser = new KXml2SAXParser(in);
//			parser.parseXML(handler);
//		    stream.close();
			
			XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
			XMLMetadataParser handler = new XMLMetadataParser();
			parser.setContentHandler(handler);
			InputSource is = new InputSource(stream);
			parser.parse(is);
		    
		    components = handler.getComponentsMetadata();

		    
		} catch (MalformedURLException e) {
			getLog().error("Malformed URL for " + outputDirectory+path+ "("+e.getMessage()+")");
			throw new MojoExecutionException("[iPOJO] Malformed URL for " + outputDirectory+path);
		} catch (IOException e) { 
        	getLog().error("Cannot open the file : " + outputDirectory+path + "("+e.getMessage()+")");
			throw new MojoExecutionException("[iPOJO] Cannot open the file : " + outputDirectory+path);
//        } catch (XmlPullParserException e) {
//        	getLog().error("Error when  parsing the XML file " + outputDirectory+path+ "("+e.getMessage()+")");
//			throw new MojoExecutionException("[iPOJO] Error when  parsing the XML file " + outputDirectory+path);
		} catch (Exception e) {
			getLog().error("Parsing Error when parsing the XML file " + outputDirectory+path+ "("+e.getMessage()+")");
			throw new MojoExecutionException("[iPOJO] Parsing Error when parsing the XML file " + outputDirectory+path);
		}
		
		if(components == null || components.length == 0) {
			getLog().error("No component in " + outputDirectory+path);
			throw new MojoExecutionException("[iPOJO] No component in " + outputDirectory+path);
		}
		
		return components;
	}
	
	private Element[] parseManifestMetadata(String metadata) throws MojoExecutionException {
		Element[] components = null;
		ManifestMetadataParser parser = new ManifestMetadataParser();
    	try {
			parser.parse(metadata);
			components = parser.getComponentsMetadata();
		} catch (ParseException e) {
			getLog().error("Parsing Error when parsing the Manifest metadata " +  metadata + "("+e.getMessage()+")");
			throw new MojoExecutionException("[iPOJO] Parsing Error when parsing the Manifest metadata " +  metadata);
			
		}
    	
		if(components == null || components.length == 0) {
			getLog().error("No component in " + metadata);
			throw new MojoExecutionException("[iPOJO] No component in " + metadata);
		}
        
        return components;
	}
	
	private void iPojoManipulation() throws MojoExecutionException {
		//Try to read the content of a file of the ouptut directory
		getLog().info("iPOJO Manipulation ...");
		
		Element[] components = null;
		
		// Get the metadata.xml location 
		String path = (String) osgiManifest.getEntries().get("iPOJO-Metadata");
		
		if(path != null) {
			if(!path.startsWith("/")) { path = "/" + path; }
			components = parseXMLMetadata(path);
		} else {
			String meta_ = (String) osgiManifest.getEntries().get("iPOJO-Components");
			if(meta_ != null) {
				components = parseManifestMetadata(meta_);
			} else {
				getLog().error("Neither iPOJO-Metadata nor iPOJO-Components are in the manifest, please in the osgi-bundle packaging instead og ipojo-bundle");
				throw new MojoExecutionException("[iPOJO] Neither iPOJO-Metadata nor iPOJO-Components are in the manifest");
			}
				
		}
		
			
		Manipulator manipulator = new Manipulator();
		String[] metadata = new String[components.length];
		String meta = "";
		if(namespaces == null) { namespaces = new String[components.length][]; }
        for(int i = 0; i < components.length; i++) {
        	getLog().info("Component Class Name : " + components[i].getAttribute("className"));
        	namespaces[i] = components[i].getNamespaces();
        	try {
				manipulator.preProcess(components[i].getAttribute("className"), outputDirectory);
			} catch (Exception e) {
				getLog().error("Manipulation error in the class : " + components[i].getAttribute("className") + "("+e.getMessage()+")");
				throw new MojoExecutionException("[iPOJO] Manipulation error in the class : " + components[i].getAttribute("className"));
			}
        	
        	getLog().info("Add manipulation metadata for : " + components[i].getAttribute("className"));
        	// Insert information to metadata
        	Element elem = new Element("Manipulation", "");
        	for(int j = 0; j < manipulator.getInterfaces().length; j++) {
        		// Create an interface element for each implemented interface
        		Element itf = new Element("Interface", "");
        		Attribute att =new Attribute("name", manipulator.getInterfaces()[j]);
        		itf.addAttribute(att);
        		elem.addElement(itf);
        	}

        	for(Iterator it = manipulator.getFields().keySet().iterator(); it.hasNext(); ) {
        		Element field = new Element("Field", "");
        		String name = (String) it.next();
        		String type = (String) manipulator.getFields().get(name);
        		Attribute attName =new Attribute("name", name);
        		Attribute attType =new Attribute("type", type);
        		field.addAttribute(attName);
        		field.addAttribute(attType);
        		elem.addElement(field);
        	}

        	components[i].addElement(elem);
        	
        	// Transform the metadate to manifest metadata
        	metadata[i] = buildManifestMetadata(components[i], "");
        	meta = meta + metadata[i];
        }
        
        getLog().info("Metadata of the bundles : " + meta);
	    archiveConfig.addManifestEntry("iPOJO-Components", meta);
	        
	    getLog().info("Set the bundle activator");
	    setBundleActivator();
			
	    getLog().info("iPOJO Manipulation ... SUCCESS");
		
	}

	private String buildManifestMetadata(Element element, String actual) {
		String result="";
		if(element.getNameSpace().equals("")) { result = actual + element.getName() + " { "; }
		else { result = actual + element.getNameSpace()+":"+element.getName() + " { ";	}
		
		for(int i = 0; i < element.getAttributes().length; i++) {
			if(element.getAttributes()[i].getNameSpace().equals("")) { 
				result = result + "$" + element.getAttributes()[i].getName() + "="+element.getAttributes()[i].getValue() + " ";
			}
			else {
				result = result + "$" + element.getAttributes()[i].getNameSpace()+ ":" + element.getAttributes()[i].getName() + "="+element.getAttributes()[i].getValue() + " ";
			}
		}
		for(int i = 0; i < element.getElements().length; i++) {
			result = buildManifestMetadata(element.getElements()[i], result);
		}
		return result +"}";
	}

	private void setBundleActivator() throws MojoExecutionException {
		archiveConfig.addManifestEntry("Bundle-Activator", "org.apache.felix.ipojo.Activator");		
	}

//	private void verifyBundleActivator(Jar mainJar) {
//		String ba = osgiManifest.getBundleActivator();
//		if (ba == null || ba.trim().length() == 0) {
//			switch ( mainJar.activators.size() ) {
//				case 0: break;
//				case 1: archiveConfig.addManifestEntry("Bundle-Activator", mainJar.activators.get(0));
//				break;
//				default:
//					getLog().info("[OSGi] No Bundle-Activator specified and multiple found" );
//				break;
//			}
//		}
//		else {
//			if( ! mainJar.activators.contains(ba))
//				getLog().warn("[OSGi] UNABLE TO VERIFY BUNDLE ACTIVATOR: " + ba);
//		}
//	}

	private void createBundleClasspathHeader(List bundleClassPath) {
		StringBuffer sb = new StringBuffer();
		String del = ".,";
		for (Iterator i = bundleClassPath.iterator(); i.hasNext();) {
			sb.append(del);
			sb.append(i.next());
			del = ",";
		}
		if (sb.length() > 0)
			archiveConfig.addManifestEntry("Bundle-Classpath", sb.toString());
	}

	/**
	 * Iterate over the declared exports from the POM, verify that they are
	 * present, add the uses clause if necessary and finally add the manifest
	 * entry.
	 * 
	 * @param contained Set of contained packages
	 * @param exports Map with the export clauses from the POM
	 * @param uses Map with use clauses
	 * @throws MojoExecutionException
	 */
	void verifyDeclaredExports(Map exports, Set contained)
			throws MojoExecutionException {
		Set declaredExports = exports.keySet();
		for (Iterator i = declaredExports.iterator(); i.hasNext();) {
			String pack = (String) i.next();
			if (!contained.contains(pack)) {
				getLog()
						.error("[OSGi] EXPORTED PACKAGE NOT IN BUNDLE: " + pack);
				throw new MojoExecutionException(
						"Exported Package not found in bundle or JARs on bundle class path "
								+ pack);
			}

		}
	}

	/**
	 * Print out the export headers after adding the uses clause.
	 * 
	 * @param exports
	 * @param uses
	 * @throws MojoExecutionException
	 */
	void createExportHeader(Map exports, Map uses)
			throws MojoExecutionException {
		if (exports.size() > 0) {
			Set declaredExports = exports.keySet();
			for (Iterator i = declaredExports.iterator(); i.hasNext();) {
				String pack = (String) i.next();
				Map clause = (Map) exports.get(pack);

				if (bundleManifestVersion >= 2) {
					Set t = (Set) uses.get(pack);
					if (t != null && !t.isEmpty()) {
						StringBuffer sb = new StringBuffer();
						String del = "\"";
						for (Iterator u = t.iterator(); u.hasNext();) {
							String usedPackage = (String) u.next();
							if (!usedPackage.equals(pack)) {
								sb.append(del);
								sb.append(usedPackage);
								del = ",";
							}
						}
						sb.append("\"");
						clause.put("uses:", sb.toString());
					}
				}
			}
			archiveConfig.addManifestEntry(
					"Export-Package",
					printClauses(exports));
		}
	}

	/**
	 * Verify that the declared imports match the referred packages.
	 * 
	 * @param referred referred package
	 * @param imports imported packages from POM
	 * @throws MojoExecutionException
	 */
	void verifyDeclaredImports(Set referred, Map imports)
			throws MojoExecutionException {
		Set declaredImports = imports.keySet();
		Set test = new HashSet(referred);
		test.removeAll(declaredImports);
		for (Iterator m = test.iterator(); m.hasNext();) {
			Object o = m.next();
			if(o.equals("org.apache.felix.ipojo")) { break; } // Skip iPOJO, it will be add at the end of the packaging
			getLog().warn("[OSGi] MISSING IMPORT: " + o);
			//throw new MojoExecutionException("Missing Import " + o);
		}

		test = new HashSet(declaredImports);
		test.removeAll(referred);
		for (Iterator m = test.iterator(); m.hasNext();) {
			getLog().warn("[OSGi] SUPERFLUOUS IMPORT: " + m.next());
			getLog()
					.warn(
							"[OSGi] Removing the POM Import-Package element will automatically generate the import clauses");
		}
	}

	/**
	 * Standard OSGi header parser. This parser can handle the format clauses
	 * ::= clause ( ',' clause ) + clause ::= name ( ';' name ) (';' key '='
	 * value )
	 * 
	 * This is mapped to a Map { name => Map { attr|directive => value } }
	 * 
	 * @param value
	 * @return
	 * @throws MojoExecutionException
	 */
	static Map parseHeader(String value) throws MojoExecutionException {
		if (value == null || value.trim().length() == 0)
			return null;

		Map result = new HashMap();
		QuotedTokenizer qt = new QuotedTokenizer(value, ";=,");
		char del;
		do {
			boolean hadAttribute = false;
			Map clause = new HashMap();
			List aliases = new ArrayList();
			aliases.add(qt.nextToken());
			del = qt.getSeparator();
			while (del == ';') {
				String adname = qt.nextToken();
				if (qt.getSeparator() != '=') {
					if (hadAttribute)
						throw new MojoExecutionException(
								"Header contains name field after attribute or directive: "
										+ adname + " from " + value);
					aliases.add(adname);
				}
				else {
					String advalue = qt.nextToken();
					clause.put(adname, advalue);
					del = qt.getSeparator();
					hadAttribute = true;
				}
			}
			for (Iterator i = aliases.iterator(); i.hasNext();) {
				result.put(i.next(), clause);
			}
		} while (del == ',');
		return result;
	}

	/**
	 * Create the import header, taking into account R4 automatic import clauses
	 * for the exports.
	 * 
	 * @param referred
	 * @param contained
	 */
	void createImportHeader(Set referred, Set contained) {
		if (referred.isEmpty())
			return;

		referred = new TreeSet(referred);

		if (bundleManifestVersion > 1) {
			referred.addAll(contained);
		}

		StringBuffer sb = new StringBuffer();
		String del = "";

		for (Iterator i = referred.iterator(); i.hasNext();) {
			sb.append(del);
			sb.append(i.next());
			del = ", ";
		}
		
		// Add handler import
		for(int j = 0; j < namespaces.length; j++) {
			for(int k = 0; k < namespaces[j].length; k++) {
				if(! namespaces[j][k].equals("")) {
					int lastIndex = namespaces[j][k].lastIndexOf('.');
					String ns = namespaces[j][k].substring(0, lastIndex);
					sb.append(del);
					sb.append(ns);
					del = ", ";
				}
			}
		}
		
		archiveConfig.addManifestEntry("Import-Package", sb.toString());
		getLog().info("Set Imports to : " + sb.toString());
	}

	/**
	 * Calculate the bundle class path based on the list of JARs in our bundle.
	 * This list includes outselves. We also calculate the Bundle-Classpath
	 * header (a bit clumsy) This is a bit cheap, so maybe this needs to be
	 * changed TODO
	 * 
	 * @param mainJar
	 * @param sb
	 * @return
	 */
	List getBundleClassPath(Jar mainJar) {
		List result = new ArrayList();
		for (Iterator i = mainJar.resources.keySet().iterator(); i.hasNext();) {
			String path = (String) i.next();
			Object resource = mainJar.resources.get(path);
			if (resource instanceof Jar) {
				result.add(path);
			}
		}
		return result;
	}

	/**
	 * We traverse through al the classes that we can find and calculate the
	 * contained and referred set and uses.
	 * 
	 * @param jar
	 * @param contained
	 * @param referred
	 * @param uses
	 */
	void analyzeJar(Jar jar, Set contained, Set referred, Map uses) {
		String prefix = "";
		Set set = jar.getEntryPaths(prefix);
		for (Iterator r = set.iterator(); r.hasNext();) {
			String path = (String) r.next();
			Object resource = jar.getEntry(path);
			if (resource instanceof Clazz) {
				Clazz clazz = (Clazz) resource;
				String pathOfClass = path.substring(prefix.length());
				String pack = Clazz.getPackage(pathOfClass);
				contained.add(pack);
				referred.addAll(clazz.getReferred());

				// Add all the used packages
				// to this package
				Set t = (Set) uses.get(pack);
				if (t == null)
					uses.put(pack, t = new HashSet());
				t.addAll(clazz.getReferred());
				t.remove(pack);
			}
		}
	}

	/**
	 * Print a standard Map based OSGi header.
	 * 
	 * @param exports map { name => Map { attribute|directive => value } }
	 * @return the clauses
	 */

	String printClauses(Map exports) {
		StringBuffer sb = new StringBuffer();
		String del = "";
		for (Iterator i = exports.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			Map map = (Map) exports.get(name);
			sb.append(del);
			sb.append(name);

			for (Iterator j = map.keySet().iterator(); j.hasNext();) {
				String key = (String) j.next();
				String value = (String) map.get(key);
				sb.append(";");
				sb.append(key);
				sb.append("=");
				sb.append(value);
			}
			del = ", ";
		}
		return sb.toString();
	}

	/**
	 * Check if the BundleManifest version is set correctly, base the manifest
	 * version on it.
	 * 
	 * @throws MojoExecutionException
	 */
	void verifyDeclaredBundleManifestVersion() throws MojoExecutionException {
		String mfv = osgiManifest.getBundleManifestVersion();
		if (mfv != null && mfv.trim().length() != 0) {
			try {
				bundleManifestVersion = Integer.parseInt(mfv);
				if (bundleManifestVersion != 2)
					throw new MojoExecutionException(
							"Bundle-ManifestVersion must be 2, it is " + mfv);
			}
			catch (Exception e) {
				throw new MojoExecutionException(
						"Bundle-ManifestVersion must be an integer: " + mfv);
			}
		}
	}

	/**
	 * TODO: Decide if we accept merging of entire manifest.mf files Here's a
	 * big question to make a final decision at some point: Do accept merging of
	 * manifest entries located in some file somewhere in the project directory?
	 * If so, do we allow both file and configuration based entries to be
	 * specified simultaneously and how do we merge these?
	 */
	private void addManifestFile() {
		if (manifestFile != null) {
			File file = new File(project.getBasedir().getAbsolutePath(),
					manifestFile);
			getLog().info(
					"Manifest file: " + file.getAbsolutePath()
							+ " will be used");
			archiveConfig.setManifestFile(file);
		}
		else {
			getLog().info("No manifest file specified. Default will be used.");
		}
	}

	/**
	 * Look for any OSGi specified manifest entries in the maven-osgi-plugin
	 * configuration section of the POM. If we find some, then add them to the
	 * target artifact's manifest.
	 */
	private void addManifestEntries() {
		if (osgiManifest != null && osgiManifest.getEntries().size() > 0) {
			Map entries = osgiManifest.getEntries();

			getLog().info(
					"Bundle manifest will be modified with the following entries: "
							+ entries.toString());
			archiveConfig.addManifestEntries(entries);
		}
		else {
			getLog()
					.info(
							"No OSGi bundle manifest entries have been specified in the POM.");
		}
	}

	/**
	 * We are going to iterate through the POM's specified JAR dependencies. If
	 * a dependency has a scope of either RUNTIME or COMPILE, then we'll JAR
	 * them up inside the OSGi bundle artifact. We will then add the
	 * Bundle-Classpath manifest entry.
	 */
	private void addEmbeddedJars() throws MojoExecutionException {
		Set artifacts = project.getArtifacts();

		for (Iterator it = artifacts.iterator(); it.hasNext();) {
			Artifact artifact = (Artifact) it.next();
			if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
					&& !Artifact.SCOPE_TEST.equals(artifact.getScope())) {
				String type = artifact.getType();

				if ("jar".equals(type)) {
					File depFile = artifact.getFile();

					try {
						FileUtils.copyFileToDirectory(depFile, outputDirectory);

					}
					catch (Exception e) {
						String errmsg = "Error copying "
								+ depFile.getAbsolutePath() + " to "
								+ outputDirectory.getAbsolutePath();
						throw new MojoExecutionException(errmsg, e);
					}
				}
			}
		}
	}

	/**
	 * Auto-set the bundle version.
	 */
	private void addBundleVersion() {
		// Maven uses a '-' to separate the version qualifier,
		// while OSGi uses a '.', so we need to convert to a '.'
		StringBuffer sb = new StringBuffer(project.getVersion());
		if (sb.indexOf("-") >= 0) {
			sb.setCharAt(sb.indexOf("-"), '.');
		}
		archiveConfig.addManifestEntry("Bundle-Version", sb.toString());
	}

	/**
	 * Returns a string array of the includes to be used when assembling/copying
	 * the war.
	 * 
	 * @return an array of tokens to include
	 */
	private String[] getIncludes() {
		return new String[] {jarSourceIncludes};
	}

	/**
	 * Returns a string array of the excludes to be used when assembling/copying
	 * the jar.
	 * 
	 * @return an array of tokens to exclude
	 */
	private String[] getExcludes() {
		List excludeList = new ArrayList(FileUtils.getDefaultExcludesAsList());

		if (jarSourceExcludes != null && !"".equals(jarSourceExcludes)) {
			excludeList.add(jarSourceExcludes);
		}

		return (String[]) excludeList.toArray(EMPTY_STRING_ARRAY);
	}
}
