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

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.kar.KarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Implementation of the KAR service.
 */
public class KarServiceImpl implements KarService {

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
            if (entry.getName().startsWith("resource")) {
                String resourceEntryName = entry.getName().substring("resource/".length());
                extract(zipFile, entry, resourceEntryName, base);
            }
        }
        
        installFeatures(featuresRepositoriesInKar);
        
        zipFile.close();
    }
    
    public void uninstall(String karName) throws Exception {
        File karStorage = new File(storage);
        File karFile = new File(karStorage, karName);
        
        if (!karFile.exists()) {
            throw new IllegalArgumentException("The KAR " + karName + " is not installed");
        }

        karFile.delete();
    }
    
    public void uninstall(String karName, boolean clean) throws Exception {
        // TODO
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
            byte[] buffer = new byte[8192];
            int count = is.read(buffer);
            while (count >= 0) {
                fos.write(buffer, 0, count);
                count = is.read(buffer);
            }
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
