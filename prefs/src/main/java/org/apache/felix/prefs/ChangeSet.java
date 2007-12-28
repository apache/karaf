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

import java.util.*;

/**
 * This class keeps track of the changes to a preferences node.
 */
public class ChangeSet {

    /** Do we have changes at all? */
    protected boolean hasChanges = false;

    /** A set of changed/added properties. */
    protected final Set changedProperties = new HashSet();

    /** A set of removed properties. */
    protected final Set removedProperties = new HashSet();

    /** A set of added children. */
    protected final Set addedChildren = new HashSet();

    /** A set of removed children. */
    protected final Set removedChildren = new HashSet();

    /**
     * Do we have changes?
     * @return True if there are any changes.
     */
    public boolean hasChanges() {
        return this.hasChanges;
    }

    /**
     * Inform that a property has been added/changed.
     * @param name The name of the property.
     */
    public void propertyChanged(String name) {
        this.hasChanges = true;
        this.removedProperties.remove(name);
        this.changedProperties.add(name);
    }

    /**
     * Inform that a property has removed.
     * @param name The name of the property.
     */
    public void propertyRemoved(String name) {
        this.hasChanges = true;
        this.changedProperties.remove(name);
        this.removedProperties.add(name);
    }

    /**
     * Inform that a child has been added.
     * @param name The name of the child.
     */
    public void childAdded(String name) {
        this.hasChanges = true;
        this.removedChildren.remove(name);
        this.addedChildren.add(name);
    }

    /**
     * Inform that a child has been removed.
     * @param name The name of the child.
     */
    public void childRemoved(String name) {
        this.hasChanges = true;
        this.addedChildren.remove(name);
        this.removedChildren.add(name);
    }

    /**
     * Reset state to unchanged.
     */
    public void clear() {
        this.hasChanges = false;
        this.removedChildren.clear();
        this.removedProperties.clear();
        this.addedChildren.clear();
        this.changedProperties.clear();
    }

    /**
     * Import the changes from the other change set.
     * @param other
     */
    public void importChanges(ChangeSet other) {
        if (other.hasChanges) {
            this.hasChanges = true;
            this.addedChildren.addAll(other.addedChildren);
            this.removedChildren.addAll(other.removedChildren);
            this.changedProperties.addAll(other.changedProperties);
            this.removedProperties.addAll(other.removedProperties);
        }
    }

    /**
     * Return a collection with the changed property names.
     * @return A collection.
     */
    public Collection getChangedProperties() {
        return Collections.unmodifiableCollection(this.changedProperties);
    }

    /**
     * Return a collection with the removed property names.
     * @return A collection.
     */
    public Collection getRemovedProperties() {
        return Collections.unmodifiableCollection(this.removedProperties);
    }

    /**
     * Return a collection with the added children names.
     * @return A collection.
     */
    public Collection getAddedChildren() {
        return Collections.unmodifiableCollection(this.addedChildren);
    }

    /**
     * Return a collection with the removed children names.
     * @return A collection.
     */
    public Collection getRemovedChildren() {
        return Collections.unmodifiableCollection(this.removedChildren);
    }
}
