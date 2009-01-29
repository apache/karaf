/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

/**
 * This class can calculate the required headers for a (potential) JAR file. It
 * analyzes a directory or JAR for the packages that are contained and that are
 * referred to by the bytecodes. The user can the use regular expressions to
 * define the attributes and directives. The matching is not fully regex for
 * convenience. A * and ? get a . prefixed and dots are escaped.
 * 
 * <pre>
 *                                                             			*;auto=true				any		
 *                                                             			org.acme.*;auto=true    org.acme.xyz
 *                                                             			org.[abc]*;auto=true    org.acme.xyz
 * </pre>
 * 
 * Additional, the package instruction can start with a '=' or a '!'. The '!'
 * indicates negation. Any matching package is removed. The '=' is literal, the
 * expression will be copied verbatim and no matching will take place.
 * 
 * Any headers in the given properties are used in the output properties.
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.jar.Attributes.*;
import java.util.regex.*;

import aQute.bnd.service.*;
import aQute.lib.filter.*;

public class Analyzer extends Processor {

    static Pattern                         doNotCopy      = Pattern
                                                                  .compile("CVS|.svn");
    static String                          version;
    static Pattern                         versionPattern = Pattern
                                                                  .compile("(\\d+\\.\\d+)\\.\\d+.*");
    final Map<String, Map<String, String>> contained      = newHashMap();                            // package
    final Map<String, Map<String, String>> referred       = newHashMap();                            // package
    final Map<String, Set<String>>         uses           = newHashMap();                            // package
    Map<String, Clazz>                     classspace;
    Map<String, Map<String, String>>       exports;
    Map<String, Map<String, String>>       imports;
    Map<String, Map<String, String>>       bundleClasspath;                                          // Bundle
    final Map<String, Map<String, String>> ignored        = newHashMap();                            // Ignored
    // packages
    Jar                                    dot;
    Map<String, Map<String, String>>       classpathExports;

    String                                 activator;

    final List<Jar>                        classpath      = newList();

    static Properties                      bndInfo;

    boolean                                analyzed;
    String                                 bsn;

    public Analyzer(Processor parent) {
        super(parent);
    }

    public Analyzer() {
    }

    /**
     * Specifically for Maven
     * 
     * @param properties
     *            the properties
     */

    public static Properties getManifest(File dirOrJar) throws IOException {
        Analyzer analyzer = new Analyzer();
        analyzer.setJar(dirOrJar);
        Properties properties = new Properties();
        properties.put(IMPORT_PACKAGE, "*");
        properties.put(EXPORT_PACKAGE, "*");
        analyzer.setProperties(properties);
        Manifest m = analyzer.calcManifest();
        Properties result = new Properties();
        for (Iterator<Object> i = m.getMainAttributes().keySet().iterator(); i
                .hasNext();) {
            Attributes.Name name = (Attributes.Name) i.next();
            result.put(name.toString(), m.getMainAttributes().getValue(name));
        }
        return result;
    }

    /**
     * Calcualtes the data structures for generating a manifest.
     * 
     * @throws IOException
     */
    public void analyze() throws IOException {
        if (!analyzed) {
            analyzed = true;
            classpathExports = newHashMap();
            activator = getProperty(BUNDLE_ACTIVATOR);
            bundleClasspath = parseHeader(getProperty(BUNDLE_CLASSPATH));

            analyzeClasspath();

            classspace = analyzeBundleClasspath(dot, bundleClasspath,
                    contained, referred, uses);

            for (AnalyzerPlugin plugin : getPlugins(AnalyzerPlugin.class)) {
                if (plugin instanceof AnalyzerPlugin) {
                    AnalyzerPlugin analyzer = (AnalyzerPlugin) plugin;
                    try {
                        boolean reanalyze = analyzer.analyzeJar(this);
                        if (reanalyze)
                            classspace = analyzeBundleClasspath(dot,
                                    bundleClasspath, contained, referred, uses);
                    } catch (Exception e) {
                        error("Plugin Analyzer " + analyzer
                                + " throws exception " + e);
                        e.printStackTrace();
                    }
                }
            }

            if (activator != null) {
                // Add the package of the activator to the set
                // of referred classes. This must be done before we remove
                // contained set.
                int n = activator.lastIndexOf('.');
                if (n > 0) {
                    referred.put(activator.substring(0, n),
                            new LinkedHashMap<String, String>());
                }
            }

            referred.keySet().removeAll(contained.keySet());
            if (referred.containsKey(".")) {
                error("The default package '.' is not permitted by the Import-Package syntax. \n"
                        + " This can be caused by compile errors in Eclipse because Eclipse creates \n"
                        + "valid class files regardless of compile errors.\n"
                        + "The following package(s) import from the default package "
                        + getUsedBy("."));
            }

            Map<String, Map<String, String>> exportInstructions = parseHeader(getProperty(EXPORT_PACKAGE));
            Map<String, Map<String, String>> additionalExportInstructions = parseHeader(getProperty(EXPORT_CONTENTS));
            exportInstructions.putAll(additionalExportInstructions);
            Map<String, Map<String, String>> importInstructions = parseHeader(getImportPackages());
            Map<String, Map<String, String>> dynamicImports = parseHeader(getProperty(DYNAMICIMPORT_PACKAGE));

            if (dynamicImports != null) {
                // Remove any dynamic imports from the referred set.
                referred.keySet().removeAll(dynamicImports.keySet());
            }

            Map<String, Map<String, String>> superfluous = newHashMap();
            // Tricky!
            for (Iterator<String> i = exportInstructions.keySet().iterator(); i
                    .hasNext();) {
                String instr = i.next();
                if (!instr.startsWith("!"))
                    superfluous.put(instr, exportInstructions.get(instr));
            }

            exports = merge("export-package", exportInstructions, contained,
                    superfluous.keySet(), null);

            // disallow export of default package
            exports.remove(".");

            for (Iterator<Map.Entry<String, Map<String, String>>> i = superfluous
                    .entrySet().iterator(); i.hasNext();) {
                // It is possible to mention metadata directories in the export
                // explicitly, they are then exported and removed from the
                // warnings. Note that normally metadata directories are not
                // exported.
                Map.Entry<String, Map<String, String>> entry = i.next();
                String pack = entry.getKey();
                if (isDuplicate(pack))
                    i.remove();
                else if (isMetaData(pack)) {
                    exports.put(pack, entry.getValue());
                    i.remove();
                }
            }

            if (!superfluous.isEmpty()) {
                warning("Superfluous export-package instructions: "
                        + superfluous.keySet());
            }

            // Add all exports that do not have an -noimport: directive
            // to the imports.
            Map<String, Map<String, String>> referredAndExported = newMap(referred);
            referredAndExported.putAll(addExportsToImports(exports));

            // match the imports to the referred and exported packages,
            // merge the info for matching packages
            Set<String> extra = new TreeSet<String>(importInstructions.keySet());
            imports = merge("import-package", importInstructions,
                    referredAndExported, extra, ignored);

            // Instructions that have not been used could be superfluous
            // or if they do not contain wildcards, should be added
            // as extra imports, the user knows best.
            for (Iterator<String> i = extra.iterator(); i.hasNext();) {
                String p = i.next();
                if (p.startsWith("!") || p.indexOf('*') >= 0
                        || p.indexOf('?') >= 0 || p.indexOf('[') >= 0) {
                    if (!isResourceOnly())
                        warning("Did not find matching referal for " + p);
                } else {
                    Map<String, String> map = importInstructions.get(p);
                    imports.put(p, map);
                }
            }

            // See what information we can find to augment the
            // imports. I.e. look on the classpath
            augmentImports();

            // Add the uses clause to the exports
            doUses(exports, uses, imports);
        }
    }

    /**
     * Copy the input collection into an output set but skip names that have
     * been marked as duplicates or are optional.
     * 
     * @param superfluous
     * @return
     */
    Set<Instruction> removeMarkedDuplicates(Collection<Instruction> superfluous) {
        Set<Instruction> result = new HashSet<Instruction>();
        for (Iterator<Instruction> i = superfluous.iterator(); i.hasNext();) {
            Instruction instr = (Instruction) i.next();
            if (!isDuplicate(instr.getPattern()) && !instr.isOptional())
                result.add(instr);
        }
        return result;
    }

    /**
     * Analyzer has an empty default but the builder has a * as default.
     * 
     * @return
     */
    protected String getImportPackages() {
        return getProperty(IMPORT_PACKAGE);
    }

    /**
     * 
     * @return
     */
    boolean isResourceOnly() {
        return getProperty(RESOURCEONLY, "false").equalsIgnoreCase("true");
    }

    /**
     * Answer the list of packages that use the given package.
     */
    Set<String> getUsedBy(String pack) {
        Set<String> set = newSet();
        for (Iterator<Map.Entry<String, Set<String>>> i = uses.entrySet()
                .iterator(); i.hasNext();) {
            Map.Entry<String, Set<String>> entry = i.next();
            Set<String> used = entry.getValue();
            if (used.contains(pack))
                set.add(entry.getKey());
        }
        return set;
    }

    /**
     * One of the main workhorses of this class. This will analyze the current
     * setp and calculate a new manifest according to this setup. This method
     * will also set the manifest on the main jar dot
     * 
     * @return
     * @throws IOException
     */
    public Manifest calcManifest() throws IOException {
        analyze();
        Manifest manifest = new Manifest();
        Attributes main = manifest.getMainAttributes();

        main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        main.putValue(BUNDLE_MANIFESTVERSION, "2");

        boolean noExtraHeaders = "true"
                .equalsIgnoreCase(getProperty(NOEXTRAHEADERS));

        if (!noExtraHeaders) {
            main.putValue(CREATED_BY, System.getProperty("java.version") + " ("
                    + System.getProperty("java.vendor") + ")");
            main.putValue(TOOL, "Bnd-" + getVersion());
            main.putValue(BND_LASTMODIFIED, "" + System.currentTimeMillis());
        }
        String exportHeader = printClauses(exports,
                "uses:|include:|exclude:|mandatory:|" + IMPORT_DIRECTIVE, true);

        if (exportHeader.length() > 0)
            main.putValue(EXPORT_PACKAGE, exportHeader);
        else
            main.remove(EXPORT_PACKAGE);

        Map<String, Map<String, String>> temp = removeKeys(imports, "java.");
        if (!temp.isEmpty()) {
            main.putValue(IMPORT_PACKAGE, printClauses(temp, "resolution:"));
        } else {
            main.remove(IMPORT_PACKAGE);
        }

        temp = newMap(contained);
        temp.keySet().removeAll(exports.keySet());

        if (!temp.isEmpty())
            main.putValue(PRIVATE_PACKAGE, printClauses(temp, ""));
        else
            main.remove(PRIVATE_PACKAGE);

        if (!ignored.isEmpty()) {
            main.putValue(IGNORE_PACKAGE, printClauses(ignored, ""));
        } else {
            main.remove(IGNORE_PACKAGE);
        }

        if (bundleClasspath != null && !bundleClasspath.isEmpty())
            main.putValue(BUNDLE_CLASSPATH, printClauses(bundleClasspath, ""));
        else
            main.remove(BUNDLE_CLASSPATH);

        Map<String, Map<String, String>> l = doServiceComponent(getProperty(SERVICE_COMPONENT));
        if (!l.isEmpty())
            main.putValue(SERVICE_COMPONENT, printClauses(l, ""));
        else
            main.remove(SERVICE_COMPONENT);

        for (Enumeration<?> h = getProperties().propertyNames(); h
                .hasMoreElements();) {
            String header = (String) h.nextElement();
            if (header.trim().length() == 0) {
                warning("Empty property set with value: "
                        + getProperties().getProperty(header));
                continue;
            }
            if (!Character.isUpperCase(header.charAt(0))) {
                if (header.charAt(0) == '@')
                    doNameSection(manifest, header);
                continue;
            }

            if (header.equals(BUNDLE_CLASSPATH)
                    || header.equals(EXPORT_PACKAGE)
                    || header.equals(IMPORT_PACKAGE))
                continue;
            
            if ( header.equalsIgnoreCase("Name")) {
                error("Your bnd file contains a header called 'Name'. This interferes with the manifest name section.");
                continue;
            }

            if (Verifier.HEADER_PATTERN.matcher(header).matches()) {
                String value = getProperty(header);
                if (value != null && main.getValue(header) == null) {
                    if (value.trim().length() == 0)
                        main.remove(header);
                    else
                        main.putValue(header, value);
                }
            } else {
                // TODO should we report?
            }
        }

        //
        // Calculate the bundle symbolic name if it is
        // not set.
        // 1. set
        // 2. name of properties file (must be != bnd.bnd)
        // 3. name of directory, which is usualy project name
        //
        String bsn = getBsn();
        if (main.getValue(BUNDLE_SYMBOLICNAME) == null) {
            main.putValue(BUNDLE_SYMBOLICNAME, bsn);
        }

        //
        // Use the same name for the bundle name as BSN when
        // the bundle name is not set
        //
        if (main.getValue(BUNDLE_NAME) == null) {
            main.putValue(BUNDLE_NAME, bsn);
        }

        if (main.getValue(BUNDLE_VERSION) == null)
            main.putValue(BUNDLE_VERSION, "0");

        // Copy old values into new manifest, when they
        // exist in the old one, but not in the new one
        merge(manifest, dot.getManifest());

        // Remove all the headers mentioned in -removeheaders
        Map<String, Map<String, String>> removes = parseHeader(getProperty(REMOVE_HEADERS));
        for (Iterator<String> i = removes.keySet().iterator(); i.hasNext();) {
            String header = i.next();
            for (Iterator<Object> j = main.keySet().iterator(); j.hasNext();) {
                Attributes.Name attr = (Attributes.Name) j.next();
                if (attr.toString().matches(header)) {
                    j.remove();
                    progress("Removing header: " + header);
                }
            }
        }

        dot.setManifest(manifest);
        return manifest;
    }

    /**
     * This method is called when the header starts with a @, signifying
     * a name section header. The name part is defined by replacing all the @
     * signs to a /, removing the first and the last, and using the last
     * part as header name:
     * <pre>
     * @org@osgi@service@event@Implementation-Title
     * </pre>
     * This will be the header Implementation-Title in the org/osgi/service/event
     * named section.
     *  
     * @param manifest
     * @param header
     */
    private void doNameSection(Manifest manifest, String header) {
        String path = header.replace('@', '/');
        int n = path.lastIndexOf('/');
        // Must succeed because we start with @
        String name = path.substring(n + 1);
        // Skip first /
        path = path.substring(1, n);
        if (name.length() != 0 && path.length() != 0) {
            Attributes attrs = manifest.getAttributes(path);
            if (attrs == null) {
                attrs = new Attributes();
                manifest.getEntries().put(path, attrs);
            }
            attrs.putValue(name, getProperty(header));
        } else {
            warning(
                    "Invalid header (starts with @ but does not seem to be for the Name section): %s",
                    header);
        }
    }

    /**
     * Clear the key part of a header. I.e. remove everything from the first ';'
     * 
     * @param value
     * @return
     */
    public String getBsn() {
        String value = getProperty(BUNDLE_SYMBOLICNAME);
        if (value == null) {
            if (getPropertiesFile() != null)
                value = getPropertiesFile().getName();

            if (value == null || value.equals("bnd.bnd"))
                value = getBase().getName();
            else if (value.endsWith(".bnd"))
                value = value.substring(0, value.length() - 4);
        }

        if (value == null)
            return "untitled";

        int n = value.indexOf(';');
        if (n > 0)
            value = value.substring(0, n);
        return value.trim();
    }

    /**
     * Calculate an export header solely based on the contents of a JAR file
     * 
     * @param bundle
     *            The jar file to analyze
     * @return
     */
    public String calculateExportsFromContents(Jar bundle) {
        String ddel = "";
        StringBuffer sb = new StringBuffer();
        Map<String, Map<String, Resource>> map = bundle.getDirectories();
        for (Iterator<String> i = map.keySet().iterator(); i.hasNext();) {
            String directory = (String) i.next();
            if (directory.equals("META-INF")
                    || directory.startsWith("META-INF/"))
                continue;
            if (directory.equals("OSGI-OPT")
                    || directory.startsWith("OSGI-OPT/"))
                continue;
            if (directory.equals("/"))
                continue;

            if (directory.endsWith("/"))
                directory = directory.substring(0, directory.length() - 1);

            directory = directory.replace('/', '.');
            sb.append(ddel);
            sb.append(directory);
            ddel = ",";
        }
        return sb.toString();
    }

    /**
     * Check if a service component header is actually referring to a class. If
     * so, replace the reference with an XML file reference. This makes it
     * easier to create and use components.
     * 
     * @throws UnsupportedEncodingException
     * 
     */
    public Map<String, Map<String, String>> doServiceComponent(
            String serviceComponent) throws IOException {
        Map<String, Map<String, String>> list = newMap();
        Map<String, Map<String, String>> sc = parseHeader(serviceComponent);
        if (!sc.isEmpty()) {
            for (Iterator<Map.Entry<String, Map<String, String>>> i = sc
                    .entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Map<String, String>> entry = i.next();
                String name = entry.getKey();
                Map<String, String> info = entry.getValue();
                if (name == null) {
                    error("No name in Service-Component header: " + info);
                    continue;
                }
                if (dot.exists(name)) {
                    // Normal service component
                    list.put(name, info);
                } else {
                    String impl = name;
                    if (info.containsKey(COMPONENT_IMPLEMENTATION))
                        impl = info.get(COMPONENT_IMPLEMENTATION);

                    if (!checkClass(impl))
                        error("Not found Service-Component header: " + name);
                    else {
                        // We have a definition, so make an XML resources
                        Resource resource = createComponentResource(name, info);
                        dot.putResource("OSGI-INF/" + name + ".xml", resource);
                        Map<String, String> empty = Collections.emptyMap();
                        list.put("OSGI-INF/" + name + ".xml", empty);
                    }
                }
            }
        }
        return list;
    }

    public Map<String, Map<String, String>> getBundleClasspath() {
        return bundleClasspath;
    }

    public Map<String, Map<String, String>> getContained() {
        return contained;
    }

    public Map<String, Map<String, String>> getExports() {
        return exports;
    }

    public Map<String, Map<String, String>> getImports() {
        return imports;
    }

    public Jar getJar() {
        return dot;
    }

    public Map<String, Map<String, String>> getReferred() {
        return referred;
    }

    /**
     * Return the set of unreachable code depending on exports and the bundle
     * activator.
     * 
     * @return
     */
    public Set<String> getUnreachable() {
        Set<String> unreachable = new HashSet<String>(uses.keySet()); // all
        for (Iterator<String> r = exports.keySet().iterator(); r.hasNext();) {
            String packageName = r.next();
            removeTransitive(packageName, unreachable);
        }
        if (activator != null) {
            String pack = activator.substring(0, activator.lastIndexOf('.'));
            removeTransitive(pack, unreachable);
        }
        return unreachable;
    }

    public Map<String, Set<String>> getUses() {
        return uses;
    }

    /**
     * Get the version from the manifest, a lot of work!
     * 
     * @return version or unknown.
     */
    public String getVersion() {
        return getBndInfo("version", "<unknown version>");
    }

    public long getBndLastModified() {
        String time = getBndInfo("modified", "0");
        try {
            return Long.parseLong(time);
        } catch (Exception e) {
        }
        return 0;
    }

    public String getBndInfo(String key, String defaultValue) {
        if (bndInfo == null) {
            bndInfo = new Properties();
            try {
                InputStream in = Analyzer.class.getResourceAsStream("bnd.info");
                if (in != null) {
                    bndInfo.load(in);
                    in.close();
                }
            } catch (IOException ioe) {
                warning("Could not read bnd.info in " + Analyzer.class.getPackage()
                        + ioe);
            }
        }
        return bndInfo.getProperty(key, defaultValue);
    }

    /**
     * Merge the existing manifest with the instructions.
     * 
     * @param manifest
     *            The manifest to merge with
     * @throws IOException
     */
    public void mergeManifest(Manifest manifest) throws IOException {
        if (manifest != null) {
            Attributes attributes = manifest.getMainAttributes();
            for (Iterator<Object> i = attributes.keySet().iterator(); i
                    .hasNext();) {
                Name name = (Name) i.next();
                String key = name.toString();
                // Dont want instructions
                if (key.startsWith("-"))
                    continue;

                if (getProperty(key) == null)
                    setProperty(key, (String) attributes.get(name));
            }
        }
    }

    // public Signer getSigner() {
    // String sign = getProperty("-sign");
    // if (sign == null) return null;
    //
    // Map parsed = parseHeader(sign);
    // Signer signer = new Signer();
    // String password = (String) parsed.get("password");
    // if (password != null) {
    // signer.setPassword(password);
    // }
    //
    // String keystore = (String) parsed.get("keystore");
    // if (keystore != null) {
    // File f = new File(keystore);
    // if (!f.isAbsolute()) f = new File(base, keystore);
    // signer.setKeystore(f);
    // } else {
    // error("Signing requires a keystore");
    // return null;
    // }
    //
    // String alias = (String) parsed.get("alias");
    // if (alias != null) {
    // signer.setAlias(alias);
    // } else {
    // error("Signing requires an alias for the key");
    // return null;
    // }
    // return signer;
    // }

    public void setBase(File file) {
        super.setBase(file);
        getProperties().put("project.dir", getBase().getAbsolutePath());
    }

    /**
     * Set the classpath for this analyzer by file.
     * 
     * @param classpath
     * @throws IOException
     */
    public void setClasspath(File[] classpath) throws IOException {
        List<Jar> list = new ArrayList<Jar>();
        for (int i = 0; i < classpath.length; i++) {
            if (classpath[i].exists()) {
                Jar current = new Jar(classpath[i]);
                list.add(current);
            } else {
                error("Missing file on classpath: " + classpath[i]);
            }
        }
        for (Iterator<Jar> i = list.iterator(); i.hasNext();) {
            addClasspath(i.next());
        }
    }

    public void setClasspath(Jar[] classpath) {
        for (int i = 0; i < classpath.length; i++) {
            addClasspath(classpath[i]);
        }
    }

    public void setClasspath(String[] classpath) {
        for (int i = 0; i < classpath.length; i++) {
            Jar jar = getJarFromName(classpath[i], " setting classpath");
            if (jar != null)
                addClasspath(jar);
        }
    }

    /**
     * Set the JAR file we are going to work in. This will read the JAR in
     * memory.
     * 
     * @param jar
     * @return
     * @throws IOException
     */
    public Jar setJar(File jar) throws IOException {
        Jar jarx = new Jar(jar);
        addClose(jarx);
        return setJar(jarx);
    }

    /**
     * Set the JAR directly we are going to work on.
     * 
     * @param jar
     * @return
     */
    public Jar setJar(Jar jar) {
        this.dot = jar;
        return jar;
    }

    protected void begin() {
        super.begin();

        updateModified(getBndLastModified(), "bnd last modified");
        String doNotCopy = getProperty(DONOTCOPY);
        if (doNotCopy != null)
            Analyzer.doNotCopy = Pattern.compile(doNotCopy);

        verifyManifestHeadersCase(getProperties());
    }

    /**
     * Check if the given class or interface name is contained in the jar.
     * 
     * @param interfaceName
     * @return
     */
    boolean checkClass(String interfaceName) {
        String path = interfaceName.replace('.', '/') + ".class";
        if (classspace.containsKey(path))
            return true;

        String pack = interfaceName;
        int n = pack.lastIndexOf('.');
        if (n > 0)
            pack = pack.substring(0, n);
        else
            pack = ".";

        return imports.containsKey(pack);
    }

    /**
     * Create the resource for a DS component.
     * 
     * @param list
     * @param name
     * @param info
     * @throws UnsupportedEncodingException
     */
    Resource createComponentResource(String name, Map<String, String> info)
            throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        pw.println("<?xml version='1.0' encoding='utf-8'?>");
        pw.print("<component name='" + name + "'");

        String factory = info.get(COMPONENT_FACTORY);
        if (factory != null)
            pw.print(" factory='" + factory + "'");

        String immediate = info.get(COMPONENT_IMMEDIATE);
        if (immediate != null)
            pw.print(" immediate='" + immediate + "'");

        String enabled = info.get(COMPONENT_ENABLED);
        if (enabled != null)
            pw.print(" enabled='" + enabled + "'");

        pw.println(">");

        // Allow override of the implementation when people
        // want to choose their own name
        String impl = (String) info.get(COMPONENT_IMPLEMENTATION);
        pw.println("  <implementation class='" + (impl == null ? name : impl)
                + "'/>");

        String provides = info.get(COMPONENT_PROVIDE);
        boolean servicefactory = Boolean.getBoolean(info
                .get(COMPONENT_SERVICEFACTORY)
                + "");
        provides(pw, provides, servicefactory);
        properties(pw, info);
        reference(info, pw);
        pw.println("</component>");
        pw.close();
        byte[] data = out.toByteArray();
        out.close();
        return new EmbeddedResource(data, 0);
    }

    /**
     * Try to get a Jar from a file name/path or a url, or in last resort from
     * the classpath name part of their files.
     * 
     * @param name
     *            URL or filename relative to the base
     * @param from
     *            Message identifying the caller for errors
     * @return null or a Jar with the contents for the name
     */
    Jar getJarFromName(String name, String from) {
        File file = new File(name);
        if (!file.isAbsolute())
            file = new File(getBase(), name);

        if (file.exists())
            try {
                Jar jar = new Jar(file);
                addClose(jar);
                return jar;
            } catch (Exception e) {
                error("Exception in parsing jar file for " + from + ": " + name
                        + " " + e);
            }
        // It is not a file ...
        try {
            // Lets try a URL
            URL url = new URL(name);
            Jar jar = new Jar(fileName(url.getPath()));
            addClose(jar);
            URLConnection connection = url.openConnection();
            InputStream in = connection.getInputStream();
            long lastModified = connection.getLastModified();
            if (lastModified == 0)
                // We assume the worst :-(
                lastModified = System.currentTimeMillis();
            EmbeddedResource.build(jar, in, lastModified);
            in.close();
            return jar;
        } catch (IOException ee) {
            // Check if we have files on the classpath
            // that have the right name, allows us to specify those
            // names instead of the full path.
            for (Iterator<Jar> cp = getClasspath().iterator(); cp.hasNext();) {
                Jar entry = cp.next();
                if (entry.source != null && entry.source.getName().equals(name)) {
                    return entry;
                }
            }
            // error("Can not find jar file for " + from + ": " + name);
        }
        return null;
    }

    private String fileName(String path) {
        int n = path.lastIndexOf('/');
        if (n > 0)
            return path.substring(n + 1);
        return path;
    }

    /**
     * 
     * @param manifest
     * @throws Exception
     */
    void merge(Manifest result, Manifest old) throws IOException {
        if (old != null) {
            for (Iterator<Map.Entry<Object, Object>> e = old
                    .getMainAttributes().entrySet().iterator(); e.hasNext();) {
                Map.Entry<Object, Object> entry = e.next();
                Attributes.Name name = (Attributes.Name) entry.getKey();
                String value = (String) entry.getValue();
                if (name.toString().equalsIgnoreCase("Created-By"))
                    name = new Attributes.Name("Originally-Created-By");
                if (!result.getMainAttributes().containsKey(name))
                    result.getMainAttributes().put(name, value);
            }

            // do not overwrite existing entries
            Map<String, Attributes> oldEntries = old.getEntries();
            Map<String, Attributes> newEntries = result.getEntries();
            for (Iterator<Map.Entry<String, Attributes>> e = oldEntries
                    .entrySet().iterator(); e.hasNext();) {
                Map.Entry<String, Attributes> entry = e.next();
                if (!newEntries.containsKey(entry.getKey())) {
                    newEntries.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Print the Service-Component properties element
     * 
     * @param pw
     * @param info
     */
    void properties(PrintWriter pw, Map<String, String> info) {
        Collection<String> properties = split(info.get(COMPONENT_PROPERTIES));
        for (Iterator<String> p = properties.iterator(); p.hasNext();) {
            String clause = p.next();
            int n = clause.indexOf('=');
            if (n <= 0) {
                error("Not a valid property in service component: " + clause);
            } else {
                String type = null;
                String name = clause.substring(0, n);
                if (name.indexOf('@') >= 0) {
                    String parts[] = name.split("@");
                    name = parts[1];
                    type = parts[0];
                }
                String value = clause.substring(n + 1).trim();
                // TODO verify validity of name and value.
                pw.print("<property name='");
                pw.print(name);
                pw.print("'");

                if (type != null) {
                    if (VALID_PROPERTY_TYPES.matcher(type).matches()) {
                        pw.print(" type='");
                        pw.print(type);
                        pw.print("'");
                    } else {
                        warning("Invalid property type '" + type
                                + "' for property " + name);
                    }
                }

                String parts[] = value.split("\\s*(\\||\\n)\\s*");
                if (parts.length > 1) {
                    pw.println(">");
                    for (String part : parts) {
                        pw.println(part);
                    }
                    pw.println("</property>");
                } else {
                    pw.print(" value='");
                    pw.print(parts[0]);
                    pw.print("'/>");
                }
            }
        }
    }

    /**
     * @param pw
     * @param provides
     */
    void provides(PrintWriter pw, String provides, boolean servicefactory) {
        if (provides != null) {
            if (!servicefactory)
                pw.println("  <service>");
            else
                pw.println("  <service servicefactory='true'>");

            StringTokenizer st = new StringTokenizer(provides, ",");
            while (st.hasMoreTokens()) {
                String interfaceName = st.nextToken();
                pw.println("    <provide interface='" + interfaceName + "'/>");
                if (!checkClass(interfaceName))
                    error("Component definition provides a class that is neither imported nor contained: "
                            + interfaceName);
            }
            pw.println("  </service>");
        }
    }

    final static Pattern REFERENCE = Pattern.compile("([^(]+)(\\(.+\\))?");

    /**
     * @param info
     * @param pw
     */

    void reference(Map<String, String> info, PrintWriter pw) {
        Collection<String> dynamic = split(info.get(COMPONENT_DYNAMIC));
        Collection<String> optional = split(info.get(COMPONENT_OPTIONAL));
        Collection<String> multiple = split(info.get(COMPONENT_MULTIPLE));

        for (Iterator<Map.Entry<String, String>> r = info.entrySet().iterator(); r
                .hasNext();) {
            Map.Entry<String, String> ref = r.next();
            String referenceName = (String) ref.getKey();
            String target = null;
            String interfaceName = (String) ref.getValue();
            if (interfaceName == null || interfaceName.length() == 0) {
                error("Invalid Interface Name for references in Service Component: "
                        + referenceName + "=" + interfaceName);
            }
            char c = interfaceName.charAt(interfaceName.length() - 1);
            if ("?+*~".indexOf(c) >= 0) {
                if (c == '?' || c == '*' || c == '~')
                    optional.add(referenceName);
                if (c == '+' || c == '*')
                    multiple.add(referenceName);
                if (c == '+' || c == '*' || c == '?')
                    dynamic.add(referenceName);
                interfaceName = interfaceName.substring(0, interfaceName
                        .length() - 1);
            }

            // TODO check if the interface is contained or imported

            if (referenceName.endsWith(":")) {
                if (!SET_COMPONENT_DIRECTIVES.contains(referenceName))
                    error("Unrecognized directive in Service-Component header: "
                            + referenceName);
                continue;
            }

            Matcher m = REFERENCE.matcher(interfaceName);
            if (m.matches()) {
                interfaceName = m.group(1);
                target = m.group(2);
            }

            if (!checkClass(interfaceName))
                error("Component definition refers to a class that is neither imported nor contained: "
                        + interfaceName);

            pw.print("  <reference name='" + referenceName + "' interface='"
                    + interfaceName + "'");

            String cardinality = optional.contains(referenceName) ? "0" : "1";
            cardinality += "..";
            cardinality += multiple.contains(referenceName) ? "n" : "1";
            if (!cardinality.equals("1..1"))
                pw.print(" cardinality='" + cardinality + "'");

            if (Character.isLowerCase(referenceName.charAt(0))) {
                String z = referenceName.substring(0, 1).toUpperCase()
                        + referenceName.substring(1);
                pw.print(" bind='set" + z + "'");
                // TODO Verify that the methods exist

                // TODO ProSyst requires both a bind and unbind :-(
                // if ( dynamic.contains(referenceName) )
                pw.print(" unbind='unset" + z + "'");
                // TODO Verify that the methods exist
            }
            if (dynamic.contains(referenceName)) {
                pw.print(" policy='dynamic'");
            }

            if (target != null) {
                Filter filter = new Filter(target);
                if (filter.verify() == null)
                    pw.print(" target='" + filter.toString() + "'");
                else
                    error("Target for " + referenceName
                            + " is not a correct filter: " + target + " "
                            + filter.verify());
            }
            pw.println("/>");
        }
    }

    String stem(String name) {
        int n = name.lastIndexOf('.');
        if (n > 0)
            return name.substring(0, n);
        else
            return name;
    }

    /**
     * Bnd is case sensitive for the instructions so we better check people are
     * not using an invalid case. We do allow this to set headers that should
     * not be processed by us but should be used by the framework.
     * 
     * @param properties
     *            Properties to verify.
     */

    void verifyManifestHeadersCase(Properties properties) {
        for (Iterator<Object> i = properties.keySet().iterator(); i.hasNext();) {
            String header = (String) i.next();
            for (int j = 0; j < headers.length; j++) {
                if (!headers[j].equals(header)
                        && headers[j].equalsIgnoreCase(header)) {
                    warning("Using a standard OSGi header with the wrong case (bnd is case sensitive!), using: "
                            + header + " and expecting: " + headers[j]);
                    break;
                }
            }
        }
    }

    /**
     * We will add all exports to the imports unless there is a -noimport
     * directive specified on an export. This directive is skipped for the
     * manifest.
     * 
     * We also remove any version parameter so that augmentImports can do the
     * version policy.
     * 
     */
    Map<String, Map<String, String>> addExportsToImports(
            Map<String, Map<String, String>> exports) {
        Map<String, Map<String, String>> importsFromExports = newHashMap();
        for (Map.Entry<String, Map<String, String>> packageEntry : exports
                .entrySet()) {
            String packageName = packageEntry.getKey();
            Map<String, String> parameters = packageEntry.getValue();
            String noimport = (String) parameters.get(NO_IMPORT_DIRECTIVE);
            if (noimport == null || !noimport.equalsIgnoreCase("true")) {
                if (parameters.containsKey("version")) {
                    parameters = newMap(parameters);
                    parameters.remove("version");
                }
                importsFromExports.put(packageName, parameters);
            }
        }
        return importsFromExports;
    }

    /**
     * Create the imports/exports by parsing
     * 
     * @throws IOException
     */
    void analyzeClasspath() throws IOException {
        classpathExports = newHashMap();
        for (Iterator<Jar> c = getClasspath().iterator(); c.hasNext();) {
            Jar current = c.next();
            checkManifest(current);
            for (Iterator<String> j = current.getDirectories().keySet()
                    .iterator(); j.hasNext();) {
                String dir = j.next();
                Resource resource = current.getResource(dir + "/packageinfo");
                if (resource != null) {
                    InputStream in = resource.openInputStream();
                    String version = parsePackageInfo(in);
                    in.close();
                    setPackageInfo(dir, "version", version);
                }
            }
        }
    }

    /**
     * 
     * @param jar
     */
    void checkManifest(Jar jar) {
        try {
            Manifest m = jar.getManifest();
            if (m != null) {
                String exportHeader = m.getMainAttributes().getValue(
                        EXPORT_PACKAGE);
                if (exportHeader != null) {
                    Map<String, Map<String, String>> exported = parseHeader(exportHeader);
                    if (exported != null)
                        classpathExports.putAll(exported);
                }
            }
        } catch (Exception e) {
            warning("Erroneous Manifest for " + jar + " " + e);
        }
    }

    /**
     * Find some more information about imports in manifest and other places.
     */
    void augmentImports() {
        for (String packageName : imports.keySet()) {
            setProperty(CURRENT_PACKAGE, packageName);
            try {
                Map<String, String> importAttributes = imports.get(packageName);
                Map<String, String> exporterAttributes = classpathExports
                        .get(packageName);
                if (exporterAttributes == null)
                    exporterAttributes = exports.get(packageName);

                if (exporterAttributes != null) {
                    augmentVersion(importAttributes, exporterAttributes);
                    augmentMandatory(importAttributes, exporterAttributes);
                    if (exporterAttributes.containsKey(IMPORT_DIRECTIVE))
                        importAttributes.put(IMPORT_DIRECTIVE,
                                exporterAttributes.get(IMPORT_DIRECTIVE));
                }

                // Convert any attribute values that have macros.
                for (String key : importAttributes.keySet()) {
                    String value = importAttributes.get(key);
                    if (value.indexOf('$') >= 0) {
                        value = getReplacer().process(value);
                        importAttributes.put(key, value);
                    }
                }

                // You can add a remove-attribute: directive with a regular
                // expression for attributes that need to be removed. We also
                // remove all attributes that have a value of !. This allows
                // you to use macros with ${if} to remove values.
                String remove = importAttributes
                        .remove(REMOVE_ATTRIBUTE_DIRECTIVE);
                Instruction removeInstr = null;

                if (remove != null)
                    removeInstr = Instruction.getPattern(remove);

                for (Iterator<Map.Entry<String, String>> i = importAttributes
                        .entrySet().iterator(); i.hasNext();) {
                    Map.Entry<String, String> entry = i.next();
                    if (entry.getValue().equals("!"))
                        i.remove();
                    else if (removeInstr != null
                            && removeInstr.matches((String) entry.getKey()))
                        i.remove();
                    else {
                        // Not removed ...
                    }
                }

            } finally {
                unsetProperty(CURRENT_PACKAGE);
            }
        }
    }

    /**
     * If we use an import with mandatory attributes we better all use them
     * 
     * @param currentAttributes
     * @param exporter
     */
    private void augmentMandatory(Map<String, String> currentAttributes,
            Map<String, String> exporter) {
        String mandatory = (String) exporter.get("mandatory:");
        if (mandatory != null) {
            String[] attrs = mandatory.split("\\s*,\\s*");
            for (int i = 0; i < attrs.length; i++) {
                if (!currentAttributes.containsKey(attrs[i]))
                    currentAttributes.put(attrs[i], exporter.get(attrs[i]));
            }
        }
    }

    /**
     * Check if we can augment the version from the exporter.
     * 
     * We allow the version in the import to specify a @ which is replaced with
     * the exporter's version.
     * 
     * @param currentAttributes
     * @param exporter
     */
    private void augmentVersion(Map<String, String> currentAttributes,
            Map<String, String> exporter) {

        String exportVersion = (String) exporter.get("version");
        if (exportVersion == null)
            exportVersion = (String) exporter.get("specification-version");
        if (exportVersion == null)
            return;

        exportVersion = cleanupVersion(exportVersion);

        setProperty("@", exportVersion);

        String importRange = currentAttributes.get("version");
        if (importRange != null) {
            importRange = cleanupVersion(importRange);
            importRange = getReplacer().process(importRange);
        } else
            importRange = getVersionPolicy();

        unsetProperty("@");

        // See if we can borrow the version
        // we mist replace the ${@} with the version we
        // found this can be useful if you want a range to start
        // with the found version.
        currentAttributes.put("version", importRange);
    }

    /**
     * Add the uses clauses
     * 
     * @param exports
     * @param uses
     * @throws MojoExecutionException
     */
    void doUses(Map<String, Map<String, String>> exports,
            Map<String, Set<String>> uses,
            Map<String, Map<String, String>> imports) {
        if ("true".equalsIgnoreCase(getProperty(NOUSES)))
            return;

        for (Iterator<String> i = exports.keySet().iterator(); i.hasNext();) {
            String packageName = i.next();
            setProperty(CURRENT_PACKAGE, packageName);
            try {
                Map<String, String> clause = exports.get(packageName);
                String override = clause.get(USES_DIRECTIVE);
                if (override == null)
                    override = USES_USES;

                Set<String> usedPackages = uses.get(packageName);
                if (usedPackages != null) {
                    // Only do a uses on exported or imported packages
                    // and uses should also not contain our own package
                    // name
                    Set<String> sharedPackages = new HashSet<String>();
                    sharedPackages.addAll(imports.keySet());
                    sharedPackages.addAll(exports.keySet());
                    usedPackages.retainAll(sharedPackages);
                    usedPackages.remove(packageName);

                    StringBuffer sb = new StringBuffer();
                    String del = "";
                    for (Iterator<String> u = usedPackages.iterator(); u
                            .hasNext();) {
                        String usedPackage = u.next();
                        if (!usedPackage.startsWith("java.")) {
                            sb.append(del);
                            sb.append(usedPackage);
                            del = ",";
                        }
                    }
                    if (override.indexOf('$') >= 0) {
                        setProperty(CURRENT_USES, sb.toString());
                        override = getReplacer().process(override);
                        unsetProperty(CURRENT_USES);
                    } else
                        // This is for backward compatibility 0.0.287
                        // can be deprecated over time
                        override = override
                                .replaceAll(USES_USES, sb.toString()).trim();
                    if (override.endsWith(","))
                        override = override.substring(0, override.length() - 1);
                    if (override.startsWith(","))
                        override = override.substring(1);
                    if (override.length() > 0) {
                        clause.put("uses:", override);
                    }
                }
            } finally {
                unsetProperty(CURRENT_PACKAGE);
            }
        }
    }

    /**
     * Transitively remove all elemens from unreachable through the uses link.
     * 
     * @param name
     * @param unreachable
     */
    void removeTransitive(String name, Set<String> unreachable) {
        if (!unreachable.contains(name))
            return;

        unreachable.remove(name);

        Set<String> ref = uses.get(name);
        if (ref != null) {
            for (Iterator<String> r = ref.iterator(); r.hasNext();) {
                String element = (String) r.next();
                removeTransitive(element, unreachable);
            }
        }
    }

    /**
     * Helper method to set the package info
     * 
     * @param dir
     * @param key
     * @param value
     */
    void setPackageInfo(String dir, String key, String value) {
        if (value != null) {
            String pack = dir.replace('/', '.');
            Map<String, String> map = classpathExports.get(pack);
            if (map == null) {
                map = new HashMap<String, String>();
                classpathExports.put(pack, map);
            }
            map.put(key, value);
        }
    }

    public void close() {
        super.close();
        if (dot != null)
            dot.close();

        if (classpath != null)
            for (Iterator<Jar> j = classpath.iterator(); j.hasNext();) {
                Jar jar = j.next();
                jar.close();
            }
    }

    /**
     * Findpath looks through the contents of the JAR and finds paths that end
     * with the given regular expression
     * 
     * ${findpath (; reg-expr (; replacement)? )? }
     * 
     * @param args
     * @return
     */
    public String _findpath(String args[]) {
        return findPath("findpath", args, true);
    }

    public String _findname(String args[]) {
        return findPath("findname", args, false);
    }

    String findPath(String name, String[] args, boolean fullPathName) {
        if (args.length > 3) {
            warning("Invalid nr of arguments to " + name + " "
                    + Arrays.asList(args) + ", syntax: ${" + name
                    + " (; reg-expr (; replacement)? )? }");
            return null;
        }

        String regexp = ".*";
        String replace = null;

        switch (args.length) {
        case 3:
            replace = args[2];
        case 2:
            regexp = args[1];
        }
        StringBuffer sb = new StringBuffer();
        String del = "";

        Pattern expr = Pattern.compile(regexp);
        for (Iterator<String> e = dot.getResources().keySet().iterator(); e
                .hasNext();) {
            String path = e.next();
            if (!fullPathName) {
                int n = path.lastIndexOf('/');
                if (n >= 0) {
                    path = path.substring(n + 1);
                }
            }

            Matcher m = expr.matcher(path);
            if (m.matches()) {
                if (replace != null)
                    path = m.replaceAll(replace);

                sb.append(del);
                sb.append(path);
                del = ", ";
            }
        }
        return sb.toString();
    }

    public void putAll(Map<String, String> additional, boolean force) {
        for (Iterator<Map.Entry<String, String>> i = additional.entrySet()
                .iterator(); i.hasNext();) {
            Map.Entry<String, String> entry = i.next();
            if (force || getProperties().get(entry.getKey()) == null)
                setProperty((String) entry.getKey(), (String) entry.getValue());
        }
    }

    boolean firstUse = true;

    public List<Jar> getClasspath() {
        if (firstUse) {
            firstUse = false;
            String cp = getProperty(CLASSPATH);
            if (cp != null)
                for (String s : split(cp)) {
                    Jar jar = getJarFromName(s, "getting classpath");
                    if (jar != null)
                        addClasspath(jar);
                }
        }
        return classpath;
    }

    public void addClasspath(Jar jar) {
        if (isPedantic() && jar.getResources().isEmpty())
            warning("There is an empty jar or directory on the classpath: "
                    + jar.getName());

        classpath.add(jar);
    }

    public void addClasspath(File cp) throws IOException {
        if (!cp.exists())
            warning("File on classpath that does not exist: " + cp);
        Jar jar = new Jar(cp);
        addClose(jar);
        classpath.add(jar);
    }

    public void clear() {
        classpath.clear();
    }

    public Jar getTarget() {
        return dot;
    }

    public Map<String, Clazz> analyzeBundleClasspath(Jar dot,
            Map<String, Map<String, String>> bundleClasspath,
            Map<String, Map<String, String>> contained,
            Map<String, Map<String, String>> referred,
            Map<String, Set<String>> uses) throws IOException {
        Map<String, Clazz> classSpace = new HashMap<String, Clazz>();

        if (bundleClasspath.isEmpty()) {
            analyzeJar(dot, "", classSpace, contained, referred, uses);
        } else {
            for (String path : bundleClasspath.keySet()) {
                if (path.equals(".")) {
                    analyzeJar(dot, "", classSpace, contained, referred, uses);
                    continue;
                }
                //
                // There are 3 cases:
                // - embedded JAR file
                // - directory
                // - error
                //

                Resource resource = dot.getResource(path);
                if (resource != null) {
                    try {
                        Jar jar = new Jar(path);
                        addClose(jar);
                        EmbeddedResource.build(jar, resource);
                        analyzeJar(jar, "", classSpace, contained, referred,
                                uses);
                    } catch (Exception e) {
                        warning("Invalid bundle classpath entry: " + path + " "
                                + e);
                    }
                } else {
                    if (dot.getDirectories().containsKey(path)) {
                        analyzeJar(dot, path + '/', classSpace, contained, referred,
                                uses);
                    } else {
                        warning("No sub JAR or directory " + path);
                    }
                }
            }
        }
        return classSpace;
    }

    /**
     * We traverse through all the classes that we can find and calculate the
     * contained and referred set and uses. This method ignores the Bundle
     * classpath.
     * 
     * @param jar
     * @param contained
     * @param referred
     * @param uses
     * @throws IOException
     */
    private void analyzeJar(Jar jar, String prefix,
            Map<String, Clazz> classSpace,
            Map<String, Map<String, String>> contained,
            Map<String, Map<String, String>> referred,
            Map<String, Set<String>> uses) throws IOException {

        next: for (String path : jar.getResources().keySet()) {
            if (path.startsWith(prefix)) {
                String relativePath = path.substring(prefix.length());
                String pack = getPackage(relativePath);

                if (pack != null && !contained.containsKey(pack)) {
                    if (!isMetaData(relativePath)) {

                        Map<String, String> map = new LinkedHashMap<String, String>();
                        contained.put(pack, map);
                        Resource pinfo = jar.getResource(prefix
                                + pack.replace('.', '/') + "/packageinfo");
                        if (pinfo != null) {
                            InputStream in = pinfo.openInputStream();
                            String version = parsePackageInfo(in);
                            in.close();
                            if (version != null)
                                map.put("version", version);
                        }
                    }
                }

                if (path.endsWith(".class")) {
                    Resource resource = jar.getResource(path);
                    Clazz clazz;

                    try {
                        InputStream in = resource.openInputStream();
                        clazz = new Clazz(relativePath, in);
                        in.close();
                    } catch (Throwable e) {
                        error("Invalid class file: " + relativePath, e);
                        e.printStackTrace();
                        continue next;
                    }

                    String calculatedPath = clazz.getClassName() + ".class";
                    if (!calculatedPath.equals(relativePath))
                        error("Class in different directory than declared. Path from class name is "
                                + calculatedPath
                                + " but the path in the jar is "
                                + relativePath
                                + " from " + jar);

                    classSpace.put(relativePath, clazz);
                    referred.putAll(clazz.getReferred());

                    // Add all the used packages
                    // to this package
                    Set<String> t = uses.get(pack);
                    if (t == null)
                        uses.put(pack, t = new LinkedHashSet<String>());
                    t.addAll(clazz.getReferred().keySet());
                    t.remove(pack);
                }
            }
        }
    }

    /**
     * Clean up version parameters. Other builders use more fuzzy definitions of
     * the version syntax. This method cleans up such a version to match an OSGi
     * version.
     * 
     * @param VERSION_STRING
     * @return
     */
    static Pattern fuzzyVersion      = Pattern
                                             .compile(
                                                     "(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
                                                     Pattern.DOTALL);
    static Pattern fuzzyVersionRange = Pattern
                                             .compile(
                                                     "(\\(|\\[)\\s*([-\\da-zA-Z.]+)\\s*,\\s*([-\\da-zA-Z.]+)\\s*(\\]|\\))",
                                                     Pattern.DOTALL);
    static Pattern fuzzyModifier     = Pattern.compile("(\\d+[.-])*(.*)",
                                             Pattern.DOTALL);

    static Pattern nummeric          = Pattern.compile("\\d*");

    static public String cleanupVersion(String version) {
        if (Verifier.VERSIONRANGE.matcher(version).matches())
            return version;

        Matcher m = fuzzyVersionRange.matcher(version);
        if (m.matches()) {
            String prefix = m.group(1);
            String first = m.group(2);
            String last = m.group(3);
            String suffix = m.group(4);
            return prefix + cleanupVersion(first) + "," + cleanupVersion(last)
                    + suffix;
        } else {
            m = fuzzyVersion.matcher(version);
            if (m.matches()) {
                StringBuffer result = new StringBuffer();
                String major = m.group(1);
                String minor = m.group(3);
                String micro = m.group(5);
                String qualifier = m.group(7);

                if (major != null) {
                    result.append(major);
                    if (minor != null) {
                        result.append(".");
                        result.append(minor);
                        if (micro != null) {
                            result.append(".");
                            result.append(micro);
                            if (qualifier != null) {
                                result.append(".");
                                cleanupModifier(result, qualifier);
                            }
                        } else if (qualifier != null) {
                            result.append(".0.");
                            cleanupModifier(result, qualifier);
                        }
                    } else if (qualifier != null) {
                        result.append(".0.0.");
                        cleanupModifier(result, qualifier);
                    }
                    return result.toString();
                }
            }
        }
        return version;
    }

    static void cleanupModifier(StringBuffer result, String modifier) {
        Matcher m = fuzzyModifier.matcher(modifier);
        if (m.matches())
            modifier = m.group(2);

        for (int i = 0; i < modifier.length(); i++) {
            char c = modifier.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')
                result.append(c);
        }
    }

    /**
     * Decide if the package is a metadata package.
     * 
     * @param pack
     * @return
     */
    boolean isMetaData(String pack) {
        for (int i = 0; i < METAPACKAGES.length; i++) {
            if (pack.startsWith(METAPACKAGES[i]))
                return true;
        }
        return false;
    }

    public String getPackage(String clazz) {
        int n = clazz.lastIndexOf('/');
        if (n < 0)
            return ".";
        return clazz.substring(0, n).replace('/', '.');
    }

    //
    // We accept more than correct OSGi versions because in a later
    // phase we actually cleanup maven versions. But it is a bit yucky
    //
    static String parsePackageInfo(InputStream jar) throws IOException {
        try {
            Properties p = new Properties();
            p.load(jar);
            jar.close();
            if (p.containsKey("version")) {
                return p.getProperty("version");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getVersionPolicy() {
        return getProperty(VERSIONPOLICY, "${version;==;${@}}");
    }

    /**
     * The extends macro traverses all classes and returns a list of class names
     * that extend a base class.
     */

    static String _classesHelp = "${classes;'implementing'|'extending'|'importing'|'named'|'version'|'any';<pattern>}, Return a list of class fully qualified class names that extend/implement/import any of the contained classes matching the pattern\n";

    public String _classes(String args[]) {
        // Macro.verifyCommand(args, _classesHelp, new
        // Pattern[]{null,Pattern.compile("(implementing|implements|extending|extends|importing|imports|any)"),
        // null}, 3,3);
        Set<Clazz> matched = new HashSet<Clazz>(classspace.values());
        for (int i = 1; i < args.length; i += 2) {
            if (args.length < i + 1)
                throw new IllegalArgumentException(
                        "${classes} macro must have odd number of arguments. "
                                + _classesHelp);

            String typeName = args[i];
            Clazz.QUERY type = null;
            if (typeName.equals("implementing")
                    || typeName.equals("implements"))
                type = Clazz.QUERY.IMPLEMENTS;
            else if (typeName.equals("extending") || typeName.equals("extends"))
                type = Clazz.QUERY.EXTENDS;
            else if (typeName.equals("importing") || typeName.equals("imports"))
                type = Clazz.QUERY.IMPORTS;
            else if (typeName.equals("all"))
                type = Clazz.QUERY.ANY;
            else if (typeName.equals("version"))
                type = Clazz.QUERY.VERSION;
            else if (typeName.equals("named"))
                type = Clazz.QUERY.NAMED;

            if (type == null)
                throw new IllegalArgumentException(
                        "${classes} has invalid type: " + typeName + ". "
                                + _classesHelp);
            // The argument is declared as a dotted name but the classes
            // use a slashed named. So convert the name before we make it a
            // instruction.
            String pattern = args[i + 1].replace('.', '/');
            Instruction instr = Instruction.getPattern(pattern);

            for (Iterator<Clazz> c = matched.iterator(); c.hasNext();) {
                Clazz clazz = c.next();
                if (!clazz.is(type, instr, classspace))
                    c.remove();
            }
        }
        if (matched.isEmpty())
            return "";

        return join(matched);
    }

    /**
     * Get the exporter of a package ...
     */

    public String _exporters(String args[]) throws Exception {
        Macro
                .verifyCommand(
                        args,
                        "${exporters;<packagename>}, returns the list of jars that export the given package",
                        null, 2, 2);
        StringBuilder sb = new StringBuilder();
        String del = "";
        String pack = args[1].replace('.', '/');
        for (Jar jar : classpath) {
            if (jar.getDirectories().containsKey(pack)) {
                sb.append(del);
                sb.append(jar.getName());
            }
        }
        return sb.toString();
    }

    public Map<String, Clazz> getClassspace() {
        return classspace;
    }

}
