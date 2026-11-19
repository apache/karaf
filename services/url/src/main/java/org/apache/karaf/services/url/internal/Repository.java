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
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A repository, configured as with Pax URL: <code>url[@option]*</code>, where the options are
 * <code>@id=...</code>, <code>@snapshots</code>, <code>@noreleases</code>, <code>@multi</code>, ...
 */
final class Repository {

    // the options supported by Pax URL: any other @ segment is part of the URL (/var/jenkins/workspace/job@2,
    // https://user:password@host, ...)
    private static final List<String> FLAGS = Arrays.asList("snapshots", "noreleases", "multi");
    private static final List<String> OPTIONS = Arrays.asList("id", "update", "releasesUpdate", "snapshotsUpdate",
            "checksum", "releasesChecksum", "snapshotsChecksum");

    private static final Pattern CREDENTIALS = Pattern.compile("://[^/]*@");

    /**
     * The identifier, from the <code>@id=</code> option, or generated from the URL.
     */
    final String id;

    /**
     * The URL, without the options and the credentials.
     */
    final String url;

    /**
     * The directory of a file based repository, <code>null</code> for a remote repository.
     */
    final File directory;

    final boolean multi;
    final boolean snapshots;
    final boolean releases;

    /**
     * The policies of the repository options (<code>@update=</code>, <code>@snapshotsUpdate=</code>,
     * <code>@checksum=</code>, ...), <code>null</code> when they are not set. As the releases of the local repository
     * are never checked again, <code>@releasesUpdate=</code> has no effect.
     */
    final UpdatePolicy snapshotsUpdatePolicy;
    final ChecksumPolicy releasesChecksumPolicy;
    final ChecksumPolicy snapshotsChecksumPolicy;

    private final String user;
    private final String password;

    /**
     * The proxy to access the repository, <code>null</code> for a direct access.
     */
    final Proxy proxy;

    /**
     * Provides the credentials to the host of the repository and to the proxy, <code>null</code> without
     * credentials.
     */
    final Authenticator authenticator;

    private Repository(String id, String url, File directory, boolean multi, boolean snapshots, boolean releases,
                       UpdatePolicy snapshotsUpdatePolicy, ChecksumPolicy releasesChecksumPolicy,
                       ChecksumPolicy snapshotsChecksumPolicy, String user, String password,
                       MavenSettings.Proxy proxy) throws MalformedURLException {
        this.id = id != null ? id : "repository-" + Integer.toHexString(url.hashCode());
        this.url = url;
        this.directory = directory;
        this.multi = multi;
        this.snapshots = snapshots;
        this.releases = releases;
        this.snapshotsUpdatePolicy = snapshotsUpdatePolicy;
        this.releasesChecksumPolicy = releasesChecksumPolicy;
        this.snapshotsChecksumPolicy = snapshotsChecksumPolicy;
        this.user = user;
        this.password = password;
        this.proxy = proxy != null
                ? new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxy.host, proxy.port)) : null;
        boolean serverCredentials = user != null;
        boolean proxyCredentials = proxy != null && proxy.username != null;
        this.authenticator = serverCredentials || proxyCredentials
                ? new RepositoryAuthenticator(directory == null ? new URL(url).getHost() : null, user, password,
                        proxy, proxyCredentials)
                : null;
    }

    static Repository parse(String spec) throws MalformedURLException {
        String[] segments = spec.split("@");
        StringBuilder url = new StringBuilder(segments[0].trim());
        String id = null;
        boolean multi = false;
        boolean snapshots = false;
        boolean releases = true;
        Map<String, String> policies = new HashMap<>();
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i].trim();
            if ("multi".equalsIgnoreCase(segment)) {
                multi = true;
            } else if ("snapshots".equalsIgnoreCase(segment)) {
                snapshots = true;
            } else if ("noreleases".equalsIgnoreCase(segment)) {
                releases = false;
            } else if (segment.startsWith("id=")) {
                id = segment.substring("id=".length()).trim();
            } else if (isOption(segment)) {
                int separator = segment.indexOf('=');
                policies.put(segment.substring(0, separator).trim(), segment.substring(separator + 1).trim());
            } else {
                url.append('@').append(segments[i]);
            }
        }
        String location = url.toString().trim();
        if (location.isEmpty()) {
            throw new MalformedURLException("Missing repository URL");
        }
        // as Pax URL, @update= and @checksum= are for both the releases and the snapshots
        String snapshotsUpdate = policies.getOrDefault("snapshotsUpdate", policies.get("update"));
        UpdatePolicy snapshotsUpdatePolicy = snapshotsUpdate != null ? UpdatePolicy.parse(snapshotsUpdate) : null;
        ChecksumPolicy releasesChecksumPolicy =
                ChecksumPolicy.parse(policies.getOrDefault("releasesChecksum", policies.get("checksum")));
        ChecksumPolicy snapshotsChecksumPolicy =
                ChecksumPolicy.parse(policies.getOrDefault("snapshotsChecksum", policies.get("checksum")));

        if (location.startsWith("file:")) {
            return new Repository(id, location, toFile(location), multi, snapshots, releases, snapshotsUpdatePolicy,
                    releasesChecksumPolicy, snapshotsChecksumPolicy, null, null, null);
        }
        int schemeEnd = location.indexOf("://");
        if (schemeEnd < 0) {
            // a path
            return new Repository(id, location, new File(location), multi, snapshots, releases, snapshotsUpdatePolicy,
                    releasesChecksumPolicy, snapshotsChecksumPolicy, null, null, null);
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
        // validate the URL
        new URL(location);
        String user = null;
        String password = null;
        if (userInfo != null) {
            int separator = userInfo.indexOf(':');
            user = decode(separator >= 0 ? userInfo.substring(0, separator) : userInfo);
            password = separator >= 0 ? decode(userInfo.substring(separator + 1)) : "";
        }
        return new Repository(id, location, null, multi, snapshots, releases, snapshotsUpdatePolicy,
                releasesChecksumPolicy, snapshotsChecksumPolicy, user, password, null);
    }

    /**
     * Applies the Maven settings to a remote repository: its mirror, the credentials of its server (when the URL has
     * none), and the proxy to access it.
     */
    Repository configure(MavenSettings settings) throws MalformedURLException {
        if (directory != null) {
            return this;
        }
        MavenSettings.Mirror mirror = settings.getMirror(id, url);
        String targetId = mirror != null ? mirror.id : id;
        String targetUrl = mirror != null ? mirror.url : url;
        // the credentials of the URL are for its host, never for a mirror
        MavenSettings.Server server = user == null || mirror != null ? settings.getServer(targetId) : null;
        MavenSettings.Proxy targetProxy = settings.getProxy(new URL(targetUrl));
        if (mirror == null && server == null && targetProxy == null) {
            return this;
        }
        String targetUser = server != null ? server.username : mirror != null ? null : user;
        String targetPassword = server != null ? server.password : mirror != null ? null : password;
        return new Repository(targetId, targetUrl, null, multi, snapshots, releases, snapshotsUpdatePolicy,
                releasesChecksumPolicy, snapshotsChecksumPolicy, targetUser,
                targetPassword != null ? targetPassword : "", targetProxy);
    }

    /**
     * Resolves a local repository specification, stripping any repository options (such as <code>@id=local</code>)
     * while keeping <code>@</code> characters that belong to the path, and resolving relative paths against
     * <code>karafHome</code> when provided.
     *
     * @return the directory, or <code>null</code> if the specification has no path
     */
    static File toLocalRepository(String spec, String karafHome) {
        if (spec == null) {
            return null;
        }
        String[] segments = spec.split("@");
        StringBuilder path = new StringBuilder(segments[0].trim());
        for (int i = 1; i < segments.length; i++) {
            if (!isOption(segments[i].trim())) {
                path.append('@').append(segments[i]);
            }
        }
        String location = path.toString().trim();
        if (location.isEmpty()) {
            return null;
        }
        File file;
        if (location.startsWith("file:")) {
            try {
                file = toFile(location);
            } catch (MalformedURLException e) {
                file = new File(location);
            }
        } else {
            file = new File(location);
        }
        if (!file.isAbsolute() && karafHome != null) {
            file = new File(karafHome, file.getPath());
        }
        return file;
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
     * As Pax URL, accepts the characters a hand written URI may contain (spaces, backslashes) and the relative URIs
     * (<code>file:system</code>).
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

    @Override
    public String toString() {
        return id + " (" + url + ")";
    }

    /**
     * Provides the credentials to the host of the repository only, not to a host it redirects to, and to the proxy.
     */
    private static final class RepositoryAuthenticator extends Authenticator {

        private final String host;
        private final String user;
        private final String password;
        private final MavenSettings.Proxy proxy;

        RepositoryAuthenticator(String host, String user, String password, MavenSettings.Proxy proxy,
                                boolean proxyCredentials) {
            this.host = host;
            this.user = user;
            this.password = password;
            this.proxy = proxyCredentials ? proxy : null;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (getRequestorType() == RequestorType.PROXY) {
                if (proxy != null && proxy.host.equalsIgnoreCase(getRequestingHost())) {
                    return new PasswordAuthentication(proxy.username,
                            proxy.password != null ? proxy.password.toCharArray() : new char[0]);
                }
            } else if (user != null && host != null && host.equalsIgnoreCase(getRequestingHost())) {
                return new PasswordAuthentication(user, password.toCharArray());
            }
            return null;
        }

    }

}
