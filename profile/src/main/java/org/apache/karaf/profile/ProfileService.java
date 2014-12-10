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

    //
    // Lock management
    //

    /**
     * Acquire a write lock for the profile.
     */
    LockHandle acquireWriteLock();

    /**
     * Acquire a read lock for the profile.
     * A read lock cannot be upgraded to a write lock.
     */
    LockHandle acquireReadLock();

    //
    // PlaceholderResolver management
    //

    /**
     * Register the given resolver.
     * @param resolver the resolver to register
     */
    void registerResolver(PlaceholderResolver resolver);

    /**
     * Unregister the given resolver.
     * @param resolver the resolver to unregister
     */
    void unregisterResolver(PlaceholderResolver resolver);

    //
    // Profile management
    //
    
    /**
     * Create the given profile in the data store.
     */
    void createProfile(Profile profile);
    
    /**
     * Create the given profile in the data store.
     */
    void updateProfile(Profile profile);

    /**
     * True if the given profile exists in the given version.
     */
    boolean hasProfile(String profileId);

    /**
     * Get the profile for the given version and id.
     * @return The profile or null
     */
    Profile getProfile(String profileId);

    /**
     * Get the profile for the given version and id.
     * @throws IllegalStateException if the required profile does not exist
     */
    Profile getRequiredProfile(String profileId);

    /** 
     * Get the list of profiles associated with the given version.
     */
    Collection<String> getProfiles();

    /**
     * Delete the given profile from the data store.
     */
    void deleteProfile(String profileId);

    /**
     * Compute the overlay profile.
     *
     * The overlay profile is computed by getting all the parent profiles
     * and overriding the settings by children profiles.
     */
    Profile getOverlayProfile(Profile profile);

    /**
     * Compute the overlay profile.
     *
     * The overlay profile is computed by getting all the parent profiles
     * and overriding the settings by children profiles.
     */
    Profile getOverlayProfile(Profile profile, String environment);

    /**
     * Compute the effective profile.
     *
     * The effective profile is computed by performing all substitutions
     * in the given profile configurations.
     */
    Profile getEffectiveProfile(Profile profile);

    /**
     * Compute the effective profile.
     *
     * The effective profile is computed by performing all substitutions
     * in the given profile configurations.
     *
     * @param defaultsToEmptyString if no substitution is valid, defaults to an empty string
     */
    Profile getEffectiveProfile(Profile profile, boolean defaultsToEmptyString);

}
