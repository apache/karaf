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

import org.osgi.framework.Version;

/**
 * Simple class to model an OSGi Import-Package
 */
public class Import {

    private final String packageName;
    private final Version version;
    private final String value;

    /**
     * Create a new import based on the string value found in MANIFEST.MF
     *
     * @param value the MANIFEST.MF value
     */
    protected Import(String value) {
        super();
        this.value = value;
        if (value.contains(";")) {
            this.packageName = value.split(";")[0];
        } else {
            this.packageName = value;
        }
        if (value.contains("version=")) {
            this.version = extractVersion(value);
        } else {
            this.version = Version.emptyVersion;
        }
    }

    /*
     * Extract the version from the string
     */
    private Version extractVersion(String value) {
        int begin = value.indexOf("version=") + 8;
        int end = value.indexOf(";", begin);
        if (end < 0) {
            return Version.parseVersion(unquote(value.substring(begin)));
        } else {
            return Version.parseVersion(unquote(value.substring(begin, end)));
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

    public Version getVersion() {
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
        for (String imp : value.split(",")) {
            imports.add(new Import(imp));
        }
        return imports;
    }
}
