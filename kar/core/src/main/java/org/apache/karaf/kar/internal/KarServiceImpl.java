/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.kar.internal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.kar.KarService;
import org.ops4j.pax.url.mvn.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Implementation of the KAR service.
 */
public class KarServiceImpl implements KarService {

    private static final String MANIFEST_ATTR_KARAF_FEATURE_START = "Karaf-Feature-Start";

    public static final Logger LOGGER = LoggerFactory.getLogger(KarServiceImpl.class);

    private String storage = "./target/data/kar"; // ${KARAF.DATA}/kar
    private String base = "./";
    private String localRepo = "./target/local-repo"; // ${KARAF.BASE}/system
    private FeaturesService featuresService;
    
    private DocumentBuilderFactory dbf;

    /**
     * Init method.
     *
     * @throws Exception in case of init failure.
     */
    public void init() throws Exception {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        
        File karStorageDir = new File(storage);
        if (!karStorageDir.exists()) {
            LOGGER.debug("Create the KAR storage directory");
            karStorageDir.mkdirs();
        }
        if (!karStorageDir.isDirectory()) {
            throw new IllegalStateException("KAR storage " + storage + " is not a directory");
        }
    }
    
    public void install(URI url) throws Exception {
        File file = new File(url.toURL().getFile());
        String karName = file.getName();
        
        LOGGER.debug("Installing KAR {} from {}", karName, url);
        
        LOGGER.debug("Check if the KAR file already exists in the storage directory");
        File karStorage = new File(storage);
        File karFile = new File(karStorage, karName);
        
        if (karFile.exists()) {
            LOGGER.warn("The KAR file " + karName + " is already installed. Override it.");
        }
        
        LOGGER.debug("Copy the KAR file from {} to {}", url, karFile.getParent());
        copy(url, karFile);
        
        LOGGER.debug("Uncompress the KAR file {} into the system repository", karName);
        ZipFile zipFile = new ZipFile(karFile);
        
        List<URI> featuresRepositoriesInKar = new ArrayList<URI>();
        boolean shouldInstallFeatures = true;
        
        @SuppressWarnings("unchecked")
        Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            String repoEntryName = getRepoEntryName(entry);

            if (repoEntryName != null) {
                File extract = extract(zipFile, entry, repoEntryName, localRepo);
                if (isFeaturesRepository(extract)) {
                    addToFeaturesRepositories(extract.toURI());
                    featuresRepositoriesInKar.add(extract.toURI());
                }
            }
            
            if ("meta-inf/manifest.mf".equals(entry.getName().toLowerCase())) {
                InputStream is = zipFile.getInputStream(entry);
                Manifest manifest = new Manifest(is);
                Attributes attr = manifest.getMainAttributes();
                String featureStartSt = (String)attr.get(new Attributes.Name(MANIFEST_ATTR_KARAF_FEATURE_START));
                if ("true".equals(featureStartSt)) {
                    shouldInstallFeatures = false;
                }
                is.close();
            }

            if (entry.getName().startsWith("resource")) {
                String resourceEntryName = entry.getName().substring("resource/".length());
                extract(zipFile, entry, resourceEntryName, base);
            }
        }
        
        if (shouldInstallFeatures) {
            installFeatures(featuresRepositoriesInKar);
        }
        
        zipFile.close();
    }

    public void uninstall(String karName) throws Exception {
        uninstall(karName, false);
    }

    public void uninstall(String karName, boolean clean) throws Exception {
        File karStorage = new File(storage);
        File karFile = new File(karStorage, karName);
        
        if (!karFile.exists()) {
            throw new IllegalArgumentException("The KAR " + karName + " is not installed");
        }

        if (clean) {
            LOGGER.debug("Looking for KAR entries to purge the local repository");
            List<URI> featuresRepositories = new ArrayList<URI>();
            ZipFile zipFile = new ZipFile(karFile);
            @SuppressWarnings("unchecked")
            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String repoEntryName = getRepoEntryName(entry);
                if (repoEntryName != null) {
                    File toDelete = new File(localRepo + File.separator + repoEntryName);
                    if (isFeaturesRepository(toDelete)) {
                        featuresRepositories.add(toDelete.toURI());
                    } else {
                        if (toDelete.isFile() && toDelete.exists()) {
                            toDelete.delete();
                        }
                    }
                }
                if (entry.getName().startsWith("resource")) {
                    String resourceEntryName = entry.getName().substring("resource/".length());
                    File toDelete = new File(base + File.separator + resourceEntryName);
                    if (toDelete.isFile() && toDelete.exists()) {
                        toDelete.delete();
                    }
                }
            }
            zipFile.close();

            uninstallFeatures(featuresRepositories);
            for (URI featuresRepository : featuresRepositories) {
                featuresService.removeRepository(featuresRepository);
                File toDelete = new File(featuresRepository);
                if (toDelete.exists() && toDelete.isFile()) {
                    toDelete.delete();
                }
            }
        }
        
        karFile.delete();
    }
    
    public List<String> list() throws Exception {
        File karStorage = new File(storage);
        List<String> kars = new ArrayList<String>();
        for (File kar : karStorage.listFiles()) {
            kars.add(kar.getName());
        }
        return kars;
    }

    /**
     * Create a destination file using a source URL.
     * 
     * @param url the source URL. 
     * @param file the destination file
     * @throws Exception in case of copy failure
     */
    private void copy(URI url, File file) throws Exception {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = url.toURL().openStream();
            fos = new FileOutputStream(file);
            copyStream(is,  fos);
        } finally {
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.flush();
                fos.close();
            }
        }
    }

    /**
     * Get the entry name filtering the repository  folder.
     *
     * @param entry the "normal" entry name.
     * @return the entry with the repository folder filtered.
     */
    private String getRepoEntryName(ZipEntry entry) {
        String entryName = entry.getName();
        if (entryName.startsWith("repository")) {
            return entryName.substring("repository/".length());
        }
        if (entryName.startsWith("META-INF") || entryName.startsWith("resources")) {
            return null;
        }
        return entryName;
    }

    /**
     * Extract an entry from a KAR file.
     * 
     * @param zipFile the KAR file (zip file).
     * @param zipEntry the entry in the KAR file.
     * @param repoEntryName the target extract name.
     * @param base the base directory where to extract the file.
     * @return the extracted file.
     * @throws Exception in the extraction fails.
     */
    private File extract(ZipFile zipFile, ZipEntry zipEntry, String repoEntryName, String base) throws Exception {
        File extract;
        if (zipEntry.isDirectory()) {
            extract = new File(base + File.separator + repoEntryName);
            LOGGER.debug("Creating directory {}", extract.getName());
            extract.mkdirs();
        } else {
            extract = new File(base + File.separator + repoEntryName);
            extract.getParentFile().mkdirs();
            
            InputStream in = zipFile.getInputStream(zipEntry);
            FileOutputStream out = new FileOutputStream(extract);
            
            byte[] buffer = new byte[8192];
            int count = in.read(buffer);
            int totalBytes = 0;
            
            while (count >= 0) {
                out.write(buffer, 0, count);
                totalBytes += count;
                count = in.read(buffer);
            }

            LOGGER.debug("Extracted {} bytes to {}", totalBytes, extract);

            in.close();
            out.flush();
            out.close();
        }
        return extract;
    }

    /**
     * Check if a file is a features XML.
     *
     * @param artifact the file to check.
     * @return true if the artifact is a features XML, false else.
     */
    private boolean isFeaturesRepository(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".xml")) {
                Document doc = parse(artifact);
                String name = doc.getDocumentElement().getLocalName();
                String uri  = doc.getDocumentElement().getNamespaceURI();
                if ("features".equals(name) && (uri == null || "".equals(uri) || uri.startsWith("http://karaf.apache.org/xmlns/features/v"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("File '{}' is not a features file.", artifact.getName(), e);
        }
        return false;
    }

    /**
     * Parse a features XML.
     *
     * @param artifact the features XML to parse.
     * @return the parsed document.
     * @throws Exception in case of parsing failure.
     */
    private Document parse(File artifact) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
            }
            public void error(SAXParseException exception) throws SAXException {
            }
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        return db.parse(artifact);
    }

    /**
     * Add an URI to the list of features repositories.
     *
     * @param uri the URI to add.
     * @throws Exception in case of add failure.
     */
    private void addToFeaturesRepositories(URI uri) throws Exception {
        try {
            featuresService.removeRepository(uri);
            featuresService.addRepository(uri);
            LOGGER.info("Added feature repository '{}'", uri);
        } catch (Exception e) {
            LOGGER.warn("Unable to add repository '{}'", uri, e);
        }
    }

    /**
     * Install all features contained in the list of features XML.
     *
     * @param featuresRepositories the list of features XML.
     */
    private void installFeatures(List<URI> featuresRepositories) {
        for (Repository repository : featuresService.listRepositories()) {
            for (URI karFeatureRepoUri : featuresRepositories) {
                if (repository.getURI().equals(karFeatureRepoUri)) {
                    try {
                        for (Feature feature : repository.getFeatures()) {
                            try {
                                featuresService.installFeature(feature, EnumSet.noneOf(FeaturesService.Option.class));
                            } catch (Exception e) {
                                LOGGER.warn("Unable to install Kar feature {}", feature.getName() + "/" + feature.getVersion(), e);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Can't get features for KAR {}", karFeatureRepoUri, e);
                    }
                }
            }
        }
    }
    
    @Override
    public void create(String repoName, Set<String> features, PrintStream console) {
        FileOutputStream fos = null;
        JarOutputStream jos = null;
        try {
            String karPath = base + File.separator + "data" + File.separator + "kar" + File.separator + repoName + ".kar";
            File karFile = new File(karPath);
            karFile.getParentFile().mkdirs();
            fos = new FileOutputStream(karFile);
            String manifestSt = "Manifest-Version: 1.0\n" +
                MANIFEST_ATTR_KARAF_FEATURE_START +": true\n";
            InputStream manifestIs = new ByteArrayInputStream(manifestSt.getBytes("UTF-8"));
            Manifest manifest = new Manifest(manifestIs);
            jos = new JarOutputStream(new BufferedOutputStream(fos, 100000), manifest);
            
            Map<URI, Integer> locationMap = new HashMap<URI, Integer>();

            Repository repo = featuresService.getRepository(repoName);
            copyResourceToJar(jos, repo.getURI(), locationMap, console);
            
            for (Feature feature : repo.getFeatures()) {
                List<BundleInfo> bundles = feature.getBundles();
                for (BundleInfo bundleInfo : bundles) {
                    URI location = new URI(bundleInfo.getLocation());
                    copyResourceToJar(jos, location, locationMap, console);
                }
                List<ConfigFileInfo> configFiles = feature.getConfigurationFiles();
                for (ConfigFileInfo configFileInfo : configFiles) {
                    URI location = new URI(configFileInfo.getLocation());
                    copyResourceToJar(jos, location, locationMap, console);
                }
            }
            
            console.println("Kar file created : " + karPath);
        } catch (Exception e) {
            throw new RuntimeException("Error creating kar " + e.getMessage(), e);
        } finally {
            if (jos != null) {
                try {
                    jos.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing jar stream", e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing jar file stream", e);
                }
            }
        }
        
    }

    private void copyResourceToJar(JarOutputStream jos, URI location, Map<URI, Integer> locationMap, PrintStream console) {
        if (locationMap.containsKey(location)) {
            return;
        }
        try {
            console.println("Adding " + location);
            String noPrefixLocation = location.toString().substring(location.toString().lastIndexOf(":") + 1);
            Parser parser = new Parser(noPrefixLocation);
            InputStream is = location.toURL().openStream();
            String path = "repository/" + parser.getArtifactPath();
            jos.putNextEntry(new JarEntry(path));
            copyStream(is, jos);
            is.close();
            locationMap.put(location, 1);
        } catch (Exception e) {
            LOGGER.error("Error adding " + location, e);
        }
    }
    
    public static long copyStream(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[10000];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Uninstall all features contained in the list of features XML.
     *
     * @param featuresRepositories the list of features XML.
     */
    private void uninstallFeatures(List<URI> featuresRepositories) {
        for (Repository repository : featuresService.listRepositories()) {
            for (URI karFeatureRepoUri : featuresRepositories) {
                if (repository.getURI().equals(karFeatureRepoUri)) {
                    try {
                        for (Feature feature : repository.getFeatures()) {
                            try {
                                featuresService.uninstallFeature(feature.getName(), feature.getVersion());
                            } catch (Exception e) {
                                LOGGER.warn("Unable to uninstall Kar feature {}", feature.getName() + "/" + feature.getVersion(), e);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Can't get features for KAR {}", karFeatureRepoUri, e);
                    }
                }
            }
        }
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getLocalRepo() {
        return localRepo;
    }

    public void setLocalRepo(String localRepo) {
        this.localRepo = localRepo;
    }

}
