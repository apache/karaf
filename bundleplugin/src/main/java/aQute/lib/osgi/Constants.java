package aQute.lib.osgi;

import java.util.*;
import java.util.regex.*;

public interface Constants {
    public final static String   BUNDLE_CLASSPATH                    = "Bundle-ClassPath";
    public final static String   BUNDLE_COPYRIGHT                    = "Bundle-Copyright";
    public final static String   BUNDLE_DESCRIPTION                  = "Bundle-Description";
    public final static String   BUNDLE_NAME                         = "Bundle-Name";
    public final static String   BUNDLE_NATIVECODE                   = "Bundle-NativeCode";
    public final static String   EXPORT_PACKAGE                      = "Export-Package";
    public final static String   EXPORT_SERVICE                      = "Export-Service";
    public final static String   IMPORT_PACKAGE                      = "Import-Package";
    public final static String   DYNAMICIMPORT_PACKAGE               = "DynamicImport-Package";
    public final static String   IMPORT_SERVICE                      = "Import-Service";
    public final static String   BUNDLE_VENDOR                       = "Bundle-Vendor";
    public final static String   BUNDLE_VERSION                      = "Bundle-Version";
    public final static String   BUNDLE_DOCURL                       = "Bundle-DocURL";
    public final static String   BUNDLE_CONTACTADDRESS               = "Bundle-ContactAddress";
    public final static String   BUNDLE_ACTIVATOR                    = "Bundle-Activator";
    public final static String   BUNDLE_REQUIREDEXECUTIONENVIRONMENT = "Bundle-RequiredExecutionEnvironment";
    public final static String   BUNDLE_SYMBOLICNAME                 = "Bundle-SymbolicName";
    public final static String   BUNDLE_LOCALIZATION                 = "Bundle-Localization";
    public final static String   REQUIRE_BUNDLE                      = "Require-Bundle";
    public final static String   FRAGMENT_HOST                       = "Fragment-Host";
    public final static String   BUNDLE_MANIFESTVERSION              = "Bundle-ManifestVersion";
    public final static String   SERVICE_COMPONENT                   = "Service-Component";
    public final static String   BUNDLE_LICENSE                      = "Bundle-License";
    public static final String   PRIVATE_PACKAGE                     = "Private-Package";
    public static final String   IGNORE_PACKAGE                      = "Ignore-Package";
    public static final String   INCLUDE_RESOURCE                    = "Include-Resource";
    public static final String   CONDITIONAL_PACKAGE                 = "Conditional-Package";
    public static final String   BND_LASTMODIFIED                    = "Bnd-LastModified";
    public static final String   CREATED_BY                          = "Created-By";
    public static final String   TOOL                                = "Tool";

    public final static String   headers[]                           = {
            BUNDLE_ACTIVATOR, BUNDLE_CONTACTADDRESS, BUNDLE_COPYRIGHT,
            BUNDLE_DESCRIPTION, BUNDLE_DOCURL, BUNDLE_LOCALIZATION,
            BUNDLE_NATIVECODE, BUNDLE_VENDOR, BUNDLE_VERSION, BUNDLE_LICENSE,
            BUNDLE_CLASSPATH, SERVICE_COMPONENT, EXPORT_PACKAGE,
            IMPORT_PACKAGE, BUNDLE_LOCALIZATION, BUNDLE_MANIFESTVERSION,
            BUNDLE_NAME, BUNDLE_NATIVECODE,
            BUNDLE_REQUIREDEXECUTIONENVIRONMENT, BUNDLE_SYMBOLICNAME,
            BUNDLE_VERSION, FRAGMENT_HOST, PRIVATE_PACKAGE, IGNORE_PACKAGE,
            INCLUDE_RESOURCE, REQUIRE_BUNDLE, IMPORT_SERVICE, EXPORT_SERVICE,
            CONDITIONAL_PACKAGE, BND_LASTMODIFIED                   };

    public static final String   BUILDPATH                           = "-buildpath";
    public static final String   CONDUIT                             = "-conduit";
    public static final String   CLASSPATH                           = "-classpath";
    public static final String   DEPENDSON                           = "-dependson";
    public static final String   DONOTCOPY                           = "-donotcopy";
    public static final String   EXPORT_CONTENTS                     = "-exportcontents";
    public static final String   FAIL_OK                             = "-failok";
    public static final String   INCLUDE                             = "-include";
    public static final String   MAKE                                = "-make";
    public static final String   MANIFEST                            = "-manifest";
    public static final String   NOEXTRAHEADERS                      = "-noextraheaders";
    public static final String   NOUSES                              = "-nouses";
    public static final String   NOPE                                = "-nope";
    public static final String   PEDANTIC                            = "-pedantic";
    public static final String   PLUGIN                              = "-plugin";
    public static final String   POM                                 = "-pom";
    public static final String   REMOVE_HEADERS                      = "-removeheaders";
    public static final String   RESOURCEONLY                        = "-resourceonly";
    public static final String   SOURCES                             = "-sources";
    public static final String   SOURCEPATH                          = "-sourcepath";
    public static final String   SUB                                 = "-sub";
    public static final String   RUNPROPERTIES                       = "-runproperties";
    public static final String   RUNSYSTEMPACKAGES                   = "-runsystempackages";
    public static final String   RUNBUNDLES                          = "-runbundles";
    public static final String   RUNPATH                             = "-runpath";
    public static final String   RUNVM                               = "-runvm";

    public static final String   REPORTNEWER                         = "-reportnewer";
    public static final String   TESTPACKAGES                        = "-testpackages";
    public static final String   TESTREPORT                          = "-testreport";
    public static final String   UNDERTEST                           = "-undertest";
    public static final String   VERBOSE                             = "-verbose";
    public static final String   VERSIONPOLICY                       = "-versionpolicy";
    public static final String   SIGN                                = "-sign";

    public static final String   options[]                           = {
            BUILDPATH, CONDUIT, CLASSPATH, DEPENDSON, DONOTCOPY,
            EXPORT_CONTENTS, FAIL_OK, INCLUDE, MAKE, MANIFEST, NOEXTRAHEADERS,
            NOUSES, NOPE, PEDANTIC, PLUGIN, POM, REMOVE_HEADERS, RESOURCEONLY,
            SOURCES, SOURCEPATH, SOURCES, SOURCEPATH, SUB, RUNBUNDLES, RUNPATH,
            RUNSYSTEMPACKAGES, RUNPROPERTIES, REPORTNEWER, UNDERTEST,
            TESTPACKAGES, TESTREPORT, VERBOSE                       };

    public static final char     DUPLICATE_MARKER                    = '~';

    public static final String   SPLIT_PACKAGE_DIRECTIVE             = "-split-package:";
    public static final String   IMPORT_DIRECTIVE                    = "-import:";
    public static final String   NO_IMPORT_DIRECTIVE                 = "-noimport:";
    public static final String   REMOVE_ATTRIBUTE_DIRECTIVE          = "-remove-attribute:";
    public static final String   USES_DIRECTIVE                      = "uses:";
    public static final String   PRESENCE_DIRECTIVE                  = "presence:";

    public static final String   KEYSTORE_LOCATION_DIRECTIVE         = "keystore:";
    public static final String   KEYSTORE_PROVIDER_DIRECTIVE         = "provider:";
    public static final String   KEYSTORE_PASSWORD_DIRECTIVE         = "password:";
    public static final String   SIGN_PASSWORD_DIRECTIVE             = "sign-password:";

    public static final String   directives[]                        = {
            SPLIT_PACKAGE_DIRECTIVE, NO_IMPORT_DIRECTIVE, IMPORT_DIRECTIVE,
            "resolution:", "include:", "uses:", "exclude:", USES_DIRECTIVE,
            KEYSTORE_LOCATION_DIRECTIVE, KEYSTORE_PROVIDER_DIRECTIVE,
            KEYSTORE_PASSWORD_DIRECTIVE, SIGN_PASSWORD_DIRECTIVE,

                                                                     // TODO
                                                                     };

    public static final String   USES_USES                           = "<<USES>>";
    public static final String   CURRENT_USES                        = "@uses";
    public static final String   IMPORT_REFERENCE                    = "reference";
    public static final String   IMPORT_PRIVATE                      = "private";
    public static final String[] importDirectives                    = {
            IMPORT_REFERENCE, IMPORT_PRIVATE                        };

    public static final String   COMPONENT_FACTORY                   = "factory:";
    public static final String   COMPONENT_SERVICEFACTORY            = "servicefactory:";
    public static final String   COMPONENT_IMMEDIATE                 = "immediate:";
    public static final String   COMPONENT_ENABLED                   = "enabled:";
    public static final String   COMPONENT_DYNAMIC                   = "dynamic:";
    public static final String   COMPONENT_MULTIPLE                  = "multiple:";
    public static final String   COMPONENT_PROVIDE                   = "provide:";
    public static final String   COMPONENT_OPTIONAL                  = "optional:";
    public static final String   COMPONENT_PROPERTIES                = "properties:";
    public static final String   COMPONENT_IMPLEMENTATION            = "implementation:";
    public static final String[] componentDirectives                 = new String[] {
            COMPONENT_FACTORY, COMPONENT_IMMEDIATE, COMPONENT_ENABLED,
            COMPONENT_DYNAMIC, COMPONENT_MULTIPLE, COMPONENT_PROVIDE,
            COMPONENT_OPTIONAL, COMPONENT_PROPERTIES, COMPONENT_IMPLEMENTATION, COMPONENT_SERVICEFACTORY };

    // static Map EES = new HashMap();
    static Set<String>           SET_COMPONENT_DIRECTIVES            = new HashSet<String>(
                                                                             Arrays
                                                                                     .asList(componentDirectives));

    static final Pattern         VALID_PROPERTY_TYPES                = Pattern
                                                                             .compile("(String|Long|Double|Float|Integer|Byte|Character|Boolean|Short)");

    public final static String   DEFAULT_BND_EXTENSION               = ".bnd";
    public final static String   DEFAULT_JAR_EXTENSION               = ".jar";
    public final static String   DEFAULT_BAR_EXTENSION               = ".bar";
    String[]                     METAPACKAGES                        = {
            "META-INF", "OSGI-INF", "OSGI-OPT"                      };

    int                          STRATEGY_HIGHEST                    = 1;
    int                          STRATEGY_LOWEST                     = -1;

    final static String          CURRENT_VERSION                     = "@";
    final static String          CURRENT_PACKAGE                     = "@package";
}
