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
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
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

    private final MavenConfiguration configuration;
    private final List<Repository> defaultRepositories;
    private final List<Repository> repositories;

    public MavenResolverImpl(MavenConfiguration configuration) {
        this.configuration = configuration;
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
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(groupId).append('/').append(artifactId);
        if (version != null && !version.isEmpty()) {
            sb.append('/').append(version);
        }
        if (extension != null && !extension.isEmpty()) {
            sb.append('/').append(extension);
        }
        if (classifier != null && !classifier.isEmpty()) {
            if (extension == null || extension.isEmpty()) {
                sb.append('/');
            }
            sb.append('/').append(classifier);
        }
        Parser parser = new Parser(sb.toString());
        return resolve(parser);
    }

    @Override
    public RetryChance isRetryableException(Exception exception) {
        // the downloads from the remote repositories are already retried (connection.retryCount)
        return RetryChance.NEVER;
    }

    private File resolve(Parser parser) throws IOException {
        String artifactPath = parser.getArtifactPath();
        checkArtifactPath(artifactPath, parser);

        // 1. Check default repositories (local file-based)
        for (Repository repository : defaultRepositories) {
            File file = resolveFromDirectory(repository, artifactPath);
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

        // 3. Try remote repositories, and then the repository of the URL (mvn:repository_url!groupId/artifactId...)
        List<Repository> remoteRepositories = repositories;
        if (parser.getRepositoryURL() != null) {
            remoteRepositories = new ArrayList<>(repositories);
            remoteRepositories.add(Repository.parse(parser.getRepositoryURL()));
        }
        for (Repository repository : remoteRepositories) {
            File file = repository.directory != null
                    ? resolveFromDirectory(repository, artifactPath)
                    : resolveFromRemoteRepository(repository, artifactPath, localFile);
            if (file != null) {
                LOG.debug("Resolved {} from remote repository {}", artifactPath, repository.url);
                return file;
            }
        }

        throw new IOException("Could not resolve artifact: mvn:" + parser.toMvnURI());
    }

    private static List<Repository> parseRepositories(List<String> specs, boolean fileBasedOnly) {
        List<Repository> result = new ArrayList<>();
        for (String spec : specs) {
            // an invalid repository must not prevent the resolution from the other ones
            try {
                Repository repository = Repository.parse(spec);
                if (fileBasedOnly && repository.directory == null) {
                    LOG.warn("Ignoring the default repository {}: the default repositories are file based",
                            Repository.maskCredentials(spec));
                } else {
                    result.add(repository);
                }
            } catch (MalformedURLException e) {
                LOG.warn("Ignoring the repository {}: {}", Repository.maskCredentials(spec), e.getMessage());
            }
        }
        return result;
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

    private File resolveFromRemoteRepository(Repository repository, String artifactPath, File localFile) {
        // Build the full URL
        String url = repository.url;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += artifactPath;

        int retryCount = configuration.getRetryCount();
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                return downloadArtifact(repository, url, localFile);
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

    private URLConnection openConnection(Repository repository, String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
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

    private File downloadArtifact(Repository repository, String url, File localFile) throws IOException {
        LOG.debug("Downloading {}", url);

        URLConnection connection = openConnection(repository, url);
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
                verifyChecksum(repository, url, tmpFile.toFile());
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

    private void verifyChecksum(Repository repository, String url, File file) throws IOException {
        String checksumPolicy = configuration.getChecksumPolicy();
        if ("ignore".equals(checksumPolicy)) {
            return;
        }

        // Try SHA-1 first, then MD5
        for (String[] checksum : CHECKSUMS) {
            String remoteChecksum = fetchChecksum(repository, url + "." + checksum[0]);
            if (remoteChecksum != null) {
                String localChecksum = computeChecksum(file, checksum[1]);
                if (!remoteChecksum.equalsIgnoreCase(localChecksum)) {
                    checksumFailure(checksumPolicy, checksum[1] + " checksum mismatch for " + url);
                }
                return;
            }
        }
        // as Maven, a download that cannot be verified is a failure for the fail policy
        checksumFailure(checksumPolicy, "No checksum available for " + url);
    }

    private static void checksumFailure(String checksumPolicy, String message) throws IOException {
        if ("fail".equals(checksumPolicy)) {
            throw new IOException(message);
        }
        LOG.warn(message);
    }

    private String fetchChecksum(Repository repository, String url) {
        try {
            URLConnection connection = openConnection(repository, url);
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

    /**
     * A repository, configured as with Pax URL: <code>url[@option]*</code>, where the options are
     * <code>@id=...</code>, <code>@snapshots</code>, <code>@noreleases</code>, <code>@multi</code>, ...
     */
    static final class Repository {

        // the options supported by Pax URL: any other @ segment is part of the URL (/var/jenkins/workspace/job@2,
        // https://user:password@host, ...)
        private static final List<String> FLAGS = Arrays.asList("snapshots", "noreleases", "multi");
        private static final List<String> OPTIONS = Arrays.asList("id", "update", "releasesUpdate", "snapshotsUpdate",
                "checksum", "releasesChecksum", "snapshotsChecksum");

        private static final Pattern CREDENTIALS = Pattern.compile("://[^/]*@");

        /**
         * The URL, without the options and the credentials.
         */
        final String url;

        /**
         * The directory of a file based repository, <code>null</code> for a remote repository.
         */
        final File directory;

        final boolean multi;

        /**
         * Provides the credentials of the URL to the host of the repository, <code>null</code> without credentials.
         */
        final Authenticator authenticator;

        private Repository(String url, File directory, boolean multi, Authenticator authenticator) {
            this.url = url;
            this.directory = directory;
            this.multi = multi;
            this.authenticator = authenticator;
        }

        static Repository parse(String spec) throws MalformedURLException {
            String[] segments = spec.split("@");
            StringBuilder url = new StringBuilder(segments[0].trim());
            boolean multi = false;
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i].trim();
                if ("multi".equalsIgnoreCase(segment)) {
                    multi = true;
                } else if (!isOption(segment)) {
                    url.append('@').append(segments[i]);
                }
            }
            String location = url.toString().trim();
            if (location.isEmpty()) {
                throw new MalformedURLException("Missing repository URL");
            }

            if (location.startsWith("file:")) {
                return new Repository(location, toFile(location), multi, null);
            }
            int schemeEnd = location.indexOf("://");
            if (schemeEnd < 0) {
                // a path
                return new Repository(location, new File(location), multi, null);
            }

            // the credentials of the URL (https://user:password@host/...) authenticate on the host of the repository
            String userInfo = null;
            int authorityStart = schemeEnd + "://".length();
            int authorityEnd = location.indexOf('/', authorityStart);
            if (authorityEnd < 0) {
                authorityEnd = location.length();
            }
            int userInfoEnd = location.lastIndexOf('@', authorityEnd - 1);
            if (userInfoEnd >= authorityStart) {
                userInfo = location.substring(authorityStart, userInfoEnd);
                location = location.substring(0, authorityStart) + location.substring(userInfoEnd + 1);
            }
            URL repositoryUrl = new URL(location);
            Authenticator authenticator = null;
            if (userInfo != null) {
                int separator = userInfo.indexOf(':');
                String user = decode(separator >= 0 ? userInfo.substring(0, separator) : userInfo);
                String password = separator >= 0 ? decode(userInfo.substring(separator + 1)) : "";
                authenticator = new RepositoryAuthenticator(repositoryUrl.getHost(), user, password);
            }
            return new Repository(location, null, multi, authenticator);
        }

        private static boolean isOption(String segment) {
            for (String flag : FLAGS) {
                if (flag.equalsIgnoreCase(segment)) {
                    return true;
                }
            }
            int separator = segment.indexOf('=');
            return separator > 0 && OPTIONS.contains(segment.substring(0, separator).trim());
        }

        /**
         * As Pax URL, accepts the characters a hand written URI may contain (spaces, backslashes) and the relative
         * URIs (<code>file:system</code>).
         */
        private static File toFile(String url) throws MalformedURLException {
            String spec = url.replace('\\', '/').replace(" ", "%20");
            try {
                URI uri = new URI(spec);
                if (uri.isOpaque()) {
                    return new File(uri.getSchemeSpecificPart());
                }
                try {
                    return new File(uri);
                } catch (IllegalArgumentException e) {
                    // an authority, a query or a fragment
                    return new File(uri.getPath());
                }
            } catch (URISyntaxException e) {
                MalformedURLException exception = new MalformedURLException("Invalid repository URL " + url + ": "
                        + e.getMessage());
                exception.initCause(e);
                throw exception;
            }
        }

        private static String decode(String value) {
            try {
                // the user info is percent encoded, a + is not a space
                return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return value;
            }
        }

        static String maskCredentials(String spec) {
            return CREDENTIALS.matcher(spec).replaceFirst("://***@");
        }

    }

    /**
     * Provides the credentials to the host of the repository only: not to a host it redirects to, nor to a proxy.
     */
    private static final class RepositoryAuthenticator extends Authenticator {

        private final String host;
        private final String user;
        private final String password;

        RepositoryAuthenticator(String host, String user, String password) {
            this.host = host;
            this.user = user;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (getRequestorType() == RequestorType.SERVER && host.equalsIgnoreCase(getRequestingHost())) {
                return new PasswordAuthentication(user, password.toCharArray());
            }
            return null;
        }

    }

}
