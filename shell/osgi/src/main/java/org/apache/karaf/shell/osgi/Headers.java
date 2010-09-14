/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.osgi;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

import java.util.*;

@Command(scope = "osgi", name = "headers", description = "Displays OSGi headers of a given bundle")
public class Headers extends OsgiCommandSupport {

    protected final static String BUNDLE_PREFIX = "Bundle-";
    protected final static String PACKAGE_SUFFFIX = "-Package";
    protected final static String SERVICE_SUFFIX = "-Service";
    protected final static String USES_ATTRIB = "uses:=";
    protected final static String VERSION_ATTRIB = "version=";
    protected final static String RESOLUTION_ATTRIB = "resolution:=";
    protected final static String IMPORT_PACKAGES_ATTRIB = "Import-Package";
    protected final static String REQUIRE_BUNDLE_ATTRIB = "Require-Bundle";

    private ServiceReference ref;
    private PackageAdmin admin;

    @Argument(index = 0, name = "ids", description = "A list of bundle IDs separated by whitespaces", required = false, multiValued = true)
    List<Long> ids;

    protected Object doExecute() throws Exception {
        // Get package admin service.
        ref = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            System.out.println("PackageAdmin service is unavailable.");
            return null;
        }

        try {
            admin = (PackageAdmin) getBundleContext().getService(ref);
            if (admin == null) {
                System.out.println("PackageAdmin service is unavailable.");
                return null;
            }

            if (ids != null && !ids.isEmpty()) {
                for (long id : ids) {
                    Bundle bundle = getBundleContext().getBundle(id);
                    if (bundle != null) {
                        printHeaders(bundle);
                    } else {
                        System.err.println("Bundle ID " + id + " is invalid.");
                    }
                }
            } else {
                Bundle[] bundles = getBundleContext().getBundles();
                for (int i = 0; i < bundles.length; i++) {
                    printHeaders(bundles[i]);
                }
            }
        } finally {
            getBundleContext().ungetService(ref);
        }

        return null;
    }

    protected void printHeaders(Bundle bundle) throws Exception {
        String title = Util.getBundleName(bundle);
        System.out.println("\n" + title);
        System.out.println(Util.getUnderlineString(title));
        System.out.println(generateFormattedOutput(bundle));
    }

    protected String generateFormattedOutput(Bundle bundle) {
        StringBuffer output = new StringBuffer();
        Map<String, Object> otherAttribs = new HashMap<String, Object>();
        Map<String, Object> bundleAttribs = new HashMap<String, Object>();
        Map<String, Object> serviceAttribs = new HashMap<String, Object>();
        Map<String, Object> packagesAttribs = new HashMap<String, Object>();
        Dictionary dict = bundle.getHeaders();
        Enumeration keys = dict.keys();

        // do an initial loop and separate the attributes in different groups
        while (keys.hasMoreElements()) {
            String k = (String) keys.nextElement();
            Object v = dict.get(k);
            if (k.startsWith(BUNDLE_PREFIX)) {
                // starts with Bundle-xxx
                bundleAttribs.put(k, v);
            } else if (k.endsWith(SERVICE_SUFFIX)) {
                // ends with xxx-Service
                serviceAttribs.put(k, v);
            } else if (k.endsWith(PACKAGE_SUFFFIX)) {
                // ends with xxx-Package
                packagesAttribs.put(k, v);
            } else if (k.endsWith(REQUIRE_BUNDLE_ATTRIB)) {
                // require bundle statement
                packagesAttribs.put(k, v);
            } else {
                // the remaining attribs
                otherAttribs.put(k, v);
            }
        }

        // we will display the formatted result like this:
        // Bundle-Name (ID)
        // -----------------------
        // all other attributes
        //
        // all Bundle attributes
        //
        // all Service attributes
        //
        // all Package attributes
        Iterator<Map.Entry<String, Object>> it = otherAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(String.format("%s = %s\n", e.getKey(), Util.getValueString(e.getValue())));
        }
        if (otherAttribs.size() > 0) {
            output.append('\n');
        }

        it = bundleAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(String.format("%s = %s\n", e.getKey(), Util.getValueString(e.getValue())));
        }
        if (bundleAttribs.size() > 0) {
            output.append('\n');
        }

        it = serviceAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(String.format("%s = %s\n", e.getKey(), Util.getValueString(e.getValue())));
        }
        if (serviceAttribs.size() > 0) {
            output.append('\n');
        }

        it = packagesAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            if (e.getKey().equals(REQUIRE_BUNDLE_ATTRIB)) {
                output.append(String.format("%s = %s\n", e.getKey(), getFormattedBundles(Util.getValueString(e.getValue()))));
            } else {
                output.append(String.format("%s = \n%s\n", e.getKey(), getFormattedPackages(Util.getValueString(e.getValue()), e.getKey().trim().equals(IMPORT_PACKAGES_ATTRIB))));
            }
        }
        if (packagesAttribs.size() > 0) {
            output.append('\n');
        }

        return output.toString();
    }

    protected String getFormattedPackages(String packagesString, boolean colorize) {
        StringBuffer output = new StringBuffer();

        List<PackageDefinition> packages = splitPackages(packagesString, colorize);
        boolean first = true;
        for (PackageDefinition def : packages) {
            if (first) {
                first = false;
            } else {
                output.append(",\n");
            }
            output.append(def.toString());
        }
        return output.toString();
    }

    protected String getFormattedBundles(String bundlesString) {
        StringBuffer output = new StringBuffer();

        List<BundleDefinition> bundles = splitBundles(bundlesString);
        boolean first = true;
        for (BundleDefinition def : bundles) {
            if (first) {
                first = false;
            } else {
                output.append(",\n");
            }
            output.append(def.toString());
        }
        return output.toString();
    }

    protected List<PackageDefinition> splitPackages(String packagesString, boolean colorize) {
        boolean inQuotes = false;
        List<PackageDefinition> parts = new ArrayList<PackageDefinition>();
        StringBuffer statement = new StringBuffer();
        for (int index = 0; index < packagesString.length(); index++) {
            char c = packagesString.charAt(index);

            if (c == '"') {
                // quote switcher
                inQuotes = !inQuotes;
            }

            if (c == ',' && !inQuotes) {
                // package statement ends here
                parts.add(new PackageDefinition(statement.toString(), colorize));
                statement.setLength(0);
            } else {
                statement.append(c);
            }
        }

        if (statement.length() > 0) {
            parts.add(new PackageDefinition(statement.toString(), colorize));
        }

        return parts;
    }

    protected List<BundleDefinition> splitBundles(String bundlesString) {
        boolean inQuotes = false;
        List<BundleDefinition> parts = new ArrayList<BundleDefinition>();
        StringBuffer statement = new StringBuffer();
        for (int index = 0; index < bundlesString.length(); index++) {
            char c = bundlesString.charAt(index);

            if (c == '"') {
                // quote switcher
                inQuotes = !inQuotes;
            }

            if (c == ',' && !inQuotes) {
                // package statement ends here
                parts.add(new BundleDefinition(statement.toString()));
                statement.setLength(0);
            } else {
                statement.append(c);
            }
        }

        if (statement.length() > 0) {
            parts.add(new BundleDefinition(statement.toString()));
        }

        return parts;
    }

    class PackageDefinition {
        private String packageStr;
        private String nameStr;
        private String usesStr;
        private String versionStr;
        private String resolutionStr;
        private List<String> usesItems;
        private List<String> parameters;
        private boolean colorize;

        public PackageDefinition(String packageString, boolean colorize) {
            this.packageStr = packageString;
            this.colorize = colorize;
            this.usesItems = new LinkedList<String>();
            this.parameters = new LinkedList<String>();
            parse();
        }

        private void parse() {
            boolean inQuotes = false;
            StringBuffer statement = new StringBuffer();
            boolean first = true;
            for (int index = 0; index < packageStr.length(); index++) {
                char c = packageStr.charAt(index);

                if (c == '"') {
                    // quote switcher
                    inQuotes = !inQuotes;
                }

                if (c == ';' && !inQuotes) {
                    // part finished
                    apply(statement.toString(), first);
                    first = false;
                    statement.setLength(0);
                } else {
                    statement.append(c);
                }
            }

            if (statement.length() > 0) {
                apply(statement.toString(), first);
            }
        }

        private void apply(String part, boolean first) {
            if (part.startsWith(USES_ATTRIB)) {
                // uses definition
                this.usesStr = part;
                StringTokenizer usesTok = new StringTokenizer(this.usesStr.substring(this.usesStr.indexOf(USES_ATTRIB) + USES_ATTRIB.length()).replaceAll("\"", ""), ",");
                while (usesTok.hasMoreTokens()) {
                    this.usesItems.add(usesTok.nextToken());
                }
            } else if (part.startsWith(VERSION_ATTRIB)) {
                // version definition
                this.versionStr = part;
            } else if (part.startsWith(RESOLUTION_ATTRIB)) {
                // resolution definition
                this.resolutionStr = part;
            } else {
                if (first) {
                    // must be package name
                    this.nameStr = part;
                } else {
                    parameters.add(part);
                }
            }
        }

        public String getName() {
            return this.nameStr;
        }

        public String getResolution() {
            return this.resolutionStr;
        }

        public String getVersion() {
            return this.versionStr;
        }

        public List<String> getUsesItems() {
            return this.usesItems;
        }

        public String toString() {
            StringBuffer output = new StringBuffer();

            // output should look like this...
            // <package>;
            // uses:=<package>,
            // <anotherPackage>,
            // <etc>;
            // <parameters>;
            // version="<version>",
            // resolution:="<resolution>",
            // <blank line>
            output.append(String.format("\t%s", this.getName()));

            // we do a line feed if there are uses defined
            if (this.usesItems.size() > 0) {
                output.append(String.format(";\n\t%s \"", USES_ATTRIB));
                boolean first = true;
                for (String usesItem : this.usesItems) {
                    if (first) {
                        first = false;
                    } else {
                        output.append(",\n\t\t");
                    }
                    output.append(usesItem);
                }
                output.append("\"");

                for (String param : this.parameters) {
                    if (first) {
                        first = false;
                    } else {
                        output.append(";\n\t");
                    }
                    output.append(String.format("%s", param));
                }

                if (this.getVersion() != null) {
                    output.append(String.format(";\n\t%s", this.getVersion()));
                }
                if (this.getResolution() != null) {
                    output.append(String.format(";\n\t%s", this.getResolution()));
                }
            } else {
                // if there are no uses defined we put all on a single line
                boolean first = true;
                for (String param : this.parameters) {
                    if (first) {
                        first = false;
                    } else {
                        output.append(",\n\t\t");
                    }
                    output.append(String.format(";%s", param));
                }

                if (this.getVersion() != null) {
                    output.append(String.format(";%s", this.getVersion()));
                }
                if (this.getResolution() != null) {
                    output.append(String.format(";%s", this.getResolution()));
                }
            }

            String retVal;

            if (colorize) {
                boolean isSatisfied = checkPackage(getName(), getVersion());
                if (isSatisfied) {
                    // color it green
                    retVal = Ansi.ansi().fg(Ansi.Color.GREEN).a(output.toString()).reset().toString();
                } else {
                    // color it red
                    retVal = Ansi.ansi().fg(Ansi.Color.RED).a(output.toString()).reset().toString();
                }
            } else {
                retVal = output.toString();
            }

            return retVal;
        }

        private boolean checkPackage(String packageName, String versionInfo) {
            boolean satisfied = false;
            String version = versionInfo == null ? null : versionInfo.substring(versionInfo.indexOf('"') + 1, versionInfo.lastIndexOf('"'));
            VersionRange range = version == null ? null : VersionRange.parseVersionRange(version);

            if (admin != null) {
                ExportedPackage[] packages = admin.getExportedPackages(packageName);
                if (version == null) {
                    satisfied = packages != null && packages.length > 0;
                } else {
                    if (packages != null) {
                        for (ExportedPackage export : packages) {
                            if (range.contains(export.getVersion())) {
                                satisfied = true;
                                break;
                            }
                        }
                    }
                }
            }

            return satisfied;
        }
    }

    class BundleDefinition {
        private String bundleStr;
        private String nameStr;
        private String versionStr;
        private String resolutionStr;

        public BundleDefinition(String bundleString) {
            this.bundleStr = bundleString;
            parse();
        }

        private void parse() {
            boolean inQuotes = false;
            StringBuffer statement = new StringBuffer();
            for (int index = 0; index < this.bundleStr.length(); index++) {
                char c = this.bundleStr.charAt(index);

                if (c == '"') {
                    // quote switcher
                    inQuotes = !inQuotes;
                }

                if (c == ';' && !inQuotes) {
                    // part finished
                    apply(statement.toString());
                    statement.setLength(0);
                } else {
                    statement.append(c);
                }
            }

            if (statement.length() > 0) {
                apply(statement.toString());
            }
        }

        private void apply(String part) {
            if (part.startsWith(VERSION_ATTRIB)) {
                // version definition
                this.versionStr = part;
            } else if (part.startsWith(RESOLUTION_ATTRIB)) {
                // resolution definition
                this.resolutionStr = part;
            } else {
                // must be bundle name
                this.nameStr = part;
            }
        }

        public String getName() {
            return this.nameStr;
        }

        public String getResolution() {
            return this.resolutionStr;
        }

        public String getVersion() {
            return this.versionStr;
        }

        public String toString() {
            StringBuffer output = new StringBuffer();

            // output should look like this...
            // <bundle>;
            // version="<version>",
            // resolution:="<resolution>",
            // <blank line>
            output.append(String.format("\t%s", this.getName()));

            // if there are no uses defined we put all on a single line
            if (this.getVersion() != null) {
                output.append(String.format(";%s", this.getVersion()));
            }
            if (this.getResolution() != null) {
                output.append(String.format(";%s", this.getResolution()));
            }

            String retVal;

            boolean isSatisfied = checkBundle(getName(), getVersion());
            if (isSatisfied) {
                // color it green
                retVal = Ansi.ansi().fg(Ansi.Color.GREEN).a(output.toString()).reset().toString();
            } else {
                // color it red
                retVal = Ansi.ansi().fg(Ansi.Color.RED).a(output.toString()).reset().toString();
            }

            return retVal;
        }

        private boolean checkBundle(String bundleName, String versionInfo) {
            boolean satisfied = false;
            String version = versionInfo == null ? null : versionInfo.substring(versionInfo.indexOf('"') + 1, versionInfo.lastIndexOf('"'));

            if (admin != null) {
                Bundle[] bundles = admin.getBundles(bundleName, version);
                satisfied = bundles != null && bundles.length > 0;
            }

            return satisfied;
        }
    }
}
