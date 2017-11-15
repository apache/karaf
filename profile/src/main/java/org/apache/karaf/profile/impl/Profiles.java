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
package org.apache.karaf.profile.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.profile.PlaceholderResolver;
import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;

import static org.apache.karaf.profile.impl.Utils.assertNotNull;

/**
 * Static utilities to work with {@link Profile profiles}.
 */
public final class Profiles {

    public static final String PROFILE_FOLDER_SUFFIX = ".profile";

    /**
     * <p>Loads profiles from given directory path. A profile is represented as directory with <code>.profile</code>
     * extension. Subdirectories constitute part of {@linl Profile#getId} - directory separators are changed to
     * <code>-</code>.</p>
     * <p>For example, profile contained in directory <code>mq/broker/standalone.profile</code> will have
     * id = <code>mq-broker-standalone</code>.</p>
     * @param root
     * @return
     * @throws IOException
     */
    public static Map<String, Profile> loadProfiles(final Path root) throws IOException {
        final Map<String, Profile> profiles = new HashMap<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                ProfileBuilder builder;
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path fileName = dir.getFileName();
                    if (fileName != null && (fileName.toString().endsWith(PROFILE_FOLDER_SUFFIX)
                            || fileName.toString().endsWith(PROFILE_FOLDER_SUFFIX + "/"))) {
                        String profileId = root.relativize(dir).toString();
                        if (profileId.endsWith("/")) {
                            profileId = profileId.substring(0, profileId.length() - 1);
                        }
                        profileId = profileId.replaceAll(root.getFileSystem().getSeparator(), "-");
                        profileId = profileId.substring(0, profileId.length() - PROFILE_FOLDER_SUFFIX.length());
                        builder = ProfileBuilder.Factory.create(profileId);
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    if (builder != null) {
                        Profile profile = builder.getProfile();
                        profiles.put(profile.getId(), profile);
                        builder = null;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (builder != null) {
                        String pid = file.getFileName().toString();
                        byte[] data = Files.readAllBytes(file);
                        builder.addFileConfiguration(pid, data);
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        );
        return profiles;
    }

    /**
     * Deletes profile by given {@link Profile#getId()} from <code>root</code> path.
     * @param root
     * @param id
     * @throws IOException
     */
    public static void deleteProfile(Path root, String id) throws IOException {
        Path path = root.resolve(id.replaceAll("-", root.getFileSystem().getSeparator()) + PROFILE_FOLDER_SUFFIX);
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Writes given {@link Profile} under a path specified as <code>root</code>. Directory name to store a profile is
     * derived from {@link Profile#getId()}
     * @param root
     * @param profile
     * @throws IOException
     */
    public static void writeProfile(Path root, Profile profile) throws IOException {
        Path path = root.resolve(profile.getId().replaceAll("-", root.getFileSystem().getSeparator()) + PROFILE_FOLDER_SUFFIX);
        Files.createDirectories(path);
        for (Map.Entry<String, byte[]> entry : profile.getFileConfigurations().entrySet()) {
            Files.write(path.resolve(entry.getKey()), entry.getValue(), StandardOpenOption.CREATE_NEW);
        }
    }

    /**
     * <p>Gets an <em>overlay</em> profile for given <code>profile</code>, where passed in map of additional profiles
     * is searched for possible parent profiles of given <code>profile</code>.</p>
     * @param profile
     * @param profiles
     * @return
     */
    public static Profile getOverlay(Profile profile, Map<String, Profile> profiles) {
        return getOverlay(profile, profiles, null);
    }

    /**
     * <p>Gets an <em>overlay</em> profile for given <code>profile</code>, where passed in map of additional profiles
     * is searched for possible parent profiles of given <code>profile</code>.</p>
     * <p><code>environment</code> may be used to select different <em>variants</em> of profile configuration files.
     * For example, if <code>environment</code> is specified, configuration for <code>my.pid</code> PID will be read
     * from <code>my.pid.cfg#&lt;environment&gt;</code>.</p>
     * @param profile
     * @param profiles
     * @param environment
     * @return
     */
    public static Profile getOverlay(Profile profile, Map<String, Profile> profiles, String environment) {
        assertNotNull(profile, "profile is null");
        assertNotNull(profile, "profiles is null");
        if (profile.isOverlay()) {
            return profile;
        } else {
            String profileId = profile.getId();
            ProfileBuilder builder = ProfileBuilder.Factory.create(profileId);
            new OverlayOptionsProvider(profiles, profile, environment).addOptions(builder);
            return builder.getProfile();
        }
    }

    /**
     * Gets an <code>effective</code> profile with single property placeholder resolver for <code>${profile:xxx}</code>
     * placeholders and with <code>finalSubstitution</code> set to <code>true</code>.
     * @param profile
     * @return
     */
    public static Profile getEffective(final Profile profile) {
        return getEffective(profile, true);
    }

    /**
     * Gets an <code>effective</code> profile with single property placeholder resolver for <code>${profile:xxx}</code>
     * placeholders.
     * @param profile
     * @param finalSubstitution
     * @return
     */
    public static Profile getEffective(final Profile profile, boolean finalSubstitution) {
        return getEffective(profile,
                Collections.singleton(new PlaceholderResolvers.ProfilePlaceholderResolver()),
                finalSubstitution);
    }

    /**
     * Gets an <code>effective</code> profile with <code>finalSubstitution</code> set to <code>true</code>.
     * @param profile
     * @param resolvers
     * @return
     */
    public static Profile getEffective(final Profile profile,
                                       final Collection<PlaceholderResolver> resolvers) {
        return getEffective(profile, resolvers, true);
    }

    /**
     * <p>Gets an <em>effective</em> profile for given <code>profile</code>. Effective profile has all property
     * placeholders resolved. When <code>finalSubstitution</code> is <code>true</code>, placeholders that can't
     * be resolved are replaced with empty strings. When it's <code>false</code>, placeholders are left unchanged.</p>
     * @param profile
     * @param resolvers
     * @param finalSubstitution
     * @return
     */
    public static Profile getEffective(final Profile profile,
                                       final Collection<PlaceholderResolver> resolvers,
                                       boolean finalSubstitution) {
        assertNotNull(profile, "profile is null");
        assertNotNull(profile, "resolvers is null");

        final Map<String, TypedProperties> originals = new HashMap<>();
        final Map<String, TypedProperties> originals2 = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : profile.getFileConfigurations().entrySet()) {
            if (entry.getKey().endsWith(Profile.PROPERTIES_SUFFIX)) {
                try {
                    String key = entry.getKey().substring(0, entry.getKey().length() - Profile.PROPERTIES_SUFFIX.length());
                    TypedProperties props = new TypedProperties(false);
                    props.load(new ByteArrayInputStream(entry.getValue()));
                    originals.put(key, props);
                    props = new TypedProperties(false);
                    props.load(new ByteArrayInputStream(entry.getValue()));
                    originals2.put(key, props);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Can not load properties for " + entry.getKey());
                }
            }
        }
        final Map<String, Map<String, String>> dynamic = TypedProperties.prepare(originals);
        TypedProperties.substitute(originals, dynamic, (pid, key, value) -> {
            if (value != null) {
                for (PlaceholderResolver resolver : resolvers) {
                    if (resolver.getScheme() == null) {
                        String val = resolver.resolve(dynamic, pid, key, value);
                        if (val != null) {
                            return val;
                        }
                    }
                }
                if (value.contains(":")) {
                    String scheme = value.substring(0, value.indexOf(":"));
                    String toSubst = value.substring(scheme.length() + 1);
                    for (PlaceholderResolver resolver : resolvers) {
                        if (scheme.equals(resolver.getScheme())) {
                            String val = resolver.resolve(dynamic, pid, key, toSubst);
                            if (val != null) {
                                return val;
                            }
                        }
                    }
                }
            }
            return null;
        }, finalSubstitution);

         // Force computation while preserving layout
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        for (String cfg : originals.keySet()) {
            TypedProperties original = originals.get(cfg);
            TypedProperties original2 = originals2.get(cfg);
            original2.putAll(original);
            builder.addFileConfiguration(cfg + Profile.PROPERTIES_SUFFIX, Utils.toBytes(original2));
        }
        // Compute the new profile
        return builder.getProfile();
    }

    /**
     * <p>Helper internal class to configure {@link ProfileBuilder} used to create an <em>overlay</em> profile.</p>
     * <p>There are strict rules built on a concept of profiles being <em>containers of file configurations</em>.
     * Each profile may contain files with the same name. Profiles may be set in multi-parent - child relationship.
     * Such graph of profiles is searched in depth-first fashion, while child (being a root of the graph) has
     * highest priority.</p>
     * <p>Files from higher-priority profile override files from parent profiles. Special case are PID files (with
     * {@link Profile#PROPERTIES_SUFFIX} extension). These files are not simply taken from child profiles. Child
     * profiles may have own version of given PID configuration file, but these files are overwritten at property
     * level.</p>
     * <p>For example, if parent profile specifies:<pre>
     * property1 = v1
     * property2 = v2
     * </pre> and child profile specifies:<pre>
     * property1 = v1a
     * property3 = v3a
     * </pre>an <em>overlay</em> profile for child profile uses:<pre>
     * property1 = v1a
     * property2 = v2
     * property3 = v3a
     * </pre></p>
     */
    static private class OverlayOptionsProvider {

        private final Map<String, Profile> profiles;
        private final Profile self;
        private final String environment;

        private static class SupplementControl {
            byte[] data;
            TypedProperties props;
        }

        private OverlayOptionsProvider(Map<String, Profile> profiles, Profile self, String environment) {
            this.profiles = profiles;
            this.self = self;
            this.environment = environment;
        }

        private ProfileBuilder addOptions(ProfileBuilder builder) {
            builder.setAttributes(self.getAttributes());
            builder.setFileConfigurations(getFileConfigurations());
            builder.setOverlay(true);
            return builder;
        }

        private Map<String, byte[]> getFileConfigurations() {
            Map<String, SupplementControl> aggregate = new HashMap<>();
            for (Profile profile : getInheritedProfiles()) {
                supplement(profile, aggregate);
            }

            Map<String, byte[]> rc = new HashMap<>();
            for (Map.Entry<String, SupplementControl> entry : aggregate.entrySet()) {
                SupplementControl ctrl = entry.getValue();
                if (ctrl.props != null) {
                    ctrl.data = Utils.toBytes(ctrl.props);
                }
                rc.put(entry.getKey(), ctrl.data);
            }
            return rc;
        }

        private List<Profile> getInheritedProfiles() {
            List<Profile> profiles = new ArrayList<>();
            fillParentProfiles(self, profiles);
            return profiles;
        }

        private void fillParentProfiles(Profile profile, List<Profile> profiles) {
            if (!profiles.contains(profile)) {
                for (String parentId : profile.getParentIds()) {
                    Profile parent = getRequiredProfile(parentId);
                    fillParentProfiles(parent, profiles);
                }
                profiles.add(profile);
            }
        }

        private void supplement(Profile profile, Map<String, SupplementControl> aggregate) {
            Map<String, byte[]> configs = profile.getFileConfigurations();
            for (String key : configs.keySet()) {
                // Ignore environment specific configs
                if (key.contains("#")) {
                    continue;
                }
                byte[] value = configs.get(key);
                if (environment != null && configs.containsKey(key + "#" + environment)) {
                    value = configs.get(key + "#" + environment);
                }
                // we can use fine grained inheritance based updating if it's
                // a properties file.
                if (key.endsWith(Profile.PROPERTIES_SUFFIX)) {
                    SupplementControl ctrl = aggregate.get(key);
                    if (ctrl != null) {
                        // we can update the file..
                        TypedProperties childMap = Utils.toProperties(value);
                        if (childMap.remove(Profile.DELETED) != null) {
                            ctrl.props.clear();
                        }

                        // Update the entries...
                        for (Map.Entry<String, Object> p : childMap.entrySet()) {
                            if (Profile.DELETED.equals(p.getValue())) {
                                ctrl.props.remove(p.getKey());
                            } else {
                                ctrl.props.put(p.getKey(), p.getValue());
                            }
                        }

                    } else {
                        // new file..
                        ctrl = new SupplementControl();
                        ctrl.props = Utils.toProperties(value);
                        aggregate.put(key, ctrl);
                    }
                } else {
                    // not a properties file? we can only overwrite.
                    SupplementControl ctrl = new SupplementControl();
                    ctrl.data = value;
                    aggregate.put(key, ctrl);
                }
            }
        }

        private Profile getRequiredProfile(String id) {
            Profile profile = profiles.get(id);
            if (profile == null) {
                throw new IllegalStateException("Unable to find required profile " + id);
            }
            return profile;
        }
    }

    private Profiles() { }

}
