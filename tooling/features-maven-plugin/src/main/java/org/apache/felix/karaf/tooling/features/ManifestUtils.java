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
package org.apache.felix.karaf.tooling.features;

import java.util.LinkedList;
import java.util.List;

import org.osgi.impl.bundle.obr.resource.Manifest;
import org.osgi.impl.bundle.obr.resource.ManifestEntry;

/**
 * A set of utility methods to ease working with {@link org.osgi.impl.bundle.obr.resource.Manifest} and
 * {@link org.osgi.impl.bundle.obr.resource.ManifestEntry}
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
    public static List<ManifestEntry> getImports(Manifest manifest) {
        if (manifest.getImports() == null) {
            return new LinkedList<ManifestEntry>();
        } else {
            return manifest.getImports();
        }
    }

    /**
     * Get the list of non-optional imports from the manifest.
     *
     * @param manifest the manifest
     * @return the list of non-optional imports
     */
    public static List<ManifestEntry> getMandatoryImports(Manifest manifest) {
        List<ManifestEntry> result = new LinkedList<ManifestEntry>();
        for (ManifestEntry entry : getImports(manifest)) {
            if (!isOptional(entry)) {
                result.add(entry);
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
    public static List<ManifestEntry> getExports(Manifest manifest) {
        if (manifest.getExports() == null) {
            return new LinkedList<ManifestEntry>();
        } else {
            return manifest.getExports();
        }
    }

    /**
     * Check if a given manifest entry represents an optional import
     *
     * @param entry the manifest entry
     * @return <code>true</code> for an optional import, <code>false</code> for mandatory imports
     */
    public static boolean isOptional(ManifestEntry entry) {
        return "optional".equals(entry.getDirective("resolution"));
    }

    /**
     * Check if the manifest contains the mandatory Bundle-Symbolic-Name
     *
     * @param manifest the manifest
     * @return <code>true</code> if the manifest specifies a Bundle-Symbolic-Name
     */
    public static boolean isBundle(Manifest manifest) {
        return manifest.getBsn() != null;
    }

    public static boolean matches(ManifestEntry requirement, ManifestEntry export) {
        if (requirement.getName().equals(export.getName())) {
            if (requirement.getVersion().isRange()) {
                return requirement.getVersion().compareTo(export.getVersion()) == 0;
            } else {
                return requirement.getVersion().compareTo(export.getVersion()) <= 0;                
            }
        }
        return false;
    }
}
