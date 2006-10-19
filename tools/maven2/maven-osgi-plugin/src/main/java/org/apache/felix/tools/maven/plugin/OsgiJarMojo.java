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

package org.apache.felix.tools.maven.plugin;

import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import org.apache.maven.archiver.*;
import org.apache.maven.artifact.*;
import org.apache.maven.plugin.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.*;
import org.codehaus.plexus.util.FileUtils;

/**
 * Package an OSGi jar "bundle."
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Apache Felix Project</a>
 * @version $Rev$, $Date$
 * @goal osgi-bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description build an OSGi bundle jar
 */
public class OsgiJarMojo extends AbstractMojo {
    public static final String OSGI_REFERENCES = "osgi.references";
    public static final String AUTO_DETECT = "auto-detect";

	private static final String[]		EMPTY_STRING_ARRAY		= {};

	int									bundleManifestVersion	= 1;

    /**
     * Jars to Inline
     *
     * @parameter
     */
    private List inlinedArtifacts = new ArrayList();

    /**
     * Packages to ignore when generating import-package header.
     *
     * @parameter
     */
    private String ignorePackage;

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
	 * @parameter expression="${org.apache.felix.tools.maven.plugin.OsgiManifest}"
	 */
	private OsgiManifest				osgiManifest;

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

        outputDirectory.mkdirs();

        verifyDeclaredBundleManifestVersion();

		getLog().info("Generating JAR bundle " + jarFile.getAbsolutePath());

		MavenArchiver archiver = new MavenArchiver();

		archiver.setArchiver(jarArchiver);
		archiver.setOutputFile(jarFile);

		addManifestFile();
		addManifestEntries();

        // Inline the contents of the indicated jar artifacts
        inlineArtifacts();

        // Add the JARs that were specified in the POM
		// as "not" provided and which are not inlined
		addEmbeddedJars();

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

        // Check if the osgi.references property is set and display result
        // if necessary.
		checkReferencesProperty(uses);

        // Dump referenced package information if debug logging is enabled.
		if (getLog().isDebugEnabled()) {
		    printReferencedPackages(uses);
		}

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

        // Remove any ignored packages from the referred set.
        Set ignorePackageSet = parseIgnorePackage();
        referred.removeAll(ignorePackageSet);

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

		verifyBundleActivator(mainJar);
		
		archiver.createArchive(project, archiveConfig);
		project.getArtifact().setFile(jarFile);
	}

	private void verifyBundleActivator(Jar mainJar) {
		String ba = osgiManifest.getBundleActivator();
		if ((ba != null) && ba.equals(AUTO_DETECT)) {
			switch ( mainJar.activators.size() ) {
				case 0:
                    break;
				case 1:
                    archiveConfig.addManifestEntry("Bundle-Activator", mainJar.activators.get(0));
				    break;
				default:
					getLog().info("[OSGi] Multiple activators found, unable to auto-detect." );
				    break;
			}
		}
		else if (ba != null) {
			if (!mainJar.activators.contains(ba))
				getLog().warn("[OSGi] UNABLE TO VERIFY BUNDLE ACTIVATOR: " + ba);
		}
	}

	private void createBundleClasspathHeader(List bundleClassPath) {
		StringBuffer sb = new StringBuffer();
		for (Iterator i = bundleClassPath.iterator(); i.hasNext(); ) {
			sb.append(i.next());
            if (i.hasNext()) {
                sb.append(",");
            }
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
			// For backwards compatibility with existing builds, only
            // issue a warning for missing imports. On the other hand,
            // if packages are being ignored, then this is a new project
            // so be more strict and throw an error.
            if (osgiManifest.getIgnorePackage() == null) {
                getLog().warn("[OSGi] MISSING IMPORT: " + o);
            }
            else {
                getLog().error("[OSGi] MISSING IMPORT: " + o);
                throw new MojoExecutionException("Missing Import " + o);
            }
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
				if ((del = qt.getSeparator()) != '=') {
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
		archiveConfig.addManifestEntry("Import-Package", sb.toString());
	}

	/**
	 * Calculate the bundle class path based on the list of JARs in our bundle.
	 * This list includes outselves. We also calculate the Bundle-Classpath
	 * header (a bit clumsy) This is a bit cheap, so maybe this needs to be
	 * changed TODO
	 * 
	 * @param mainJar
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
        if (osgiManifest.getBundleVersion() == null) {
            addBundleVersion();
        } 
        if (osgiManifest != null && osgiManifest.getEntries().size() > 0) {
			Map entries = osgiManifest.getEntries();

			getLog().debug(
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
     *
     * Artifacts which are inlined will not be included.
	 */
	private void addEmbeddedJars() throws MojoExecutionException {
		Set artifacts = project.getArtifacts();
		Map artifactMap = ArtifactUtils.artifactMapByArtifactId( artifacts );

		for (Iterator it = artifacts.iterator(); it.hasNext();) {
			Artifact artifact = (Artifact) it.next();
			if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
					&& !Artifact.SCOPE_TEST.equals(artifact.getScope())) {
				String type = artifact.getType();

				if ("jar".equals(type)) {
                    // Do not include artifacts which are inlined
                    if (!(inlinedArtifacts.contains(artifact.getArtifactId()))) {
                      
                        // Do not include artifacts provided by another bundle
                        boolean isProvidedByBundle = false;
                        Iterator iter = artifact.getDependencyTrail().iterator();
                        iter.next(); // skip this project
                        while ( iter.hasNext() )
                        {  
                            String id = (String) iter.next();
                            Artifact element = (Artifact) artifactMap.get( id );
                            if ( element != null 
                                    && "osgi-bundle".equals( element.getType() ) )
                            {
                                isProvidedByBundle = true;
                                break;
                            }
                        }
                        if ( isProvidedByBundle )
                            continue;
                        
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
	}

	/**
	 * Auto-set the bundle version.
	 */
	private void addBundleVersion() {
        // Maven uses a '-' to separate the version qualifier,
	    // while OSGi uses a '.', so we need to convert to a '.'
        String version = project.getVersion().replace('-', '.');
        osgiManifest.setBundleVersion(version);
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

    private Set parseIgnorePackage() {
        HashSet result = new HashSet();
        if ((ignorePackage == null) && (osgiManifest.getIgnorePackage() != null)) {
            ignorePackage = osgiManifest.getIgnorePackage();
            getLog().warn("DEPRECATED METADATA! "
                + "The <ignorePackage> tag should be set inside the <configuration> "
                + "tag, not in the <osgiManifest> tag.");            
        }
        if (ignorePackage != null) {
            StringTokenizer st = new StringTokenizer(ignorePackage, ",", false);
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }
        return result;
    }

    private void checkReferencesProperty(Map uses) {
        String interestedIn = System.getProperty(OSGI_REFERENCES);
        if (interestedIn == null) {
            return;
        }
        StringBuffer buf = new StringBuffer();
        Iterator it1 = uses.entrySet().iterator();
        while (it1.hasNext()) {
            Map.Entry entry = (Map.Entry) it1.next();
            String pack = (String) entry.getKey();
            Set references = (Set) entry.getValue();
            Iterator it2 = references.iterator();
            while (it2.hasNext()) {
                String packReferred = (String) it2.next();
                if (interestedIn.equals(packReferred)) {
                    buf.append("          |-- ");
                    buf.append(pack);
                    buf.append('\n');
                    break;
                }
            }
        }
        if (buf.length() == 0) {
            getLog().info("Noone uses " + interestedIn);
        }
        else {
            int changePos = buf.lastIndexOf("|");
            buf.setCharAt(changePos, '+');
            getLog().info(interestedIn + " used by;\n" + buf);
        }
    }

    private void printReferencedPackages(Map uses) {
        StringBuffer buf = new StringBuffer();
        Iterator it1 = uses.entrySet().iterator();
        while (it1.hasNext()) {
            Map.Entry entry = (Map.Entry) it1.next();
            String pack = (String) entry.getKey();
            buf.append(pack);
            buf.append('\n');
            Set references = (Set) entry.getValue();
            Iterator it2 = references.iterator();
            while (it2.hasNext()) {
                String packReferred = (String) it2.next();
                buf.append("          |-- ");
                buf.append(packReferred);
                buf.append('\n');
            }
            int changePos = buf.lastIndexOf("|");
            if (changePos >= 0) {
                buf.setCharAt(changePos, '+');
            }
            buf.append('\n');
        }
        getLog().debug("Referenced package list: \n" + buf.toString());
    }

    private void inlineArtifacts() throws MojoExecutionException {
		Set artifacts = project.getArtifacts();

		for (Iterator it = artifacts.iterator(); it.hasNext();) {
			Artifact artifact = (Artifact) it.next();
			if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
					&& !Artifact.SCOPE_TEST.equals(artifact.getScope())) {
				String type = artifact.getType();

				if ("jar".equals(type)) {
                    if ((inlinedArtifacts.contains(artifact.getArtifactId()))) {
                        File depFile = artifact.getFile();

                        try {
                            ZipFile inlinedJar = new ZipFile(depFile);
                            Enumeration entries = inlinedJar.entries();
                            byte[] buffer = new byte[4096];
                            while (entries.hasMoreElements()) {
                                ZipEntry entry = (ZipEntry) entries.nextElement();
                                if (entry.isDirectory()) {
                                    new File(outputDirectory, entry.getName()).mkdirs();
                                } else {
                                    // Have to do this because some JARs ship without directories, i.e., just files.
                                    new File(outputDirectory, entry.getName()).getParentFile().mkdirs();
                                    FileOutputStream out = new FileOutputStream(new File(outputDirectory, entry.getName()));
                                    InputStream in = inlinedJar.getInputStream(entry);
                                    for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                                        out.write(buffer, 0, read);
                                    }
                                    in.close();
                                    out.close();
                                }
                            }
                        } catch (Exception e) {
                            String errmsg = "Error inlining "
                                            + depFile.getAbsolutePath() + " to "
                                            + outputDirectory.getAbsolutePath();
                            throw new MojoExecutionException(errmsg, e);
                        }
                    }
                }
			}
		}
    }
}