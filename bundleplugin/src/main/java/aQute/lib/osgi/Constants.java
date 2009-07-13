package aQute.lib.osgi;

import java.util.*;
import java.util.regex.*;

public interface Constants {
    /*
     * Defined in OSGi
     */
    /**
     * @syntax Bundle-ActivationPolicy ::= policy ( ’;’ directive )* policy ::=
     *         ’lazy’
     */
    String               BUNDLE_ACTIVATION_POLICY                  = "Bundle-ActivationPolicy";
    String               BUNDLE_ACTIVATOR                          = "Bundle-Activator";
    String               BUNDLE_BLUEPRINT                          = "Bundle-Copyright";
    String               BUNDLE_CATEGORY                           = "Bundle-Category";
    String               BUNDLE_CLASSPATH                          = "Bundle-ClassPath";
    String               BUNDLE_CONTACTADDRESS                     = "Bundle-ContactAddress";
    String               BUNDLE_COPYRIGHT                          = "Bundle-Copyright";
    String               BUNDLE_DESCRIPTION                        = "Bundle-Description";
    String               BUNDLE_DOCURL                             = "Bundle-DocURL";
    String               BUNDLE_ICON                               = "Bundle-Icon";
    String               BUNDLE_LICENSE                            = "Bundle-License";
    String               BUNDLE_LOCALIZATION                       = "Bundle-Localization";
    String               BUNDLE_MANIFESTVERSION                    = "Bundle-ManifestVersion";
    String               BUNDLE_NAME                               = "Bundle-Name";
    String               BUNDLE_NATIVECODE                         = "Bundle-NativeCode";
    String               BUNDLE_REQUIREDEXECUTIONENVIRONMENT       = "Bundle-RequiredExecutionEnvironment";
    String               BUNDLE_SYMBOLICNAME                       = "Bundle-SymbolicName";
    String               BUNDLE_UPDATELOCATION                     = "Bundle-UpdateLocation";
    String               BUNDLE_VENDOR                             = "Bundle-Vendor";
    String               BUNDLE_VERSION                            = "Bundle-Version";
    String               DYNAMICIMPORT_PACKAGE                     = "DynamicImport-Package";
    String               EXPORT_PACKAGE                            = "Export-Package";
    String               EXPORT_SERVICE                            = "Export-Service";
    String               FRAGMENT_HOST                             = "Fragment-Host";
    String               IMPORT_PACKAGE                            = "Import-Package";
    String               IMPORT_SERVICE                            = "Import-Service";
    String               REQUIRE_BUNDLE                            = "Require-Bundle";
    String               SERVICE_COMPONENT                         = "Service-Component";

    String               PRIVATE_PACKAGE                           = "Private-Package";
    String               IGNORE_PACKAGE                            = "Ignore-Package";
    String               INCLUDE_RESOURCE                          = "Include-Resource";
    String               CONDITIONAL_PACKAGE                       = "Conditional-Package";
    String               BND_LASTMODIFIED                          = "Bnd-LastModified";
    String               CREATED_BY                                = "Created-By";
    String               TOOL                                      = "Tool";
    String               TESTCASES                                 = "Test-Cases";

    String               headers[]                                 = {
            BUNDLE_ACTIVATOR, BUNDLE_CONTACTADDRESS, BUNDLE_COPYRIGHT,
            BUNDLE_DESCRIPTION, BUNDLE_DOCURL, BUNDLE_LOCALIZATION,
            BUNDLE_NATIVECODE, BUNDLE_VENDOR, BUNDLE_VERSION, BUNDLE_LICENSE,
            BUNDLE_CLASSPATH, SERVICE_COMPONENT, EXPORT_PACKAGE,
            IMPORT_PACKAGE, BUNDLE_LOCALIZATION, BUNDLE_MANIFESTVERSION,
            BUNDLE_NAME, BUNDLE_NATIVECODE,
            BUNDLE_REQUIREDEXECUTIONENVIRONMENT, BUNDLE_SYMBOLICNAME,
            BUNDLE_VERSION, FRAGMENT_HOST, PRIVATE_PACKAGE, IGNORE_PACKAGE,
            INCLUDE_RESOURCE, REQUIRE_BUNDLE, IMPORT_SERVICE, EXPORT_SERVICE,
            CONDITIONAL_PACKAGE, BND_LASTMODIFIED, TESTCASES      };

    String               BUILDPATH                                 = "-buildpath";
    String               BUMPPOLICY                                = "-bumppolicy";
    String               CONDUIT                                   = "-conduit";
    String               DEPENDSON                                 = "-dependson";
    String               DEPLOYREPO                                = "-deployrepo";
    String               DONOTCOPY                                 = "-donotcopy";
    String               DEBUG                                     = "-debug";
    String               EXPORT_CONTENTS                           = "-exportcontents";
    String               FAIL_OK                                   = "-failok";
    String               INCLUDE                                   = "-include";
    String               INCLUDERESOURCE                           = "-includeresource";
    String               MAKE                                      = "-make";
    String               MANIFEST                                  = "-manifest";
    String               NOEXTRAHEADERS                            = "-noextraheaders";
    String               NOMANIFEST                                = "-nomanifest";
    String               NOUSES                                    = "-nouses";
    String               NOPE                                      = "-nope";
    String               PEDANTIC                                  = "-pedantic";
    String               PLUGIN                                    = "-plugin";
    String               POM                                       = "-pom";
    String               RELEASEREPO                               = "-releaserepo";
    String               REMOVE_HEADERS                            = "-removeheaders";
    String               RESOURCEONLY                              = "-resourceonly";
    String               SOURCES                                   = "-sources";
    String               SOURCEPATH                                = "-sourcepath";
    String               SUB                                       = "-sub";
    String               RUNPROPERTIES                             = "-runproperties";
    String               RUNSYSTEMPACKAGES                         = "-runsystempackages";
    String               RUNBUNDLES                                = "-runbundles";
    String               RUNPATH                                   = "-runpath";
    String               RUNVM                                     = "-runvm";

    String               REPORTNEWER                               = "-reportnewer";
    String               SIGN                                      = "-sign";
    String               TESTPACKAGES                              = "-testpackages";
    String               TESTREPORT                                = "-testreport";
    String               TESTBUNDLES                               = "-testbundles";
    String               UNDERTEST                                 = "-undertest";
    String               VERBOSE                                   = "-verbose";
    String               VERSIONPOLICY                             = "-versionpolicy";

    // Deprecated
    String               CLASSPATH                                 = "-classpath";

    String               options[]                                 = {
            BUILDPATH, BUMPPOLICY, CONDUIT, CLASSPATH, DEPENDSON, DONOTCOPY,
            EXPORT_CONTENTS, FAIL_OK, INCLUDE, INCLUDERESOURCE, MAKE, MANIFEST,
            NOEXTRAHEADERS, NOUSES, NOPE, PEDANTIC, PLUGIN, POM,
            REMOVE_HEADERS, RESOURCEONLY, SOURCES, SOURCEPATH, SOURCES,
            SOURCEPATH, SUB, RUNBUNDLES, RUNPATH, RUNSYSTEMPACKAGES,
            RUNPROPERTIES, REPORTNEWER, UNDERTEST, TESTBUNDLES, TESTPACKAGES,
            TESTREPORT, VERBOSE, NOMANIFEST, DEPLOYREPO, RELEASEREPO };

    // Ignore bundle specific headers. These bundles do not make
    // a lot of sense to inherit
    String[]             BUNDLE_SPECIFIC_HEADERS                   = new String[] {
            INCLUDE_RESOURCE, BUNDLE_ACTIVATOR, BUNDLE_CLASSPATH, BUNDLE_NAME,
            BUNDLE_NATIVECODE, BUNDLE_SYMBOLICNAME, IMPORT_PACKAGE,
            EXPORT_PACKAGE, DYNAMICIMPORT_PACKAGE, FRAGMENT_HOST,
            REQUIRE_BUNDLE, PRIVATE_PACKAGE, EXPORT_CONTENTS, TESTCASES,
            NOMANIFEST                                            };

    char                 DUPLICATE_MARKER                          = '~';

    String               SPLIT_PACKAGE_DIRECTIVE                   = "-split-package:";
    String               IMPORT_DIRECTIVE                          = "-import:";
    String               NO_IMPORT_DIRECTIVE                       = "-noimport:";
    String               REMOVE_ATTRIBUTE_DIRECTIVE                = "-remove-attribute:";

    String               USES_DIRECTIVE                            = "uses:";
    String               MANDATORY_DIRECTIVE                       = "mandatory:";
    String               INCLUDE_DIRECTIVE                         = "include:";
    String               EXCLUDE_DIRECTIVE                         = "exclude:";
    String               PRESENCE_DIRECTIVE                        = "presence:";
    String               SINGLETON_DIRECTIVE                       = "singleton:";
    String               EXTENSION_DIRECTIVE                       = "extension:";
    String               VISIBILITY_DIRECTIVE                      = "visibility:";
    String               FRAGMENT_ATTACHMENT_DIRECTIVE             = "fragment-attachment:";
    String               RESOLUTION_DIRECTIVE                      = "resolution:";
    String               PATH_DIRECTIVE                            = "path:";
    String               SIZE_ATTRIBUTE                            = "size";
    String               LINK_ATTRIBUTE                            = "link";
    String               NAME_ATTRIBUTE                            = "name";
    String               DESCRIPTION_ATTRIBUTE                     = "description";
    String               OSNAME_ATTRIBUTE                          = "osname";
    String               OSVERSION_ATTRIBUTE                       = "osversion";
    String               PROCESSOR_ATTRIBUTE                       = "processor";
    String               LANGUAGE_ATTRIBUTE                        = "language";
    String               SELECTION_FILTER_ATTRIBUTE                = "selection-filter";
    String               BLUEPRINT_WAIT_FOR_DEPENDENCIES_ATTRIBUTE = "blueprint.wait-for-dependencies";
    String               BLUEPRINT_TIMEOUT_ATTRIBUTE               = "blueprint.timeout";
    String               VERSION_ATTRIBUTE                         = "version";
    String               BUNDLE_SYMBOLIC_NAME_ATTRIBUTE            = "bundle-symbolic-name";
    String               BUNDLE_VERSION_ATTRIBUTE                  = "bundle-version";

    String               KEYSTORE_LOCATION_DIRECTIVE               = "keystore:";
    String               KEYSTORE_PROVIDER_DIRECTIVE               = "provider:";
    String               KEYSTORE_PASSWORD_DIRECTIVE               = "password:";
    String               SIGN_PASSWORD_DIRECTIVE                   = "sign-password:";

    String               NONE                                      = "none";

    String               directives[]                              = {
            SPLIT_PACKAGE_DIRECTIVE, NO_IMPORT_DIRECTIVE, IMPORT_DIRECTIVE,
            RESOLUTION_DIRECTIVE, INCLUDE_DIRECTIVE, USES_DIRECTIVE,
            EXCLUDE_DIRECTIVE, KEYSTORE_LOCATION_DIRECTIVE,
            KEYSTORE_PROVIDER_DIRECTIVE, KEYSTORE_PASSWORD_DIRECTIVE,
            SIGN_PASSWORD_DIRECTIVE,

                                                                   // TODO
                                                                   };

    String               USES_USES                                 = "<<USES>>";
    String               CURRENT_USES                              = "@uses";
    String               IMPORT_REFERENCE                          = "reference";
    String               IMPORT_PRIVATE                            = "private";
    String[]             importDirectives                          = {
            IMPORT_REFERENCE, IMPORT_PRIVATE                      };

    static final Pattern VALID_PROPERTY_TYPES                      = Pattern
                                                                           .compile("(String|Long|Double|Float|Integer|Byte|Character|Boolean|Short)");

    String               DEFAULT_BND_EXTENSION                     = ".bnd";
    String               DEFAULT_JAR_EXTENSION                     = ".jar";
    String               DEFAULT_BAR_EXTENSION                     = ".bar";
    String[]             METAPACKAGES                              = {
            "META-INF", "OSGI-INF", "OSGI-OPT"                    };

    int                  STRATEGY_HIGHEST                          = 1;
    int                  STRATEGY_LOWEST                           = -1;

    String               CURRENT_VERSION                           = "@";
    String               CURRENT_PACKAGE                           = "@package";
}
