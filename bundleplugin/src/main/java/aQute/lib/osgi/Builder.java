package aQute.lib.osgi;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;

import aQute.bnd.make.*;
import aQute.bnd.service.*;

/**
 * Include-Resource: ( [name '=' ] file )+
 * 
 * Private-Package: package-decl ( ',' package-decl )*
 * 
 * Export-Package: package-decl ( ',' package-decl )*
 * 
 * Import-Package: package-decl ( ',' package-decl )*
 * 
 * @version $Revision: 1.20 $
 */
public class Builder extends Analyzer {
    private static final int    SPLIT_MERGE_LAST  = 1;
    private static final int    SPLIT_MERGE_FIRST = 2;
    private static final int    SPLIT_ERROR       = 3;
    private static final int    SPLIT_FIRST       = 4;
    private static final int    SPLIT_DEFAULT     = 0;

    private static final File[] EMPTY_FILE        = new File[0];

    List<File>                  sourcePath        = new ArrayList<File>();
    Pattern                     NAME_URL          = Pattern
                                                          .compile("(.*)(http://.*)");

    Make                        make              = new Make(this);

    public Builder(Processor parent) {
        super(parent);
    }

    public Builder() {
    }

    public Jar build() throws Exception {
        if (getProperty(NOPE) != null)
            return null;

        String sub = getProperty(SUB);
        if (sub != null && sub.trim().length() > 0)
            error("Specified "
                    + SUB
                    + " but calls build() instead of builds() (might be a programmer error)");

        if (getProperty(CONDUIT) != null)
            error("Specified "
                    + CONDUIT
                    + " but calls build() instead of builds() (might be a programmer error");

        dot = new Jar("dot");
        addClose(dot);
        try {
            long modified = Long.parseLong(getProperty("base.modified"));
            dot.updateModified(modified, "Base modified");
        } catch (Exception e) {
        }

        doExpand(dot);
        doIncludeResources(dot);

        doConditional(dot);

        // NEW!
        // Check if we override the calculation of the
        // manifest. We still need to calculated it because
        // we need to have analyzed the classpath.

        Manifest manifest = calcManifest();

        String mf = getProperty(MANIFEST);
        if (mf != null) {
            File mff = getFile(mf);
            if (mff.isFile()) {
                try {
                    InputStream in = new FileInputStream(mff);
                    manifest = new Manifest(in);
                    in.close();
                } catch (Exception e) {
                    error(MANIFEST + " while reading manifest file", e);
                }
            } else {
                error(MANIFEST + ", no such file " + mf);
            }
        }

        if (getProperty(NOMANIFEST) == null)
            dot.setManifest(manifest);
        else
            dot.setNoManifest(true);

        // This must happen after we analyzed so
        // we know what it is on the classpath
        addSources(dot);
        if (getProperty(POM) != null)
            doPom(dot);

        doVerify(dot);

        if (dot.getResources().isEmpty())
            error("The JAR is empty");

        dot.updateModified(lastModified(), "Last Modified Processor");
        dot.setName(getBsn());

        sign(dot);
        return dot;
    }

    /**
     * Sign the jar file.
     * 
     * -sign : <alias> [ ';' 'password:=' <password> ] [ ';' 'keystore:='
     * <keystore> ] [ ';' 'sign-password:=' <pw> ] ( ',' ... )*
     * 
     * @return
     */

    void sign(Jar jar) throws Exception {
        String signing = getProperty("-sign");
        if (signing == null)
            return;

        trace("Signing %s, with %s", getBsn(), signing);
        List<SignerPlugin> signers = getPlugins(SignerPlugin.class);

        Map<String, Map<String, String>> infos = parseHeader(signing);
        for (Map.Entry<String, Map<String, String>> entry : infos.entrySet()) {
            for (SignerPlugin signer : signers) {
                signer.sign(this, entry.getKey());
            }
        }
    }

    public boolean hasSources() {
        return isTrue(getProperty(SOURCES));
    }

    protected String getImportPackages() {
        String ip = super.getImportPackages();
        if (ip != null)
            return ip;

        return "*";
    }

    private void doConditional(Jar dot) throws IOException {
        Map<String, Map<String, String>> conditionals = getHeader(CONDITIONAL_PACKAGE);
        if ( conditionals.isEmpty() )
            return;
        
        int size;
        do {
            size = dot.getDirectories().size();
            analyze();
            analyzed = false;
            Map<String, Map<String, String>> imports = getImports();

            // Match the packages specified in conditionals
            // against the imports. Any match must become a
            // Private-Package
            Map<String, Map<String, String>> filtered = merge(
                    CONDITIONAL_PACKAGE, conditionals, imports,
                    new HashSet<String>(), null);

            // Imports can also specify a private import. These
            // packages must also be copied to the bundle
            for (Map.Entry<String, Map<String, String>> entry : getImports()
                    .entrySet()) {
                String type = entry.getValue().get(IMPORT_DIRECTIVE);
                if (type != null && type.equals("private"))
                    filtered.put(entry.getKey(), entry.getValue());
            }

            // remove existing packages to prevent merge errors
            filtered.keySet().removeAll(dot.getPackages());
            doExpand(dot, CONDITIONAL_PACKAGE + " Private imports",
                    replaceWitInstruction(filtered, CONDITIONAL_PACKAGE), false);
        } while (dot.getDirectories().size() > size);
        analyzed = true;
    }

    /**
     * Intercept the call to analyze and cleanup versions after we have analyzed
     * the setup. We do not want to cleanup if we are going to verify.
     */

    public void analyze() throws IOException {
        super.analyze();
        cleanupVersion(imports);
        cleanupVersion(exports);
        String version = getProperty(BUNDLE_VERSION);
        if (version != null)
            setProperty(BUNDLE_VERSION, cleanupVersion(version));
    }

    public void cleanupVersion(Map<String, Map<String, String>> mapOfMap) {
        for (Iterator<Map.Entry<String, Map<String, String>>> e = mapOfMap
                .entrySet().iterator(); e.hasNext();) {
            Map.Entry<String, Map<String, String>> entry = e.next();
            Map<String, String> attributes = entry.getValue();
            if (attributes.containsKey("version")) {
                attributes.put("version", cleanupVersion(attributes
                        .get("version")));
            }
        }
    }

    /**
     * 
     */
    private void addSources(Jar dot) {
        if (!hasSources())
            return;

        Set<String> packages = new HashSet<String>();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getProperties().store(out, "Generated by BND, at " + new Date());
            dot.putResource("OSGI-OPT/bnd.bnd", new EmbeddedResource(out
                    .toByteArray(), 0));
            out.close();
        } catch (Exception e) {
            error("Can not embed bnd file in JAR: " + e);
        }

        for (Iterator<String> cpe = classspace.keySet().iterator(); cpe
                .hasNext();) {
            String path = cpe.next();
            path = path.substring(0, path.length() - ".class".length())
                    + ".java";
            String pack = getPackage(path).replace('.', '/');
            if (pack.length() > 1)
                pack = pack + "/";
            boolean found = false;
            String[] fixed = { "packageinfo", "package.html",
                    "module-info.java", "package-info.java" };
            for (Iterator<File> i = getSourcePath().iterator(); i.hasNext();) {
                File root = i.next();
                File f = getFile(root, path);
                if (f.exists()) {
                    found = true;
                    if (!packages.contains(pack)) {
                        packages.add(pack);
                        File bdir = getFile(root, pack);
                        for (int j = 0; j < fixed.length; j++) {
                            File ff = getFile(bdir, fixed[j]);
                            if (ff.isFile()) {
                                dot.putResource("OSGI-OPT/src/" + pack
                                        + fixed[j], new FileResource(ff));
                            }
                        }
                    }
                    dot
                            .putResource("OSGI-OPT/src/" + path,
                                    new FileResource(f));
                }
            }
            if (!found) {
                for (Jar jar : classpath) {
                    Resource resource = jar.getResource(path);
                    if (resource != null) {
                        dot.putResource("OSGI-OPT/src", resource);
                    } else {
                        resource = jar.getResource("OSGI-OPT/src/" + path);
                        if (resource != null) {
                            dot.putResource("OSGI-OPT/src", resource);
                        }
                    }
                }
            }
            if (getSourcePath().isEmpty())
                warning("Including sources but " + SOURCEPATH
                        + " does not contain any source directories ");
            // TODO copy from the jars where they came from
        }
    }

    boolean firstUse = true;

    public Collection<File> getSourcePath() {
        if (firstUse) {
            firstUse = false;
            String sp = getProperty(SOURCEPATH);
            if (sp != null) {
                Map<String, Map<String, String>> map = parseHeader(sp);
                for (Iterator<String> i = map.keySet().iterator(); i.hasNext();) {
                    String file = i.next();
                    if (!isDuplicate(file)) {
                        File f = getFile(file);
                        if (!f.isDirectory()) {
                            error("Adding a sourcepath that is not a directory: "
                                    + f);
                        } else {
                            sourcePath.add(f);
                        }
                    }
                }
            }
        }
        return sourcePath;
    }

    private void doVerify(Jar dot) throws Exception {
        Verifier verifier = new Verifier(dot, getProperties());
        verifier.setPedantic(isPedantic());

        // Give the verifier the benefit of our analysis
        // prevents parsing the files twice
        verifier.setClassSpace(classspace, contained, referred, uses);
        verifier.verify();
        getInfo(verifier);
    }

    private void doExpand(Jar jar) throws IOException {
        if (getClasspath().size() == 0
                && (getProperty(EXPORT_PACKAGE) != null || getProperty(PRIVATE_PACKAGE) != null))
            warning("Classpath is empty. Private-Package and Export-Package can only expand from the classpath when there is one");

        Map<Instruction, Map<String, String>> privateMap = replaceWitInstruction(
                getHeader(PRIVATE_PACKAGE), PRIVATE_PACKAGE);
        Map<Instruction, Map<String, String>> exportMap = replaceWitInstruction(
                getHeader(EXPORT_PACKAGE), EXPORT_PACKAGE);

        if (isTrue(getProperty(Constants.UNDERTEST))) {
            privateMap.putAll(replaceWitInstruction(parseHeader(getProperty(
                    Constants.TESTPACKAGES, "test;presence:=optional")),
                    TESTPACKAGES));
        }
        if (!privateMap.isEmpty())
            doExpand(jar, "Private-Package, or -testpackages", privateMap, true);

        if (!exportMap.isEmpty()) {
            Jar exports = new Jar("exports");
            doExpand(exports, "Export-Package", exportMap, true);
            jar.addAll(exports);
            exports.close();
        }

        if (privateMap.isEmpty() && exportMap.isEmpty() && !isResourceOnly()) {
            warning("Neither Export-Package, Private-Package, -testpackages is set, therefore no packages will be included");
        }
    }

    /**
     * 
     * @param jar
     * @param name
     * @param instructions
     */
    private void doExpand(Jar jar, String name,
            Map<Instruction, Map<String, String>> instructions,
            boolean mandatory) {
        Set<Instruction> superfluous = removeMarkedDuplicates(instructions
                .keySet());

        for (Iterator<Jar> c = getClasspath().iterator(); c.hasNext();) {
            Jar now = c.next();
            doExpand(jar, instructions, now, superfluous);
        }

        if (mandatory && superfluous.size() > 0) {
            StringBuffer sb = new StringBuffer();
            String del = "Instructions in " + name + " that are never used: ";
            for (Iterator<Instruction> i = superfluous.iterator(); i.hasNext();) {
                Instruction p = i.next();
                sb.append(del);
                sb.append(p.getPattern());
                del = ", ";
            }
            warning(sb.toString());
        }
    }

    /**
     * Iterate over each directory in the class path entry and check if that
     * directory is a desired package.
     * 
     * @param included
     * @param classpathEntry
     */
    private void doExpand(Jar jar,
            Map<Instruction, Map<String, String>> included, Jar classpathEntry,
            Set<Instruction> superfluous) {

        loop: for (Map.Entry<String, Map<String, Resource>> directory : classpathEntry
                .getDirectories().entrySet()) {
            String path = directory.getKey();

            if (doNotCopy.matcher(getName(path)).matches())
                continue;

            if (directory.getValue() == null)
                continue;

            String pack = path.replace('/', '.');
            Instruction instr = matches(included, pack, superfluous);
            if (instr != null) {
                // System.out.println("Pattern match: " + pack + " " +
                // instr.getPattern() + " " + instr.isNegated());
                if (!instr.isNegated()) {
                    Map<String, Resource> contents = directory.getValue();

                    // What to do with split packages? Well if this
                    // directory already exists, we will check the strategy
                    // and react accordingly.
                    boolean overwriteResource = true;
                    if (jar.hasDirectory(path)) {
                        Map<String, String> directives = included.get(instr);

                        switch (getSplitStrategy((String) directives
                                .get(SPLIT_PACKAGE_DIRECTIVE))) {
                        case SPLIT_MERGE_LAST:
                            overwriteResource = true;
                            break;

                        case SPLIT_MERGE_FIRST:
                            overwriteResource = false;
                            break;

                        case SPLIT_ERROR:
                            error(diagnostic(pack, getClasspath(),
                                    classpathEntry.source));
                            continue loop;

                        case SPLIT_FIRST:
                            continue loop;

                        default:
                            warning(diagnostic(pack, getClasspath(),
                                    classpathEntry.source));
                            overwriteResource = false;
                            break;
                        }
                    }

                    jar.addDirectory(contents, overwriteResource);

                    String key = path + "/bnd.info";
                    Resource r = jar.getResource(key);
                    if (r != null)
                        jar.putResource(key, new PreprocessResource(this, r));

                    if (hasSources()) {
                        String srcPath = "OSGI-OPT/src/" + path;
                        Map<String, Resource> srcContents = classpathEntry
                                .getDirectories().get(srcPath);
                        if (srcContents != null) {
                            jar.addDirectory(srcContents, overwriteResource);
                        }
                    }
                }
            }
        }
    }

    /**
     * Analyze the classpath for a split package
     * 
     * @param pack
     * @param classpath
     * @param source
     * @return
     */
    private String diagnostic(String pack, List<Jar> classpath, File source) {
        // Default is like merge-first, but with a warning
        // Find the culprits
        pack = pack.replace('.', '/');
        List<Jar> culprits = new ArrayList<Jar>();
        for (Iterator<Jar> i = classpath.iterator(); i.hasNext();) {
            Jar culprit = (Jar) i.next();
            if (culprit.getDirectories().containsKey(pack)) {
                culprits.add(culprit);
            }
        }
        return "Split package "
                + pack
                + "\nUse directive -split-package:=(merge-first|merge-last|error|first) on Export/Private Package instruction to get rid of this warning\n"
                + "Package found in   " + culprits + "\n"
                + "Reference from     " + source + "\n" + "Classpath          "
                + classpath;
    }

    private int getSplitStrategy(String type) {
        if (type == null)
            return SPLIT_DEFAULT;

        if (type.equals("merge-last"))
            return SPLIT_MERGE_LAST;

        if (type.equals("merge-first"))
            return SPLIT_MERGE_FIRST;

        if (type.equals("error"))
            return SPLIT_ERROR;

        if (type.equals("first"))
            return SPLIT_FIRST;

        error("Invalid strategy for split-package: " + type);
        return SPLIT_DEFAULT;
    }

    private Instruction matches(
            Map<Instruction, Map<String, String>> instructions, String pack,
            Set<Instruction> superfluousPatterns) {
        for (Instruction pattern : instructions.keySet()) {
            if (pattern.matches(pack)) {
                superfluousPatterns.remove(pattern);
                return pattern;
            }
        }
        return null;
    }

    private Map<String, Map<String, String>> getHeader(String string) {
        if (string == null)
            return Collections.emptyMap();
        return parseHeader(getProperty(string));
    }

    /**
     * Parse the Bundle-Includes header. Files in the bundles Include header are
     * included in the jar. The source can be a directory or a file.
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void doIncludeResources(Jar jar) throws Exception {
        String includes = getProperty("Bundle-Includes");
        if (includes == null) {
            includes = getProperty(INCLUDERESOURCE);
            if (includes == null)
                includes = getProperty("Include-Resource");
        } else
            warning("Please use -includeresource instead of Bundle-Includes");

        if (includes == null)
            return;

        Map<String, Map<String, String>> clauses = parseHeader(includes);

        for (Iterator<Map.Entry<String, Map<String, String>>> i = clauses
                .entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, Map<String, String>> entry = i.next();
            doIncludeResource(jar, entry.getKey(), entry.getValue());
        }
    }

    private void doIncludeResource(Jar jar, String name,
            Map<String, String> extra) throws ZipException, IOException,
            Exception {
        boolean preprocess = false;
        if (name.startsWith("{") && name.endsWith("}")) {
            preprocess = true;
            name = name.substring(1, name.length() - 1).trim();
        }

        if (name.startsWith("@")) {
            extractFromJar(jar, name.substring(1));
        } else if (extra.containsKey("literal")) {
            String literal = (String) extra.get("literal");
            Resource r = new EmbeddedResource(literal.getBytes("UTF-8"), 0);
            String x = (String) extra.get("extra");
            if (x != null)
                r.setExtra(x);
            jar.putResource(name, r);
        } else {
            String source;
            File sourceFile;
            String destinationPath;

            String parts[] = name.split("\\s*=\\s*");
            if (parts.length == 1) {
                // Just a copy, destination path defined by
                // source path.
                source = parts[0];
                sourceFile = getFile(source);
                // Directories should be copied to the root
                // but files to their file name ...
                if (sourceFile.isDirectory())
                    destinationPath = "";
                else
                    destinationPath = sourceFile.getName();
            } else {
                source = parts[1];
                sourceFile = getFile(source);
                destinationPath = parts[0];

                // Handle directories
                if (sourceFile.isDirectory()) {
                    destinationPath = doResourceDirectory(jar, extra,
                            preprocess, sourceFile, destinationPath);
                    return;
                }
            }

            //destinationPath = checkDestinationPath(destinationPath);

            if (!sourceFile.exists()) {
                noSuchFile(jar, name, extra, source, destinationPath);
            } else
                copy(jar, destinationPath, sourceFile, preprocess, extra);
        }
    }

    private String doResourceDirectory(Jar jar, Map<String, String> extra,
            boolean preprocess, File sourceFile, String destinationPath)
            throws Exception {
        String filter = extra.get("filter:");
        boolean flatten = isTrue(extra.get("flatten:"));
        boolean recursive = true;
        String directive = extra.get("recursive:");
        if (directive != null) {
            recursive = isTrue(directive);
        }

        InstructionFilter iFilter = null;
        if (filter != null) {
            iFilter = new InstructionFilter(Instruction
                    .getPattern(filter), recursive);
        } else {
            iFilter = new InstructionFilter(null, recursive);
        }

        destinationPath = checkDestinationPath(destinationPath);

        File[] files = resolveFiles(sourceFile, iFilter, recursive);
        for (File file : files) {
            String dp;
            if (flatten) {
                if (destinationPath.length() == 0) {
                    dp = file.getName();
                } else {
                    dp = destinationPath + "/"
                            + file.getName();
                }
            } else {
                dp = destinationPath
                        + file.getParentFile().getAbsolutePath()
                                .substring(
                                        sourceFile
                                                .getAbsolutePath()
                                                .length()).replace('\\','/');
                if (dp.length() > 0) {
                    dp += "/" + file.getName();
                } else {
                    dp = file.getName();
                }
            }
            copy(jar, dp, file, preprocess, extra);
        }
        return destinationPath;
    }

    private String checkDestinationPath(String destinationPath) {

        // Some people insist on ending a directory with
        // a slash ... it now also works if you do /=dir
        if (destinationPath.endsWith("/"))
            destinationPath = destinationPath.substring(0, destinationPath
                    .length() - 1);
        return destinationPath;
    }

    private File[] resolveFiles(File dir, FileFilter filter, boolean recursive) {
        return resolveFiles(dir, filter, null, recursive);
    }

    private File[] resolveFiles(File dir, FileFilter filter, File[] files,
            boolean recursive) {
        if (files == null) {
            files = EMPTY_FILE;
        }

        if (Analyzer.doNotCopy.matcher(dir.getName()).matches()) {
            return files;
        }

        File[] fs = dir.listFiles(filter);
        for (File file : fs) {
            if (file.isDirectory()) {
                if (recursive) {
                    files = resolveFiles(file, filter, files, recursive);
                }
            } else {
                if (files.length == 0) {
                    files = new File[] { file };
                } else {
                    File[] newFiles = new File[files.length + 1];
                    System.arraycopy(files, 0, newFiles, 0, files.length);
                    newFiles[newFiles.length - 1] = file;
                    files = newFiles;
                }
            }
        }
        return files;
    }

    private void noSuchFile(Jar jar, String clause, Map<String, String> extra,
            String source, String destinationPath) throws Exception {
        Jar src = getJarFromName(source, "Include-Resource " + source);
        if (src != null) {
            JarResource jarResource = new JarResource(src);
            jar.putResource(destinationPath, jarResource);
        } else {
            Resource lastChance = make.process(source);
            if (lastChance != null) {
                String x = extra.get("extra");
                if (x != null)
                    lastChance.setExtra(x);
                jar.putResource(destinationPath, lastChance);
            } else
                error("Input file does not exist: " + source);
        }
    }

    /**
     * Extra resources from a Jar and add them to the given jar. The clause is
     * the
     * 
     * @param jar
     * @param clauses
     * @param i
     * @throws ZipException
     * @throws IOException
     */
    private void extractFromJar(Jar jar, String name) throws ZipException,
            IOException {
        // Inline all resources and classes from another jar
        // optionally appended with a modified regular expression
        // like @zip.jar!/META-INF/MANIFEST.MF
        int n = name.lastIndexOf("!/");
        Pattern filter = null;
        if (n > 0) {
            String fstring = name.substring(n + 2);
            name = name.substring(0, n);
            filter = wildcard(fstring);
        }
        Jar sub = getJarFromName(name, "extract from jar");
        if (sub == null)
            error("Can not find JAR file " + name);
        else
            jar.addAll(sub, filter);
    }

    private Pattern wildcard(String spec) {
        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < spec.length(); j++) {
            char c = spec.charAt(j);
            switch (c) {
            case '.':
                sb.append("\\.");
                break;

            case '*':
                // test for ** (all directories)
                if (j < spec.length() - 1 && spec.charAt(j + 1) == '*') {
                    sb.append(".*");
                    j++;
                } else
                    sb.append("[^/]*");
                break;
            default:
                sb.append(c);
                break;
            }
        }
        String s = sb.toString();
        try {
            return Pattern.compile(s);
        } catch (Exception e) {
            error("Invalid regular expression on wildcarding: " + spec
                    + " used *");
        }
        return null;
    }

    private void copy(Jar jar, String path, File from, boolean preprocess,
            Map<String, String> extra) throws Exception {
        if (doNotCopy.matcher(from.getName()).matches())
            return;

        if (from.isDirectory()) {
            String next = path;
            if (next.length() != 0 && ! next.endsWith("/"))
                next += '/';

            File files[] = from.listFiles();
            for (int i = 0; i < files.length; i++) {
                copy(jar, next + files[i].getName(), files[i], preprocess,
                        extra);
            }
        } else {
            if (from.exists()) {
                Resource resource = new FileResource(from);
                if (preprocess) {
                    resource = new PreprocessResource(this, resource);
                }
                String x = extra.get("extra");
                if (x != null)
                    resource.setExtra(x);
                if ( path.endsWith("/"))
                    path = path + from.getName();
                jar.putResource(path, resource);
            } else {
                error("Input file does not exist: " + from);
            }
        }
    }

    private String getName(String where) {
        int n = where.lastIndexOf('/');
        if (n < 0)
            return where;

        return where.substring(n + 1);
    }

    public void setSourcepath(File[] files) {
        for (int i = 0; i < files.length; i++)
            addSourcepath(files[i]);
    }

    public void addSourcepath(File cp) {
        if (!cp.exists())
            warning("File on sourcepath that does not exist: " + cp);

        sourcePath.add(cp);
    }

    /**
     * Create a POM reseource for Maven containing as much information as
     * possible from the manifest.
     * 
     * @param output
     * @param builder
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void doPom(Jar dot) throws FileNotFoundException, IOException {
        {
            Manifest manifest = dot.getManifest();
            String name = manifest.getMainAttributes().getValue(
                    Analyzer.BUNDLE_NAME);
            String description = manifest.getMainAttributes().getValue(
                    Analyzer.BUNDLE_DESCRIPTION);
            String docUrl = manifest.getMainAttributes().getValue(
                    Analyzer.BUNDLE_DOCURL);
            String version = manifest.getMainAttributes().getValue(
                    Analyzer.BUNDLE_VERSION);
            String bundleVendor = manifest.getMainAttributes().getValue(
                    Analyzer.BUNDLE_VENDOR);
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(s);
            String bsn = manifest.getMainAttributes().getValue(
                    Analyzer.BUNDLE_SYMBOLICNAME);
            String licenses = manifest.getMainAttributes().getValue(
                    BUNDLE_LICENSE);

            if (bsn == null) {
                error("Can not create POM unless Bundle-SymbolicName is set");
                return;
            }

            bsn = bsn.trim();
            int n = bsn.lastIndexOf('.');
            if (n <= 0) {
                error("Can not create POM unless Bundle-SymbolicName contains a .");
                ps.close();
                s.close();
                return;
            }
            String groupId = bsn.substring(0, n);
            String artifactId = bsn.substring(n + 1);
            ps
                    .println("<project xmlns='http://maven.apache.org/POM/4.0.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd'>");
            ps.println("  <modelVersion>4.0.0</modelVersion>");
            ps.println("  <groupId>" + groupId + "</groupId>");

            n = artifactId.indexOf(';');
            if (n > 0)
                artifactId = artifactId.substring(0, n).trim();

            ps.println("  <artifactId>" + artifactId + "</artifactId>");
            ps.println("  <version>" + version + "</version>");
            if (description != null) {
                ps.println("  <description>");
                ps.print("    ");
                ps.println(description);
                ps.println("  </description>");
            }
            if (name != null) {
                ps.print("  <name>");
                ps.print(name);
                ps.println("</name>");
            }
            if (docUrl != null) {
                ps.print("  <url>");
                ps.print(docUrl);
                ps.println("</url>");
            }

            if (bundleVendor != null) {
                Matcher m = NAME_URL.matcher(bundleVendor);
                String namePart = bundleVendor;
                String urlPart = null;
                if (m.matches()) {
                    namePart = m.group(1);
                    urlPart = m.group(2);
                }
                ps.println("  <organization>");
                ps.print("    <name>");
                ps.print(namePart.trim());
                ps.println("</name>");
                if (urlPart != null) {
                    ps.print("    <url>");
                    ps.print(urlPart.trim());
                    ps.println("</url>");
                }
                ps.println("  </organization>");
            }
            if (licenses != null) {
                ps.println("  <licenses>");
                Map<String, Map<String, String>> map = parseHeader(licenses);
                for (Iterator<Map.Entry<String, Map<String, String>>> e = map
                        .entrySet().iterator(); e.hasNext();) {
                    Map.Entry<String, Map<String, String>> entry = e.next();
                    ps.println("    <license>");
                    Map<String, String> values = entry.getValue();
                    print(ps, values, "name", "name", (String) values
                            .get("url"));
                    print(ps, values, "url", "url", null);
                    print(ps, values, "distribution", "distribution", "repo");
                    ps.println("    </license>");
                }
                ps.println("  </licenses>");
            }
            ps.println("</project>");
            ps.close();
            s.close();
            dot
                    .putResource("pom.xml", new EmbeddedResource(s
                            .toByteArray(), 0));
        }
    }

    /**
     * Utility function to print a tag from a map
     * 
     * @param ps
     * @param values
     * @param string
     * @param tag
     * @param object
     */
    private void print(PrintStream ps, Map<String, String> values,
            String string, String tag, String object) {
        String value = (String) values.get(string);
        if (value == null)
            value = object;
        if (value == null)
            return;
        ps.println("    <" + tag + ">" + value.trim() + "</" + tag + ">");
    }

    public void close() {
        super.close();
    }

    /**
     * Build Multiple jars. If the -sub command is set, we filter the file with
     * the given patterns.
     * 
     * @return
     * @throws Exception
     */
    public Jar[] builds() throws Exception {
        begin();

        // Are we acting as a conduit for another JAR?
        String conduit = getProperty(CONDUIT);
        if (conduit != null) {
            Map<String, Map<String, String>> map = parseHeader(conduit);
            Jar[] result = new Jar[map.size()];
            int n = 0;
            for (String file : map.keySet()) {
                Jar c = new Jar(getFile(file));
                addClose(c);
                String name = map.get(file).get("name");
                if (name != null)
                    c.setName(name);

                result[n++] = c;
            }
            return result;
        }

        // If no -sub property, then reuse this builder object
        // other wise, build all the sub parts.
        String sub = getProperty(SUB);
        if (sub == null) {
            Jar jar = build();
            if (jar == null)
                return new Jar[0];

            return new Jar[] { jar };
        }

        List<Jar> result = new ArrayList<Jar>();

        // Get the Instruction objects that match the sub header
        Set<Instruction> subs = replaceWitInstruction(parseHeader(sub), SUB)
                .keySet();

        // Get the member files of this directory
        List<File> members = new ArrayList<File>(Arrays.asList(getBase()
                .listFiles()));

        getProperties().remove(SUB);
        // For each member file
        nextFile: while (members.size() > 0) {

            File file = members.remove(0);
            if (file.equals(getPropertiesFile()))
                continue nextFile;

            for (Iterator<Instruction> i = subs.iterator(); i.hasNext();) {

                Instruction instruction = i.next();
                if (instruction.matches(file.getName())) {

                    if (!instruction.isNegated()) {

                        Builder builder = null;
                        try {
                            builder = getSubBuilder();
                            addClose(builder);
                            builder.setProperties(file);
                            builder.setProperty(SUB, "");
                            // Recursively build
                            // TODO
                            Jar jar = builder.build();
                            jar.setName(builder.getBsn());
                            result.add(jar);
                        } catch (Exception e) {
                            e.printStackTrace();
                            error("Sub Building " + file, e);
                        }
                        if (builder != null)
                            getInfo(builder, file.getName() + ": ");
                    }

                    // Because we matched (even though we could be negated)
                    // we skip any remaining searches
                    continue nextFile;
                }
            }
        }
        setProperty(SUB, sub);
        return result.toArray(new Jar[result.size()]);
    }

    public Builder getSubBuilder() throws Exception {
        Builder builder = new Builder(this);
        builder.setBase(getBase());

        for (Jar file : getClasspath()) {
            builder.addClasspath(file);
        }

        return builder;
    }

    /**
     * A macro to convert a maven version to an OSGi version
     */

    public String _maven_version(String args[]) {
        if (args.length > 2)
            error("${maven_version} macro receives too many arguments "
                    + Arrays.toString(args));
        else if (args.length < 2)
            error("${maven_version} macro has no arguments, use ${maven_version;1.2.3-SNAPSHOT}");
        else {
            return cleanupVersion(args[1]);
        }
        return null;
    }

    public String _permissions(String args[]) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (String arg : args) {
            if ("packages".equals(arg) || "all".equals(arg)) {
                for (String imp : getImports().keySet()) {
                    if (!imp.startsWith("java.")) {
                        sb.append("(org.osgi.framework.PackagePermission \"");
                        sb.append(imp);
                        sb.append("\" \"import\")\r\n");
                    }
                }
                for (String exp : getExports().keySet()) {
                    sb.append("(org.osgi.framework.PackagePermission \"");
                    sb.append(exp);
                    sb.append("\" \"export\")\r\n");
                }
            } else if ("admin".equals(arg) || "all".equals(arg)) {
                sb.append("(org.osgi.framework.AdminPermission)");
            } else if ("permissions".equals(arg))
                ;
            else
                error("Invalid option in ${permissions}: %s", arg);
        }
        return sb.toString();
    }

    public void removeBundleSpecificHeaders() {
        Set<String> set = new HashSet<String>(Arrays
                .asList(BUNDLE_SPECIFIC_HEADERS));
        setForceLocal(set);
    }

}
