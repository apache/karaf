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
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.karaf.features.spi.MavenResolver;
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

    /**
     * The checksums tried to verify a download: file extension and digest algorithm.
     */
    private static final String[][] CHECKSUMS = { { "sha1", "SHA-1" }, { "md5", "MD5" } };

    /**
     * The delay before the first new attempt of a failed download, doubled for each next one.
     */
    private static final long RETRY_DELAY = 500;

    /**
     * In the directory of a snapshot in the local repository, the time of the last check of each remote repository
     * (<code>id.lastCheck</code>), and the version of each file downloaded from a remote repository
     * (<code>file.updated</code>, with <code>file.lastModified</code> to know if the file was replaced since).
     */
    private static final String STATUS_FILE = "karaf-url-status.properties";
    private static final String LAST_CHECK = ".lastCheck";
    private static final String UPDATED = ".updated";
    private static final String LAST_MODIFIED = ".lastModified";

    /**
     * The metadata of the artifacts installed in a repository (<code>mvn install</code>).
     */
    private static final String LOCAL_METADATA = "maven-metadata-local.xml";

    private final MavenConfiguration configuration;
    private final MavenSettings settings;
    /**
     * The global policies, <code>null</code> when they are not set.
     */
    private final UpdatePolicy updatePolicy;
    private final ChecksumPolicy checksumPolicy;
    private final List<Repository> defaultRepositories;
    private final List<Repository> repositories;

    public MavenResolverImpl(MavenConfiguration configuration) {
        this.configuration = configuration;
        this.settings = configuration.getSettings() != null ? configuration.getSettings() : MavenSettings.EMPTY;
        String globalUpdatePolicy = configuration.getUpdatePolicy();
        this.updatePolicy = globalUpdatePolicy != null && !globalUpdatePolicy.trim().isEmpty()
                ? UpdatePolicy.parse(globalUpdatePolicy) : null;
        this.checksumPolicy = ChecksumPolicy.parse(configuration.getChecksumPolicy());
        this.defaultRepositories = parseRepositories(configuration.getDefaultRepositories(), true);
        this.repositories = parseRepositories(configuration.getRepositories(), false);
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
    public RetryChance isRetryableException(Exception exception) {
        // the downloads from the remote repositories are already retried (connection.retryCount)
        return RetryChance.NEVER;
    }

    private File resolve(Parser parser) throws IOException {
        // a timestamped version is resolved as its snapshot
        String artifactPath = parser.getArtifactPath();
        checkArtifactPath(artifactPath, parser);
        boolean snapshot = parser.getVersion().endsWith("-SNAPSHOT");

        // 1. Check default repositories (local file-based)
        for (Repository repository : defaultRepositories) {
            File file = resolveFromDirectory(repository, artifactPath);
            if (file != null) {
                LOG.debug("Resolved {} from default repository", artifactPath);
                return file;
            }
        }

        // 2. Check local repository, a snapshot may have a newer version in the remote repositories
        File localFile = new File(configuration.getLocalRepository(), artifactPath);
        if (localFile.isFile() && !snapshot) {
            LOG.debug("Resolved {} from local repository", artifactPath);
            return localFile;
        }

        // 3. Try the remote repositories with the snapshots or the releases, and then the repository of the URL
        // (mvn:repository_url!groupId/artifactId...)
        List<Repository> remoteRepositories = new ArrayList<>(repositories);
        if (parser.getRepositoryURL() != null) {
            remoteRepositories.add(Repository.parse(parser.getRepositoryURL()).configure(settings));
        }
        remoteRepositories.removeIf(repository -> snapshot ? !repository.snapshots : !repository.releases);
        List<IOException> failures = new ArrayList<>();
        File file = snapshot
                ? resolveSnapshot(parser, artifactPath, remoteRepositories, localFile, failures)
                : resolveFromRepositories(artifactPath, remoteRepositories, localFile, false, failures);
        if (file != null) {
            return file;
        }

        String message = "Could not resolve artifact: mvn:" + parser.toMvnURI();
        if (!failures.isEmpty()) {
            message += failures.stream().map(Throwable::getMessage).collect(Collectors.joining("; ", " (", ")"));
        }
        IOException exception = new IOException(message);
        failures.forEach(exception::addSuppressed);
        throw exception;
    }

    private List<Repository> parseRepositories(List<String> specs, boolean fileBasedOnly) {
        List<Repository> result = new ArrayList<>();
        for (String spec : specs) {
            // an invalid repository must not prevent the resolution from the other ones
            try {
                Repository repository = Repository.parse(spec);
                if (fileBasedOnly && repository.directory == null) {
                    LOG.warn("Ignoring the default repository {}: the default repositories are file based",
                            Repository.maskCredentials(spec));
                } else {
                    result.add(repository.configure(settings));
                }
            } catch (MalformedURLException e) {
                LOG.warn("Ignoring the repository {}: {}", Repository.maskCredentials(spec), e.getMessage());
            }
        }
        return result;
    }

    /**
     * As Pax URL, the global policy when it is set, else the one of the repository, else daily.
     */
    private UpdatePolicy updatePolicy(Repository repository) {
        if (updatePolicy != null) {
            return updatePolicy;
        }
        return repository.snapshotsUpdatePolicy != null ? repository.snapshotsUpdatePolicy : UpdatePolicy.DAILY;
    }

    /**
     * As Pax URL, the global policy when it is set, else the one of the repository, else warn.
     */
    private ChecksumPolicy checksumPolicy(Repository repository, boolean snapshot) {
        if (checksumPolicy != null) {
            return checksumPolicy;
        }
        ChecksumPolicy policy = snapshot ? repository.snapshotsChecksumPolicy : repository.releasesChecksumPolicy;
        return policy != null ? policy : ChecksumPolicy.WARN;
    }

    /**
     * The coordinates come from the URL: they must not lead outside of the repositories (<code>mvn:a/../..</code> for
     * instance).
     */
    private static void checkArtifactPath(String artifactPath, Parser parser) throws IOException {
        Path base = Paths.get("repository");
        boolean valid;
        try {
            // a backslash is a separator on Windows, and may be one for the remote repositories
            valid = artifactPath.indexOf('\\') < 0 && base.resolve(artifactPath).normalize().startsWith(base);
        } catch (InvalidPathException e) {
            valid = false;
        }
        if (!valid) {
            throw new IOException("Invalid Maven artifact path " + artifactPath + " for mvn:" + parser.toMvnURI());
        }
    }

    private static File resolveFromDirectory(Repository repository, String artifactPath) {
        if (repository.multi) {
            // each sub directory is a repository (the KAR repositories for instance)
            File[] directories = repository.directory.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    File file = new File(directory, artifactPath);
                    if (file.isFile()) {
                        return file;
                    }
                }
            }
            return null;
        }

        File file = new File(repository.directory, artifactPath);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    /**
     * @param path the path of the artifact in the repositories
     * @param localFile where to download the artifact
     * @param snapshot whether the artifact is a snapshot, for the checksum policy
     * @param failures where to add the errors of the repositories
     * @return the artifact, <code>null</code> if no repository has it
     */
    private File resolveFromRepositories(String path, List<Repository> repositories, File localFile,
                                         boolean snapshot, List<IOException> failures) {
        for (Repository repository : repositories) {
            try {
                File file = repository.directory != null
                        ? resolveFromDirectory(repository, path)
                        : download(repository, path, localFile, checksumPolicy(repository, snapshot));
                if (file != null) {
                    LOG.debug("Resolved {} from remote repository {}", path, repository);
                    return file;
                }
            } catch (IOException e) {
                failures.add(new IOException(repository + ": " + e.getMessage(), e));
            }
        }
        return null;
    }

    /**
     * Resolves the newest version of a snapshot, as Maven: from the <code>maven-metadata.xml</code> file of the
     * remote repositories, checked as the update policy tells when the local repository already has the snapshot.
     */
    private File resolveSnapshot(Parser parser, String artifactPath, List<Repository> repositories, File localFile,
                                 List<IOException> failures) throws IOException {
        String metadataPath = parser.getVersionMetadataPath(parser.getVersion());
        long now = System.currentTimeMillis();
        Repository newestRepository = null;
        SnapshotMetadata newest = null;
        for (Repository repository : repositories) {
            try {
                SnapshotMetadata metadata = readSnapshotMetadata(repository, metadataPath, parser, localFile, now);
                if (metadata != null && (newest == null || metadata.updated > newest.updated)) {
                    newest = metadata;
                    newestRepository = repository;
                }
            } catch (IOException e) {
                failures.add(new IOException(repository + ": " + e.getMessage(), e));
            }
        }

        File file = null;
        if (newest == null) {
            // no metadata, a non unique snapshot: its version can't be compared with the one of the local copy, so it
            // is only downloaded when there is no local copy
            if (!localFile.isFile()) {
                file = resolveFromRepositories(artifactPath, repositories, localFile, true, failures);
            }
        } else if (localFile.isFile() && localUpdated(localFile, parser) >= newest.updated) {
            LOG.debug("Resolved {} from local repository, not older than {} in {}", artifactPath, newest.version,
                    newestRepository);
            return localFile;
        } else {
            String path = snapshotPath(artifactPath, parser, newest.version);
            checkArtifactPath(path, parser);
            file = resolveFromRepositories(path, List.of(newestRepository), localFile, true, failures);
            if (localFile.equals(file)) {
                String name = localFile.getName();
                updateStatus(localFile.getParentFile(), Map.of(name + UPDATED, Long.toString(newest.updated),
                        name + LAST_MODIFIED, Long.toString(localFile.lastModified())));
            }
        }
        if (file == null && localFile.isFile()) {
            if (!failures.isEmpty()) {
                LOG.warn("Could not check the remote repositories for a newer version of {}, using the local one: {}",
                        artifactPath, failures.stream().map(Throwable::getMessage).collect(Collectors.joining("; ")));
            }
            return localFile;
        }
        return file;
    }

    /**
     * @return the snapshot in the repository, <code>null</code> if the repository doesn't have it
     */
    private SnapshotMetadata readSnapshotMetadata(Repository repository, String metadataPath, Parser parser,
                                                  File localFile, long now) throws IOException {
        byte[] metadata;
        if (repository.directory != null) {
            File file = new File(repository.directory, metadataPath);
            if (!file.isFile()) {
                // a repository the artifacts are installed in
                file = new File(file.getParentFile(), LOCAL_METADATA);
            }
            metadata = file.isFile() ? Files.readAllBytes(file.toPath()) : null;
        } else {
            // as Maven, the metadata of each remote repository is kept in the local repository
            File localDirectory = localFile.getParentFile();
            File cache = new File(localDirectory, "maven-metadata-" + toFileName(repository.id) + ".xml");
            if (localFile.isFile()
                    && !updatePolicy(repository).isUpdateRequired(lastCheck(localDirectory, repository), now)) {
                metadata = cache.isFile() ? Files.readAllBytes(cache.toPath()) : null;
            } else {
                String url = repository.url + (repository.url.endsWith("/") ? "" : "/") + metadataPath;
                try {
                    metadata = withRetries(url, () -> fetch(repository, url));
                } finally {
                    // as Maven, a failed check is not attempted again before the next update either
                    setLastCheck(localDirectory, repository, now);
                }
                if (metadata != null) {
                    write(cache, metadata);
                } else {
                    Files.deleteIfExists(cache.toPath());
                }
            }
        }
        if (metadata == null) {
            return null;
        }
        return SnapshotMetadata.parse(metadata, parser.getVersion(), parser.getType(), parser.getClassifier());
    }

    /**
     * @return the path of a version of a snapshot, in the directory of the snapshot
     */
    private static String snapshotPath(String artifactPath, Parser parser, String version) {
        String classifier = parser.getClassifier();
        return artifactPath.substring(0, artifactPath.lastIndexOf('/') + 1)
                + parser.getArtifact() + "-" + version
                + (classifier != null && !classifier.isEmpty() ? "-" + classifier : "")
                + "." + parser.getType();
    }

    /**
     * The id of a repository may come from the URL (<code>mvn:http://host/repository@id=...!g/a/v</code>): in a file
     * name, it must not be a path.
     */
    private static String toFileName(String id) {
        String name = id.replaceAll("[^A-Za-z0-9_.-]", "_");
        // maven-metadata-local.xml is the metadata of the artifacts installed in the local repository
        return "local".equals(name) ? "local_" : name;
    }

    /**
     * As Maven, the version of the local copy of a snapshot is the one of its <code>maven-metadata-local.xml</code>
     * when it was installed (<code>mvn install</code>), and the one it was downloaded with by this resolver, as long
     * as it was not replaced since. Its modification time is not used: a snapshot is versioned when its deployment
     * starts, and the local repository may have been copied.
     *
     * @return when the local copy was deployed or installed, 0 if unknown
     */
    private static long localUpdated(File localFile, Parser parser) {
        File localDirectory = localFile.getParentFile();
        Properties status = readStatus(localDirectory);
        String name = localFile.getName();
        long updated = 0;
        if (Long.toString(localFile.lastModified()).equals(status.getProperty(name + LAST_MODIFIED))) {
            updated = getLong(status, name + UPDATED);
        }
        File localMetadata = new File(localDirectory, LOCAL_METADATA);
        if (localMetadata.isFile()) {
            try {
                SnapshotMetadata metadata = SnapshotMetadata.parse(Files.readAllBytes(localMetadata.toPath()),
                        parser.getVersion(), parser.getType(), parser.getClassifier());
                if (metadata != null) {
                    updated = Math.max(updated, metadata.updated);
                }
            } catch (IOException e) {
                LOG.debug("Could not read {}: {}", localMetadata, e.getMessage());
            }
        }
        return updated;
    }

    private static long lastCheck(File localDirectory, Repository repository) {
        return getLong(readStatus(localDirectory), repository.id + LAST_CHECK);
    }

    private static void setLastCheck(File localDirectory, Repository repository, long time) {
        updateStatus(localDirectory, Map.of(repository.id + LAST_CHECK, Long.toString(time)));
    }

    private static long getLong(Properties properties, String key) {
        try {
            return Long.parseLong(properties.getProperty(key, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Properties readStatus(File localDirectory) {
        Properties properties = new Properties();
        File status = new File(localDirectory, STATUS_FILE);
        if (status.isFile()) {
            try (Reader reader = Files.newBufferedReader(status.toPath())) {
                properties.load(reader);
            } catch (IOException e) {
                LOG.debug("Could not read {}: {}", status, e.getMessage());
            }
        }
        return properties;
    }

    private static void updateStatus(File localDirectory, Map<String, String> values) {
        File status = new File(localDirectory, STATUS_FILE);
        try {
            Files.createDirectories(localDirectory.toPath());
            Properties properties = readStatus(localDirectory);
            properties.putAll(values);
            Path tmpFile = Files.createTempFile(localDirectory.toPath(), STATUS_FILE + ".", ".tmp");
            try {
                try (Writer writer = Files.newBufferedWriter(tmpFile)) {
                    properties.store(writer, null);
                }
                move(tmpFile, status.toPath());
            } finally {
                Files.deleteIfExists(tmpFile);
            }
        } catch (IOException e) {
            LOG.debug("Could not write {}: {}", status, e.getMessage());
        }
    }

    private static void write(File file, byte[] content) {
        try {
            Path tmpFile = Files.createTempFile(file.getParentFile().toPath(), file.getName() + ".", ".tmp");
            try {
                Files.write(tmpFile, content);
                move(tmpFile, file.toPath());
            } finally {
                Files.deleteIfExists(tmpFile);
            }
        } catch (IOException e) {
            LOG.debug("Could not write {}: {}", file, e.getMessage());
        }
    }

    /**
     * Downloads a file of a remote repository, with new attempts for the errors which may not happen again.
     *
     * @return <code>localFile</code>, <code>null</code> if the repository doesn't have the file
     */
    private File download(Repository repository, String path, File localFile, ChecksumPolicy checksumPolicy)
            throws IOException {
        String url = repository.url + (repository.url.endsWith("/") ? "" : "/") + path;
        return withRetries(url, () -> downloadArtifact(repository, url, localFile, checksumPolicy));
    }

    private interface Download<T> {

        T run() throws IOException;

    }

    private <T> T withRetries(String url, Download<T> download) throws IOException {
        int retryCount = configuration.getRetryCount();
        for (int attempt = 0; ; attempt++) {
            try {
                return download.run();
            } catch (IOException e) {
                if (attempt >= retryCount || !isRetryable(e)) {
                    throw e;
                }
                long delay = RETRY_DELAY << Math.min(attempt, 10);
                LOG.debug("Retry {}/{} for {} in {} ms: {}", attempt + 1, retryCount, url, delay, e.getMessage());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    InterruptedIOException exception = new InterruptedIOException("Interrupted download of " + url);
                    exception.addSuppressed(e);
                    throw exception;
                }
            }
        }
    }

    /**
     * As Apache HttpClient, the errors which happen again (unknown host, refused connection, TLS handshake, timeout)
     * are not retried, nor the HTTP errors except the server errors and the rate limit.
     */
    static boolean isRetryable(IOException exception) {
        if (exception instanceof HttpStatusException) {
            int status = ((HttpStatusException) exception).status;
            return status == 408 || status == 429 || status >= 500;
        }
        if (exception instanceof ChecksumException) {
            // a corrupted transfer
            return ((ChecksumException) exception).mismatch;
        }
        return !(exception instanceof UnknownHostException
                || exception instanceof ConnectException
                || exception instanceof NoRouteToHostException
                || exception instanceof SSLException
                || exception instanceof InterruptedIOException
                || exception instanceof MalformedURLException
                || exception instanceof FileSystemException);
    }

    private URLConnection openConnection(Repository repository, String url) throws IOException {
        URL target = new URL(url);
        URLConnection connection = repository.proxy != null ? target.openConnection(repository.proxy)
                : target.openConnection();
        if (connection instanceof HttpsURLConnection && !configuration.isCertificateCheck()) {
            disableSslVerification((HttpsURLConnection) connection);
        }
        if (connection instanceof HttpURLConnection && repository.authenticator != null) {
            ((HttpURLConnection) connection).setAuthenticator(repository.authenticator);
        }
        connection.setConnectTimeout(configuration.getConnectionTimeout());
        connection.setReadTimeout(configuration.getReadTimeout());
        return connection;
    }

    /**
     * @return <code>false</code> if the file is not found
     * @throws HttpStatusException for the other errors
     */
    private static boolean checkResponse(URLConnection connection, String url) throws IOException {
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // read the error response, so the connection can be reused
                try (InputStream error = httpConnection.getErrorStream()) {
                    if (error != null) {
                        error.transferTo(OutputStream.nullOutputStream());
                    }
                } catch (IOException e) {
                    httpConnection.disconnect();
                }
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return false;
                }
                throw new HttpStatusException(responseCode, url);
            }
        }
        return true;
    }

    /**
     * @return the content of the file, <code>null</code> if it is not found
     */
    private byte[] fetch(Repository repository, String url) throws IOException {
        URLConnection connection = openConnection(repository, url);
        if (!checkResponse(connection, url)) {
            return null;
        }
        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private File downloadArtifact(Repository repository, String url, File localFile, ChecksumPolicy checksumPolicy)
            throws IOException {
        LOG.debug("Downloading {}", url);

        URLConnection connection = openConnection(repository, url);
        if (!checkResponse(connection, url)) {
            return null;
        }

        // Download to a temp file of its own, so the concurrent downloads of the same artifact (by instances sharing
        // the local repository for instance) do not mix their content, then move it to the target location
        Path target = localFile.toPath();
        Files.createDirectories(target.getParent());
        Path tmpFile = Files.createTempFile(target.getParent(), localFile.getName() + ".", ".tmp");
        try {
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Verify checksum if available
            if (connection instanceof HttpURLConnection) {
                verifyChecksum(repository, url, tmpFile.toFile(), checksumPolicy);
            }

            move(tmpFile, target);
        } finally {
            Files.deleteIfExists(tmpFile);
        }

        return localFile;
    }

    private static void move(Path source, Path target) throws IOException {
        // the target is always either the previous or the new content, never a partial one (gh-2808)
        try {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // on Windows, the artifact just downloaded by a concurrent resolution may be in use
            if (!Files.isRegularFile(target)) {
                throw e;
            }
            LOG.debug("Could not replace {}, using the existing file: {}", target, e.getMessage());
        }
    }

    private void verifyChecksum(Repository repository, String url, File file, ChecksumPolicy checksumPolicy)
            throws IOException {
        if (checksumPolicy == ChecksumPolicy.IGNORE) {
            return;
        }

        // Try SHA-1 first, then MD5
        for (String[] checksum : CHECKSUMS) {
            String remoteChecksum = fetchChecksum(repository, url + "." + checksum[0]);
            if (remoteChecksum != null) {
                String localChecksum = computeChecksum(file, checksum[1]);
                if (!remoteChecksum.equalsIgnoreCase(localChecksum)) {
                    checksumFailure(checksumPolicy, new ChecksumException(checksum[1] + " checksum mismatch for "
                            + url, true));
                }
                return;
            }
        }
        // as Maven, a download that cannot be verified is a failure for the fail policy
        checksumFailure(checksumPolicy, new ChecksumException("No checksum available for " + url, false));
    }

    private static void checksumFailure(ChecksumPolicy checksumPolicy, ChecksumException exception) throws IOException {
        if (checksumPolicy == ChecksumPolicy.FAIL) {
            throw exception;
        }
        LOG.warn(exception.getMessage());
    }

    private String fetchChecksum(Repository repository, String url) {
        try {
            byte[] data = fetch(repository, url);
            if (data == null) {
                return null;
            }
            String checksum = new String(data).trim();
            // Handle checksum files that contain filename after the hash
            int space = checksum.indexOf(' ');
            if (space > 0) {
                checksum = checksum.substring(0, space);
            }
            return checksum;
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

    /**
     * An HTTP error, other than not found.
     */
    static final class HttpStatusException extends IOException {

        final int status;

        HttpStatusException(int status, String url) {
            super("HTTP " + status + " for " + url);
            this.status = status;
        }

    }

    static final class ChecksumException extends IOException {

        /**
         * <code>true</code> for a checksum mismatch, <code>false</code> when there is no checksum.
         */
        final boolean mismatch;

        ChecksumException(String message, boolean mismatch) {
            super(message);
            this.mismatch = mismatch;
        }

    }

}
