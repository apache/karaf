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

import java.io.*;
import java.util.*;

import org.apache.felix.prefs.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This implementating of the backing store uses the bundle mechanism to store
 * binary data.
 */
public class DataFileBackingStoreImpl extends StreamBackingStoreImpl {

    /** The root directory (or null if not available) */
    protected final File rootDirectory;

    public DataFileBackingStoreImpl(BundleContext context) {
        super(context);
        this.rootDirectory = context.getDataFile("");
    }

    /**
     * @see org.apache.felix.sandbox.preferences.impl.StreamBackingStoreImpl#checkAccess()
     */
    protected void checkAccess() throws BackingStoreException {
        if ( this.rootDirectory == null ) {
            throw new BackingStoreException("Saving of data files to the bundle context is currently not supported.");
        }
    }

    /**
     * @see org.apache.felix.sandbox.preferences.impl.StreamBackingStoreImpl#getOutputStream(org.apache.felix.sandbox.preferences.PreferencesDescription)
     */
    protected OutputStream getOutputStream(PreferencesDescription desc) throws IOException {
        final File file = this.getFile(desc);
        return new FileOutputStream(file);
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#availableBundles()
     */
    public Long[] availableBundles() {
         // If the root directory is not available, then we do nothing!
        try {
            this.checkAccess();
        } catch (BackingStoreException ignore) {
            return new Long[0];
        }
        final Set bundleIds = new HashSet();
        final File[] children = this.rootDirectory.listFiles();
        for( int i=0; i<children.length; i++ ) {
            final File current = children[i];

            final PreferencesDescription desc = this.getDescription(current);
            if ( desc != null ) {
                bundleIds.add(desc.getBundleId());
            }
        }
        return (Long[])bundleIds.toArray(new Long[bundleIds.size()]);
    }

    protected PreferencesDescription getDescription(File file) {
        final String fileName = file.getName();
        // parse the file name to get: bundle id, user|system identifer
        if ( fileName.startsWith("P") && fileName.endsWith(".ser") ) {
            final String name = fileName.substring(1, fileName.length() - 4);
            final String key;
            final String identifier;
            int pos = name.indexOf("_");
            if ( pos != -1 ) {
                identifier = name.substring(pos+1);
                key = name.substring(0, pos);
            } else {
                key = name;
                identifier = null;
            }
            final Long bundleId = Long.valueOf(key);
            return new PreferencesDescription(bundleId, identifier);
        }
        return null;
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#remove(java.lang.Long)
     */
    public void remove(Long bundleId) throws BackingStoreException {
        this.checkAccess();
        final File[] children = this.rootDirectory.listFiles();
        for( int i=0; i<children.length; i++ ) {
            final File current = children[i];

            final PreferencesDescription desc = this.getDescription(current);
            if ( desc != null ) {
                if ( desc.getBundleId().equals(bundleId) ) {
                    current.delete();
                }
            }
        }
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#loadAll(org.apache.felix.prefs.BackingStoreManager, java.lang.Long)
     */
    public PreferencesImpl[] loadAll(BackingStoreManager manager, Long bundleId) throws BackingStoreException {
        this.checkAccess();
        final List list = new ArrayList();
        final File[] children = this.rootDirectory.listFiles();
        for( int i=0; i<children.length; i++ ) {
            final File current = children[i];

            final PreferencesDescription desc = this.getDescription(current);
            if ( desc != null ) {
                if ( desc.getBundleId().equals(bundleId) ) {
                    final PreferencesImpl root = new PreferencesImpl(desc, manager);
                    try {
                        final FileInputStream fis = new FileInputStream(current);
                        this.read(root, fis);
                        fis.close();
                    } catch (IOException ioe) {
                        throw new BackingStoreException("Unable to load preferences.", ioe);
                    }
                    list.add(root);
                }
            }
        }
        return (PreferencesImpl[])list.toArray(new PreferencesImpl[list.size()]);
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#load(org.apache.felix.prefs.BackingStoreManager, org.apache.felix.prefs.PreferencesDescription)
     */
    public PreferencesImpl load(BackingStoreManager manager, PreferencesDescription desc) throws BackingStoreException {
        this.checkAccess();
        final File file = this.getFile(desc);
        if ( file.exists() ) {
            try {
                final PreferencesImpl root = new PreferencesImpl(desc, manager);
                final FileInputStream fis = new FileInputStream(file);
                this.read(root, fis);
                fis.close();

                return root;
            } catch (IOException ioe) {
                throw new BackingStoreException("Unable to load preferences.", ioe);
            }
        }
        return null;
    }

    /**
     * Get the file fo the preferences tree.
     * @param desc
     * @return
     */
    protected File getFile(PreferencesDescription desc) {
        final StringBuffer buffer = new StringBuffer("P");
        buffer.append(desc.getBundleId());
        if ( desc.getIdentifier() != null ) {
            buffer.append('_');
            buffer.append(desc.getIdentifier());
        }
        buffer.append(".ser");
        final File file = new File(this.rootDirectory, buffer.toString());
        return file;
    }
}
