/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;

/**
 * Include-Resource: ( [name '=' ] file )+
 * 
 * Private-Package: package-decl ( ',' package-decl )*
 * 
 * Export-Package: package-decl ( ',' package-decl )*
 * 
 * Import-Package: package-decl ( ',' package-decl )*
 * 
 * @version $Revision: 1.4 $
 */
public class Builder extends Analyzer {
    private static final int SPLIT_MERGE_LAST  = 1;
    private static final int SPLIT_MERGE_FIRST = 2;
    private static final int SPLIT_ERROR       = 3;
    private static final int SPLIT_FIRST       = 4;
    private static final int SPLIT_DEFAULT     = 0;

    List                     tempJars          = new ArrayList();
    boolean                  sources           = false;
    File[]                   sourcePath;
    Pattern                  NAME_URL          = Pattern
                                                       .compile("(.*)(http://.*)");

    public Jar build() throws Exception {
        begin();

        dot = new Jar("dot");
        doPrebuild(dot, parseHeader(getProperty("-prebuild")));
        doExpand(dot);
        doIncludeResources(dot);

        doConditional(dot);

        dot.setManifest(calcManifest());
        // This must happen after we analyzed so
        // we know what it is on the classpath
        addSources(dot);
        if (getProperty(POM) != null)
            doPom(dot);

        doVerify(dot);
        if (dot.getResources().isEmpty())
            error("The JAR is empty");

        dot.updateModified(lastModified());
        return dot;
    }

    void begin() {
        super.begin();
        if (getProperty(IMPORT_PACKAGE) == null)
            setProperty(IMPORT_PACKAGE, "*");

        sources = getProperty(SOURCES) != null;
    }

    private void doConditional(Jar dot) throws IOException {
        Map conditionals = getHeader(CONDITIONAL_PACKAGE);
        if (conditionals != null && conditionals.size() > 0) {
            int size;
            do {
                size = dot.getDirectories().size();
                analyze();
                analyzed = false;
                Map imports = getImports();

                Map filtered = merge(CONDITIONAL_PACKAGE, conditionals,
                        imports, new HashSet());

                // remove existing packages to prevent merge errors
                filtered.keySet().removeAll(dot.getPackages());
                filtered = replaceWithPattern(filtered);
                doExpand(dot, CONDITIONAL_PACKAGE, filtered);
            } while (dot.getDirectories().size() > size);
        }
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

    public void cleanupVersion(Map mapOfMap) {
        for (Iterator e = mapOfMap.entrySet().iterator(); e.hasNext();) {
            Map.Entry entry = (Map.Entry) e.next();
            Map attributes = (Map) entry.getValue();
            if (attributes.containsKey("version")) {
                attributes.put("version", cleanupVersion((String) attributes
                        .get("version")));
            }
        }
    }

    /**
     * Clean up version parameters. Other builders use more fuzzy definitions of
     * the version syntax. This method cleans up such a version to match an OSGi
     * version.
     * 
     * @param version
     * @return
     */
    static Pattern fuzzyVersion  = Pattern
                                         .compile(
                                                 "(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
                                                 Pattern.DOTALL);
    static Pattern fuzzyModifier = Pattern.compile("(\\d+[.-])*(.*)",
                                         Pattern.DOTALL);

    static Pattern nummeric      = Pattern.compile("\\d*");

    static public String cleanupVersion(String version) {
        Matcher m = fuzzyVersion.matcher(version);
        if (m.matches()) {
            StringBuffer result = new StringBuffer();
            String d1 = m.group(1);
            String d2 = m.group(3);
            String d3 = m.group(5);
            String qualifier = m.group(7);

            if (d1 != null) {
                result.append(d1);
                if (d2 != null) {
                    result.append(".");
                    result.append(d2);
                    if (d3 != null) {
                        result.append(".");
                        result.append(d3);
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
     * 
     */
    private void addSources(Jar dot) {
        if (!sources)
            return;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getProperties().store(out, "Generated by BND, at " + new Date());
            dot.putResource("OSGI-OPT/bnd.bnd", new EmbeddedResource(out
                    .toByteArray(), 0));
            out.close();
        } catch (Exception e) {
            error("Can not embed bnd file in JAR: " + e);
        }

        for (Iterator cpe = classspace.keySet().iterator(); cpe.hasNext();) {
            String path = (String) cpe.next();
            path = path.substring(0, path.length() - ".class".length())
                    + ".java";

            for (int i = 0; i < sourcePath.length; i++) {
                File root = sourcePath[i];
                File f = getFile(root, path);
                if (f.exists()) {
                    dot
                            .putResource("OSGI-OPT/src/" + path,
                                    new FileResource(f));
                }
            }
        }
    }

    private void doVerify(Jar dot) throws Exception {
        Verifier verifier = new Verifier(dot, getProperties());
        verifier.setPedantic(isPedantic());
        verifier.verify();
        errors.addAll(verifier.getErrors());
        warnings.addAll(verifier.getWarnings());
    }

    private void doExpand(Jar jar) throws IOException {
        if (classpath.size() == 0
                && (getProperty(EXPORT_PACKAGE) != null || getProperty(PRIVATE_PACKAGE) != null))
            warning("Classpath is empty. Private-Package and Export-Package can only expand from the classpath when there is one");

        Map prive = replaceWithPattern(getHeader(Analyzer.PRIVATE_PACKAGE));
        Map export = replaceWithPattern(getHeader(Analyzer.EXPORT_PACKAGE));
        if (prive.isEmpty() && export.isEmpty()) {
            warnings
                    .add("Neither Export-Package nor Private-Package is set, therefore no packages will be included");
        }
        doExpand(jar, EXPORT_PACKAGE, export);
        doExpand(jar, PRIVATE_PACKAGE, prive);
    }

    private void doExpand(Jar jar, String name, Map instructions) {
        Set superfluous = new HashSet(instructions.keySet());
        for (Iterator c = classpath.iterator(); c.hasNext();) {
            Jar now = (Jar) c.next();
            doExpand(jar, instructions, now, superfluous);
        }
        if (superfluous.size() > 0) {
            StringBuffer sb = new StringBuffer();
            String del = "Instructions for " + name + " that are never used: ";
            for (Iterator i = superfluous.iterator(); i.hasNext();) {
                Instruction p = (Instruction) i.next();
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
    private void doExpand(Jar jar, Map included, Jar classpathEntry,
            Set superfluous) {

        loop: for (Iterator p = classpathEntry.getDirectories().entrySet()
                .iterator(); p.hasNext();) {
            Map.Entry directory = (Map.Entry) p.next();
            String path = (String) directory.getKey();

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
                    Map contents = (Map) directory.getValue();

                    // What to do with split packages? Well if this
                    // directory already exists, we will check the strategy
                    // and react accordingly.
                    boolean overwriteResource = true;
                    if (jar.hasDirectory(path)) {
                        Map directives = (Map) included.get(instr);

                        switch (getSplitStrategy((String) directives
                                .get(SPLIT_PACKAGE_DIRECTIVE))) {
                        case SPLIT_MERGE_LAST:
                            overwriteResource = true;
                            break;

                        case SPLIT_MERGE_FIRST:
                            overwriteResource = false;
                            break;

                        case SPLIT_ERROR:
                            error("Split package generates error: " + pack);
                            continue loop;

                        case SPLIT_FIRST:
                            continue loop;

                        default:
                            // Default is like merge-first, but with a warning
                            warning("There are split packages, use directive -split-package:=(merge-first|merge-last) on instruction to get rid of this warning: "
                                    + pack
                                    + ", classpath: "
                                    + classpath
                                    + " from: " + classpathEntry.source);
                            overwriteResource = false;
                            break;
                        }
                    }
                    jar.addDirectory(contents, overwriteResource);
                }
            }
        }
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

    private Map replaceWithPattern(Map header) {
        Map map = new LinkedHashMap();
        for (Iterator e = header.entrySet().iterator(); e.hasNext();) {
            Map.Entry entry = (Map.Entry) e.next();
            String pattern = (String) entry.getKey();
            Instruction instr = Instruction.getPattern(pattern);
            map.put(instr, entry.getValue());
        }
        return map;
    }

    private Instruction matches(Map instructions, String pack,
            Set superfluousPatterns) {
        for (Iterator i = instructions.keySet().iterator(); i.hasNext();) {
            Instruction pattern = (Instruction) i.next();
            if (pattern.matches(pack)) {
                superfluousPatterns.remove(pattern);
                return pattern;
            }
        }
        return null;
    }

    private Map getHeader(String string) {
        if (string == null)
            return new LinkedHashMap();
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
        Macro macro = new Macro(getProperties(), this);

        String includes = getProperty("Bundle-Includes");
        if (includes == null)
            includes = getProperty("Include-Resource");
        else
            warnings
                    .add("Please use Include-Resource instead of Bundle-Includes");

        if (includes == null)
            return;

        for (Iterator i = getClauses(includes).iterator(); i.hasNext();) {
            boolean preprocess = false;
            String clause = (String) i.next();
            if (clause.startsWith("{") && clause.endsWith("}")) {
                preprocess = true;
                clause = clause.substring(1, clause.length() - 1).trim();
            }

            Map extra = new HashMap();
            int n = clause.indexOf(';');
            if (n > 0) {
                String attributes = clause.substring(n + 1);
                String parts[] = attributes.split("\\s*;\\s*");
                for (int j = 0; j < parts.length; j++) {
                    String assignment[] = parts[j].split("\\s*=\\s*");
                    if (assignment.length == 2)
                        extra.put(assignment[0], assignment[1]);
                    else
                        error("Invalid attribute on Include-Resource: "
                                + clause);
                }
                clause = clause.substring(0, n);
            }

            if (clause.startsWith("@")) {
                extractFromJar(jar, clause.substring(1));
            } else {
                String parts[] = clause.split("\\s*=\\s*");

                String source;
                File sourceFile;
                String destinationPath;

                if (parts.length == 1) {
                    // Just a copy, destination path defined by
                    // source path.
                    source = parts[0];
                    sourceFile = getFile(base, source);
                    // Directories should be copied to the root
                    // but files to their file name ...
                    if (sourceFile.isDirectory())
                        destinationPath = "";
                    else
                        destinationPath = sourceFile.getName();
                } else {
                    source = parts[1];
                    sourceFile = getFile(base, source);
                    destinationPath = parts[0];
                }

                // Some people insist on ending a directory with
                // a slash ... it now also works if you do /=dir
                if (destinationPath.endsWith("/"))
                    destinationPath = destinationPath.substring(0,
                            destinationPath.length() - 1);

                if (!sourceFile.exists()) {
                    Jar src = getJarFromName(source, "Include-Resource "
                            + source);
                    if (src != null) {
                        JarResource jarResource = new JarResource(src);
                        jar.putResource(destinationPath, jarResource);
                    } else {
                        error("Input file does not exist: " + source);
                    }
                } else
                    copy(jar, destinationPath, sourceFile, preprocess ? macro
                            : null, extra);
            }
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
        else {
            jar.addAll(sub, filter);
            tempJars.add(sub);
        }
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

    private void copy(Jar jar, String path, File from, Macro macro, Map extra)
            throws Exception {
        if (doNotCopy.matcher(from.getName()).matches())
            return;

        if (from.isDirectory()) {
            String next = path;
            if (next.length() != 0)
                next += '/';

            File files[] = from.listFiles();
            for (int i = 0; i < files.length; i++) {
                copy(jar, next + files[i].getName(), files[i], macro, extra);
            }
        } else {
            if (from.exists()) {
                if (macro != null) {
                    String content = read(from);
                    content = macro.process(content);
                    Resource resource = new EmbeddedResource(content
                            .getBytes("UTF-8"), from.lastModified());

                    String x = (String) extra.get("extra");
                    if (x != null)
                        resource.setExtra(x);
                    jar.putResource(path, resource);
                } else
                    jar.putResource(path, new FileResource(from));
            } else {
                error("Input file does not exist: " + from);
            }
        }
    }

    private String read(File from) throws Exception {
        long size = from.length();
        byte[] buffer = new byte[(int) size];
        FileInputStream in = new FileInputStream(from);
        in.read(buffer);
        in.close();
        return new String(buffer, "UTF-8");
    }

    private String getName(String where) {
        int n = where.lastIndexOf('/');
        if (n < 0)
            return where;

        return where.substring(n + 1);
    }

    public void setSourcepath(File[] files) {
        sourcePath = files;
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
                errors
                        .add("Can not create POM unless Bundle-SymbolicName is set");
                return;
            }

            bsn = bsn.trim();
            int n = bsn.lastIndexOf('.');
            if (n <= 0) {
                errors
                        .add("Can not create POM unless Bundle-SymbolicName contains a .");
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
                Map map = parseHeader(licenses);
                for (Iterator e = map.entrySet().iterator(); e.hasNext();) {
                    Map.Entry entry = (Map.Entry) e.next();
                    ps.println("    <license>");
                    Map values = (Map) entry.getValue();
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
     * NEW
     * 
     */
    void doPrebuild(Jar dot, Map clauses) {
        for (Iterator i = clauses.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            Map args = (Map) entry.getValue();
            File file = getFile(base, name);
            File dir = file.getParentFile();
            if (!dir.exists())
                error("No directory for prebuild: " + file.getAbsolutePath());
            else {
                Instruction instr = Instruction.getPattern(file.getName());
                File[] children = dir.listFiles();
                for (int c = 0; c < children.length; c++) {
                    File child = children[c];
                    if (instr.matches(child.getName()) && !instr.isNegated()) {
                        try {
                            progress("Prebuilding "+child);
                            doPrebuild(dot, child, args);
                        } catch (Exception e) {
                            error("Can not build: "+child);
                        }
                    }
                }
            }
        }
    }

    void doPrebuild(Jar dot, File f, Map args) throws Exception {
        Builder builder = new Builder();
        builder.setBase(base);
        builder.setProperties(f);

        Properties p = new Properties();
        p.putAll(getProperties());
        p.keySet().removeAll(builder.getProperties().keySet());
        builder.getProperties().putAll(p);

        Jar jar = builder.build();
        String path = (String) args.get("path");
        if (path != null)
            path = f.getName();

        dot.putResource(path, new JarResource(jar));
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
    private void print(PrintStream ps, Map values, String string, String tag,
            String object) {
        String value = (String) values.get(string);
        if (value == null)
            value = object;
        if (value == null)
            return;
        ps.println("    <" + tag + ">" + value.trim() + "</" + tag + ">");
    }

    public void close() {
        for (Iterator j = tempJars.iterator(); j.hasNext();) {
            Jar jar = (Jar) j.next();
            jar.close();
        }
        super.close();
    }
}
