package aQute.lib.osgi;

import java.util.*;
import java.util.regex.*;

public interface Constants {
    String               BUNDLE_CLASSPATH                    = "Bundle-ClassPath";
    String               BUNDLE_COPYRIGHT                    = "Bundle-Copyright";
    String               BUNDLE_DESCRIPTION                  = "Bundle-Description";
    String               BUNDLE_NAME                         = "Bundle-Name";
    String               BUNDLE_NATIVECODE                   = "Bundle-NativeCode";
    String               EXPORT_PACKAGE                      = "Export-Package";
    String               EXPORT_SERVICE                      = "Export-Service";
    String               IMPORT_PACKAGE                      = "Import-Package";
    String               DYNAMICIMPORT_PACKAGE               = "DynamicImport-Package";
    String               IMPORT_SERVICE                      = "Import-Service";
    String               BUNDLE_VENDOR                       = "Bundle-Vendor";
    String               BUNDLE_VERSION                      = "Bundle-Version";
    String               BUNDLE_DOCURL                       = "Bundle-DocURL";
    String               BUNDLE_CONTACTADDRESS               = "Bundle-ContactAddress";
    String               BUNDLE_ACTIVATOR                    = "Bundle-Activator";
    String               BUNDLE_REQUIREDEXECUTIONENVIRONMENT = "Bundle-RequiredExecutionEnvironment";
    String               BUNDLE_SYMBOLICNAME                 = "Bundle-SymbolicName";
    String               BUNDLE_LOCALIZATION                 = "Bundle-Localization";
    String               REQUIRE_BUNDLE                      = "Require-Bundle";
    String               FRAGMENT_HOST                       = "Fragment-Host";
    String               BUNDLE_MANIFESTVERSION              = "Bundle-ManifestVersion";
    String               SERVICE_COMPONENT                   = "Service-Component";
    String               BUNDLE_LICENSE                      = "Bundle-License";
    String               PRIVATE_PACKAGE                     = "Private-Package";
    String               IGNORE_PACKAGE                      = "Ignore-Package";
    String               INCLUDE_RESOURCE                    = "Include-Resource";
    String               CONDITIONAL_PACKAGE                 = "Conditional-Package";
    String               BND_LASTMODIFIED                    = "Bnd-LastModified";
    String               CREATED_BY                          = "Created-By";
    String               TOOL                                = "Tool";

    String               headers[]                           = {
            BUNDLE_ACTIVATOR, BUNDLE_CONTACTADDRESS, BUNDLE_COPYRIGHT,
            BUNDLE_DESCRIPTION, BUNDLE_DOCURL, BUNDLE_LOCALIZATION,
            BUNDLE_NATIVECODE, BUNDLE_VENDOR, BUNDLE_VERSION, BUNDLE_LICENSE,
            BUNDLE_CLASSPATH, SERVICE_COMPONENT, EXPORT_PACKAGE,
            IMPORT_PACKAGE, BUNDLE_LOCALIZATION, BUNDLE_MANIFESTVERSION,
            BUNDLE_NAME, BUNDLE_NATIVECODE,
            BUNDLE_REQUIREDEXECUTIONENVIRONMENT, BUNDLE_SYMBOLICNAME,
            BUNDLE_VERSION, FRAGMENT_HOST, PRIVATE_PACKAGE, IGNORE_PACKAGE,
            INCLUDE_RESOURCE, REQUIRE_BUNDLE, IMPORT_SERVICE, EXPORT_SERVICE,
            CONDITIONAL_PACKAGE, BND_LASTMODIFIED           };

    String               BUILDPATH                           = "-buildpath";
    String               BUMPPOLICY                          = "-bumppolicy";
    String               CONDUIT                             = "-conduit";
    String               CLASSPATH                           = "-classpath";
    String               DEPENDSON                           = "-dependson";
    String               DONOTCOPY                           = "-donotcopy";
    String               EXPORT_CONTENTS                     = "-exportcontents";
    String               FAIL_OK                             = "-failok";
    String               INCLUDE                             = "-include";
    String               INCLUDERESOURCE                             = "-includeresource";
    String               MAKE                                = "-make";
    String               MANIFEST                            = "-manifest";
    String               NOEXTRAHEADERS                      = "-noextraheaders";
    String               NOUSES                              = "-nouses";
    String               NOPE                                = "-nope";
    String               PEDANTIC                            = "-pedantic";
    String               PLUGIN                              = "-plugin";
    String               POM                                 = "-pom";
    String               REMOVE_HEADERS                      = "-removeheaders";
    String               RESOURCEONLY                        = "-resourceonly";
    String               SOURCES                             = "-sources";
    String               SOURCEPATH                          = "-sourcepath";
    String               SUB                                 = "-sub";
    String               RUNPROPERTIES                       = "-runproperties";
    String               RUNSYSTEMPACKAGES                   = "-runsystempackages";
    String               RUNBUNDLES                          = "-runbundles";
    String               RUNPATH                             = "-runpath";
    String               RUNVM                               = "-runvm";

    String               REPORTNEWER                         = "-reportnewer";
    String               SIGN                                = "-sign";
    String               TESTPACKAGES                        = "-testpackages";
    String               TESTREPORT                          = "-testreport";
    String               UNDERTEST                           = "-undertest";
    String               VERBOSE                             = "-verbose";
    String               VERSIONPOLICY                       = "-versionpolicy";

    String               options[]                           = { BUILDPATH,
            BUMPPOLICY, CONDUIT, CLASSPATH, DEPENDSON, DONOTCOPY,
            EXPORT_CONTENTS, FAIL_OK, INCLUDE, INCLUDERESOURCE, MAKE, MANIFEST, NOEXTRAHEADERS,
            NOUSES, NOPE, PEDANTIC, PLUGIN, POM, REMOVE_HEADERS, RESOURCEONLY,
            SOURCES, SOURCEPATH, SOURCES, SOURCEPATH, SUB, RUNBUNDLES, RUNPATH,
            RUNSYSTEMPACKAGES, RUNPROPERTIES, REPORTNEWER, UNDERTEST,
            TESTPACKAGES, TESTREPORT, VERBOSE               };

    char                 DUPLICATE_MARKER                    = '~';

    String               SPLIT_PACKAGE_DIRECTIVE             = "-split-package:";
    String               IMPORT_DIRECTIVE                    = "-import:";
    String               NO_IMPORT_DIRECTIVE                 = "-noimport:";
    String               REMOVE_ATTRIBUTE_DIRECTIVE          = "-remove-attribute:";
    String               USES_DIRECTIVE                      = "uses:";
    String               PRESENCE_DIRECTIVE                  = "presence:";

    String               KEYSTORE_LOCATION_DIRECTIVE         = "keystore:";
    String               KEYSTORE_PROVIDER_DIRECTIVE         = "provider:";
    String               KEYSTORE_PASSWORD_DIRECTIVE         = "password:";
    String               SIGN_PASSWORD_DIRECTIVE             = "sign-password:";

    String               directives[]                        = {
            SPLIT_PACKAGE_DIRECTIVE, NO_IMPORT_DIRECTIVE, IMPORT_DIRECTIVE,
            "resolution:", "include:", "uses:", "exclude:", USES_DIRECTIVE,
            KEYSTORE_LOCATION_DIRECTIVE, KEYSTORE_PROVIDER_DIRECTIVE,
            KEYSTORE_PASSWORD_DIRECTIVE, SIGN_PASSWORD_DIRECTIVE,

                                                             // TODO
                                                             };

    String               USES_USES                           = "<<USES>>";
    String               CURRENT_USES                        = "@uses";
    String               IMPORT_REFERENCE                    = "reference";
    String               IMPORT_PRIVATE                      = "private";
    String[]             importDirectives                    = {
            IMPORT_REFERENCE, IMPORT_PRIVATE                };

    String               COMPONENT_FACTORY                   = "factory:";
    String               COMPONENT_SERVICEFACTORY            = "servicefactory:";
    String               COMPONENT_IMMEDIATE                 = "immediate:";
    String               COMPONENT_ENABLED                   = "enabled:";
    String               COMPONENT_DYNAMIC                   = "dynamic:";
    String               COMPONENT_MULTIPLE                  = "multiple:";
    String               COMPONENT_PROVIDE                   = "provide:";
    String               COMPONENT_OPTIONAL                  = "optional:";
    String               COMPONENT_PROPERTIES                = "properties:";
    String               COMPONENT_IMPLEMENTATION            = "implementation:";
    String[]             componentDirectives                 = new String[] {
            COMPONENT_FACTORY, COMPONENT_IMMEDIATE, COMPONENT_ENABLED,
            COMPONENT_DYNAMIC, COMPONENT_MULTIPLE, COMPONENT_PROVIDE,
            COMPONENT_OPTIONAL, COMPONENT_PROPERTIES, COMPONENT_IMPLEMENTATION,
            COMPONENT_SERVICEFACTORY                        };

    // static Map EES = new HashMap();
    static Set<String>   SET_COMPONENT_DIRECTIVES            = new HashSet<String>(
                                                                     Arrays
                                                                             .asList(componentDirectives));

    static final Pattern VALID_PROPERTY_TYPES                = Pattern
                                                                     .compile("(String|Long|Double|Float|Integer|Byte|Character|Boolean|Short)");

    String               DEFAULT_BND_EXTENSION               = ".bnd";
    String               DEFAULT_JAR_EXTENSION               = ".jar";
    String               DEFAULT_BAR_EXTENSION               = ".bar";
    String[]             METAPACKAGES                        = { "META-INF",
            "OSGI-INF", "OSGI-OPT"                          };

    int                  STRATEGY_HIGHEST                    = 1;
    int                  STRATEGY_LOWEST                     = -1;

    String               CURRENT_VERSION                     = "@";
    String               CURRENT_PACKAGE                     = "@package";
}
