/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.sigil.config;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;


public interface IBldProject
{

    static final String PROJECT_FILE = "sigil.properties";
    static final String PROJECT_DEFAULTS = "../sigil-defaults.properties";


    void save() throws IOException;


    void saveAs( File path ) throws IOException;


    void saveTo( OutputStream out ) throws IOException;


    /**
     * gets default package version ranges.
     */
    Properties getDefaultPackageVersions();


    /**
     * gets default package version range for named package.
     * Also handles wildcards in defaults.
     */
    String getDefaultPackageVersion( String name );


    /**
     * get project options.
     */
    Properties getOptions();


    /**
     * get project version.
     */
    String getVersion();


    /**
     * gets dependencies (Package-Import and Require-Bundle) needed to compile.
     */
    IBundleModelElement getDependencies();


    /**
     * gets project source directories.
     * This is a convenient way to specify bundle contents
     * when the project doesn't contains multiple bundles.
     */
    List<String> getSourceDirs();


    /**
     * gets the list of packages represented by getSourceDirs().
     * @throws IOException 
     */
    List<String> getSourcePkgs();


    /**
     * gets bundle ids.
     */
    List<String> getBundleIds();


    /**
     * gets bundles.
     */
    List<IBldBundle> getBundles();


    /**
     * convert specified bundle to SigilBundle.
     */
    ISigilBundle getSigilBundle( String id );


    /**
     * convert SigilBundle to specified bundle.
     */
    void setSigilBundle( String id, ISigilBundle sb );


    /**
     * converts default bundle to SigilBundle.
     */
    ISigilBundle getDefaultBundle();


    /**
     * converts SigilBundle to default bundle.
     */
    void setDefaultBundle( ISigilBundle sb );


    /**
     * resolves a relative path against the project file location.
     */
    File resolve( String path );


    /**
     * gets the last modification date of the project file.
     */
    long getLastModified();

    interface IBldBundle
    {
        /**
         * gets bundle activator
         */
        String getActivator();


        /**
         * gets bundle id within project.
         */
        String getId();


        /**
         * gets bundle version.
         */
        String getVersion();


        /**
         * gets the Bundle-SymbolicName.
         */
        String getSymbolicName();


        /**
         * gets bundles export-packages.
         */
        List<IPackageExport> getExports();


        /**
         * gets project import-packages. 
         */
        List<IPackageImport> getImports();


        /**
         * gets project require-bundles. 
         */
        List<IRequiredBundle> getRequires();


        /**
         * get bundle fragment-host. 
         */
        IRequiredBundle getFragmentHost();


        /**
         * gets bundle libs. 
         */
        Map<String, Map<String, String>> getLibs();


        /**
         * gets the bundle contents
         * @return list of package patterns.
         */
        List<String> getContents();


        /**
         * gets the bundle's associated dljar contents.
         * This is a convenience which avoids having to define another bundle
         * just for the dljar, which is then added to the parent bundle.
         * @return list of package patterns.
         */
        List<String> getDownloadContents();


        /**
         * gets the additional resources.
         * @return map with key as path in bundle, value as path in file system.
         * Paths are resolved relative to location of project file and also from classpath.
         */
        Map<String, String> getResources();


        /**
         * gets additional bundle headers.
         */
        Properties getHeaders();


        /**
         * resolves a relative path against the project file location.
         */
        File resolve( String path );
    }
}
