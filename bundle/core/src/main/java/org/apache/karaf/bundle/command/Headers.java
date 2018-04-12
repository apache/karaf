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
package org.apache.karaf.bundle.command;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

@Command(scope = "bundle", name = "headers", description = "Displays OSGi headers of a given bundles.")
@Service
public class Headers extends BundlesCommand {

    protected final static String KARAF_PREFIX = "Karaf-";
    protected final static String BUNDLE_PREFIX = "Bundle-";
    protected final static String PACKAGE_SUFFFIX = "-Package";
    protected final static String SERVICE_SUFFIX = "-Service";
    protected final static String CAPABILITY_SUFFIX = "-Capability";
    protected final static String IMPORT_PACKAGES_ATTRIB = "Import-Package";
    protected final static String REQUIRE_BUNDLE_ATTRIB = "Require-Bundle";

    @Option(name = "--indent", description = "Indentation method")
    int indent = -1;
    
    @Option(name = "--no-uses", description = "Print or not the Export-Package uses section")
    boolean noUses = false;

    @Reference(optional = true)
    Terminal terminal;

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
        String title = ShellUtil.getBundleName(bundle);
        System.out.println("\n" + title);
        System.out.println(ShellUtil.getUnderlineString(title));
        if (indent == 0) {
            Dictionary<String, String> dict = bundle.getHeaders();
            Enumeration<String> keys = dict.keys();
            while (keys.hasMoreElements()) {
                Object k = keys.nextElement();
                Object v = dict.get(k);
                System.out.println(k + " = " + ShellUtil.getValueString(v));
            }
        } else {
            System.out.println(generateFormattedOutput(bundle));
        }
    }

    protected String generateFormattedOutput(Bundle bundle) {
        StringBuilder output = new StringBuilder();
        Map<String, Object> otherAttribs = new TreeMap<>();
        Map<String, Object> karafAttribs = new TreeMap<>();
        Map<String, Object> bundleAttribs = new TreeMap<>();
        Map<String, Object> serviceAttribs = new TreeMap<>();
        Map<String, Object> packagesAttribs = new TreeMap<>();
        Dictionary<String, String> dict = bundle.getHeaders();
        Enumeration<String> keys = dict.keys();

        // do an initial loop and separate the attributes in different groups
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            Object v = dict.get(k);
            if (k.startsWith(KARAF_PREFIX)) {
                // starts with Karaf-xxx
                karafAttribs.put(k, v);
            } else if (k.startsWith(BUNDLE_PREFIX)) {
                // starts with Bundle-xxx
                bundleAttribs.put(k, v);
            } else if (k.endsWith(SERVICE_SUFFIX) || k.endsWith(CAPABILITY_SUFFIX)) {
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
        // all Karaf attributes
        //
        // all Bundle attributes
        //
        // all Service attributes
        //
        // all Package attributes
        Iterator<Map.Entry<String, Object>> it = otherAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(String.format("%s = %s\n", e.getKey(), ShellUtil.getValueString(e.getValue())));
        }
        if (otherAttribs.size() > 0) {
            output.append('\n');
        }

        it = karafAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(String.format("%s = %s\n", e.getKey(), ShellUtil.getValueString(e.getValue())));
        }
        if (karafAttribs.size() > 0) {
            output.append('\n');
        }

        it = bundleAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(String.format("%s = %s\n", e.getKey(), ShellUtil.getValueString(e.getValue())));
        }
        if (bundleAttribs.size() > 0) {
            output.append('\n');
        }

        it = serviceAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(e.getKey());
            output.append(" = \n");
            formatHeader(ShellUtil.getValueString(e.getValue()), null, output, indent);
            output.append("\n");
        }
        if (serviceAttribs.size() > 0) {
            output.append('\n');
        }

        Map<String, ClauseFormatter> formatters = new HashMap<>();
        formatters.put(REQUIRE_BUNDLE_ATTRIB, new ClauseFormatter() {
            public void pre(Clause clause, StringBuilder output) {
                boolean isSatisfied = checkBundle(clause.getName(), clause.getAttribute("bundle-version"));
                output.append(isSatisfied ? SimpleAnsi.COLOR_DEFAULT : SimpleAnsi.COLOR_RED);
            }
            public void post(Clause clause, StringBuilder output) {
                output.append(SimpleAnsi.RESET);
            }
        });
        formatters.put(IMPORT_PACKAGES_ATTRIB, new ClauseFormatter() {
            public void pre(Clause clause, StringBuilder output) {
                boolean isSatisfied = checkPackage(clause.getName(), clause.getAttribute("version"));
                boolean isOptional = "optional".equals(clause.getDirective("resolution"));
                output.append(isSatisfied ? SimpleAnsi.COLOR_DEFAULT : SimpleAnsi.COLOR_RED);
                output.append(isSatisfied || isOptional ? SimpleAnsi.INTENSITY_NORMAL : SimpleAnsi.INTENSITY_BOLD);
            }
            public void post(Clause clause, StringBuilder output) {
                output.append(SimpleAnsi.RESET);
            }
        });

        it = packagesAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(e.getKey());
            output.append(" = \n");
            formatHeader(ShellUtil.getValueString(e.getValue()), formatters.get(e.getKey()), output, indent);
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
        return terminal != null ? terminal.getWidth() : 0;
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
        Arrays.sort(directives, Comparator.comparing(Directive::getName));
        Arrays.sort(attributes, Comparator.comparing(Attribute::getName));
        builder.append(name);
        for (int i = 0; directives != null && i < directives.length; i++) {
            if (noUses && directives[i].getName().equalsIgnoreCase("uses")) {
                continue;
            }
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
        VersionRange vr = VersionRange.parseVersionRange(version);
        Bundle[] bundles = bundleContext.getBundles();
        for (int i = 0; (bundles != null) && (i < bundles.length); i++) {
            String sym = bundles[i].getSymbolicName();
            if ((sym != null) && sym.equals(bundleName)) {
                if (vr.contains(bundles[i].getVersion())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkPackage(String packageName, String version) {
        VersionRange range = VersionRange.parseVersionRange(version);
        Bundle[] bundles = bundleContext.getBundles();
        for (int i = 0; (bundles != null) && (i < bundles.length); i++) {
            BundleWiring wiring = bundles[i].adapt(BundleWiring.class);
            List<BundleCapability> caps = wiring != null ? wiring.getCapabilities(BundleRevision.PACKAGE_NAMESPACE) : null;
            if (caps != null) {
                for (BundleCapability cap : caps) {
                    String n = getAttribute(cap, BundleRevision.PACKAGE_NAMESPACE);
                    String v = getAttribute(cap, Constants.VERSION_ATTRIBUTE);
                    if (packageName.equals(n) && range.contains(VersionTable.getVersion(v))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private String getAttribute(BundleCapability cap, String name)  {
        Object obj = cap.getAttributes().get(name);
        return obj != null ? obj.toString() : null;
    }

}
