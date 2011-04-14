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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jline.Terminal;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

@Command(scope = "osgi", name = "headers", description = "Displays OSGi headers of a given bundle.")
public class Headers extends BundlesCommandOptional {

    protected final static String BUNDLE_PREFIX = "Bundle-";
    protected final static String PACKAGE_SUFFFIX = "-Package";
    protected final static String SERVICE_SUFFIX = "-Service";
    protected final static String IMPORT_PACKAGES_ATTRIB = "Import-Package";
    protected final static String REQUIRE_BUNDLE_ATTRIB = "Require-Bundle";

    private ServiceReference ref;
    private PackageAdmin admin;

    @Option(name = "--indent", description = "Indentation method")
    int indent = -1;

    protected void doExecute(List<Bundle> bundles) throws Exception {
        // Get package admin service.
        ref = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            System.out.println("PackageAdmin service is unavailable.");
            return;
        }

        try {
            admin = (PackageAdmin) getBundleContext().getService(ref);
            if (admin == null) {
                System.out.println("PackageAdmin service is unavailable.");
                return;
            }

            if (bundles == null) {
                Bundle[] allBundles = getBundleContext().getBundles();
                for (int i = 0; i < allBundles.length; i++) {
                    printHeaders(allBundles[i]);
                }
            } else {
                for (Bundle bundle : bundles) {
                    printHeaders(bundle);
                }
            }
        } finally {
            getBundleContext().ungetService(ref);
        }
    }

    protected void printHeaders(Bundle bundle) throws Exception {
        String title = Util.getBundleName(bundle);
        System.out.println("\n" + title);
        System.out.println(Util.getUnderlineString(title));
        if (indent == 0) {
            Dictionary dict = bundle.getHeaders();
            Enumeration keys = dict.keys();
            while (keys.hasMoreElements()) {
                Object k = keys.nextElement();
                Object v = dict.get(k);
                System.out.println(k + " = " + Util.getValueString(v));
            }
        } else {
            System.out.println(generateFormattedOutput(bundle));
        }
    }

    protected String generateFormattedOutput(Bundle bundle) {
        StringBuilder output = new StringBuilder();
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
            output.append(e.getKey());
            output.append(" = \n");
            formatHeader(Util.getValueString(e.getValue()), null, output, indent);
            output.append("\n");
        }
        if (serviceAttribs.size() > 0) {
            output.append('\n');
        }

        Map<String, ClauseFormatter> formatters = new HashMap<String, ClauseFormatter>();
        formatters.put(REQUIRE_BUNDLE_ATTRIB, new ClauseFormatter() {
            public void pre(Clause clause, StringBuilder output) {
                boolean isSatisfied = checkBundle(clause.getName(), clause.getAttribute("version"));
                Ansi.ansi(output).fg(isSatisfied ? Ansi.Color.DEFAULT : Ansi.Color.RED).a("");
            }
            public void post(Clause clause, StringBuilder output) {
                Ansi.ansi(output).reset().a("");
            }
        });
        formatters.put(IMPORT_PACKAGES_ATTRIB, new ClauseFormatter() {
            public void pre(Clause clause, StringBuilder output) {
                boolean isSatisfied = checkPackage(clause.getName(), clause.getAttribute("version"));
                boolean isOptional = "optional".equals(clause.getDirective("resolution"));
                Ansi.ansi(output).fg(isSatisfied ? Ansi.Color.DEFAULT : Ansi.Color.RED)
                                 .a(isSatisfied || isOptional ? Ansi.Attribute.INTENSITY_BOLD_OFF : Ansi.Attribute.INTENSITY_BOLD)
                                 .a("");
            }
            public void post(Clause clause, StringBuilder output) {
                Ansi.ansi(output).reset().a("");
            }
        });

        it = packagesAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(e.getKey());
            output.append(" = \n");
            formatHeader(Util.getValueString(e.getValue()), formatters.get(e.getKey()), output, indent);
            output.append("\n");
        }
        if (packagesAttribs.size() > 0) {
            output.append('\n');
        }

        return output.toString();
    }

    protected interface ClauseFormatter {
        void pre(Clause clause, StringBuilder output);
        void post(Clause clause, StringBuilder output);
    }

    protected void formatHeader(String header, ClauseFormatter formatter, StringBuilder builder, int indent) {
        Clause[] clauses = Parser.parseHeader(header);
        formatClauses(clauses, formatter, builder, indent);
    }

    protected void formatClauses(Clause[] clauses, ClauseFormatter formatter, StringBuilder builder, int indent) {
        boolean first = true;
        for (Clause clause : clauses) {
            if (first) {
                first = false;
            } else {
                builder.append(",\n");
            }
            formatClause(clause, formatter, builder, indent);
        }
    }

    protected void formatClause(Clause clause, ClauseFormatter formatter, StringBuilder builder, int indent) {
        builder.append("\t");
        if (formatter != null) {
            formatter.pre(clause, builder);
        }
        formatClause(clause, builder, indent);
        if (formatter != null) {
            formatter.post(clause, builder);
        }
    }

    protected int getTermWidth() {
        Terminal term = (Terminal) session.get(".jline.terminal");
        return term != null ? term.getWidth() : 80;

    }

    protected void formatClause(Clause clause, StringBuilder builder, int indent) {
        if (indent < 0) {
            if (clause.toString().length() < getTermWidth() - 8) { // -8 for tabs
                indent = 1;
            } else {
                indent = 3;
            }
        }
        String name = clause.getName();
        Directive[] directives = clause.getDirectives();
        Attribute[] attributes = clause.getAttributes();
        Arrays.sort(directives, new Comparator<Directive>() {
            public int compare(Directive o1, Directive o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Arrays.sort(attributes, new Comparator<Attribute>() {
            public int compare(Attribute o1, Attribute o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        builder.append(name);
        for (int i = 0; directives != null && i < directives.length; i++) {
            builder.append(";");
            if (indent > 1) {
                builder.append("\n\t\t");
            }
            builder.append(directives[i].getName()).append(":=");
            String v = directives[i].getValue();
            if (v.contains(",")) {
                if (indent > 2 && v.length() > 20) {
                    v = v.replace(",", ",\n\t\t\t");
                }
                builder.append("\"").append(v).append("\"");
            } else {
                builder.append(v);
            }
        }
        for (int i = 0; attributes != null && i < attributes.length; i++) {
            builder.append(";");
            if (indent > 1) {
                builder.append("\n\t\t");
            }
            builder.append(attributes[i].getName()).append("=");
            String v = attributes[i].getValue();
            if (v.contains(",")) {
                if (indent > 2 && v.length() > 20) {
                    v = v.replace(",", ",\n\t\t\t");
                }
                builder.append("\"").append(v).append("\"");
            } else {
                builder.append(v);
            }
        }
    }


   private boolean checkBundle(String bundleName, String version) {
        if (admin != null) {
            Bundle[] bundles = admin.getBundles(bundleName, version);
            return bundles != null && bundles.length > 0;
        }
        return false;
    }

    private boolean checkPackage(String packageName, String version) {
        VersionRange range = VersionRange.parseVersionRange(version);
        if (admin != null) {
            ExportedPackage[] packages = admin.getExportedPackages(packageName);
            if (packages != null) {
                for (ExportedPackage export : packages) {
                    if (range.contains(export.getVersion())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
