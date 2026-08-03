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
package org.apache.karaf.services.url.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.karaf.services.url.MavenResolver;
import org.apache.karaf.util.maven.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link MavenResolver} service.
 * <p>
 * Resolves Maven artifacts from local default repositories first,
 * then falls back to remote repositories, downloading to the local repository.
 */
public class MavenResolverImpl implements MavenResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolverImpl.class);

    private static final int BUFFER_SIZE = 8192;

    private final MavenConfiguration configuration;

    public MavenResolverImpl(MavenConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public File resolve(String url) throws IOException {
        if (url == null) {
            throw new IOException("URL cannot be null");
        }
        String path = url;
        if (path.startsWith("mvn:")) {
            path = path.substring("mvn:".length());
        }
        Parser parser = new Parser(path);
        return resolve(parser);
    }

    @Override
    public File resolve(String groupId, String artifactId, String version, String type, String classifier) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(groupId).append('/').append(artifactId);
        if (version != null && !version.isEmpty()) {
            sb.append('/').append(version);
        }
        if (type != null && !type.isEmpty()) {
            sb.append('/').append(type);
        }
        if (classifier != null && !classifier.isEmpty()) {
            if (type == null || type.isEmpty()) {
                sb.append('/');
            }
            sb.append('/').append(classifier);
        }
        Parser parser = new Parser(sb.toString());
        return resolve(parser);
    }

    @Override
    public File getLocalRepository() {
        return configuration.getLocalRepository();
    }

    private File resolve(Parser parser) throws IOException {
        String artifactPath = parser.getArtifactPath();

        // 1. Check default repositories (local file-based)
        for (String repo : configuration.getDefaultRepositories()) {
            File file = resolveFromDefaultRepository(repo, artifactPath);
            if (file != null) {
                LOG.debug("Resolved {} from default repository", artifactPath);
                return file;
            }
        }

        // 2. Check local repository
        File localFile = new File(configuration.getLocalRepository(), artifactPath);
        if (localFile.exists() && localFile.isFile()) {
            LOG.debug("Resolved {} from local repository", artifactPath);
            return localFile;
        }

        // 3. Try remote repositories
        for (String repo : configuration.getRepositories()) {
            File file = resolveFromRemoteRepository(repo, artifactPath, localFile);
            if (file != null) {
                LOG.debug("Resolved {} from remote repository {}", artifactPath, repo);
                return file;
            }
        }

        throw new IOException("Could not resolve artifact: mvn:" + parser.toMvnURI());
    }

    private File resolveFromDefaultRepository(String repo, String artifactPath) {
        // Parse repository URL, stripping @id=... @snapshots @noreleases flags
        String repoUrl = stripRepositoryFlags(repo);

        File repoDir;
        if (repoUrl.startsWith("file:")) {
            repoDir = new File(URI.create(repoUrl));
        } else {
            repoDir = new File(repoUrl);
        }

        File file = new File(repoDir, artifactPath);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    private File resolveFromRemoteRepository(String repo, String artifactPath, File localFile) throws IOException {
        String repoUrl = stripRepositoryFlags(repo);

        // Skip file-based repos that were already checked
        if (repoUrl.startsWith("file:")) {
            return null;
        }

        // Build the full URL
        String url = repoUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += artifactPath;

        int retryCount = configuration.getRetryCount();
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                return downloadArtifact(url, localFile);
            } catch (IOException e) {
                if (attempt < retryCount) {
                    LOG.debug("Retry {}/{} for {}: {}", attempt + 1, retryCount, url, e.getMessage());
                } else {
                    LOG.debug("Failed to download {} after {} attempts: {}", url, retryCount + 1, e.getMessage());
                }
            }
        }
        return null;
    }

    private File downloadArtifact(String url, File localFile) throws IOException {
        LOG.debug("Downloading {}", url);

        URLConnection connection = new URL(url).openConnection();
        if (connection instanceof HttpsURLConnection && !configuration.isCertificateCheck()) {
            disableSslVerification((HttpsURLConnection) connection);
        }

        connection.setConnectTimeout(configuration.getConnectionTimeout());
        connection.setReadTimeout(configuration.getReadTimeout());

        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode + " for " + url);
            }
        }

        // Download to a temp file, then move to the target location
        File parentDir = localFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Could not create directory: " + parentDir);
        }

        File tmpFile = new File(parentDir, localFile.getName() + ".tmp");
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(tmpFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            tmpFile.delete();
            throw e;
        }

        // Verify checksum if available
        if (connection instanceof HttpURLConnection) {
            verifyChecksum(url, tmpFile);
        }

        // Atomic rename
        if (!tmpFile.renameTo(localFile)) {
            // Fallback: copy and delete
            tmpFile.delete();
            throw new IOException("Could not move downloaded file to " + localFile);
        }

        return localFile;
    }

    private void verifyChecksum(String url, File file) throws IOException {
        String checksumPolicy = configuration.getChecksumPolicy();
        if ("ignore".equals(checksumPolicy)) {
            return;
        }

        // Try SHA-1 first, then MD5
        String remoteChecksum = fetchChecksum(url + ".sha1");
        if (remoteChecksum != null) {
            String localChecksum = computeChecksum(file, "SHA-1");
            if (!remoteChecksum.equalsIgnoreCase(localChecksum)) {
                String msg = "SHA-1 checksum mismatch for " + url;
                if ("fail".equals(checksumPolicy)) {
                    file.delete();
                    throw new IOException(msg);
                }
                LOG.warn(msg);
            }
            return;
        }

        remoteChecksum = fetchChecksum(url + ".md5");
        if (remoteChecksum != null) {
            String localChecksum = computeChecksum(file, "MD5");
            if (!remoteChecksum.equalsIgnoreCase(localChecksum)) {
                String msg = "MD5 checksum mismatch for " + url;
                if ("fail".equals(checksumPolicy)) {
                    file.delete();
                    throw new IOException(msg);
                }
                LOG.warn(msg);
            }
        }
    }

    private String fetchChecksum(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(configuration.getConnectionTimeout());
            connection.setReadTimeout(configuration.getReadTimeout());
            if (connection instanceof HttpURLConnection) {
                int code = ((HttpURLConnection) connection).getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    return null;
                }
            }
            try (InputStream in = connection.getInputStream()) {
                byte[] data = in.readAllBytes();
                String checksum = new String(data).trim();
                // Handle checksum files that contain filename after the hash
                int space = checksum.indexOf(' ');
                if (space > 0) {
                    checksum = checksum.substring(0, space);
                }
                return checksum;
            }
        } catch (IOException e) {
            LOG.debug("Could not fetch checksum from {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String computeChecksum(File file, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream in = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Algorithm not available: " + algorithm, e);
        }
    }

    private void disableSslVerification(HttpsURLConnection connection) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            LOG.warn("Could not disable SSL verification", e);
        }
    }

    static String stripRepositoryFlags(String repo) {
        // Strip flags like @id=central @snapshots @noreleases @multi @update=...
        String result = repo;
        while (result.contains("@")) {
            int atIdx = result.indexOf('@');
            int nextComma = result.indexOf(',', atIdx);
            int nextSpace = result.indexOf(' ', atIdx);
            int end;
            if (nextComma >= 0 && nextSpace >= 0) {
                end = Math.min(nextComma, nextSpace);
            } else if (nextComma >= 0) {
                end = nextComma;
            } else if (nextSpace >= 0) {
                end = nextSpace;
            } else {
                end = result.length();
            }
            result = result.substring(0, atIdx) + result.substring(end);
        }
        return result.trim();
    }

}
