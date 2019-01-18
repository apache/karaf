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
package org.apache.karaf.kar.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.karaf.util.StreamUtils;
import org.apache.karaf.util.maven.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a Karaf Kar archive
 * 
 * A Kar archive is a jar file with a special structure that can be used
 * to deploy feature repositories, maven repo contents and resources for the
 * karaf installation.
 * 
 * meta-inf/Manifest: 
 *   Karaf-Feature-Start: (true|false) Controls if the features in the feature repos should be started on deploy
 *   Karaf-Feature-Repos: (uri)* If present then only the given feature repo urls are added to karaf if it is not
 *      present then the karaf file is scanned for repo files
 *      
 * repository/
 *   Everything below this directory is treated as a maven repository. On deploy the contents
 *   will be copied to a directory below data. This directory will then be added to the 
 *   maven repos of pax url maven
 *   
 * resource/
 *   Everything below this directory will be copied to the karaf base dir on deploy
 * 
 */
public class Kar {

    public static final Logger LOGGER = LoggerFactory.getLogger(KarServiceImpl.class);
    public static final String MANIFEST_ATTR_KARAF_FEATURE_START = "Karaf-Feature-Start";
    public static final String MANIFEST_ATTR_KARAF_FEATURE_REPOS = "Karaf-Feature-Repos";
    private final URI karUri;
    private boolean shouldInstallFeatures;
    private List<URI> featureRepos;

    public Kar(URI karUri) {
        this.karUri = karUri;
    }

    /**
     * Extract a kar from a given URI into a repository dir and resource dir
     * and populate shouldInstallFeatures and featureRepos
     *
     * @param repoDir directory to write the repository contents of the kar to
     * @param resourceDir directory to write the resource contents of the kar to
     */
    public void extract(File repoDir, File resourceDir) {
        InputStream is = null;
        JarInputStream zipIs = null;
        FeatureDetector featureDetector = new FeatureDetector();
        this.featureRepos = new ArrayList<>();
        this.shouldInstallFeatures = true;

        try {
            is = karUri.toURL().openStream();
            repoDir.mkdirs();

            if (!repoDir.isDirectory()) {
                throw new RuntimeException("The KAR file " + karUri + " is already installed");
            }

            LOGGER.debug("Uncompress the KAR file {} into directory {}", karUri, repoDir);
            zipIs = new JarInputStream(is);
            boolean scanForRepos = true;

            Manifest manifest = zipIs.getManifest();
            if (manifest != null) {
                Attributes attr = manifest.getMainAttributes();
                String featureStartSt = (String)attr
                    .get(new Attributes.Name(MANIFEST_ATTR_KARAF_FEATURE_START));
                if ("false".equals(featureStartSt)) {
                    shouldInstallFeatures = false;
                }
                String featureReposAttr = (String)attr
                    .get(new Attributes.Name(MANIFEST_ATTR_KARAF_FEATURE_REPOS));
                if (featureReposAttr != null) {
                    featureRepos.add(new URI(featureReposAttr));
                    scanForRepos = false;
                }
            }

            ZipEntry entry = zipIs.getNextEntry();
            while (entry != null) {
                if (entry.getName().contains("..") || entry.getName().contains("%2e%2e")) {
                    LOGGER.warn("kar entry {} contains a .. relative path. For security reasons, it's not allowed.", entry.getName());
                } else {
                    if (entry.getName().startsWith("repository/")) {
                        String path = entry.getName().substring("repository/".length());
                        File destFile = new File(repoDir, path);
                        extract(zipIs, entry, destFile);
                        if (scanForRepos && featureDetector.isFeaturesRepository(destFile)) {
                            Map map = new HashMap<>();
                            String uri = Parser.pathToMaven(path, map);
                            if (map.get("classifier") != null && ((String) map.get("classifier")).equalsIgnoreCase("features"))
                                featureRepos.add(URI.create(uri));
                            else featureRepos.add(destFile.toURI());
                        }
                    }

                    if (entry.getName().startsWith("resources/")) {
                        String path = entry.getName().substring("resources/".length());
                        File destFile = new File(resourceDir, path);
                        extract(zipIs, entry, destFile);
                    }
                }
                entry = zipIs.getNextEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error extracting kar file " + karUri + " into dir " + repoDir + ": " + e.getMessage(), e);
        } finally {
            closeStream(zipIs);
            closeStream(is);
        }
    }

    /**
     * Extract an entry from a KAR file
     * 
     * @param is
     * @param zipEntry
     * @param dest
     * @return
     * @throws Exception
     */
    private static File extract(InputStream is, ZipEntry zipEntry, File dest) throws Exception {
        if (zipEntry.isDirectory()) {
            LOGGER.debug("Creating directory {}", dest.getName());
            dest.mkdirs();
        } else {
            dest.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(dest);
            StreamUtils.copy(is, out);
            out.close();
        }
        return dest;
    }

    private static void closeStream(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                LOGGER.warn("Error closing stream", e);
            }
        }
    }

    public String getKarName() {
        try {
            String url = karUri.toURL().toString();
            if (url.startsWith("mvn")) {
                int index = url.indexOf("/");
                url = url.substring(index + 1);
                index = url.indexOf("/");
                url = url.substring(0, index);
                return url;
            } else {
                String karName = new File(karUri.toURL().getFile()).getName();
                karName = karName.substring(0, karName.lastIndexOf("."));
                return karName;
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid kar URI " + karUri, e);
        }
    }
    
    public URI getKarUri() {
        return karUri;
    }

    public boolean isShouldInstallFeatures() {
        return shouldInstallFeatures;
    }

    public List<URI> getFeatureRepos() {
        return featureRepos;
    } 

}
