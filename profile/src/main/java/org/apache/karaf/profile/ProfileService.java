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
package org.apache.karaf.profile;

import java.util.Collection;

/**
 * The profile service
 */
public interface ProfileService {


    /**
     * Acquire a write lock for the profile.
     *
     * @return The write lock handler.
     */
    LockHandle acquireWriteLock();

    /**
     * Acquire a read lock for the profile.
     * A read lock cannot be upgraded to a write lock.
     *
     * @return The read lock handler.
     */
    LockHandle acquireReadLock();

    /**
     * Register the given resolver.
     *
     * @param resolver The resolver to register.
     */
    void registerResolver(PlaceholderResolver resolver);

    /**
     * Unregister the given resolver.
     *
     * @param resolver The resolver to unregister.
     */
    void unregisterResolver(PlaceholderResolver resolver);

    /**
     * Create the given profile in the data store.
     *
     * @param profile The profile to create.
     */
    void createProfile(Profile profile);
    
    /**
     * Create the given profile in the data store.
     *
     * @param profile The profile to update.
     */
    void updateProfile(Profile profile);

    /**
     * True if the given profile exists in the given version.
     *
     * @param profileId The profile ID.
     * @return True if the given profile exists, false else.
     */
    boolean hasProfile(String profileId);

    /**
     * Get the profile for the given version and id.
     *
     * @param profileId The profile ID.
     * @return The profile or null if not found.
     */
    Profile getProfile(String profileId);

    /**
     * Get the profile for the given version and id.
     *
     * @param profileId The profile ID.
     * @return The profile or null if not found.
     */
    Profile getRequiredProfile(String profileId);

    /** 
     * Get the list of profiles associated with the given version.
     *
     * @return The collection of all profiles.
     */
    Collection<String> getProfiles();

    /**
     * Delete the given profile from the data store.
     *
     * @param profileId The profile ID to remove.
     */
    void deleteProfile(String profileId);

    /**
     * Compute the overlay profile.
     *
     * The overlay profile is computed by getting all the parent profiles
     * and overriding the settings by children profiles.
     *
     * @param profile The profile.
     * @return The overlay profile.
     */
    Profile getOverlayProfile(Profile profile);

    /**
     * Compute the overlay profile.
     *
     * The overlay profile is computed by getting all the parent profiles
     * and overriding the settings by children profiles.
     *
     * @param profile The profile.
     * @param environment The environment.
     * @return The overlay profile.
     */
    Profile getOverlayProfile(Profile profile, String environment);

    /**
     * Compute the effective profile.
     *
     * The effective profile is computed by performing all substitutions
     * in the given profile configurations.
     *
     * @param profile The profile to compute.
     * @return The effective profile.
     */
    Profile getEffectiveProfile(Profile profile);

    /**
     * Compute the effective profile.
     *
     * The effective profile is computed by performing all substitutions
     * in the given profile configurations.
     *
     * @param profile The profile to compute.
     * @param defaultsToEmptyString if no substitution is valid, defaults to an empty string.
     * @return The effective profile.
     */
    Profile getEffectiveProfile(Profile profile, boolean defaultsToEmptyString);

}
