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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.karaf.profile.LockHandle;
import org.apache.karaf.profile.PlaceholderResolver;
import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileService;

import static org.apache.karaf.profile.impl.Utils.assertFalse;
import static org.apache.karaf.profile.impl.Utils.assertNotNull;
import static org.apache.karaf.profile.impl.Utils.join;

public class ProfileServiceImpl implements ProfileService {

    private static final long ACQUIRE_LOCK_TIMEOUT = 25 * 1000L;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final List<PlaceholderResolver> resolvers = new CopyOnWriteArrayList<>();
    private final Path profilesDirectory;
    private Map<String, Profile> cache;

    public ProfileServiceImpl(Path profilesDirectory) throws IOException {
        this.profilesDirectory = profilesDirectory;
        Files.createDirectories(profilesDirectory);
    }

    @Override
    public LockHandle acquireWriteLock() {
        return acquireLock(getLock().writeLock(), "Cannot obtain profile write lock in time");
    }

    @Override
    public LockHandle acquireReadLock() {
        return acquireLock(getLock().readLock(), "Cannot obtain profile read lock in time");
    }

    protected LockHandle acquireLock(final Lock lock, String message) {
        try {
            if (!lock.tryLock(ACQUIRE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(message);
            }
        } catch (InterruptedException ex) {
            throw new IllegalStateException(message, ex);
        }
        return lock::unlock;
    }

    protected ReadWriteLock getLock() {
        return readWriteLock;
    }

    @Override
    public void registerResolver(PlaceholderResolver resolver) {
        resolvers.add(resolver);
    }

    @Override
    public void unregisterResolver(PlaceholderResolver resolver) {
        resolvers.remove(resolver);
    }

    @Override
    @SuppressWarnings("unused")
    public void createProfile(Profile profile) {
        assertNotNull(profile, "profile is null");
        try (LockHandle lock = acquireWriteLock()) {
            String profileId = profile.getId();
            assertFalse(hasProfile(profileId), "Profile already exists: " + profileId);
            createOrUpdateProfile(null, profile);
        }
    }

    @Override
    @SuppressWarnings("unused")
    public void updateProfile(Profile profile) {
        assertNotNull(profile, "profile is null");
        try (LockHandle lock = acquireWriteLock()) {
            final String profileId = profile.getId();
            final Profile lastProfile = getRequiredProfile(profileId);
            createOrUpdateProfile(lastProfile, profile);
        }
    }

    @Override
    @SuppressWarnings("unused")
    public boolean hasProfile(String profileId) {
        assertNotNull(profileId, "profileId is null");
        try (LockHandle lock = acquireReadLock()) {
            Profile profile = getProfileFromCache(profileId);
            return profile != null;
        }
    }

    @Override
    @SuppressWarnings("unused")
    public Profile getProfile(String profileId) {
        assertNotNull(profileId, "profileId is null");
        try (LockHandle lock = acquireReadLock()) {
            return getProfileFromCache(profileId);
        }
    }

    @Override
    @SuppressWarnings("unused")
    public Profile getRequiredProfile(String profileId) {
        assertNotNull(profileId, "profileId is null");
        try (LockHandle lock = acquireReadLock()) {
            Profile profile = getProfileFromCache(profileId);
            assertNotNull(profile, "Profile does not exist: " + profileId);
            return profile;
        }
    }

    @Override
    @SuppressWarnings("unused")
    public Collection<String> getProfiles() {
        try (LockHandle lock = acquireReadLock()) {
            Collection<String> profiles = getProfilesFromCache();
            return Collections.unmodifiableCollection(profiles);
        }
    }

    @Override
    @SuppressWarnings("unused")
    public void deleteProfile(String profileId) {
        assertNotNull(profileId, "profileId is null");
        try (LockHandle lock = acquireWriteLock()) {
            final Profile lastProfile = getRequiredProfile(profileId);
            deleteProfileFromCache(lastProfile);
        }
    }

    @Override
    public Profile getOverlayProfile(Profile profile) {
        return Profiles.getOverlay(profile, loadCache());
    }

    @Override
    public Profile getOverlayProfile(Profile profile, String environment) {
        return Profiles.getOverlay(profile, loadCache(), environment);
    }

    @Override
    public Profile getEffectiveProfile(Profile profile) {
        return Profiles.getEffective(profile, resolvers);
    }

    @Override
    public Profile getEffectiveProfile(Profile profile, boolean defaultsToEmptyString) {
        return Profiles.getEffective(profile, resolvers, defaultsToEmptyString);
    }

    protected void createOrUpdateProfile(Profile lastProfile, Profile profile) {
        if (lastProfile != null) {
            deleteProfileFromCache(lastProfile);
        }
        try {
            loadCache();
            for (String parentId : profile.getParentIds()) {
                if (!cache.containsKey(parentId)) {
                    throw new IllegalStateException("Parent profile " + parentId + " does not exist");
                }
            }
            Profiles.writeProfile(profilesDirectory, profile);
            if (cache != null) {
                cache.put(profile.getId(), profile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing profiles", e);
        }
    }

    protected Profile getProfileFromCache(String profileId) {
        return loadCache().get(profileId);
    }

    protected Collection<String> getProfilesFromCache() {
        return loadCache().keySet();
    }

    protected void deleteProfileFromCache(Profile lastProfile) {
        loadCache();
        List<String> children = new ArrayList<>();
        for (Profile p : cache.values()) {
            if (p.getParentIds().contains(lastProfile.getId())) {
                children.add(p.getId());
            }
        }
        if (!children.isEmpty()) {
            throw new IllegalStateException("Profile " + lastProfile.getId() + " is a parent of " + join(", ", children));
        }
        try {
            Profiles.deleteProfile(profilesDirectory, lastProfile.getId());
            cache.remove(lastProfile.getId());
        } catch (IOException e) {
            cache = null;
            throw new IllegalStateException("Error deleting profiles", e);
        }
    }

    protected Map<String, Profile> loadCache() {
        if (cache == null) {
            try {
                cache = Profiles.loadProfiles(profilesDirectory);
            } catch (IOException e) {
                throw new IllegalStateException("Error reading profiles", e);
            }
        }
        return cache;
    }

}
