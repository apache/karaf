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
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * The part of a Maven <code>settings.xml</code> file used to resolve the artifacts: the local repository, the
 * repositories of the active profiles, the credentials of the servers, the mirrors and the proxies.
 */
final class MavenSettings {

    private static final Logger LOG = LoggerFactory.getLogger(MavenSettings.class);

    static final MavenSettings EMPTY = new MavenSettings(null, Collections.emptyList(), Collections.emptyMap(),
            Collections.emptyList(), Collections.emptyList());

    static final class Server {

        final String username;
        final String password;

        Server(String username, String password) {
            this.username = username;
            this.password = password;
        }

    }

    static final class Mirror {

        final String id;
        final String url;
        final String mirrorOf;

        Mirror(String id, String url, String mirrorOf) {
            this.id = id;
            this.url = url;
            this.mirrorOf = mirrorOf;
        }

        /**
         * Matches the repositories as Maven does: <code>*</code>, <code>external:*</code>,
         * <code>external:http:*</code>, repository ids, separated by commas, and excluded with <code>!</code>.
         */
        boolean matches(String repositoryId, String repositoryUrl) {
            boolean result = false;
            for (String token : mirrorOf.split(",")) {
                String pattern = token.trim();
                boolean exclusion = pattern.startsWith("!");
                if (exclusion) {
                    pattern = pattern.substring(1);
                }
                boolean match = "*".equals(pattern)
                        || pattern.equals(repositoryId)
                        || ("external:*".equals(pattern) && isExternal(repositoryUrl))
                        || ("external:http:*".equals(pattern) && isExternal(repositoryUrl)
                                && repositoryUrl.startsWith("http:"));
                if (match) {
                    if (exclusion) {
                        return false;
                    }
                    result = true;
                }
            }
            return result;
        }

        private static boolean isExternal(String url) {
            try {
                String host = new URL(url).getHost();
                return !url.startsWith("file:") && !"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host);
            } catch (IOException e) {
                return false;
            }
        }

    }

    static final class Proxy {

        final String protocol;
        final String host;
        final int port;
        final String username;
        final String password;
        final String nonProxyHosts;

        Proxy(String protocol, String host, int port, String username, String password, String nonProxyHosts) {
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.nonProxyHosts = nonProxyHosts;
        }

        /**
         * As Maven, a proxy is used for the repositories with the same protocol, except the non proxy hosts
         * (<code>|</code> separated, with <code>*</code> wildcards).
         */
        boolean isUsedFor(URL url) {
            if (!protocol.equalsIgnoreCase(url.getProtocol())) {
                return false;
            }
            if (nonProxyHosts != null) {
                String host = url.getHost().toLowerCase(Locale.ROOT);
                for (String nonProxyHost : nonProxyHosts.split("\\|")) {
                    String pattern = nonProxyHost.trim().toLowerCase(Locale.ROOT);
                    if (!pattern.isEmpty() && host.matches(pattern.replace(".", "\\.").replace("*", ".*"))) {
                        return false;
                    }
                }
            }
            return true;
        }

    }

    /**
     * The local repository, <code>null</code> if not defined.
     */
    final String localRepository;

    /**
     * The repositories of the active profiles, with the Pax URL syntax (<code>url@id=...@snapshots</code>).
     */
    final List<String> repositories;

    private final Map<String, Server> servers;
    private final List<Mirror> mirrors;
    private final List<Proxy> proxies;

    private MavenSettings(String localRepository, List<String> repositories, Map<String, Server> servers,
                          List<Mirror> mirrors, List<Proxy> proxies) {
        this.localRepository = localRepository;
        this.repositories = repositories;
        this.servers = servers;
        this.mirrors = mirrors;
        this.proxies = proxies;
    }

    /**
     * The settings file, as Pax URL: the given path or <code>file:</code> URL, else
     * <code>${user.home}/.m2/settings.xml</code>, <code>${maven.home}/conf/settings.xml</code> and
     * <code>${M2_HOME}/conf/settings.xml</code>.
     *
     * @return the first existing file, <code>null</code> if there is none
     */
    static File locate(String settings) {
        if (settings != null && !settings.trim().isEmpty()) {
            try {
                File file = settings.startsWith("file:") ? new File(URI.create(settings)) : new File(settings);
                if (file.isFile()) {
                    return file;
                }
                LOG.warn("The Maven settings file {} doesn't exist, looking for the default ones", settings);
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid Maven settings location {}: {}", settings, e.getMessage());
            }
        }
        List<File> candidates = new ArrayList<>();
        candidates.add(new File(System.getProperty("user.home"), ".m2/settings.xml"));
        if (System.getProperty("maven.home") != null) {
            candidates.add(new File(System.getProperty("maven.home"), "conf/settings.xml"));
        }
        if (System.getenv("M2_HOME") != null) {
            candidates.add(new File(System.getenv("M2_HOME"), "conf/settings.xml"));
        }
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    static MavenSettings load(File file) throws IOException {
        Element root;
        try (InputStream in = Files.newInputStream(file.toPath())) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            root = factory.newDocumentBuilder().parse(in).getDocumentElement();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Invalid Maven settings file " + file + ": " + e.getMessage(), e);
        }

        Map<String, Server> servers = new HashMap<>();
        for (Element server : children(child(root, "servers"), "server")) {
            String id = text(server, "id");
            String password = text(server, "password");
            if (password != null && password.startsWith("{") && password.endsWith("}")) {
                LOG.warn("The encrypted password of the server {} in {} is not supported", id, file);
                continue;
            }
            if (id != null) {
                servers.put(id, new Server(text(server, "username"), password));
            }
        }

        List<Mirror> mirrors = new ArrayList<>();
        for (Element mirror : children(child(root, "mirrors"), "mirror")) {
            String url = text(mirror, "url");
            String mirrorOf = text(mirror, "mirrorOf");
            if (url != null && mirrorOf != null) {
                mirrors.add(new Mirror(text(mirror, "id"), url, mirrorOf));
            }
        }

        List<Proxy> proxies = new ArrayList<>();
        for (Element proxy : children(child(root, "proxies"), "proxy")) {
            String host = text(proxy, "host");
            if (host == null || "false".equalsIgnoreCase(text(proxy, "active"))) {
                continue;
            }
            String protocol = text(proxy, "protocol");
            String port = text(proxy, "port");
            String password = text(proxy, "password");
            if (password != null && password.startsWith("{") && password.endsWith("}")) {
                LOG.warn("The encrypted password of the proxy {} in {} is not supported", host, file);
                password = null;
            }
            try {
                proxies.add(new Proxy(protocol != null ? protocol : "http", host,
                        port != null ? Integer.parseInt(port) : 8080, text(proxy, "username"), password,
                        text(proxy, "nonProxyHosts")));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid port {} of the proxy {} in {}", port, host, file);
            }
        }

        return new MavenSettings(text(root, "localRepository"), activeRepositories(root), servers, mirrors, proxies);
    }

    /**
     * As Maven, the profiles listed in <code>activeProfiles</code> are active, and the profiles active by default
     * when no other one is.
     */
    private static List<String> activeRepositories(Element root) {
        Set<String> activeProfiles = new HashSet<>();
        for (Element activeProfile : children(child(root, "activeProfiles"), "activeProfile")) {
            activeProfiles.add(activeProfile.getTextContent().trim());
        }
        List<Element> active = new ArrayList<>();
        List<Element> activeByDefault = new ArrayList<>();
        for (Element profile : children(child(root, "profiles"), "profile")) {
            if (activeProfiles.contains(text(profile, "id"))) {
                active.add(profile);
            } else if ("true".equalsIgnoreCase(text(child(profile, "activation"), "activeByDefault"))) {
                activeByDefault.add(profile);
            }
        }
        List<String> repositories = new ArrayList<>();
        for (Element profile : active.isEmpty() ? activeByDefault : active) {
            for (Element repository : children(child(profile, "repositories"), "repository")) {
                String url = text(repository, "url");
                if (url == null) {
                    continue;
                }
                StringBuilder spec = new StringBuilder(url);
                String id = text(repository, "id");
                if (id != null) {
                    spec.append("@id=").append(id);
                }
                // as in a POM, the releases and the snapshots are enabled by default
                if (!"false".equalsIgnoreCase(text(child(repository, "snapshots"), "enabled"))) {
                    spec.append("@snapshots");
                }
                if ("false".equalsIgnoreCase(text(child(repository, "releases"), "enabled"))) {
                    spec.append("@noreleases");
                }
                repositories.add(spec.toString());
            }
        }
        return repositories;
    }

    private static Element child(Element parent, String name) {
        List<Element> children = children(parent, name);
        return children.isEmpty() ? null : children.get(0);
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> children = new ArrayList<>();
        if (parent != null) {
            for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element && name.equals(node.getLocalName())) {
                    children.add((Element) node);
                }
            }
        }
        return children;
    }

    /**
     * The text of a child element, with the <code>${env.NAME}</code> and the system properties substituted as Maven
     * does, <code>null</code> if there is no such element.
     */
    private static String text(Element parent, String name) {
        Element element = child(parent, name);
        if (element == null) {
            return null;
        }
        String text = element.getTextContent().trim();
        return InterpolationHelper.substVars(text, name, null, null, variable -> variable.startsWith("env.")
                ? System.getenv(variable.substring("env.".length())) : System.getProperty(variable));
    }

    Server getServer(String id) {
        return id != null ? servers.get(id) : null;
    }

    /**
     * @return the mirror of the repository, <code>null</code> if it has none
     */
    Mirror getMirror(String repositoryId, String repositoryUrl) {
        // as Maven, a mirror of the repository id first, then the patterns
        for (Mirror mirror : mirrors) {
            if (mirror.mirrorOf.equals(repositoryId)) {
                return mirror;
            }
        }
        for (Mirror mirror : mirrors) {
            if (mirror.matches(repositoryId, repositoryUrl)) {
                return mirror;
            }
        }
        return null;
    }

    /**
     * @return the proxy to access the URL, <code>null</code> if there is none
     */
    Proxy getProxy(URL url) {
        for (Proxy proxy : proxies) {
            if (proxy.isUsedFor(url)) {
                return proxy;
            }
        }
        return null;
    }

}
