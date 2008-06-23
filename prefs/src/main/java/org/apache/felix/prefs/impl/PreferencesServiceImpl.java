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
package org.apache.felix.prefs.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.prefs.BackingStoreManager;
import org.apache.felix.prefs.PreferencesDescription;
import org.apache.felix.prefs.PreferencesImpl;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

/**
 * This is an implementation of the OSGI Preferences Service, Version 1.1.
 */
public class PreferencesServiceImpl implements PreferencesService {

    /** This is the system preferences tree. */
    protected PreferencesImpl systemTree;

    /** This is the map containing the user preferences trees. */
    protected final Map trees = new HashMap();

    /** The service id for the bundle this service belongs to. */
    protected final Long bundleId;

    /** The backing store manager. */
    protected final BackingStoreManager storeManager;

    public PreferencesServiceImpl(Long id,
                                  BackingStoreManager storeManager) {
        this.bundleId = id;
        this.storeManager = storeManager;
        try {
            // load prefs first
            PreferencesImpl[] prefs = null;
            prefs = this.storeManager.getStore().loadAll(storeManager, this.bundleId);
            for(int i=0;i<prefs.length;i++) {
                if ( prefs[i].getDescription().getIdentifier() == null ) {
                    this.systemTree = prefs[i];
                } else {
                    this.trees.put(prefs[i].getDescription().getIdentifier(), prefs[i]);
                }
            }
        } catch (BackingStoreException e) {
            // we ignore this here
        }
    }

    /**
     * @see org.osgi.service.prefs.PreferencesService#getSystemPreferences()
     */
    public synchronized Preferences getSystemPreferences() {
        if ( this.systemTree == null ) {
            this.systemTree = new PreferencesImpl(new PreferencesDescription(this.bundleId, null), this.storeManager);
        }
        // sync with latest version from store
        try {
            this.systemTree.sync();
        } catch (BackingStoreException ignore) {
            // we ignore this
        }
        return this.systemTree;
    }

    /**
     * @see org.osgi.service.prefs.PreferencesService#getUserPreferences(java.lang.String)
     */
    public synchronized Preferences getUserPreferences(String name) {
        PreferencesImpl result = (PreferencesImpl) this.trees.get(name);
        // if the tree does not exist yet, create it
        if (result == null || !result.isValid()) {
            result = new PreferencesImpl(new PreferencesDescription(this.bundleId, name), this.storeManager);
            this.trees.put(name, result);
        }
        // sync with latest version from store
        try {
            result.sync();
        } catch (BackingStoreException ignore) {
            // we ignore this
        }
        return result;
    }

    /**
     * @see org.osgi.service.prefs.PreferencesService#getUsers()
     */
    public synchronized String[] getUsers() {
        // TODO - we have to sync with the store
        final Set userKeys = this.trees.keySet();
        return (String[])userKeys.toArray(new String[userKeys.size()]);
    }

    protected List getAllPreferences() {
        final List list = new ArrayList();
        if ( this.systemTree != null ) {
            list.add(this.systemTree);
        }
        list.addAll(this.trees.values());
        return list;
    }
}
