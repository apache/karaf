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
package org.apache.karaf.services.mavenproxy.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AccountException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.apache.karaf.util.StreamUtils;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenProxyServlet extends HttpServlet {

    public static Logger LOGGER = LoggerFactory.getLogger(MavenProxyServlet.class);

    public static final Pattern REPOSITORY_ID_REGEX = Pattern.compile("[^ ]*(@id=([^@ ]+))+[^ ]*");

    private static final String SNAPSHOT_TIMESTAMP_REGEX = "^([0-9]{8}.[0-9]{6}-[0-9]+).*";
    private static final Pattern SNAPSHOT_TIMESTAMP_PATTERN = Pattern.compile(SNAPSHOT_TIMESTAMP_REGEX);

    //The pattern below matches a path to the following:
    //1: groupId
    //2: artifactId
    //3: version
    //4: artifact filename
    public static final Pattern ARTIFACT_REQUEST_URL_REGEX = Pattern.compile("([^ ]+)/([^/ ]+)/([^/ ]+)/([^/ ]+)");

    //The pattern bellow matches the path to the following:
    //1: groupId
    //2: artifactId
    //3: version
    //4: maven-metadata xml filename
    //7: repository id.
    //9: type
    public static final Pattern ARTIFACT_METADATA_URL_REGEX = Pattern.compile("([^ ]+)/([^/ ]+)/([^/ ]+)/(maven-metadata([-]([^ .]+))?.xml)([.]([^ ]+))?");

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    protected static final String LOCATION_HEADER = "X-Location";

    private final ConcurrentMap<String, ArtifactDownloadFuture> requestMap = new ConcurrentHashMap<>();
    private final int threadMaximumPoolSize;
    private final String realm;
    private final String downloadRole;
    private final String uploadRole;
    private ThreadPoolExecutor executorService;

    protected File tmpFolder = new File(System.getProperty("karaf.data") + File.separator + "maven" + File.separator + "proxy" + File.separator + "tmp");

    final MavenResolver resolver;

    public MavenProxyServlet(MavenResolver resolver, int threadMaximumPoolSize, String realm, String downloadRole, String uploadRole) {
        this.resolver = resolver;
        this.threadMaximumPoolSize = threadMaximumPoolSize;
        this.realm = realm;
        this.downloadRole = downloadRole;
        this.uploadRole = uploadRole;
    }


    //
    //  Lifecycle
    //

    @Override
    public void init() throws ServletException {
        if (!tmpFolder.exists() && !tmpFolder.mkdirs()) {
            throw new ServletException("Failed to create temporary artifact folder");
        }
        // Create a thread pool with the given maxmimum number of threads
        // All threads will time out after 60 seconds
        int nbThreads = threadMaximumPoolSize > 0 ? threadMaximumPoolSize : 8;
        executorService = new ThreadPoolExecutor(0, nbThreads, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory("MavenDownloadProxyServlet"));
    }

    @Override
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }



    //
    // Security
    //

    protected boolean authorize(HttpServletRequest request, HttpServletResponse response, String role) throws IOException {
        if (role == null) {
            return true;
        }
        // Return immediately if the header is missing
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.length() > 0) {

            // Get the authType (Basic, Digest) and authInfo (user/password)
            // from the header
            authHeader = authHeader.trim();
            int blank = authHeader.indexOf(' ');
            if (blank > 0) {
                String authType = authHeader.substring(0, blank);
                String authInfo = authHeader.substring(blank).trim();

                // Check whether authorization type matches
                if (authType.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
                    try {
                        String srcString = base64Decode(authInfo);
                        int i = srcString.indexOf(':');
                        String username = srcString.substring(0, i);
                        String password = srcString.substring(i + 1);

                        // authenticate
                        Subject subject = doAuthenticate(username, password, role);
                        if (subject != null) {
                            // as per the spec, set attributes
                            request.setAttribute(HttpContext.AUTHENTICATION_TYPE, HttpServletRequest.BASIC_AUTH);
                            request.setAttribute(HttpContext.REMOTE_USER, username);
                            // succeed
                            return true;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        // request authentication
        try {
            response.setHeader(HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"");
            // must response with status and flush as Jetty may report org.eclipse.jetty.server.Response Committed before 401 null
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            // failed sending the response ... cannot do anything about it
        }

        // inform HttpService that authentication failed
        return false;
    }

    private static String base64Decode(String srcString) {
        byte[] transformed = DatatypeConverter.parseBase64Binary(srcString);
        return new String(transformed, StandardCharsets.ISO_8859_1);
    }

    public Subject doAuthenticate(final String username, final String password, final String role) {
        try {
            Subject subject = new Subject();
            LoginContext loginContext = new LoginContext(realm, subject, new CallbackHandler() {
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback callback : callbacks) {
                        if (callback instanceof NameCallback) {
                            ((NameCallback) callback).setName(username);
                        } else if (callback instanceof PasswordCallback) {
                            ((PasswordCallback) callback).setPassword(password.toCharArray());
                        } else {
                            throw new UnsupportedCallbackException(callback);
                        }
                    }
                }
            });
            loginContext.login();
            if (role != null && role.length() > 0) {
                String clazz = "org.apache.karaf.jaas.boot.principal.RolePrincipal";
                String name = role;
                int idx = role.indexOf(':');
                if (idx > 0) {
                    clazz = role.substring(0, idx);
                    name = role.substring(idx + 1);
                }
                boolean found = false;
                for (Principal p : subject.getPrincipals()) {
                    if (p.getClass().getName().equals(clazz)
                            && p.getName().equals(name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new FailedLoginException("User does not have the required role " + role);
                }
            }
            return subject;
        } catch (AccountException e) {
            LOGGER.warn("Account failure", e);
            return null;
        } catch (LoginException e) {
            LOGGER.debug("Login failed", e);
            return null;
        }
    }





    //
    // Download
    //

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (!authorize(req, resp, downloadRole)) {
            return;
        }
        String tpath = req.getPathInfo();
        if (tpath == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (tpath.startsWith("/")) {
            tpath = tpath.substring(1);
        }
        final String path = tpath;

        final AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(TimeUnit.MINUTES.toMillis(5));
        final ArtifactDownloadFuture future = new ArtifactDownloadFuture(path);
        ArtifactDownloadFuture masterFuture = requestMap.putIfAbsent(path, future);
        if (masterFuture == null) {
            masterFuture = future;
            masterFuture.lock();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        File file = download(path);
                        future.setValue(file);
                    } catch (Throwable t) {
                        future.setValue(t);
                    }
                }
            });
        } else {
            masterFuture.lock();
        }
        masterFuture.addListener(new FutureListener<ArtifactDownloadFuture>() {
            @Override
            public void operationComplete(ArtifactDownloadFuture future) {
                Object value = future.getValue();
                if (value instanceof Throwable) {
                    LOGGER.warn("Error while downloading artifact: {}", ((Throwable) value).getMessage(), value);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } else if (value instanceof File) {
                    File artifactFile = (File) value;
                    try (InputStream is = new FileInputStream(artifactFile)) {
                        LOGGER.info("Writing response for file : {}", path);
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("application/octet-stream");
                        resp.setDateHeader("Date", System.currentTimeMillis());
                        resp.setHeader("Connection", "close");
                        resp.setContentLength(is.available());
                        Bundle bundle = FrameworkUtil.getBundle(getClass());
                        if (bundle != null) {
                            resp.setHeader("Server", bundle.getSymbolicName() + "/" + bundle.getVersion());
                        } else {
                            resp.setHeader("Server", "Karaf Maven Proxy");
                        }
                        StreamUtils.copy(is, resp.getOutputStream());
                    } catch (Exception e) {
                        LOGGER.warn("Error while sending artifact: {}", e.getMessage(), e);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                future.release();
                try {
                    asyncContext.complete();
                } catch (IllegalStateException e) {
                    // Ignore, the response must have already been sent with an error
                }
            }
        });
    }

    public File download(String path) throws InvalidMavenArtifactRequest {
        if (path == null) {
            throw new InvalidMavenArtifactRequest();
        }

        Matcher artifactMatcher = ARTIFACT_REQUEST_URL_REGEX.matcher(path);
        Matcher metadataMatcher = ARTIFACT_METADATA_URL_REGEX.matcher(path);

        if (metadataMatcher.matches()) {
            LOGGER.info("Received request for maven metadata : {}", path);
            try {
                MavenCoord coord = convertMetadataPathToCoord(path);
                return resolver.resolveMetadata(coord.groupId, coord.artifactId, coord.type, coord.version);
            } catch (Exception e) {
                LOGGER.warn(String.format("Could not find metadata : %s due to %s", path, e.getMessage()), e);
                return null;
            }
        } else if (artifactMatcher.matches()) {
            LOGGER.info("Received request for maven artifact : {}", path);
            try {
                MavenCoord artifact = convertArtifactPathToCoord(path);
                Path download = resolver.resolve(artifact.groupId, artifact.artifactId, artifact.classifier, artifact.type, artifact.version).toPath();
                Path tmpFile = Files.createTempFile("mvn-", ".tmp");
                Files.copy(download, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                return tmpFile.toFile();
            } catch (Exception e) {
                LOGGER.warn(String.format("Could not find artifact : %s due to %s", path, e.getMessage()), e);
                return null;
            }
        }
        return null;
    }

    private class ArtifactDownloadFuture extends DefaultFuture<ArtifactDownloadFuture> {

        private final AtomicInteger participants = new AtomicInteger();
        private final String path;

        private ArtifactDownloadFuture(String path) {
            this.path = path;
        }

        public void lock() {
            participants.incrementAndGet();
        }

        public void release() {
            if (participants.decrementAndGet() == 0) {
                requestMap.remove(path);
                Object v = getValue();
                if (v instanceof File) {
                    ((File) v).delete();
                }
            }
        }

    }



    //
    // Upload
    //

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!authorize(request, response, uploadRole)) {
            return;
        }
        try {
            String path = request.getPathInfo();
            //Make sure path is valid
            if (path != null) {
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
            }
            if (path == null || path.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            boolean result;
            // handle move
            String location = request.getHeader(LOCATION_HEADER);
            if (location != null) {
                result = upload(new File(location), path, response);
            } else {
                Path dir = tmpFolder.toPath().resolve(UUID.randomUUID().toString());
                Path temp = dir.resolve(Paths.get(path).getFileName());
                Files.createDirectories(dir);
                try (OutputStream os = Files.newOutputStream(temp)) {
                    StreamUtils.copy(request.getInputStream(), os);
                }
                result = upload(temp.toFile(), path, response);
            }

            response.setStatus(result ? HttpServletResponse.SC_ACCEPTED : HttpServletResponse.SC_NOT_ACCEPTABLE);

        } catch (InvalidMavenArtifactRequest ex) {
            // must response with status and flush as Jetty may report org.eclipse.jetty.server.Response Committed before 401 null
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (Exception ex) {
            // must response with status and flush as Jetty may report org.eclipse.jetty.server.Response Committed before 401 null
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
            response.flushBuffer();
        }

    }

    protected boolean upload(File input, String path, HttpServletResponse response) throws InvalidMavenArtifactRequest, NoSuchFileException {
        if (!input.isFile()) {
            throw new NoSuchFileException(input.toString());
        }
        if (path == null) {
            throw new InvalidMavenArtifactRequest();
        }
        // root path, try reading mvn coords
        if (path.indexOf('/') < 0) {
            try {
                String mvnCoordsPath = readMvnCoordsPath(input);
                if (mvnCoordsPath != null) {
                    return install(input, mvnCoordsPath);
                } else {
                    response.addHeader(LOCATION_HEADER, input.toString()); // we need manual mvn coords input
                    return true;
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to deploy artifact : %s due to %s", path, e.getMessage()), e);
                return false;
            }
        }

        return install(input, path);

    }

    private boolean install(File file, String path) {
        Matcher artifactMatcher = ARTIFACT_REQUEST_URL_REGEX.matcher(path);
        Matcher metadataMatcher = ARTIFACT_METADATA_URL_REGEX.matcher(path);

        if (metadataMatcher.matches()) {
            LOGGER.info("Received upload request for maven metadata : {}", path);
            try {
                MavenCoord coord = convertMetadataPathToCoord(path);
                resolver.uploadMetadata(coord.groupId, coord.artifactId, coord.type, coord.version, file);
                LOGGER.info("Maven metadata installed: {}", coord.toString());
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to upload metadata: %s due to %s", path, e.getMessage()), e);
                return false;
            }
            //If no matching metadata found return nothing
        } else if (artifactMatcher.matches()) {
            LOGGER.info("Received upload request for maven artifact : {}", path);
            try {
                MavenCoord coord = convertArtifactPathToCoord(path);
                resolver.upload(coord.groupId, coord.artifactId, coord.classifier, coord.type, coord.version, file);
                LOGGER.info("Artifact installed: {}", coord.toString());
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to upload artifact : %s due to %s", path, e.getMessage()), e);
                return false;
            }
        }
        return false;
    }

    protected static String readMvnCoordsPath(File file) throws Exception {
        try (JarFile jarFile = new JarFile(file)) {
            String previous = null;
            String match = null;

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("META-INF/maven/") && name.endsWith("pom.properties")) {
                    if (previous != null) {
                        throw new IllegalStateException(String.format("Duplicate pom.properties found: %s != %s", name, previous));
                    }

                    previous = name; // check for dups

                    Properties props = new Properties();
                    try (InputStream stream = jarFile.getInputStream(entry)) {
                        props.load(stream);
                    }
                    String groupId = props.getProperty("groupId");
                    String artifactId = props.getProperty("artifactId");
                    String version = props.getProperty("version");
                    String type = getFileExtension(file);
                    match = String.format("%s/%s/%s/%s-%s.%s", groupId, artifactId, version, artifactId, version, type != null ? type : "jar");
                }
            }

            return match;
        }
    }

    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int idx = fileName.lastIndexOf('.');
        if (idx > 1) {
            String answer = fileName.substring(idx + 1);
            if (answer.length() > 0) {
                return answer;
            }
        }
        return null;
    }

    /**
     * Converts the path of the request to maven coords.
     *
     * @param path The request path, following the format: {@code <groupId>/<artifactId>/<version>/<artifactId>-<version>-[<classifier>].extension}
     * @return A {@link MavenCoord}
     * @throws InvalidMavenArtifactRequest
     */
    protected MavenCoord convertArtifactPathToCoord(String path) throws InvalidMavenArtifactRequest {
        if (path == null) {
            throw new InvalidMavenArtifactRequest("Cannot match request path to maven url, request path is empty.");
        }
        Matcher pathMatcher = ARTIFACT_REQUEST_URL_REGEX.matcher(path);
        if (pathMatcher.matches()) {
            String groupId = pathMatcher.group(1).replaceAll("/", ".");
            String artifactId = pathMatcher.group(2);
            String version = pathMatcher.group(3);
            String filename = pathMatcher.group(4);
            String extension;
            String classifier = "";
            String filePerfix = artifactId + "-" + version;
            String stripedFileName;

            if (version.endsWith("SNAPSHOT")) {
                String baseVersion = version.replaceAll("-SNAPSHOT", "");
                String timestampedFileName = filename.substring(artifactId.length() + baseVersion.length() + 2);
                //Check if snapshot is timestamped and override the version. @{link Artifact} will still treat it as a SNAPSHOT.
                //and also in case of artifact installation the proper filename will be used.
                Matcher ts = SNAPSHOT_TIMESTAMP_PATTERN.matcher(timestampedFileName);
                if (ts.matches()) {
                    version = baseVersion + "-" + ts.group(1);
                    filePerfix = artifactId + "-" + version;
                }
                stripedFileName = filename.replaceAll(SNAPSHOT_TIMESTAMP_REGEX, "SNAPSHOT");
                stripedFileName = stripedFileName.substring(filePerfix.length());
            } else {
                stripedFileName = filename.substring(filePerfix.length());
            }

            if (stripedFileName.startsWith("-") && stripedFileName.contains(".")) {
                classifier = stripedFileName.substring(1, stripedFileName.indexOf('.'));
            }
            extension = stripedFileName.substring(stripedFileName.indexOf('.') + 1);

            MavenCoord coord = new MavenCoord();
            coord.groupId = groupId;
            coord.artifactId = artifactId;
            coord.type = extension;
            coord.classifier = classifier;
            coord.version = version;
            return coord;
        }
        return null;
    }

    /**
     * Converts the path of the request to {@link MavenCoord}.
     *
     * @param path The request path, following the format: {@code <groupId>/<artifactId>/<version>/<artifactId>-<version>-[<classifier>].extension}
     * @return A {@link MavenCoord}
     * @throws InvalidMavenArtifactRequest
     */
    protected MavenCoord convertMetadataPathToCoord(String path) throws InvalidMavenArtifactRequest {
        if (path == null) {
            throw new InvalidMavenArtifactRequest("Cannot match request path to maven url, request path is empty.");
        }
        Matcher pathMatcher = ARTIFACT_METADATA_URL_REGEX.matcher(path);
        if (pathMatcher.matches()) {
            MavenCoord coord = new MavenCoord();
            coord.groupId = pathMatcher.group(1).replaceAll("/", ".");
            coord.artifactId = pathMatcher.group(2);
            coord.version = pathMatcher.group(3);
            String type = pathMatcher.group(8);
            coord.type = type == null ? "maven-metadata.xml" : "maven-metadata.xml." + type;
            return coord;
        }
        return null;
    }

    static class MavenCoord {
        String groupId;
        String artifactId;
        String type;
        String classifier;
        String version;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(groupId).append(":").append(artifactId).append(":");
            sb.append(type).append(":");
            if (classifier != null && !classifier.isEmpty()) {
                sb.append(classifier).append(":");
            }
            sb.append(version);
            return sb.toString();
        }
    }

    /**
     * Reads a {@link java.io.File} from the {@link java.io.InputStream} then saves it under a temp location and returns the file.
     *
     * @param is           The source input stream.
     * @param tempLocation The temporary location to save the content of the stream.
     * @param name         The name of the file.
     * @return
     * @throws java.io.FileNotFoundException
     */
    protected File copyFile(InputStream is, File tempLocation, String name) throws IOException {
        Path tmpFile = tempLocation.toPath().resolve(name);
        Files.deleteIfExists(tmpFile);
        try (OutputStream os = Files.newOutputStream(tmpFile)) {
            StreamUtils.copy(is, os);
        }
        return tmpFile.toFile();
    }

}
