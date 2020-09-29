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
package org.apache.karaf.profile.command;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.ProfileConstants;
import org.apache.karaf.profile.ProfileService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Terminal;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@Command(name = "edit", scope = "profile", description = "Edits the specified profile", detailedDescription = "classpath:profileEdit.txt")
@Service
public class ProfileEdit implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEdit.class);

    static final String DELIMITER = ",";
    static final String PID_KEY_SEPARATOR = "/";

    static final String FILE_INSTALL_FILENAME_PROPERTY = "felix.fileinstall.filename";


    @Option(name = "-r", aliases = {"--repositories"}, description = "Edit the features repositories. To specify multiple repositories, specify this flag multiple times.", required = false, multiValued = true)
    private String[] repositories;

    @Option(name = "-f", aliases = {"--features"}, description = "Edit features. To specify multiple features, specify this flag multiple times. For example, --features foo --features bar.", required = false, multiValued = true)
    private String[] features;

    @Option(name = "-l", aliases = {"--libs"}, description = "Edit libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
    private String[] libs;

    @Option(name = "-n", aliases = {"--endorsed"}, description = "Edit endorsed libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
    private String[] endorsed;

    @Option(name = "-x", aliases = {"--extension"}, description = "Edit extension libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
    private String[] extension;

    @Option(name = "-b", aliases = {"--bundles"}, description = "Edit bundles. To specify multiple bundles, specify this flag multiple times.", required = false, multiValued = true)
    private String[] bundles;

    @Option(name = "-o", aliases = {"--overrides"}, description = "Edit overrides. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
    private String[] overrides;

    @Option(name = "-p", aliases = {"--pid"}, description = "Edit an OSGi configuration property, specified in the format <PID>/<Property>. To specify multiple properties, specify this flag multiple times.", required = false, multiValued = true)
    private String[] pidProperties;

    @Option(name = "-s", aliases = {"--system"}, description = "Edit the Java system properties that affect installed bundles (analogous to editing etc/system.properties in a root container).", required = false, multiValued = true)
    private String[] systemProperties;

    @Option(name = "-c", aliases = {"--config"}, description = "Edit the Java system properties that affect the karaf container (analogous to editing etc/config.properties in a root container).", required = false, multiValued = true)
    private String[] configProperties;

    @Option(name = "-i", aliases = {"--import-pid"}, description = "Imports the pids that are edited, from local OSGi config admin", required = false, multiValued = false)
    private boolean importPid = false;

    @Option(name = "--resource", description = "Selects a resource under the profile to edit. This option should only be used alone.", required = false, multiValued = false)
    private String resource;

    @Option(name = "--set", description = "Set or create values (selected by default).")
    private boolean set = true;

    @Option(name = "--delete", description = "Delete values. This option can be used to delete a feature, a bundle or a pid from the profile.")
    private boolean delete = false;

    @Option(name = "--append", description = "Append value to a delimited list. It is only usable with the system, config & pid options")
    private boolean append = false;

    @Option(name = "--remove", description = "Removes value from a delimited list. It is only usable with the system, config & pid options")
    private boolean remove = false;

    @Option(name = "--delimiter", description = "Specifies the delimiter to use for appends and removals.")
    private String delimiter = ",";

    @Argument(index = 0, name = "profile", description = "The target profile to edit", required = true, multiValued = false)
    private String profileName;

    @Reference
    private ProfileService profileService;

    @Reference
    private ConfigurationAdmin configurationAdmin;

    @Reference
    Terminal terminal;

    @Override
    public Object execute() throws Exception {
        if (delete) {
            set = false;
        }

        Profile profile = profileService.getRequiredProfile(profileName);
        editProfile(profile);
        return null;
    }

    private void editProfile(Profile profile) throws Exception {
        boolean editInLine = false;

        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        
        if (delete || remove) {
            editInLine = true;
        }

        if (features != null && features.length > 0) {
            editInLine = true;
            handleFeatures(builder, features, profile);
        }
        if (repositories != null && repositories.length > 0) {
            editInLine = true;
            handleFeatureRepositories(builder, repositories, profile);
        }
        if (libs != null && libs.length > 0) {
            editInLine = true;
            handleLibraries(builder, libs, profile, "lib", ProfileConstants.LIB_PREFIX);
        }
        if (endorsed != null && endorsed.length > 0) {
            editInLine = true;
            handleLibraries(builder, endorsed, profile, "endorsed lib", ProfileConstants.ENDORSED_PREFIX);
        }
        if (extension != null && extension.length > 0) {
            editInLine = true;
            handleLibraries(builder, extension, profile, "extension lib", ProfileConstants.EXT_PREFIX);
        }
        if (bundles != null && bundles.length > 0) {
            editInLine = true;
            handleBundles(builder, bundles, profile);
        }
        if (overrides != null && overrides.length > 0) {
            editInLine = true;
            handleOverrides(builder, overrides, profile);
        }

        if (pidProperties != null && pidProperties.length > 0) {
            editInLine = handlePid(builder, pidProperties, profile);
        }

        if (systemProperties != null && systemProperties.length > 0) {
            editInLine = true;
            handleSystemProperties(builder, systemProperties, profile);
        }

        if (configProperties != null && configProperties.length > 0) {
            editInLine = true;
            handleConfigProperties(builder, configProperties, profile);
        }

        if (!editInLine) {
            if (resource == null) {
                resource = Profile.INTERNAL_PID + Profile.PROPERTIES_SUFFIX;
            }
            //If a single pid has been selected, but not a key value has been specified or import has been selected,
            //then open the resource in the editor.
            if (pidProperties != null && pidProperties.length == 1) {
                resource = pidProperties[0] + Profile.PROPERTIES_SUFFIX;
            }
            openInEditor(profile, resource);
        }
        
        profileService.updateProfile(builder.getProfile());
    }

    /**
     * Adds or remove the specified features to the specified profile.
     */
    private void handleFeatures(ProfileBuilder builder, String[] features, Profile profile) {
        Map<String, Object> conf = getConfigurationFromBuilder(builder, Profile.INTERNAL_PID);
        for (String feature : features) {
            if (delete) {
                System.out.println("Deleting feature:" + feature + " from profile:" + profile.getId());
            } else {
                System.out.println("Adding feature:" + feature + " to profile:" + profile.getId());
            }
            updateConfig(conf, ProfileConstants.FEATURE_PREFIX + feature.replace('/', '_'), feature, set, delete);
            builder.addConfiguration(Profile.INTERNAL_PID, conf);
        }
    }

    /**
     * Adds or remove the specified feature repositories to the specified profile.
     */
    private void handleFeatureRepositories(ProfileBuilder builder, String[] repositories, Profile profile) {
        Map<String, Object> conf = getConfigurationFromBuilder(builder, Profile.INTERNAL_PID);
        for (String repositoryURI : repositories) {
            if (set) {
                System.out.println("Adding feature repository:" + repositoryURI + " to profile:" + profile.getId());
            } else if (delete) {
                System.out.println("Deleting feature repository:" + repositoryURI + " from profile:" + profile.getId());
            }
            updateConfig(conf, ProfileConstants.REPOSITORY_PREFIX + repositoryURI.replace('/', '_'), repositoryURI, set, delete);
        }
        builder.addConfiguration(Profile.INTERNAL_PID, conf);
    }

    /**
     * Adds or remove the specified libraries to the specified profile.
     * @param libs      The array of libs.
     * @param profile   The target profile.
     * @param libType   The type of lib. Used just for the command output.
     * @param libPrefix The prefix of the lib.
     */
    private void handleLibraries(ProfileBuilder builder, String[] libs, Profile profile, String libType, String libPrefix) {
        Map<String, Object> conf = getConfigurationFromBuilder(builder, Profile.INTERNAL_PID);
        for (String lib : libs) {
            if (set) {
                System.out.println("Adding "+libType+":" + lib + " to profile:" + profile.getId());
            } else if (delete) {
                System.out.println("Deleting "+libType+":" + lib + " from profile:" + profile.getId());
            }
            updateConfig(conf, libPrefix + lib.replace('/', '_'), lib, set, delete);
        }
        builder.addConfiguration(Profile.INTERNAL_PID, conf);
    }

    /**
     * Adds or remove the specified bundles to the specified profile.
     * @param bundles   The array of bundles.
     * @param profile   The target profile.
     */
    private void handleBundles(ProfileBuilder builder, String[] bundles, Profile profile) {
        Map<String, Object> conf = getConfigurationFromBuilder(builder, Profile.INTERNAL_PID);
        for (String bundle : bundles) {
            if (set) {
                System.out.println("Adding bundle:" + bundle + " to profile:" + profile.getId());
            } else if (delete) {
                System.out.println("Deleting bundle:" + bundle + " from profile:" + profile.getId());
            }
            updateConfig(conf, ProfileConstants.BUNDLE_PREFIX + bundle.replace('/', '_'), bundle, set, delete);
        }
        builder.addConfiguration(Profile.INTERNAL_PID, conf);
    }

    /**
     * Adds or remove the specified overrides to the specified profile.
     * @param overrides     The array of overrides.
     * @param profile       The target profile.
     */
    private void handleOverrides(ProfileBuilder builder, String[] overrides, Profile profile) {
        Map<String, Object> conf = getConfigurationFromBuilder(builder, Profile.INTERNAL_PID);
        for (String override : overrides) {
            if (set) {
                System.out.println("Adding override:" + override + " to profile:" + profile.getId());
            } else if (delete) {
                System.out.println("Deleting override:" + override + " from profile:" + profile.getId());
            }
            updateConfig(conf, ProfileConstants.OVERRIDE_PREFIX + override.replace('/', '_'), override, set, delete);
        }
        builder.addConfiguration(Profile.INTERNAL_PID, conf);
    }

    /**
     * Adds or remove the specified system properties to the specified profile.
     * @param pidProperties         The array of system properties.
     * @param profile               The target profile.
     * @return                      True if the edit can take place in line.
     */
    private boolean handlePid(ProfileBuilder builder, String[] pidProperties, Profile profile) {
        boolean editInline = true;
        for (String pidProperty : pidProperties) {
            String currentPid;

            String keyValuePair = "";
            if (pidProperty.contains(PID_KEY_SEPARATOR)) {
                currentPid = pidProperty.substring(0, pidProperty.indexOf(PID_KEY_SEPARATOR));
                keyValuePair = pidProperty.substring(pidProperty.indexOf(PID_KEY_SEPARATOR) + 1);
            } else {
                currentPid = pidProperty;
            }
            Map<String, Object> conf = getConfigurationFromBuilder(builder, currentPid);
            
            // We only support import when a single pid is specified
            if (pidProperties.length == 1 && importPid) {
                System.out.println("Importing pid:" + currentPid + " to profile:" + profile.getId());
                importPidFromLocalConfigAdmin(currentPid, conf);
                builder.addConfiguration(currentPid, conf);
                return true;
            }


            Map<String, String> configMap = extractConfigs(keyValuePair);
            if (configMap.isEmpty() && set) {
                editInline = false;
            } else if (configMap.isEmpty() && delete) {
                editInline = true;
                System.out.println("Deleting pid:" + currentPid + " from profile:" + profile.getId());
                builder.deleteConfiguration(currentPid);
            } else {
                for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                    String key = configEntries.getKey();
                    String value = configEntries.getValue();
                    if (value == null && delete) {
                        System.out.println("Deleting key:" + key + " from pid:" + currentPid + " and profile:" + profile.getId());
                        conf.remove(key);
                    } else {
                        if (append) {
                            System.out.println("Appending value:" + value + " key:" + key + " to pid:" + currentPid + " and profile:" + profile.getId());
                        } else if (remove) {
                            System.out.println("Removing value:" + value + " key:" + key + " from pid:" + currentPid + " and profile:" + profile.getId());
                        } else if(set) {
                            System.out.println("Setting value:" + value + " key:" + key + " on pid:" + currentPid + " and profile:" + profile.getId());
                        }
                        updatedDelimitedList(conf, key, value, delimiter, set, delete, append, remove);
                    }
                }
                editInline = true;
                builder.addConfiguration(currentPid, conf);
            }
        }
        return editInline;
    }


    /**
     * Adds or remove the specified system properties to the specified profile.
     * @param systemProperties      The array of system properties.
     * @param profile               The target profile.
     */
    private void handleSystemProperties(ProfileBuilder builder, String[] systemProperties, Profile profile) {
        Map<String, Object> conf = getConfigurationFromBuilder(builder, Profile.INTERNAL_PID);
        for (String systemProperty : systemProperties) {
            Map<String, String> configMap = extractConfigs(systemProperty);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                String value = configEntries.getValue();
                if (append) {
                    System.out.println("Appending value:" + value + " key:" + key + " from system properties and profile:" + profile.getId());
                } else if (delete) {
                    System.out.println("Deleting key:" + key + " from system properties and profile:" + profile.getId());
                } else if (set) {
                    System.out.println("Setting value:" + value + " key:" + key + " from system properties and profile:" + profile.getId());
                } else {
                    System.out.println("Removing value:" + value + " key:" + key + " from system properties and profile:" + profile.getId());
                }
                updatedDelimitedList(conf, ProfileConstants.SYSTEM_PREFIX + key, value, delimiter, set, delete, append, remove);
            }
        }
        builder.addConfiguration(Profile.INTERNAL_PID, conf);
    }

    /**
     * Adds or remove the specified config properties to the specified profile.
     * @param configProperties      The array of config properties.
     * @param profile               The target profile.
     */
    private void handleConfigProperties(ProfileBuilder builder, String[] configProperties, Profile profile) {
        Map<String, Object> conf = getConfigurationFromBuilder(builder, Profile.INTERNAL_PID);
        for (String configProperty : configProperties) {
            Map<String, String> configMap = extractConfigs(configProperty);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                String value = configEntries.getValue();
                if (append) {
                    System.out.println("Appending value:" + value + " key:" + key + " from config properties and profile:" + profile.getId());
                } else if (delete) {
                    System.out.println("Deleting key:" + key + " from config properties and profile:" + profile.getId());
                } else if (set) {
                    System.out.println("Setting value:" + value + " key:" + key + " from config properties and profile:" + profile.getId());
                }
                updatedDelimitedList(conf, ProfileConstants.CONFIG_PREFIX + key, value, delimiter, set, delete, append, remove);
            }
        }
        builder.addConfiguration(Profile.INTERNAL_PID, conf);
    }

    private void openInEditor(Profile profile, String resource) throws Exception {
        /* TODO:JLINE
        String id = profile.getId();
        String location = id + " " + resource;
        //Call the editor
        ConsoleEditor editor = editorFactory.create("simple", getTerminal());
        editor.setTitle("Profile");
        editor.setOpenEnabled(false);
        editor.setContentManager(new DatastoreContentManager(profileService));
        editor.open(location, id);
        editor.start();
        */
    }

    public void updatedDelimitedList(Map<String, Object> map, String key, String value, String delimiter, boolean set, boolean delete, boolean append, boolean remove) {
        if (append || remove) {
            String oldValue = map.containsKey(key) ? (String) map.get(key) : "";
            List<String> parts = new LinkedList<>(Arrays.asList(oldValue.split(delimiter)));
            //We need to remove any possible blanks.
            parts.remove("");
            if (append) {
                parts.add(value);
            }
            if (remove) {
                parts.remove(value);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i != 0) {
                    sb.append(delimiter);
                }
                sb.append(parts.get(i));
            }
            map.put(key, sb.toString());
        } else if (set) {
            map.put(key, value);
        } else if (delete) {
            map.remove(key);
        }
    }

    private void updateConfig(Map<String, Object> map, String key, Object value, boolean set, boolean delete) {
        if (set) {
            map.put(key, value);
        } else if (delete) {
            map.remove(key);
        }
    }

    /**
     * Imports the pid to the target Map.
     */
    private void importPidFromLocalConfigAdmin(String pid, Map<String, Object> target) {
        try {
            Configuration[] configuration = configurationAdmin.listConfigurations("(service.pid=" + pid + ")");
            if (configuration != null && configuration.length > 0) {
                Dictionary<String, Object> dictionary = configuration[0].getProcessedProperties(null);
                Enumeration<String> keyEnumeration = dictionary.keys();
                while (keyEnumeration.hasMoreElements()) {
                    String key = String.valueOf(keyEnumeration.nextElement());
                    //file.install.filename needs to be skipped as it specific to the current container.
                    if (!key.equals(FILE_INSTALL_FILENAME_PROPERTY)) {
                        String value = String.valueOf(dictionary.get(key));
                        target.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error while importing configuration {} to profile.", pid);
        }
    }

    /**
     * Extracts Key value pairs from a delimited string of key value pairs.
     * Note: The value may contain commas.
     */
    private Map<String, String> extractConfigs(String configs) {
        Map<String, String> configMap = new HashMap<>();
        //If contains key values.
        String key;
        String value;
        if (configs.contains("=")) {
            key = configs.substring(0, configs.indexOf("="));
            value = configs.substring(configs.indexOf("=") + 1);

        }  else {
            key = configs;
            value = null;
        }
        if (!key.isEmpty()) {
            configMap.put(key, value);
        }
        return configMap;
    }

    /**
    private jline.Terminal getTerminal() throws Exception {
        try {
            return (jline.Terminal) terminal.getClass().getMethod("getTerminal").invoke(terminal);
        } catch (Throwable t) {
            return new TerminalSupport(true) {
                @Override
                public int getWidth() {
                    return terminal.getWidth();
                }

                @Override
                public int getHeight() {
                    return terminal.getHeight();
                }
            };
        }
    }

    static class DatastoreContentManager implements ContentManager {

        private static final Charset UTF_8 = Charset.forName("UTF-8");

        private final ProfileService profileService;

        public DatastoreContentManager(ProfileService profileService) {
            this.profileService = profileService;
        }

        @Override
        public String load(String location) throws IOException {
            try {
                String[] parts = location.trim().split(" ");
                if (parts.length < 3) {
                    throw new IllegalArgumentException("Invalid location:" + location);
                }
                String profileId = parts[0];
                String resource = parts[1];
                Profile profile = profileService.getRequiredProfile(profileId);
                String data = new String(profile.getFileConfiguration(resource));
                return data;
            } catch (Exception e) {
                throw new IOException("Failed to read data from zookeeper.", e);
            }
        }

        @Override
        public boolean save(String content, String location) {
            try {
                String[] parts = location.trim().split(" ");
                if (parts.length < 3) {
                    throw new IllegalArgumentException("Invalid location:" + location);
                }
                String profileId = parts[0];
                String resource = parts[1];
                Profile profile = profileService.getRequiredProfile(profileId);
                ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
                builder.addFileConfiguration(resource, content.getBytes());
                profileService.updateProfile(builder.getProfile());
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        @Override
        public boolean save(String content, Charset charset, String location) {
            return save(content, location);
        }

        @Override
        public Charset detectCharset(String location) {
            return UTF_8;
        }
    }
        */

    private Map<String, Object> getConfigurationFromBuilder(ProfileBuilder builder, String pid) {
        return builder.getConfiguration(pid);
    }

}
