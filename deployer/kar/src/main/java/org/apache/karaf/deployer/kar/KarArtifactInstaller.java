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
package org.apache.karaf.deployer.kar;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class KarArtifactInstaller implements ArtifactInstaller {

    public static final String FEATURES_CLASSIFIER = "features";
    private final Logger logger = LoggerFactory.getLogger(KarArtifactInstaller.class);

    private static final String KAR_SUFFIX = ".kar";
    private static final String ZIP_SUFFIX = ".zip";

    private String base = "./";
    private String localRepoPath = "./target/local-repo";

    private String timestampPath;

    private DocumentBuilderFactory dbf;

    private FeaturesService featuresService;

    public void init() {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        timestampPath = localRepoPath + File.separator + ".timestamps";
        if (new File(timestampPath).mkdirs()) {
            logger.warn("Unable to create directory for Karaf Archive timestamps. Results may vary...");
        }

        logger.info("Karaf archives will be extracted to {}", localRepoPath);
        logger.info("Timestamps for Karaf archives will be extracted to {}", timestampPath);
    }

    public void destroy() {
        logger.info("Karaf archive installer destroyed.");
    }


    public void install(File file) throws Exception {
        // Check to see if this file has already been extracted. For example, on restart of Karaf,
        // we don't necessarily want to re-extract all the Karaf Archives!
        if (alreadyExtracted(file)) {
            logger.info("Ignoring '{}'; timestamp indicates it's already been deployed.", file);
            return;
        }

        logger.info("Installing KAR file {}", file);

        File karFile = new File(localRepoPath, file.getName());
        copy(file.toURI(), karFile);

        ZipFile zipFile = null;

        List<URI> featuresRepositoriesInKar = new ArrayList<URI>();

        try {
            zipFile = new ZipFile(karFile);

            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                String repoEntryName = getRepoEntryName(entry);

                if (repoEntryName != null) {
                    File extract = extract(zipFile, entry, repoEntryName, localRepoPath);
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
        } catch (Exception e) {
            throw e;
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }

        installFeatures(featuresRepositoriesInKar);

        updateTimestamp(file);
    }

    /**
     * Create a destination file using a source URL.
     *
     * @param url  the source URL.
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
                try {
                    is.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    private void installFeatures(List<URI> featuresRepositories) {
        for (Repository repository : featuresService.listRepositories()) {
            for (URI karFeatureRepoUri : featuresRepositories) {
                if (repository.getURI().equals(karFeatureRepoUri)) {
                    try {
                        for (Feature feature : repository.getFeatures()) {
                            try {
                                featuresService.installFeature(feature, EnumSet.noneOf(FeaturesService.Option.class));
                            } catch (Exception e) {
                                logger.warn("Unable to install Kar feature {}", feature.getName() + "/" + feature.getVersion(), e);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Can't get features for KAR {}", karFeatureRepoUri, e);
                    }
                }
            }
        }
    }

    private void uninstallFeatures(List<URI> featuresRepositories) {
        for (Repository repository : featuresService.listRepositories()) {
            for (URI karFeatureRepoUri : featuresRepositories) {
                if (repository.getURI().equals(karFeatureRepoUri)) {
                    try {
                        for (Feature feature : repository.getFeatures()) {
                            try {
                                featuresService.uninstallFeature(feature.getName(), feature.getVersion());
                            } catch (Exception e) {
                                logger.warn("Unable to uninstall Kar feature {}", feature.getName() + "/" + feature.getVersion(), e);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Can't get features for KAR {}", karFeatureRepoUri, e);
                    }
                }
            }
        }
    }

    private File extract(ZipFile zipFile, ZipEntry entry, String repoEntryName, String base) throws IOException {
        File extract;
        if (entry.isDirectory()) {
            extract = new File(base + File.separator + repoEntryName);
            logger.debug("Creating directory '{}'", extract.getName());
            extract.mkdirs();
        } else {
            extract = new File(base + File.separator + repoEntryName);
            extract.getParentFile().mkdirs();

            InputStream in = null;
            FileOutputStream out = null;

            try {
                in = zipFile.getInputStream(entry);
                out = new FileOutputStream(extract);

                byte[] buffer = new byte[8192];
                int count = in.read(buffer);
                int totalBytes = 0;
                while (count >= 0) {
                    out.write(buffer, 0, count);
                    totalBytes += count;
                    count = in.read(buffer);
                }

                logger.debug("Extracted {} bytes to {}", totalBytes, extract);
            } catch (IOException ioException) {
                throw ioException;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        // nothing to do
                    }
                }
                if (out != null) {
                    try {
                        out.flush();
                        out.close();
                    } catch (Exception e) {
                        // nothing to do
                    }
                }
            }
        }
        return extract;
    }

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

    public void uninstall(File file) throws Exception {
        logger.debug("Looking for KAR entries to purge the local repository");
        File karFile = new File(localRepoPath, file.getName());

        List<URI> featuresRepositories = new ArrayList<URI>();

        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(karFile);

            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String repoEntryName = getRepoEntryName(entry);
                if (repoEntryName != null) {
                    File toDelete = new File(localRepoPath + File.separator + repoEntryName);
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
        } catch (Exception e) {
            throw e;
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }

        uninstallFeatures(featuresRepositories);
        for (URI featuresRepository : featuresRepositories) {
            featuresService.removeRepository(featuresRepository);
            File toDelete = new File(featuresRepository);
            if (toDelete.exists() && toDelete.isFile()) {
                toDelete.delete();
            }
        }

        karFile.delete();

        File timestamp = getArchiveTimestampFile(file);
        if (timestamp.exists()) {
            logger.debug("Removing the timestamp file");
            timestamp.delete();
        }
    }

    public void update(File file) throws Exception {
        logger.warn("Karaf archive '{}' has been updated; redeploying.", file);
        uninstall(file);
        install(file);
    }

    protected void updateTimestamp(File karafArchive) throws Exception {
        File timestamp = getArchiveTimestampFile(karafArchive);

        if (timestamp.exists()) {
            logger.debug("Deleting old timestamp file '{}'", timestamp);

            if (!timestamp.delete()) {
                throw new Exception("Unable to delete archive timestamp '" + timestamp + "'");
            }
        }

        logger.debug("Creating timestamp file '{}'", timestamp);
        timestamp.createNewFile();
    }

    protected boolean alreadyExtracted(File karafArchive) {
        File timestamp = getArchiveTimestampFile(karafArchive);
        if (timestamp.exists()) {
            return timestamp.lastModified() >= karafArchive.lastModified();
        }
        return false;
    }

    protected File getArchiveTimestampFile(File karafArchive) {
        File timestampDir = new File(new File(localRepoPath), ".timestamps");
        if (!timestampDir.exists()) {
            timestampDir.mkdirs();
        }
        return new File(timestampDir, karafArchive.getName());
    }

    protected boolean isFeaturesRepository(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".xml")) {
                Document doc = parse(artifact);
                String name = doc.getDocumentElement().getLocalName();
                String uri = doc.getDocumentElement().getNamespaceURI();
                if ("features".equals(name) && (uri == null || "".equals(uri) || uri.startsWith("http://karaf.apache.org/xmlns/features/v"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("File '{}' is not a features file.", artifact.getName(), e);
        }
        return false;
    }

    protected Document parse(File artifact) throws Exception {
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

    private void addToFeaturesRepositories(URI uri) {
        // URI mvnUri = pathToMvnUri(path);
        try {
            featuresService.removeRepository(uri);
            featuresService.addRepository(uri);
            logger.info("Added feature repository '{}'.", uri);
        } catch (Exception e) {
            logger.warn("Unable to add repository '{}'", uri, e);
        }
    }

    static URI pathToMvnUri(String path) {
        String[] bits = path.split("/");
        String classifier = FEATURES_CLASSIFIER;
        String artifactType = "xml";
        String version = bits[bits.length - 2];
        String artifactId = bits[bits.length - 3];
        StringBuilder buf = new StringBuilder("mvn:");
        for (int i = 0; i < bits.length - 3; i++) {
            buf.append(bits[i]);
            if (i < bits.length - 4) {
                buf.append(".");
            }
        }
        buf.append("/").append(artifactId).append("/").append(version).append("/").append(artifactType).append("/").append(classifier);
        URI mvnUri = URI.create(buf.toString());
        return mvnUri;
    }

    public boolean canHandle(File file) {
        // If the file ends with .kar, then we can handle it!
        //
        if (file.isFile() && file.getName().endsWith(KAR_SUFFIX)) {
            logger.info("Found a .kar file to deploy.");
            return true;
        }
        // Otherwise, check to see if it's a zip file containing a META-INF/KARAF.MF manifest.
        //
        else if (file.isFile() && file.getName().endsWith(ZIP_SUFFIX)) {
            logger.debug("Found a .zip file to deploy; checking contents to see if it's a Karaf archive.");
            try {
                if (new ZipFile(file).getEntry("META-INF/KARAF.MF") != null) {
                    logger.info("Found a Karaf archive with .zip prefix; will deploy.");
                    return true;
                }
            } catch (Exception e) {
                logger.warn("Problem extracting zip file '{}'; ignoring.", file.getName(), e);
            }
        }

        return false;
    }

    public boolean deleteLocalRepository() {
        return deleteDirectory(new File(localRepoPath));
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public void setBasePath(String base) {
        this.base = base;
    }

    public void setLocalRepoPath(String localRepoPath) {
        this.localRepoPath = localRepoPath;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

}
