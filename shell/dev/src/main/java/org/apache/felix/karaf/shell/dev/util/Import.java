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
package org.apache.felix.karaf.shell.dev.util;

import java.util.LinkedList;
import java.util.List;

import org.apache.felix.utils.version.VersionRange;

/**
 * Simple class to model an OSGi Import-Package
 */
public class Import {

    private final String packageName;
    private final VersionRange version;
    private final String value;

    /**
     * Create a new import based on the string value found in MANIFEST.MF
     *
     * @param value the MANIFEST.MF value
     */
    protected Import(String value) {
        super();
        this.value = value;
        this.packageName = extractPackageName(value);
        if (value.contains("version=")) {
            this.version = extractVersion(value);
        } else {
            this.version = VersionRange.ANY_VERSION;
        }
    }

    /*
     * Extract the version from the string
     */
    private VersionRange extractVersion(String value) {
        int begin = value.indexOf("version=") + 8;
        int end = value.indexOf(";", begin);
        if (end < 0) {
            return VersionRange.parseVersionRange(unquote(value.substring(begin)));
        } else {
            return VersionRange.parseVersionRange(unquote(value.substring(begin, end)));
        }
    }

    /*
     * Remove leading/trailing quotes
     */
    private String unquote(String string) {
        return string.replace("\"", "");
    }

    public String getPackage() {
        return packageName;  
    }

    public VersionRange getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Parse the value of an Import-Package META-INF header and return
     * a list of Import instances
     */
    public static List<Import> parse(String value) {
        LinkedList<Import> imports = new LinkedList<Import>();
        for (String imp : split(value)) {
            imports.add(new Import(imp));
        }
        return imports;
    }

    /**
     * Parse the value of an Import-Package META-INF header and return
     * a list of Import instances, filtering out packages that are in the
     * Export-Package META-INF header
     *
     * @param importValue the value of the Import-Package header
     * @param exportValue the value of the Export-Package header
     */
    public static List<Import> parse(String importValue, String exportValue) {
        LinkedList<String> exports = new LinkedList<String>();
        for (String exp : split(exportValue)) {
            exports.add(extractPackageName(exp));
        }
        LinkedList<Import> imports = new LinkedList<Import>();
        for (Import imp : parse(importValue)) {
            if (!exports.contains(imp.getPackage())) {
                imports.add(imp);
            }
        }
        return imports;
    }

    /*
     * Extract the package name from the value
     * e.g. org.apache.felix.karaf;version="1.x" -> org.apache.felix.karaf
     */
    private static String extractPackageName(String value) {
        if (value.contains(";")) {
            return value.split(";")[0];
        } else {
            return value;
        }
    }

    /*
     * Counts the number of quotes in a String value
     */
    private static int quotes(String value) {
        return value.replaceAll("[^\"]", "").length();
    }

    /*
     * Split the OSGi headers on the , symbol
     */
    private static List<String> split(String value) {
        List<String> result = new LinkedList<String>();
        String[] elements = value.split(",");
        for (int i = 0; i < elements.length ; i++) {
            if (quotes(elements[i]) % 2 == 1) {
                // we probably split a version range, so joining it again with the next element
                result.add(elements[i] + "," + elements[++i]);
            } else {
                result.add(elements[i]);
            }
        }
        return result;
    }
}
