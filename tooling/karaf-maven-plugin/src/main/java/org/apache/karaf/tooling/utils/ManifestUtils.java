/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.osgi.framework.Constants;


/**
 * A set of utility methods to ease working with {@link org.apache.felix.utils.manifest.Parser} and
 * {@link org.apache.felix.utils.manifest.Clause}
 */

public class ManifestUtils {

    private ManifestUtils() {
        // hide the constructor
    }

    /**
     * Get the list of imports from the manifest.  If no imports have been defined, this method returns an empty list.
     *
     * @param manifest the manifest
     * @return the list of imports
     */
    public static List<Clause> getImports(Manifest manifest) {
        Clause[] clauses = Parser.parseHeader(getHeader(Constants.IMPORT_PACKAGE, manifest));
        return new LinkedList<>(Arrays.asList(clauses));
    }

    /**
     * Get the list of non-optional imports from the manifest.
     *
     * @param manifest the manifest
     * @return the list of non-optional imports
     */
    public static List<Clause> getMandatoryImports(Manifest manifest) {
        List<Clause> result = new LinkedList<>();
        for (Clause clause : getImports(manifest)) {
            if (!isOptional(clause)) {
                result.add(clause);
            }
        }
        return result;
    }

    /**
     * Get the list of exports from the manifest.  If no exports have been defined, this method returns an empty list.
     *
     * @param manifest the manifest
     * @return the list of exports
     */
    public static List<Clause> getExports(Manifest manifest) {
        Clause[] clauses = Parser.parseHeader(getHeader(Constants.EXPORT_PACKAGE, manifest));
        return new LinkedList<>(Arrays.asList(clauses));
    }

    /**
     * Check if a given manifest clause represents an optional import
     *
     * @param clause the manifest clause
     * @return <code>true</code> for an optional import, <code>false</code> for mandatory imports
     */
    public static boolean isOptional(Clause clause) {
        return "optional".equals(clause.getDirective("resolution"));
    }

    /**
     * Check if the manifest contains the mandatory Bundle-Symbolic-Name
     *
     * @param manifest the manifest
     * @return <code>true</code> if the manifest specifies a Bundle-Symbolic-Name
     */
    public static boolean isBundle(Manifest manifest) {
        return getBsn(manifest) != null;
    }

    public static boolean matches(Clause requirement, Clause export) {
        if (requirement.getName().equals(export.getName())) {
        	VersionRange importVersionRange = getVersionRange(requirement); 
        	VersionRange exportVersionRange = getVersionRange(export);
        	VersionRange intersection = importVersionRange.intersect(exportVersionRange);
        	return intersection != null;
        }
        return false;
    }
    
    public static String getHeader(String name, Manifest manifest) {
    	String value = manifest.getMainAttributes().getValue(name);
    	return value;    	
    }
    
    public static String getBsn(Manifest manifest) {
    	String bsn = getHeader(Constants.BUNDLE_SYMBOLICNAME, manifest);
        return bsn;
    }
    
    @SuppressWarnings("deprecation")
	public static VersionRange getVersionRange(Clause clause)
    {
        String v = clause.getAttribute(Constants.VERSION_ATTRIBUTE);
        if (v == null)
        {
            v = clause.getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION);
        }
        if (v == null)
        {
            v = clause.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        return VersionRange.parseVersionRange(v);
    }
}
