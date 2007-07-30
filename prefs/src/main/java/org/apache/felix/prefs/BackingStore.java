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
package org.apache.felix.prefs;

import org.osgi.service.prefs.BackingStoreException;

/**
 * The BackingStore for the preferences.
 *
 * This interface allows for different implementation strategies.
 */
public interface BackingStore {

    /**
     * Store the current preferences and its children in the backing
     * store.
     * The store should check, if the preferences have changed,
     * it should also check all children.
     * @param prefs The preferences.
     * @throws BackingStoreException
     */
    void store(PreferencesImpl prefs) throws BackingStoreException;

    /**
     * Update the current preferences and its children from the
     * backing store.
     */
    void update(PreferencesImpl prefs) throws BackingStoreException;

    /**
     * Return all bundle ids for which preferences are stored..
     * @return Return an array of bundle ids or an empty array.
     */
    Long[] availableBundles();

    /**
     * Remove all preferences stored for this bundle.
     * @param bundleId The bundle id.
     * @throws BackingStoreException
     */
    void remove(Long bundleId) throws BackingStoreException;

    /**
     * Load the preferences for the given description.
     * @param manager The backing store manager which should be passed to new preferences implementations.
     * @param desc
     * @return A new preferences object or null if it's not available in the backing store.
     * @throws BackingStoreException
     */
    PreferencesImpl load(BackingStoreManager manager, PreferencesDescription desc) throws BackingStoreException;

    /**
     * Load all preferences for this bundle.
     * @param manager The backing store manager which should be passed to new preferences implementations.
     * @param bundleId The bundle id.
     * @return An array with the preferences or an empty array.
     * @throws BackingStoreException
     */
    PreferencesImpl[] loadAll(BackingStoreManager manager, Long bundleId) throws BackingStoreException;
}
