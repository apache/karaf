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
package org.apache.karaf.maven.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.table.Row;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * Base class for <code>maven:</code> commands.
 * <p>Important: even if it duplicates some code from pax-url-aether, this should be treated as verification code of
 * how pax-url-aether should interact with <code>org.ops4j.pax.url.mvn</code> PID configuration.
 */
public abstract class MavenConfigurationSupport implements Action {

    public static Logger LOG = LoggerFactory.getLogger(MavenConfigurationSupport.class);
    protected static final String PID = "org.ops4j.pax.url.mvn";

    protected static final String PATTERN_PID_PROPERTY = "Explicit %s PID configuration (%s)";

    protected static final String PATTERN_SECURITY_SETTINGS = "maven-security-settings-%d.xml";
    protected static final Pattern RE_SECURITY_SETTINGS = Pattern.compile("maven-security-settings-(\\d+)\\.xml");
    protected static final String PATTERN_SETTINGS = "maven-settings-%d.xml";
    protected static final Pattern RE_SETTINGS = Pattern.compile("maven-settings-(\\d+)\\.xml");
    private static final int MAX_SEQUENCE_SIZE = 10;

    protected static final String PROPERTY_LOCAL_REPOSITORY = "localRepository";
    protected static final String PROPERTY_DEFAULT_REPOSITORIES = "defaultRepositories";
    protected static final String PROPERTY_REPOSITORIES = "repositories";
    protected static final String PROPERTY_SETTINGS_FILE = "settings";
    protected static final String PROPERTY_SECURITY_FILE = "security";
    protected static final String PROPERTY_GLOBAL_UPDATE_POLICY = "globalUpdatePolicy";
    protected static final String PROPERTY_GLOBAL_CHECKSUM_POLICY = "globalChecksumPolicy";
    protected static final String PROPERTY_UPDATE_RELEASES = "updateReleases";
    protected static final String REQUIRE_CONFIG_ADMIN_CONFIG = "requireConfigAdminConfig";
    protected static final String PROPERTY_USE_FALLBACK_REPOSITORIES = "useFallbackRepositories";
    protected static final String PROPERTY_OFFLINE = "offline";
    protected static final String PROPERTY_CERTIFICATE_CHECK = "certificateCheck";

    // TODO timeout options {
    protected static final String PROPERTY_TIMEOUT = "timeout";
    protected static final String PROPERTY_SOCKET_SO_TIMEOUT = "socket.readTimeout";
    protected static final String PROPERTY_SOCKET_SO_KEEPALIVE = "socket.keepAlive";
    protected static final String PROPERTY_SOCKET_SO_LINGER = "socket.linger";
    protected static final String PROPERTY_SOCKET_SO_REUSEADDRESS = "socket.reuseAddress";
    protected static final String PROPERTY_SOCKET_TCP_NODELAY = "socket.tcpNoDelay";
    protected static final String PROPERTY_SOCKET_CONNECTION_TIMEOUT = "socket.connectionTimeout";
    protected static final String PROPERTY_CONNECTION_BUFFER_SIZE = "connection.bufferSize";
    protected static final String PROPERTY_CONNECTION_RETRY_COUNT = "connection.retryCount";
    // }

    protected SourceAnd<File> localRepository;
    protected SourceAnd<File> settings;
    protected Settings mavenSettings;
    protected SourceAnd<File> securitySettings;
    protected SettingsSecurity mavenSecuritySettings;

    protected Map<String, Server> servers = new HashMap<>();
    protected Map<String, String> serverPasswords = new HashMap<>();
    protected Map<String, String> proxyPasswords = new HashMap<>();

    protected List<String> warnings = new LinkedList<>();

    private static final String masterMasterPassword = DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION;
    protected String masterPassword;
    protected DefaultPlexusCipher cipher;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected BundleContext context;

    @Reference
    protected Session session;

    @Override
    final public Object execute() throws Exception {
        Configuration c = cm.getConfiguration(PID);

        if (c != null && c.getProperties() != null) {
            try {
                cipher = new DefaultPlexusCipher();
                securitySettings = securitySettings((String) c.getProperties().get(PID + "." + PROPERTY_SECURITY_FILE));
                if (securitySettings != null && securitySettings.value != null) {
                    mavenSecuritySettings = readSecuritySettings(securitySettings.value);
                }

                settings = settings((String) c.getProperties().get(PID + "." + PROPERTY_SETTINGS_FILE));
                if (settings != null && settings.value != null) {
                    mavenSettings = readSettings(settings.value);
                }

                localRepository = localRepository((String) c.getProperties().get(PID + "." + PROPERTY_LOCAL_REPOSITORY));

                if (showPasswords()) {
                    decryptSettings();
                }

                doAction(PID + ".", c.getProperties());
            } catch (Exception e) {
                System.err.println(e.getMessage());
                LOG.error(e.getMessage(), e);
            }
        } else {
            System.err.printf("Can't access \"%s\" configuration\n", PID);
        }

        return null;
    }

    /**
     * Performs command action on <strong>existing</strong> <code>org.ops4j.pax.url.mvn</code>
     * PID configuration
     * @param prefix prefix for properties inside <code>org.ops4j.pax.url.mvn</code> PID
     * @param config <code>org.ops4j.pax.url.mvn</code> PID configuration taken from {@link ConfigurationAdmin}
     */
    abstract protected void doAction(String prefix, Dictionary<String, Object> config) throws Exception;

    /**
     * Gets effective location of <code>settings.xml</code> file - according to pax-url-aether rules
     * @param cmProperty property obtained from Config Admin
     * @return
     */
    protected SourceAnd<File> settings(String cmProperty) {
        SourceAnd<File> result = new SourceAnd<>();
        URL locationUrl = null;
        String probableErrorMessage = null;

        // 1. PID + ".settings"
        if (cmProperty != null && !"".equals(cmProperty.trim())) {
            result.source = String.format(PATTERN_PID_PROPERTY, PID, PID + "." + PROPERTY_SETTINGS_FILE);
            try {
                locationUrl = new URL(cmProperty);
                probableErrorMessage = String.format("%s configured in %s.%s is not accessible",
                        locationUrl, PID, PROPERTY_SETTINGS_FILE);
            } catch (MalformedURLException e) {
                File file = new File(cmProperty);
                if (file.isFile()) {
                    result.value = file;
                    return result;
                }
            }
        }

        if (locationUrl == null) {
            // 2. System.getProperty("user.home") + "/.m2/settings.xml"
            File file = new File(System.getProperty("user.home") + "/.m2/settings.xml");
            if (file.isFile()) {
                result.value = file;
                result.source = "Implicit ${user.home}/.m2/settings.xml";
                return result;
            }

            // 3. System.getProperty("maven.home") + "/conf/settings.xml"
            file = new File(System.getProperty("maven.home") + "/conf/settings.xml");
            if (file.isFile()) {
                result.value = file;
                result.source = "Implicit ${maven.home}/conf/settings.xml";
                return result;
            }

            // 4. System.getenv("M2_HOME") + "/conf/settings.xml"
            file = new File(System.getenv("M2_HOME") + "/conf/settings.xml");
            if (file.isFile()) {
                result.value = file;
                result.source = "Implicit $M2_HOME/conf/settings.xml";
                return result;
            }
        } else {
            File file = new File(locationUrl.getPath());
            result.value = file;
            if (!file.isFile()) {
                result.source = probableErrorMessage;
            }
            return result;
        }

        // 5. new org.apache.maven.settings.Settings()
        result.value = null;
        result.source = "No implicit settings.xml location is available";
        return result;
    }

    /**
     * Gets effective location of <code>settings-security.xml</code> file - according to pax-url-aether rules
     * @param cmProperty property obtained from Config Admin
     * @return
     */
    protected SourceAnd<File> securitySettings(String cmProperty) {
        SourceAnd<File> result = new SourceAnd<>();
        URL locationUrl = null;
        String probableErrorMessage = null;

        // 1. PID + ".security"
        if (cmProperty != null && !"".equals(cmProperty.trim())) {
            result.source = String.format(PATTERN_PID_PROPERTY, PID, PID + "." + PROPERTY_SECURITY_FILE);
            try {
                locationUrl = new URL(cmProperty);
                probableErrorMessage = String.format("%s configured in %s.%s is not accessible",
                        locationUrl, PID, PROPERTY_SECURITY_FILE);
            } catch (MalformedURLException e) {
                File file = new File(cmProperty);
                if (file.isFile()) {
                    result.value = file;
                    return result;
                }
            }
        }

        // 2. System.getProperty("user.home") + "/.m2/settings-security.xml"
        if (locationUrl == null) {
            File file = new File(System.getProperty("user.home") + "/.m2/settings-security.xml");
            if (file.isFile()) {
                result.value = file;
                result.source = "Implicit ${user.home}/.m2/settings-security.xml";
                return result;
            }
        } else {
            File file = new File(locationUrl.getPath());
            result.value = file;
            if (!file.isFile()) {
                result.source = probableErrorMessage;
            }
        }

        result.value = null;
        result.source = "No implicit settings-security.xml location is available";
        return result;
    }

    /**
     * Gets effective location of <em>local repository</em> - according to pax-url-aether rules
     * @param cmProperty property obtained from Config Admin
     * @return
     */
    protected SourceAnd<File> localRepository(String cmProperty) {
        SourceAnd<File> result = new SourceAnd<>();
        URL locationUrl = null;
        String probableErrorMessage = null;

        // 1. PID + ".localRepository"
        if (cmProperty != null && !"".equals(cmProperty.trim())) {
            result.source = String.format(PATTERN_PID_PROPERTY, PID, PID + "." + PROPERTY_LOCAL_REPOSITORY);
            try {
                locationUrl = new URL(cmProperty);
                probableErrorMessage = String.format("%s configured in %s.%s is not accessible",
                        locationUrl, PID, PROPERTY_LOCAL_REPOSITORY);
            } catch (MalformedURLException e) {
                File file = new File(cmProperty);
                if (file.isDirectory()) {
                    result.value = file;
                    return result;
                }
            }
        }

        // 2. from settings.xml
        if (locationUrl == null && mavenSettings != null && mavenSettings.getLocalRepository() != null) {
            result.source = String.format("Explicit <localRepository> in %s", settings.value);
            try {
                locationUrl = new URL(mavenSettings.getLocalRepository());
                probableErrorMessage = String.format("%s configured in %s is not accessible",
                        mavenSettings.getLocalRepository(), settings.value);
            } catch (MalformedURLException e) {
                File file = new File(mavenSettings.getLocalRepository());
                if (file.isDirectory()) {
                    result.value = file;
                    return result;
                }
            }
        }

        // 3. System.getProperty("user.home") + "/.m2/repository";
        if (locationUrl == null) {
            File file = new File(System.getProperty("user.home") + "/.m2/repository");
            result.value = file; // whether it exists or not
            if (file.isDirectory()) {
                result.source = "Implicit ${user.home}/.m2/repository";
            } else {
                result.source = "Implicit ${user.home}/.m2/repository (not accessible)";
            }
            return result;
        }

        File file = new File(locationUrl.getPath());
        result.value = file;
        if (!file.isDirectory()) {
            result.source = probableErrorMessage;
        }
        return result;
    }

    /**
     * Reads on demand <code>settings.xml</code> file - without password decryption. Also
     * collects declared servers by ID.
     * @param settingsFile
     */
    protected synchronized Settings readSettings(File settingsFile) throws SettingsBuildingException {
        if (!settingsFile.isFile() || !settingsFile.canRead()) {
            return null;
        }

        try {
            DefaultSettingsBuilderFactory factory = new DefaultSettingsBuilderFactory();
            DefaultSettingsBuilder builder = factory.newInstance();
            SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            request.setUserSettingsFile(settingsFile);

            SettingsBuildingResult result = builder.build(request);
            if (result.getProblems().size() > 0) {
                for (SettingsProblem problem : result.getProblems()) {
                    System.err.println(problem);
                }
                return null;
            } else {
                Settings settings = result.getEffectiveSettings();
                if (settings.getServers() != null) {
                    for (Server server : settings.getServers()) {
                        servers.put(server.getId(), server);
                    }
                }
                return settings;
            }
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Re-reads on demand <code>settings-security.xml</code> file
     * @param securitySettingsFile
     */
    protected synchronized SettingsSecurity readSecuritySettings(File securitySettingsFile) throws Exception {
        if (!securitySettingsFile.isFile() || !securitySettingsFile.canRead()) {
            return null;
        }

        try {
            return SecUtil.read(securitySettingsFile.getAbsolutePath(), true);
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * <p>Decrypts passwords inside correctly read <code>settings.xml</code>. Also tries to decrypt master password.</p>
     * <p>Not called implicitly for each action invocation.</p>
     */
    private void decryptSettings() throws Exception {
        if (mavenSecuritySettings != null && mavenSettings != null) {
            masterPassword = cipher.decryptDecorated(mavenSecuritySettings.getMaster(), masterMasterPassword);
            DefaultSecDispatcher dispatcher = new DefaultSecDispatcher(cipher);
            DefaultSettingsDecrypter decrypter = new DefaultSettingsDecrypter(new DefaultSecDispatcher(cipher));
            try {
                dispatcher.setConfigurationFile(securitySettings.value.getAbsolutePath());
                Field f = dispatcher.getClass().getDeclaredField("_cipher");
                f.setAccessible(true);
                f.set(dispatcher, cipher);

                f = decrypter.getClass().getDeclaredField("securityDispatcher");
                f.setAccessible(true);
                f.set(decrypter, dispatcher);

                DefaultSettingsDecryptionRequest req = new DefaultSettingsDecryptionRequest(mavenSettings);
                SettingsDecryptionResult res = decrypter.decrypt(req);
                if (res.getProblems() != null && res.getProblems().size() > 0) {
                    for (SettingsProblem sp : res.getProblems()) {
                        System.err.println(sp);
                    }
                }

                for (Proxy proxy : res.getProxies()) {
                    if (!cipher.isEncryptedString(proxy.getPassword())) {
                        proxyPasswords.put(proxy.getId(), proxy.getPassword());
                    }
                }
                for (Server server : res.getServers()) {
                    if (!cipher.isEncryptedString(server.getPassword())) {
                        serverPasswords.put(server.getId(), server.getPassword());
                    }
                }
            } catch (Throwable t) {
                LOG.warn("Can't decrypt " + securitySettings.value, t);
            }
        }
    }

    /**
     * Returns list of configured remote (<code>remote=true</code>) or default (<code>remote=false</code>)
     * repositories.
     * @param remote
     * @return
     */
    protected MavenRepositoryURL[] repositories(Dictionary<String, Object> config, boolean remote) throws Exception {
        String property = remote ? PID + "." + PROPERTY_REPOSITORIES : PID + "." + PROPERTY_DEFAULT_REPOSITORIES;
        String[] repositories = listOfValues((String) config.get(property));

        if (remote) {
            if (repositories.length == 0 || repositories[0].charAt(0) == '+') {
                if (repositories.length > 0) {
                    repositories[0] = repositories[0].substring(1);
                }

                List<String> newRepositories = new LinkedList<>(Arrays.asList(repositories));

                // append all repositories from all active profiles from available settings.xml
                if (mavenSettings != null) {
                    // see org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl.getRepositories()
                    Set<String> activeProfiles = new LinkedHashSet<>(mavenSettings.getActiveProfiles());
                    Map<String, Profile> profiles = mavenSettings.getProfilesAsMap();
                    profiles.values().stream()
                            .filter((profile) -> profile.getActivation() != null && profile.getActivation().isActiveByDefault())
                            .map(Profile::getId)
                            .forEach(activeProfiles::add);

                    for (String activeProfile : activeProfiles) {
                        Profile profile = profiles.get(activeProfile);
                        if (profile == null) {
                            continue;
                        }
                        for (Repository repo : profile.getRepositories()) {
                            StringBuilder builder = new StringBuilder();
                            builder.append(repo.getUrl());
                            builder.append("@id=").append(repo.getId());
                            builder.append("@_from=").append(MavenRepositoryURL.FROM.SETTINGS);

                            if (repo.getReleases() != null) {
                                if (!repo.getReleases().isEnabled()) {
                                    builder.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_DISALLOW_RELEASES);
                                }
                                SourceAnd<String> up = updatePolicy(repo.getReleases().getUpdatePolicy());
                                addPolicy(builder, "".equals(up.val()) ? "never" : up.val(), ServiceConstants.OPTION_RELEASES_UPDATE);
                                // not used in pax-url-aether
                                //addPolicy(builder, repo.getReleases().getChecksumPolicy(), "releasesChecksum");
                            }
                            if (repo.getSnapshots() != null) {
                                if (repo.getSnapshots().isEnabled()) {
                                    builder.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_ALLOW_SNAPSHOTS);
                                }
                                SourceAnd<String> up = updatePolicy(repo.getSnapshots().getUpdatePolicy());
                                addPolicy(builder, "".equals(up.val()) ? "never" : up.val(), ServiceConstants.OPTION_SNAPSHOTS_UPDATE);
                                // not used in pax-url-aether
                                //addPolicy(builder, repo.getSnapshots().getChecksumPolicy(), "snapshotsChecksum");
                            }
                            newRepositories.add(builder.toString());
                        }
                    }
                }

                repositories = newRepositories.toArray(new String[newRepositories.size()]);
            }
        }

        List<MavenRepositoryURL> result = new LinkedList<>();
        for (String repo : repositories) {
            result.add(new MavenRepositoryURL(repo));
        }
        return result.toArray(new MavenRepositoryURL[result.size()]);
    }

    private void addPolicy(StringBuilder builder, String policy, String option) {
        if (policy != null && !policy.isEmpty()) {
            builder.append("@");
            builder.append(option);
            builder.append("=");
            builder.append(policy);
        }
    }

    /**
     * Splits comma separated list of values into String array
     * @param list
     * @return
     */
    protected String[] listOfValues(String list) {
        if (list == null) {
            return new String[0];
        }

        String[] values = list.split("\\s*,\\s*");
        return Arrays.stream(values)
                .filter((value) -> (value != null && !"".equals(value.trim())))
                .toArray(String[]::new);
    }

    /**
     * Adds information used by proxy/server
     * @param row {@link org.apache.karaf.shell.support.table.ShellTable}'s row to add information to
     * @param id2Password mapping of ids (servers/proxies to decrypted passwords)
     * @param id ID of proxy or server from <code>settings.xml</code>
     * @param password password to use if decryption failed
     */
    protected void addPasswordInfo(Row row, Map<String, String> id2Password, String id, String password) {
        if (id2Password.containsKey(id)) {
            row.addContent(id2Password.get(id));
        } else {
            if (cipher.isEncryptedString(password)) {
                row.addContent(password + " (can't decrypt)");
            } else {
                row.addContent(password == null ? "" : password);
            }
        }
    }

    /**
     * Asks for confirmation (user has to press <code>y</code>) after presenting a prompt
     * @param prompt
     * @return
     */
    protected boolean confirm(String prompt) throws IOException {
        String response = session.readLine(prompt, null);
        return "y".equals(response);
    }

    /**
     * Returns new {@link File} that's part of fixed-size sequence. Keeps the sequence bounded.
     * @param dataDir
     * @param pattern
     * @param fileNameFormat
     * @return
     */
    protected File nextSequenceFile(File dataDir, Pattern pattern, String fileNameFormat) {
        File[] files = dataDir.listFiles((dir, name) -> pattern.matcher(name).matches());
        File result = null;
        if (files != null && files.length > 0) {
            List<String> names = new ArrayList<>(Arrays.stream(files).map(File::getName)
                    .collect(TreeSet::new, TreeSet::add, TreeSet::addAll));

            names.add(String.format(fileNameFormat, System.currentTimeMillis()));

            while (names.size() > MAX_SEQUENCE_SIZE) {
                String name = names.remove(0);
                new File(dataDir, name).delete();
            }
            result = new File(dataDir, names.get(names.size() - 1));
        }
        if (result == null) {
            result = new File(dataDir, String.format(fileNameFormat, new Date().getTime()));
        }

        return result;
    }

    /**
     * This method controls whether passwords are tried to be decrypted.
     * @return
     */
    protected boolean showPasswords() {
        return false;
    }

    /**
     * Parses update policy value and returns {@link SourceAnd}<code>&lt;String&gt;</code> about the value
     * @param policy
     * @return
     */
    protected SourceAnd<String> updatePolicy(String policy) {
        SourceAnd<String> result = new SourceAnd<>();
        result.value = policy;

        if (policy == null || "".equals(policy.trim())) {
            result.value = "";
            result.valid = false;
            result.source = "Implicit \"never\", but doesn't override repository-specific value";
            return result;
        }

        result.source = String.format(PATTERN_PID_PROPERTY, PID, PID + "." + PROPERTY_GLOBAL_UPDATE_POLICY);
        if ("always".equals(policy) || "never".equals(policy) || "daily".equals(policy)) {
            // ok
            result.valid = true;
        } else if (policy.startsWith("interval")) {
            int minutes = 1440;
            try {
                String n = policy.substring("interval".length() + 1);
                minutes = Integer.parseInt(n);
                result.valid = true;
            } catch (Exception e) {
                result.valid = false;
                result.value = "interval:1440";
                result.source = "Implicit \"interval:1440\" (error parsing \"" + policy + "\")";
            }
        } else {
            result.valid = false;
            result.value = "never";
            result.source = "Implicit \"never\" (unknown value \"" + policy + "\")";
        }

        return result;
    }

    /**
     * Parses checksum policy value and returns {@link SourceAnd}<code>&lt;String&gt;</code> about the value
     * @param policy
     * @return
     */
    protected SourceAnd<String> checksumPolicy(String policy) {
        SourceAnd<String> result = new SourceAnd<>();
        result.value = policy;

        if (policy == null || "".equals(policy.trim())) {
            result.valid = false;
            result.value = "warn";
            result.source = "Default \"warn\"";
            return result;
        }

        result.source = String.format(PATTERN_PID_PROPERTY, PID, PID + "." + PROPERTY_GLOBAL_CHECKSUM_POLICY);
        if ("ignore".equals(policy) || "warn".equals(policy) || "fail".equals(policy)) {
            // ok
            result.valid = true;
        } else {
            result.valid = false;
            result.value = "warn";
            result.source = "Implicit \"warn\" (unknown value \"" + policy + "\")";
        }

        return result;
    }

    /**
     * Stores changed {@link org.apache.maven.settings.Settings} in new settings.xml file and updates
     * <code>org.ops4j.pax.url.mvn.settings</code> property. Does not update
     * {@link org.osgi.service.cm.ConfigurationAdmin} config.
     */
    protected void updateSettings(String prefix, Dictionary<String, Object> config) throws IOException {
        File dataDir = context.getDataFile(".");
        if (!dataDir.isDirectory()) {
            throw new RuntimeException("Can't access data directory for " + context.getBundle().getSymbolicName() + " bundle");
        }
        File newSettingsFile = nextSequenceFile(dataDir, RE_SETTINGS, PATTERN_SETTINGS);
        config.put(prefix + PROPERTY_SETTINGS_FILE, newSettingsFile.getCanonicalPath());

        try (FileWriter fw = new FileWriter(newSettingsFile)) {
            new SettingsXpp3Writer().write(fw, mavenSettings);
        }
        System.out.println("New settings stored in \"" + newSettingsFile.getCanonicalPath() + "\"");
    }

    /**
     * Handy class containing value and information about its origin. <code>valid</code> may be used to indicate
     * if the value is correct. It may be implicit, but the interpretation of <code>valid </code> is not defined.
     * @param <T>
     */
    protected static class SourceAnd<T> {
        String source;
        T value;
        boolean valid;

        public SourceAnd() {
        }

        public SourceAnd(String source, T value) {
            this.source = source;
            this.value = value;
        }

        public String val() {
            return value == null ? "" : value.toString();
        }
    }

}
