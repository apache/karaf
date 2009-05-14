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

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This is an implementation of the preferences.
 *
 * The access to the preferences is synchronized on the instance
 * by making (nearly) all public methods synchronized. This avoids the
 * heavy management of a separate read/write lock. Such a lock
 * is too heavy for the simple operations preferences support.
 * The various getXX and putXX methods are not synchronized as they
 * all use the get/put methods which are synchronized.
 */
public class PreferencesImpl implements Preferences {

    /** The properties. */
    protected final Map properties = new HashMap();

    /** Has this node been removed? */
    protected boolean valid = true;

    /** The parent. */
    protected final PreferencesImpl parent;

    /** The child nodes. */
    protected final Map children = new HashMap();

    /** The name of the properties. */
    protected final String name;

    /** The description for this preferences. */
    protected final PreferencesDescription description;

    /** The backing store manager. */
    protected final BackingStoreManager storeManager;

    /** The change set keeps track of all changes. */
    protected final ChangeSet changeSet = new ChangeSet();

    /**
     * Construct the root node of the tree.
     * @param d The unique description.
     * @param storeManager The backing store.
     */
    public PreferencesImpl(PreferencesDescription d, BackingStoreManager storeManager) {
        this.parent = null;
        this.name = "";
        this.description = d;
        this.storeManager = storeManager;
    }

    /**
     * Construct a child node.
     * @param p The parent node.
     * @param name The node name
     */
    public PreferencesImpl(PreferencesImpl p, String name) {
        this.parent = p;
        this.name = name;
        this.description = p.description;
        this.storeManager = p.storeManager;
    }

    /**
     * Return the change set.
     */
    public ChangeSet getChangeSet() {
        return this.changeSet;
    }

    /**
     * Return the preferences description.
     * @return The preferences description.
     */
    public PreferencesDescription getDescription() {
        return this.description;
    }

    /**
     * Get the root preferences.
     */
    public PreferencesImpl getRoot() {
        PreferencesImpl root = this;
        while ( root.parent != null ) {
            root = root.parent;
        }
        return root;
    }

    /**
     * Return all children or an empty collection.
     * @return A collection containing the children.
     */
    public Collection getChildren() {
        return this.children.values();
    }

    /**
     * Return the properties set.
     */
    public Map getProperties() {
        return this.properties;
    }

    /**
     * Return the backing store manager.
     */
    public BackingStoreManager getBackingStoreManager() {
        return this.storeManager;
    }

    /**
     * Check if this node is still valid.
     * It gets invalid if it has been removed.
     */
    protected void checkValidity() throws IllegalStateException {
        if ( !this.valid ) {
            throw new IllegalStateException("The preferences node has been removed.");
        }
    }

    /**
     * The key is not allowed to be null.
     */
    protected void checkKey(String key) throws NullPointerException {
        if ( key == null ) {
            throw new NullPointerException("Key must not be null.");
        }
    }

    /**
     * The value is not allowed to be null.
     */
    protected void checkValue(Object value) throws NullPointerException {
        if ( value == null ) {
            throw new NullPointerException("Value must not be null.");
        }
    }

    public synchronized boolean isValid() {
        return this.valid;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#put(java.lang.String, java.lang.String)
     */
    public synchronized void put(String key, String value) {
        this.checkKey(key);
        this.checkValue(value);

        this.checkValidity();

        this.properties.put(key, value);
        this.changeSet.propertyChanged(key);
    }

    /**
     * @see org.osgi.service.prefs.Preferences#get(java.lang.String, java.lang.String)
     */
    public synchronized String get(String key, String def) {
        this.checkValidity();
        String value = (String) this.properties.get(key);
        if ( value == null ) {
            value = def;
        }
        return value;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#remove(java.lang.String)
     */
    public synchronized void remove(String key) {
        this.checkKey(key);
        this.checkValidity();

        this.properties.remove(key);
        this.changeSet.propertyRemoved(key);
    }

    /**
     * @see org.osgi.service.prefs.Preferences#clear()
     */
    public synchronized void clear() throws BackingStoreException {
        this.checkValidity();

        final Iterator i = this.properties.keySet().iterator();
        while ( i.hasNext() ) {
            final String key = (String)i.next();
            this.changeSet.propertyRemoved(key);
        }
        this.properties.clear();
    }

    /**
     * @see org.osgi.service.prefs.Preferences#putInt(java.lang.String, int)
     */
    public void putInt(String key, int value) {
        this.put(key, String.valueOf(value));
    }

    /**
     * @see org.osgi.service.prefs.Preferences#getInt(java.lang.String, int)
     */
    public int getInt(String key, int def) {
        int result = def;
        final String value = this.get(key, null);
        if ( value != null ) {
            try {
                result = Integer.parseInt(value);
            } catch (NumberFormatException ignore) {
                // return the default value
            }
        }
        return result;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#putLong(java.lang.String, long)
     */
    public void putLong(String key, long value) {
        this.put(key, String.valueOf(value));
    }

    /**
     * @see org.osgi.service.prefs.Preferences#getLong(java.lang.String, long)
     */
    public long getLong(String key, long def) {
        long result = def;
        final String value = this.get(key, null);
        if ( value != null ) {
            try {
                result = Long.parseLong(value);
            } catch (NumberFormatException ignore) {
                // return the default value
            }
        }
        return result;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#putBoolean(java.lang.String, boolean)
     */
    public void putBoolean(String key, boolean value) {
        this.put(key, String.valueOf(value));
    }

    /**
     * @see org.osgi.service.prefs.Preferences#getBoolean(java.lang.String, boolean)
     */
    public boolean getBoolean(String key, boolean def) {
        boolean result = def;
        final String value = this.get(key, null);
        if ( value != null ) {
            if ( value.equalsIgnoreCase("true") ) {
                result = true;
            } else if ( value.equalsIgnoreCase("false") ) {
                result = false;
            }
        }
        return result;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#putFloat(java.lang.String, float)
     */
    public void putFloat(String key, float value) {
        this.put(key, String.valueOf(value));
    }

    /**
     * @see org.osgi.service.prefs.Preferences#getFloat(java.lang.String, float)
     */
    public float getFloat(String key, float def) {
        float result = def;
        final String value = this.get(key, null);
        if ( value != null ) {
            try {
                result = Float.parseFloat(value);
            } catch (NumberFormatException ignore) {
                // return the default value
            }
        }
        return result;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#putDouble(java.lang.String, double)
     */
    public void putDouble(String key, double value) {
        this.put(key, String.valueOf(value));
    }

    /**
     * @see org.osgi.service.prefs.Preferences#getDouble(java.lang.String, double)
     */
    public double getDouble(String key, double def) {
        double result = def;
        final String value = this.get(key, null);
        if ( value != null ) {
            try {
                result = Double.parseDouble(value);
            } catch (NumberFormatException ignore) {
                // return the default value
            }
        }
        return result;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#putByteArray(java.lang.String, byte[])
     */
    public void putByteArray(String key, byte[] value) {
        this.checkKey(key);
        this.checkValue(value);
        try {
            this.put(key, new String(Base64.encodeBase64(value), "utf-8"));
        } catch (UnsupportedEncodingException ignore) {
            // utf-8 is always available
        }
    }

    /**
     * @see org.osgi.service.prefs.Preferences#getByteArray(java.lang.String, byte[])
     */
    public byte[] getByteArray(String key, byte[] def) {
        byte[] result = def;
        String value = this.get(key, null);
        if ( value != null ) {
            try {
                result = Base64.decodeBase64(value.getBytes("utf-8"));
            } catch (UnsupportedEncodingException ignore) {
                // utf-8 is always available
            }
        }
        return result;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#keys()
     */
    public synchronized String[] keys() throws BackingStoreException {
        this.sync();
        final Set keys = this.properties.keySet();
        return (String[])keys.toArray(new String[keys.size()]);
    }

    /**
     * @see org.osgi.service.prefs.Preferences#childrenNames()
     */
    public synchronized String[] childrenNames() throws BackingStoreException {
        this.sync();
        final Set names = this.children.keySet();
        return (String[])names.toArray(new String[names.size()]);
    }

    /**
     * @see org.osgi.service.prefs.Preferences#parent()
     */
    public Preferences parent() {
        this.checkValidity();
        return this.parent;
    }

    /**
     * We do not synchronize this method to avoid dead locks as this
     * method might call another preferences object in the hierarchy.
     * @see org.osgi.service.prefs.Preferences#node(java.lang.String)
     */
    public Preferences node(String pathName) {
        if ( pathName == null ) {
            throw new NullPointerException("Path must not be null.");
        }
        PreferencesImpl executingNode= this;
        synchronized ( this ) {
            this.checkValidity();
            if ( pathName.length() == 0 ) {
                return this;
            }
            if ( pathName.startsWith("/") && this.parent != null ) {
                executingNode = this.getRoot();
            }
            if ( pathName.startsWith("/") ) {
                pathName = pathName.substring(1);
            }
        }
        return executingNode.getNode(pathName, true, true);
    }

    /**
     * Get or create the node.
     * If the node already exists, it's just returned. If not
     * it is created.
     * @param pathName
     * @return The preferences impl for the path.
     */
    public PreferencesImpl getOrCreateNode(String pathName) {
        if ( pathName == null ) {
            throw new NullPointerException("Path must not be null.");
        }
        PreferencesImpl executingNode= this;
        if ( pathName.length() == 0 ) {
            return this;
        }
        if ( pathName.startsWith("/") && this.parent != null ) {
            executingNode = this.getRoot();
        }
        if ( pathName.startsWith("/") ) {
            pathName = pathName.substring(1);
        }
        return executingNode.getNode(pathName, false, true);
    }

    /**
     * Get a relative node.
     * @param path
     * @return
     */
    protected PreferencesImpl getNode(String path, boolean saveNewlyCreatedNode, boolean create) {
        if ( path.startsWith("/") ) {
            throw new IllegalArgumentException("Path must not contained consecutive slashes");
        }
        if ( path.length() == 0 ) {
            return this;
        }
        synchronized ( this ) {
            this.checkValidity();

            String subPath = null;
            int pos = path.indexOf('/');
            if ( pos != -1 ) {
                subPath = path.substring(pos+1);
                path = path.substring(0, pos);
            }
            boolean save = false;
            PreferencesImpl child = (PreferencesImpl) this.children.get(path);
            if ( child == null ) {
                if ( !create ) {
                    return null;
                }
                child = new PreferencesImpl(this, path);
                this.children.put(path, child);
                this.changeSet.childAdded(path);
                if ( saveNewlyCreatedNode ) {
                    save = true;
                }
                saveNewlyCreatedNode = false;
            }
            final PreferencesImpl result;
            if ( subPath == null ) {
                result = child;
            } else {
                result = child.getNode(subPath, saveNewlyCreatedNode, create);
            }
            if ( save ) {
                try {
                    result.flush();
                } catch (BackingStoreException ignore) {
                    // we ignore this for now
                }
            }
            return result;
        }
    }

    /**
     * We do not synchronize this method to avoid dead locks as this
     * method might call another preferences object in the hierarchy.
     * @see org.osgi.service.prefs.Preferences#nodeExists(java.lang.String)
     */
    public boolean nodeExists(String pathName) throws BackingStoreException {
        if ( pathName == null ) {
            throw new NullPointerException("Path must not be null.");
        }
        if ( pathName.length() == 0 ) {
            return this.valid;
        }
        PreferencesImpl node = this;
        synchronized ( this ) {
            this.checkValidity();
            if ( pathName.startsWith("/") && this.parent != null ) {
                node = this.getRoot();
            }
            if ( pathName.startsWith("/") ) {
                pathName = pathName.substring(1);
            }
        }
        final Preferences searchNode = node.getNode(pathName, false, false);
        return searchNode != null;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#removeNode()
     */
    public void removeNode() throws BackingStoreException {
        this.checkValidity();
        this.safelyRemoveNode();

        if ( this.parent != null ) {
            this.parent.removeChild(this);
        }
    }

    /**
     * Safely remove a node by resetting all properties and calling
     * this method on all children recursively.
     */
    protected void safelyRemoveNode() {
        if ( this.valid ) {
            Collection c = null;
            synchronized ( this ) {
                this.valid = false;
                this.properties.clear();
                c = new ArrayList(this.children.values());
                this.children.clear();
            }
            final Iterator i = c.iterator();
            while ( i.hasNext() ) {
                final PreferencesImpl child = (PreferencesImpl) i.next();
                child.safelyRemoveNode();
            }
        }
    }

    protected synchronized void removeChild(PreferencesImpl child) {
        this.children.remove(child.name());
        this.changeSet.childRemoved(child.name());
    }

    /**
     * @see org.osgi.service.prefs.Preferences#name()
     */
    public String name() {
        return this.name;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#absolutePath()
     */
    public String absolutePath() {
        if (this.parent == null) {
            return "/";
        }
        final String parentPath = this.parent.absolutePath();
        if ( parentPath.length() == 1 ) {
            return parentPath + this.name;
        }
        return parentPath + '/' + this.name;
    }

    /**
     * @see org.osgi.service.prefs.Preferences#flush()
     */
    public synchronized void flush() throws BackingStoreException {
        this.checkValidity();
        this.storeManager.getStore().store(this);
        this.changeSet.clear();
    }

    /**
     * @see org.osgi.service.prefs.Preferences#sync()
     */
    public synchronized void sync() throws BackingStoreException {
        this.checkValidity();
        this.storeManager.getStore().update(this);
        this.storeManager.getStore().store(this);
    }

    /**
     * Update from the preferences impl.
     * @param impl
     */
    public void update(PreferencesImpl impl) {
        final Iterator i = impl.properties.entrySet().iterator();
        while ( i.hasNext() ) {
            final Map.Entry entry = (Map.Entry)i.next();
            if ( !this.properties.containsKey(entry.getKey()) ) {
                this.properties.put(entry.getKey(), entry.getValue());
            }
        }
        final Iterator cI = impl.children.entrySet().iterator();
        while ( cI.hasNext() ) {
            final Map.Entry entry = (Map.Entry)cI.next();
            final String name = entry.getKey().toString();
            final PreferencesImpl child = (PreferencesImpl)entry.getValue();
            if ( !this.children.containsKey(name) ) {
                // create node
                this.node(name);
            }
            ((PreferencesImpl)this.children.get(name)).update(child);
        }
    }

    /***
     * Apply the changes done to the passed preferences object.
     * @param prefs
     */
    public void applyChanges(PreferencesImpl prefs) {
        final ChangeSet changeSet = prefs.getChangeSet();
        if ( changeSet.hasChanges ) {
            this.changeSet.importChanges(prefs.changeSet);
            Iterator i;
            // remove properties
            i = changeSet.removedProperties.iterator();
            while ( i.hasNext() ) {
                this.properties.remove(i.next());
            }
            // set/update properties
            i = changeSet.changedProperties.iterator();
            while ( i.hasNext() ) {
                final String key = (String)i.next();
                this.properties.put(key, prefs.properties.get(key));
            }
            // remove children
            i = changeSet.removedChildren.iterator();
            while ( i.hasNext() ) {
                final String name = (String)i.next();
                this.children.remove(name);
            }
            // added childs are processed in the next loop
        }
        final Iterator cI = prefs.getChildren().iterator();
        while ( cI.hasNext() ) {
            final PreferencesImpl current = (PreferencesImpl)cI.next();
            final PreferencesImpl child = this.getOrCreateNode(current.name());
            child.applyChanges(current);
        }
    }
}
