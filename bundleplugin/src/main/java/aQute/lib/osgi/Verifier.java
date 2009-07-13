/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.libg.qtokens.*;

public class Verifier extends Analyzer {

    Jar                              dot;
    Manifest                         manifest;
    Map<String, Map<String, String>> referred              = newHashMap();
    Map<String, Map<String, String>> contained             = newHashMap();
    Map<String, Set<String>>         uses                  = newHashMap();
    Map<String, Map<String, String>> mimports;
    Map<String, Map<String, String>> mdynimports;
    Map<String, Map<String, String>> mexports;
    List<Jar>                        bundleClasspath;
    Map<String, Map<String, String>> ignore                = newHashMap();                                                         // Packages
    // to
    // ignore

    Map<String, Clazz>               classSpace;
    boolean                          r3;
    boolean                          usesRequire;
    boolean                          fragment;
    Attributes                       main;

    final static Pattern             EENAME                = Pattern
                                                                   .compile("CDC-1\\.0/Foundation-1\\.0"
                                                                           + "|CDC-1\\.1/Foundation-1\\.1"
                                                                           + "|OSGi/Minimum-1\\.[1-9]"
                                                                           + "|JRE-1\\.1"
                                                                           + "|J2SE-1\\.2"
                                                                           + "|J2SE-1\\.3"
                                                                           + "|J2SE-1\\.4"
                                                                           + "|J2SE-1\\.5"
                                                                           + "|JavaSE-1\\.6"
                                                                           + "|JavaSE-1\\.7"
                                                                           + "|PersonalJava-1\\.1"
                                                                           + "|PersonalJava-1\\.2"
                                                                           + "|CDC-1\\.0/PersonalBasis-1\\.0"
                                                                           + "|CDC-1\\.0/PersonalJava-1\\.0");

    final static Pattern             BUNDLEMANIFESTVERSION = Pattern
                                                                   .compile("2");
    public final static String       SYMBOLICNAME_STRING   = "[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*";
    public final static Pattern      SYMBOLICNAME          = Pattern
                                                                   .compile(SYMBOLICNAME_STRING);

    public final static String       VERSION_STRING        = "[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[0-9A-Za-z_-]+)?)?)?";
    public final static Pattern      VERSION               = Pattern
                                                                   .compile(VERSION_STRING);
    final static Pattern             FILTEROP              = Pattern
                                                                   .compile("=|<=|>=|~=");
    public final static Pattern             VERSIONRANGE          = Pattern
                                                                   .compile("((\\(|\\[)"
                                                                           + VERSION_STRING
                                                                           + ","
                                                                           + VERSION_STRING
                                                                           + "(\\]|\\)))|"
                                                                           + VERSION_STRING);
    final static Pattern             FILE                  = Pattern
                                                                   .compile("/?[^/\"\n\r\u0000]+(/[^/\"\n\r\u0000]+)*");
    final static Pattern             WILDCARDPACKAGE       = Pattern
                                                                   .compile("((\\p{Alnum}|_)+(\\.(\\p{Alnum}|_)+)*(\\.\\*)?)|\\*");
    public final static Pattern      ISO639                = Pattern
                                                                   .compile("[A-Z][A-Z]");
    public final static Pattern      HEADER_PATTERN        = Pattern
                                                                   .compile("[A-Za-z0-9][-a-zA-Z0-9_]+");
    public final static Pattern      TOKEN                 = Pattern
                                                                   .compile("[-a-zA-Z0-9_]+");

    public final static Pattern      NUMBERPATTERN         = Pattern
                                                                   .compile("\\d+");
    public final static Pattern      PATHPATTERN           = Pattern
                                                                   .compile(".*");
    public final static Pattern      FQNPATTERN            = Pattern
                                                                   .compile(".*");
    public final static Pattern      URLPATTERN            = Pattern
                                                                   .compile(".*");
    public final static Pattern      ANYPATTERN            = Pattern
                                                                   .compile(".*");
    public final static Pattern      FILTERPATTERN         = Pattern
                                                                   .compile(".*");
    public final static Pattern TRUEORFALSEPATTERN = Pattern.compile("true|false|TRUE|FALSE");
    public static final Pattern WILDCARDNAMEPATTERN = Pattern.compile(".*");
    
    public final static String EES[] = {
        "CDC-1.0/Foundation-1.0",
        "CDC-1.1/Foundation-1.1",
        "OSGi/Minimum-1.0",
        "OSGi/Minimum-1.1",
        "OSGi/Minimum-1.2",
        "JRE-1.1",
        "J2SE-1.2",
        "J2SE-1.3",
        "J2SE-1.4",
        "J2SE-1.5",
        "JavaSE-1.6",
        "JavaSE-1.7",
        "PersonalJava-1.1",
        "PersonalJava-1.2",
        "CDC-1.0/PersonalBasis-1.0",
        "CDC-1.0/PersonalJava-1.0"
        };
    
    public final static String       OSNAMES[]             = {
            "AIX", // IBM
            "DigitalUnix", // Compaq
            "Embos", // Segger Embedded Software Solutions
            "Epoc32", // SymbianOS Symbian OS
            "FreeBSD", // Free BSD
            "HPUX", // hp-ux Hewlett Packard
            "IRIX", // Silicon Graphics
            "Linux", // Open source
            "MacOS", // Apple
            "NetBSD", // Open source
            "Netware", // Novell
            "OpenBSD", // Open source
            "OS2", // OS/2 IBM
            "QNX", // procnto QNX
            "Solaris", // Sun (almost an alias of SunOS)
            "SunOS", // Sun Microsystems
            "VxWorks", // WindRiver Systems
            "Windows95", "Win32", "Windows98", "WindowsNT", "WindowsCE",
            "Windows2000", // Win2000
            "Windows2003", // Win2003
            "WindowsXP", "WindowsVista",                  };

    public final static String       PROCESSORNAMES[]      = { "68k", // Motorola
            // 68000
            "ARM_LE", // Intel Strong ARM. Deprecated because it does not
            // specify the endianness. See the following two rows.
            "arm_le", // Intel Strong ARM Little Endian mode
            "arm_be", // Intel String ARM Big Endian mode
            "Alpha", //
            "ia64n",// Hewlett Packard 64 bit
            "ia64w",// Hewlett Packard 32 bit mode
            "Ignite", // psc1k PTSC
            "Mips", // SGI
            "PArisc", // Hewlett Packard
            "PowerPC", // power ppc Motorola/IBM Power PC
            "Sh4", // Hitachi
            "Sparc", // SUN
            "S390", // IBM Mainframe 31 bit
            "S390x", // IBM Mainframe 64-bit
            "V850E", // NEC V850E
            "x86", // pentium i386
            "i486", // i586 i686 Intel& AMD 32 bit
            "x86-64",                                     };

    Properties                       properties;

    public Verifier(Jar jar) throws Exception {
        this(jar, null);
    }

    public Verifier(Jar jar, Properties properties) throws Exception {
        this.dot = jar;
        this.properties = properties;
        this.manifest = jar.getManifest();
        if (manifest == null) {
            manifest = new Manifest();
            error("This file contains no manifest and is therefore not a bundle");
        }
        main = this.manifest.getMainAttributes();
        verifyHeaders(main);
        r3 = getHeader(Analyzer.BUNDLE_MANIFESTVERSION) == null;
        usesRequire = getHeader(Analyzer.REQUIRE_BUNDLE) != null;
        fragment = getHeader(Analyzer.FRAGMENT_HOST) != null;

        bundleClasspath = getBundleClassPath();
        mimports = parseHeader(manifest.getMainAttributes().getValue(
                Analyzer.IMPORT_PACKAGE));
        mdynimports = parseHeader(manifest.getMainAttributes().getValue(
                Analyzer.DYNAMICIMPORT_PACKAGE));
        mexports = parseHeader(manifest.getMainAttributes().getValue(
                Analyzer.EXPORT_PACKAGE));

        ignore = parseHeader(manifest.getMainAttributes().getValue(
                Analyzer.IGNORE_PACKAGE));
    }

    public Verifier() {
        // TODO Auto-generated constructor stub
    }

    private void verifyHeaders(Attributes main) {
        for (Object element : main.keySet()) {
            Attributes.Name header = (Attributes.Name) element;
            String h = header.toString();
            if (!HEADER_PATTERN.matcher(h).matches())
                error("Invalid Manifest header: " + h + ", pattern="
                        + HEADER_PATTERN);
        }
    }

    private List<Jar> getBundleClassPath() {
        List<Jar> list = newList();
        String bcp = getHeader(Analyzer.BUNDLE_CLASSPATH);
        if (bcp == null) {
            list.add(dot);
        } else {
            Map<String, Map<String, String>> entries = parseHeader(bcp);
            for (String jarOrDir : entries.keySet()) {
                if (jarOrDir.equals(".")) {
                    list.add(dot);
                } else {
                    if (jarOrDir.equals("/"))
                        jarOrDir = "";
                    if (jarOrDir.endsWith("/")) {
                        error("Bundle-Classpath directory must not end with a slash: "
                                + jarOrDir);
                        jarOrDir = jarOrDir.substring(0, jarOrDir.length() - 1);
                    }

                    Resource resource = dot.getResource(jarOrDir);
                    if (resource != null) {
                        try {
                            Jar sub = new Jar(jarOrDir);
                            addClose(sub);
                            EmbeddedResource.build(sub, resource);
                            if (!jarOrDir.endsWith(".jar"))
                                warning("Valid JAR file on Bundle-Classpath does not have .jar extension: "
                                        + jarOrDir);
                            list.add(sub);
                        } catch (Exception e) {
                            error("Invalid embedded JAR file on Bundle-Classpath: "
                                    + jarOrDir + ", " + e);
                        }
                    } else if (dot.getDirectories().containsKey(jarOrDir)) {
                        if (r3)
                            error("R3 bundles do not support directories on the Bundle-ClassPath: "
                                    + jarOrDir);

                        try {
                            Jar sub = new Jar(jarOrDir);
                            addClose(sub);
                            for (Map.Entry<String, Resource> entry : dot
                                    .getResources().entrySet()) {
                                if (entry.getKey().startsWith(jarOrDir))
                                    sub.putResource(entry.getKey().substring(
                                            jarOrDir.length() + 1), entry
                                            .getValue());
                            }
                            list.add(sub);
                        } catch (Exception e) {
                            error("Invalid embedded directory file on Bundle-Classpath: "
                                    + jarOrDir + ", " + e);
                        }
                    } else {
                        error("Cannot find a file or directory for Bundle-Classpath entry: "
                                + jarOrDir);
                    }
                }
            }
        }
        return list;
    }

    /*
     * Bundle-NativeCode ::= nativecode ( ',' nativecode )* ( ’,’ optional) ?
     * nativecode ::= path ( ';' path )* // See 1.4.2 ( ';' parameter )+
     * optional ::= ’*’
     */
    public void verifyNative() {
        String nc = getHeader("Bundle-NativeCode");
        doNative(nc);
    }

    public void doNative(String nc) {
        if (nc != null) {
            QuotedTokenizer qt = new QuotedTokenizer(nc, ",;=", false);
            char del;
            do {
                do {
                    String name = qt.nextToken();
                    if (name == null) {
                        error("Can not parse name from bundle native code header: "
                                + nc);
                        return;
                    }
                    del = qt.getSeparator();
                    if (del == ';') {
                        if (dot != null && !dot.exists(name)) {
                            error("Native library not found in JAR: " + name);
                        }
                    } else {
                        String value = null;
                        if (del == '=')
                            value = qt.nextToken();

                        String key = name.toLowerCase();
                        if (key.equals("osname")) {
                            // ...
                        } else if (key.equals("osversion")) {
                            // verify version range
                            verify(value, VERSIONRANGE);
                        } else if (key.equals("language")) {
                            verify(value, ISO639);
                        } else if (key.equals("processor")) {
                            // verify(value, PROCESSORS);
                        } else if (key.equals("selection-filter")) {
                            // verify syntax filter
                            verifyFilter(value);
                        } else if (name.equals("*") && value == null) {
                            // Wildcard must be at end.
                            if (qt.nextToken() != null)
                                error("Bundle-Native code header may only END in wildcard: nc");
                        } else {
                            warning("Unknown attribute in native code: " + name
                                    + "=" + value);
                        }
                        del = qt.getSeparator();
                    }
                } while (del == ';');
            } while (del == ',');
        }
    }

    public void verifyFilter(String value) {
        try {
            verifyFilter(value, 0);
        } catch (Exception e) {
            error("Not a valid filter: " + value + e.getMessage());
        }
    }

    private void verifyActivator() {
        String bactivator = getHeader("Bundle-Activator");
        if (bactivator != null) {
            Clazz cl = loadClass(bactivator);
            if (cl == null) {
                int n = bactivator.lastIndexOf('.');
                if (n > 0) {
                    String pack = bactivator.substring(0, n);
                    if (mimports.containsKey(pack))
                        return;
                    error("Bundle-Activator not found on the bundle class path nor in imports: "
                            + bactivator);
                } else
                    error("Activator uses default package and is not local (default package can not be imported): "
                            + bactivator);
            }
        }
    }

    private Clazz loadClass(String className) {
        String path = className.replace('.', '/') + ".class";
        return (Clazz) classSpace.get(path);
    }

    private void verifyComponent() {
        String serviceComponent = getHeader("Service-Component");
        if (serviceComponent != null) {
            Map<String, Map<String, String>> map = parseHeader(serviceComponent);
            for (String component : map.keySet()) {
                if (component.indexOf("*")<0 && !dot.exists(component)) {
                    error("Service-Component entry can not be located in JAR: "
                            + component);
                } else {
                    // validate component ...
                }
            }
        }
    }

    public void info() {
        System.out.println("Refers                           : " + referred);
        System.out.println("Contains                         : " + contained);
        System.out.println("Manifest Imports                 : " + mimports);
        System.out.println("Manifest Exports                 : " + mexports);
    }

    /**
     * Invalid exports are exports mentioned in the manifest but not found on
     * the classpath. This can be calculated with: exports - contains.
     * 
     * Unfortunately, we also must take duplicate names into account. These
     * duplicates are of course no erroneous.
     */
    private void verifyInvalidExports() {
        Set<String> invalidExport = newSet(mexports.keySet());
        invalidExport.removeAll(contained.keySet());

        // We might have duplicate names that are marked for it. These
        // should not be counted. Should we test them against the contained
        // set? Hmm. If someone wants to hang himself by using duplicates than
        // I guess he can go ahead ... This is not a recommended practice
        for (Iterator<String> i = invalidExport.iterator(); i.hasNext();) {
            String pack = i.next();
            if (isDuplicate(pack))
                i.remove();
        }

        if (!invalidExport.isEmpty())
            error("Exporting packages that are not on the Bundle-Classpath"
                    + bundleClasspath + ": " + invalidExport);
    }

    /**
     * Invalid imports are imports that we never refer to. They can be
     * calculated by removing the refered packages from the imported packages.
     * This leaves packages that the manifest imported but that we never use.
     */
    private void verifyInvalidImports() {
        Set<String> invalidImport = newSet(mimports.keySet());
        invalidImport.removeAll(referred.keySet());
        // TODO Added this line but not sure why it worked before ...
        invalidImport.removeAll(contained.keySet());
        String bactivator = getHeader(Analyzer.BUNDLE_ACTIVATOR);
        if (bactivator != null) {
            int n = bactivator.lastIndexOf('.');
            if (n > 0) {
                invalidImport.remove(bactivator.substring(0, n));
            }
        }
        if (isPedantic() && !invalidImport.isEmpty())
            warning("Importing packages that are never refered to by any class on the Bundle-Classpath"
                    + bundleClasspath + ": " + invalidImport);
    }

    /**
     * Check for unresolved imports. These are referals that are not imported by
     * the manifest and that are not part of our bundle classpath. The are
     * calculated by removing all the imported packages and contained from the
     * refered packages.
     */
    private void verifyUnresolvedReferences() {
        Set<String> unresolvedReferences = new TreeSet<String>(referred
                .keySet());
        unresolvedReferences.removeAll(mimports.keySet());
        unresolvedReferences.removeAll(contained.keySet());

        // Remove any java.** packages.
        for (Iterator<String> p = unresolvedReferences.iterator(); p.hasNext();) {
            String pack = p.next();
            if (pack.startsWith("java.") || ignore.containsKey(pack))
                p.remove();
            else {
                // Remove any dynamic imports
                if (isDynamicImport(pack))
                    p.remove();
            }
        }

        if (!unresolvedReferences.isEmpty()) {
            // Now we want to know the
            // classes that are the culprits
            Set<String> culprits = new HashSet<String>();
            for (Clazz clazz : classSpace.values()) {
                if (hasOverlap(unresolvedReferences, clazz.imports.keySet()))
                    culprits.add(clazz.getPath());
            }

            error("Unresolved references to " + unresolvedReferences
                    + " by class(es) on the Bundle-Classpath" + bundleClasspath
                    + ": " + culprits);
        }
    }

    /**
     * @param p
     * @param pack
     */
    private boolean isDynamicImport(String pack) {
        for (String pattern : mdynimports.keySet()) {
            // Wildcard?
            if (pattern.equals("*"))
                return true; // All packages can be dynamically imported

            if (pattern.endsWith(".*")) {
                pattern = pattern.substring(0, pattern.length() - 2);
                if (pack.startsWith(pattern)
                        && (pack.length() == pattern.length() || pack
                                .charAt(pattern.length()) == '.'))
                    return true;
            } else {
                if (pack.equals(pattern))
                    return true;
            }
        }
        return false;
    }

    private boolean hasOverlap(Set<?> a, Set<?> b) {
        for (Iterator<?> i = a.iterator(); i.hasNext();) {
            if (b.contains(i.next()))
                return true;
        }
        return false;
    }

    public void verify() throws IOException {
        if (classSpace == null)
            classSpace = analyzeBundleClasspath(dot,
                    parseHeader(getHeader(Analyzer.BUNDLE_CLASSPATH)),
                    contained, referred, uses);
        verifyManifestFirst();
        verifyActivator();
        verifyComponent();
        verifyNative();
        verifyInvalidExports();
        verifyInvalidImports();
        verifyUnresolvedReferences();
        verifySymbolicName();
        verifyListHeader("Bundle-RequiredExecutionEnvironment", EENAME, false);
        verifyHeader("Bundle-ManifestVersion", BUNDLEMANIFESTVERSION, false);
        verifyHeader("Bundle-Version", VERSION, true);
        verifyListHeader("Bundle-Classpath", FILE, false);
        verifyDynamicImportPackage();
        verifyBundleClasspath();
        if (usesRequire) {
            if (!getErrors().isEmpty()) {
                getWarnings()
                        .add(
                                0,
                                "Bundle uses Require Bundle, this can generate false errors because then not enough information is available without the required bundles");
            }
        }
    }

    public void verifyBundleClasspath() {
        Map<String, Map<String, String>> bcp = parseHeader(getHeader(Analyzer.BUNDLE_CLASSPATH));
        if (bcp.isEmpty() || bcp.containsKey("."))
            return;

        for (String path : dot.getResources().keySet()) {
            if (path.endsWith(".class")) {
                warning("The Bundle-Classpath does not contain the actual bundle JAR (as specified with '.' in the Bundle-Classpath) but the JAR does contain classes. Is this intentional?");
                return;
            }
        }
    }

    /**
     * <pre>
     *          DynamicImport-Package ::= dynamic-description
     *              ( ',' dynamic-description )*
     *              
     *          dynamic-description::= wildcard-names ( ';' parameter )*
     *          wildcard-names ::= wildcard-name ( ';' wildcard-name )*
     *          wildcard-name ::= package-name 
     *                         | ( package-name '.*' ) // See 1.4.2
     *                         | '*'
     * </pre>
     */
    private void verifyDynamicImportPackage() {
        verifyListHeader("DynamicImport-Package", WILDCARDPACKAGE, true);
        String dynamicImportPackage = getHeader("DynamicImport-Package");
        if (dynamicImportPackage == null)
            return;

        Map<String, Map<String, String>> map = parseHeader(dynamicImportPackage);
        for (String name : map.keySet()) {
            name = name.trim();
            if (!verify(name, WILDCARDPACKAGE))
                error("DynamicImport-Package header contains an invalid package name: "
                        + name);

            Map<String, String> sub = map.get(name);
            if (r3 && sub.size() != 0) {
                error("DynamicPackage-Import has attributes on import: "
                        + name
                        + ". This is however, an <=R3 bundle and attributes on this header were introduced in R4. ");
            }
        }
    }

    private void verifyManifestFirst() {
        if (!dot.manifestFirst) {
            error("Invalid JAR stream: Manifest should come first to be compatible with JarInputStream, it was not");
        }
    }

    private void verifySymbolicName() {
        Map<String, Map<String, String>> bsn = parseHeader(getHeader(Analyzer.BUNDLE_SYMBOLICNAME));
        if (!bsn.isEmpty()) {
            if (bsn.size() > 1)
                error("More than one BSN specified " + bsn);

            String name = (String) bsn.keySet().iterator().next();
            if (!SYMBOLICNAME.matcher(name).matches()) {
                error("Symbolic Name has invalid format: " + name);
            }
        }
    }

    /**
     * <pre>
     *         filter ::= ’(’ filter-comp ’)’
     *         filter-comp ::= and | or | not | operation
     *         and ::= ’&amp;’ filter-list
     *         or ::= ’|’ filter-list
     *         not ::= ’!’ filter
     *         filter-list ::= filter | filter filter-list
     *         operation ::= simple | present | substring
     *         simple ::= attr filter-type value
     *         filter-type ::= equal | approx | greater | less
     *         equal ::= ’=’
     *         approx ::= ’&tilde;=’
     *         greater ::= ’&gt;=’
     *         less ::= ’&lt;=’
     *         present ::= attr ’=*’
     *         substring ::= attr ’=’ initial any final
     *         inital ::= () | value
     *         any ::= ’*’ star-value
     *         star-value ::= () | value ’*’ star-value
     *         final ::= () | value
     *         value ::= &lt;see text&gt;
     * </pre>
     * 
     * @param expr
     * @param index
     * @return
     */

    int verifyFilter(String expr, int index) {
        try {
            while (Character.isWhitespace(expr.charAt(index)))
                index++;

            if (expr.charAt(index) != '(')
                throw new IllegalArgumentException(
                        "Filter mismatch: expected ( at position " + index
                                + " : " + expr);

            index++;
            while (Character.isWhitespace(expr.charAt(index)))
                index++;

            switch (expr.charAt(index)) {
            case '!':
            case '&':
            case '|':
                return verifyFilterSubExpression(expr, index) + 1;

            default:
                return verifyFilterOperation(expr, index) + 1;
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    "Filter mismatch: early EOF from " + index);
        }
    }

    private int verifyFilterOperation(String expr, int index) {
        StringBuffer sb = new StringBuffer();
        while ("=><~()".indexOf(expr.charAt(index)) < 0) {
            sb.append(expr.charAt(index++));
        }
        String attr = sb.toString().trim();
        if (attr.length() == 0)
            throw new IllegalArgumentException(
                    "Filter mismatch: attr at index " + index + " is 0");
        sb = new StringBuffer();
        while ("=><~".indexOf(expr.charAt(index)) >= 0) {
            sb.append(expr.charAt(index++));
        }
        String operator = sb.toString();
        if (!verify(operator, FILTEROP))
            throw new IllegalArgumentException(
                    "Filter error, illegal operator " + operator + " at index "
                            + index);

        sb = new StringBuffer();
        while (")".indexOf(expr.charAt(index)) < 0) {
            switch (expr.charAt(index)) {
            case '\\':
                if (expr.charAt(index + 1) == '*'
                        || expr.charAt(index + 1) == ')')
                    index++;
                else
                    throw new IllegalArgumentException(
                            "Filter error, illegal use of backslash at index "
                                    + index
                                    + ". Backslash may only be used before * or (");
            }
            sb.append(expr.charAt(index++));
        }
        return index;
    }

    private int verifyFilterSubExpression(String expr, int index) {
        do {
            index = verifyFilter(expr, index + 1);
            while (Character.isWhitespace(expr.charAt(index)))
                index++;
            if (expr.charAt(index) != ')')
                throw new IllegalArgumentException(
                        "Filter mismatch: expected ) at position " + index
                                + " : " + expr);
            index++;
        } while (expr.charAt(index) == '(');
        return index;
    }

    private String getHeader(String string) {
        return main.getValue(string);
    }

    @SuppressWarnings("unchecked")
    private boolean verifyHeader(String name, Pattern regex, boolean error) {
        String value = manifest.getMainAttributes().getValue(name);
        if (value == null)
            return false;

        QuotedTokenizer st = new QuotedTokenizer(value.trim(), ",");
        for (Iterator<String> i = st.getTokenSet().iterator(); i.hasNext();) {
            if (!verify((String) i.next(), regex)) {
                String msg = "Invalid value for " + name + ", " + value
                        + " does not match " + regex.pattern();
                if (error)
                    error(msg);
                else
                    warning(msg);
            }
        }
        return true;
    }

    private boolean verify(String value, Pattern regex) {
        return regex.matcher(value).matches();
    }

    private boolean verifyListHeader(String name, Pattern regex, boolean error) {
        String value = manifest.getMainAttributes().getValue(name);
        if (value == null)
            return false;

        Map<String, Map<String, String>> map = parseHeader(value);
        for (String header : map.keySet()) {
            if (!regex.matcher(header).matches()) {
                String msg = "Invalid value for " + name + ", " + value
                        + " does not match " + regex.pattern();
                if (error)
                    error(msg);
                else
                    warning(msg);
            }
        }
        return true;
    }

    public String getProperty(String key, String deflt) {
        if (properties == null)
            return deflt;
        return properties.getProperty(key, deflt);
    }

    public void setClassSpace(Map<String, Clazz> classspace,
            Map<String, Map<String, String>> contained,
            Map<String, Map<String, String>> referred,
            Map<String, Set<String>> uses) {
        this.classSpace = classspace;
        this.contained = contained;
        this.referred = referred;
        this.uses = uses;
    }

    public static boolean isVersion(String version) {
        return VERSION.matcher(version).matches();
    }

    public static boolean isIdentifier(String value) {
        if ( value.length() < 1 ) 
            return false;
        
        if ( !Character.isJavaIdentifierStart(value.charAt(0)))
                return false;
        
        for ( int i = 1; i<value.length(); i++ ) {
            if ( !Character.isJavaIdentifierPart(value.charAt(i)))
                return false;
        }
        return true;
    }

    public static boolean isMember(String value, String[] matches) {
        for ( String match : matches ) {
            if ( match.equals(value) )
                return true;
        }
        return false;
    }

}
