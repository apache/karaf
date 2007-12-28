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
 * This is an abstract implementation of a backing store
 * which uses streams to read/write the preferences and
 * stores a complete preferences tree in a single stream.
 */
public abstract class StreamBackingStoreImpl implements BackingStore {

    /** The bundle context. */
    protected final BundleContext bundleContext;

    public StreamBackingStoreImpl(BundleContext context) {
        this.bundleContext = context;
    }

    /**
     * This method is invoked to check if the backing store is accessible right now.
     * @throws BackingStoreException
     */
    protected abstract void checkAccess() throws BackingStoreException;

    /**
     * Get the output stream to write the preferences.
     */
    protected abstract OutputStream getOutputStream(PreferencesDescription desc)
    throws IOException;

    /**
     * @see org.apache.felix.prefs.BackingStore#store(org.apache.felix.prefs.PreferencesImpl)
     */
    public void store(PreferencesImpl prefs) throws BackingStoreException {
        // do we need to store at all?
        if ( !this.hasChanges(prefs) ) {
            return;
        }
        this.checkAccess();
        // load existing data
        final PreferencesImpl savedData = this.load(prefs.getBackingStoreManager(), prefs.getDescription());
        if ( savedData != null ) {
            // merge with saved version
            final PreferencesImpl n = savedData.getOrCreateNode(prefs.absolutePath());
            n.applyChanges(prefs);
            prefs = n;
        }
        final PreferencesImpl root = prefs.getRoot();
        try {
            final OutputStream os = this.getOutputStream(root.getDescription());
            this.write(root, os);
            os.close();
        } catch (IOException ioe) {
            throw new BackingStoreException("Unable to store preferences.", ioe);
        }
    }

    /**
     * Has the tree changes?
     */
    protected boolean hasChanges(PreferencesImpl prefs) {
        if ( prefs.getChangeSet().hasChanges() ) {
            return true;
        }
        final Iterator i = prefs.getChildren().iterator();
        while ( i.hasNext() ) {
            final PreferencesImpl current = (PreferencesImpl) i.next();
            if ( this.hasChanges(current) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#update(org.apache.felix.prefs.PreferencesImpl)
     */
    public void update(PreferencesImpl prefs) throws BackingStoreException {
        final PreferencesImpl root = this.load(prefs.getBackingStoreManager(), prefs.getDescription());
        if ( root != null ) {
            // and now update
            if ( root.nodeExists(prefs.absolutePath()) ) {
                final PreferencesImpl updated = (PreferencesImpl)root.node(prefs.absolutePath());
                prefs.update(updated);
            }
        }
    }

    /**
     * Write the preferences recursively to the output stream.
     * @param prefs
     * @param os
     * @throws IOException
     */
    protected void write(PreferencesImpl prefs, OutputStream os)
    throws IOException {
        this.writePreferences(prefs, os);
        final ObjectOutputStream oos = new ObjectOutputStream(os);
        final Collection children = prefs.getChildren();
        oos.writeInt(children.size());
        oos.flush();
        final Iterator i = children.iterator();
        while ( i.hasNext() ) {
            final PreferencesImpl child = (PreferencesImpl) i.next();
            final byte[] name = child.name().getBytes("utf-8");
            oos.writeInt(name.length);
            oos.write(name);
            oos.flush();
            this.write(child, os);
        }
    }

    /**
     * Read the preferences recursively from the input stream.
     * @param prefs
     * @param is
     * @throws IOException
     */
    protected void read(PreferencesImpl prefs, InputStream is)
    throws IOException {
        this.readPreferences(prefs, is);
        final ObjectInputStream ois = new ObjectInputStream(is);
        final int numberOfChilren = ois.readInt();
        for(int i=0; i<numberOfChilren; i++) {
            int length = ois.readInt();
            final byte[] name = new byte[length];
            ois.readFully(name);
            final PreferencesImpl impl = (PreferencesImpl)prefs.node(new String(name, "utf-8"));
            this.read(impl, is);
        }
    }

    /**
     * Load this preferences from an input stream.
     * Currently the prefs are read from an object input stream and
     * the serialization is done by hand.
     * The changeSet is neither updated nor cleared in order to provide
     * an update/sync functionality. This has to be done at a higher level.
     */
    protected void readPreferences(PreferencesImpl prefs, InputStream in) throws IOException {
        final ObjectInputStream ois = new ObjectInputStream(in);
        final int size = ois.readInt();
        for(int i=0; i<size; i++) {
            int keyLength = ois.readInt();
            int valueLength = ois.readInt();
            final byte[] key = new byte[keyLength];
            final byte[] value = new byte[valueLength];
            ois.readFully(key);
            ois.readFully(value);
            prefs.getProperties().put(new String(key, "utf-8"), new String(value, "utf-8"));
        }
    }

    /**
     * Save this preferences to an output stream.
     * Currently the prefs are written through an object output
     * stream with handmade serialization of strings.
     * The changeSet is neither updated nor cleared in order to provide
     * an update/sync functionality. This has to be done at a higher level.
     */
    protected void writePreferences(PreferencesImpl prefs, OutputStream out) throws IOException {
        final ObjectOutputStream oos = new ObjectOutputStream(out);
        final int size = prefs.getProperties().size();
        oos.writeInt(size);
        final Iterator i = prefs.getProperties().entrySet().iterator();
        while ( i.hasNext() ) {
             final Map.Entry entry = (Map.Entry)i.next();
             final byte[] key = entry.getKey().toString().getBytes("utf-8");
             final byte[] value = entry.getValue().toString().getBytes("utf-8");
             oos.writeInt(key.length);
             oos.writeInt(value.length);
             oos.write(key);
             oos.write(value);
        }
        oos.flush();
    }
}
