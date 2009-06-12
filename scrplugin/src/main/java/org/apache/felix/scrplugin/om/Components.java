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
package org.apache.felix.scrplugin.om;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>Components</code>...
 *
 * Components is just a collection of {@link Component}s.
 */
public class Components {

    /** The spec version. */
    private int specVersion;

    /** The list of {@link Component}s. */
    protected List<Component> components = new ArrayList<Component>();

    /**
     * Return the list of {@link Component}s.
     */
    public List<Component> getComponents() {
        return this.components;
    }

    /**
     * Set the list of {@link Component}s.
     */
    public void setComponents(List<Component> components) {
        this.components = components;
    }

    /**
     * Add a component to the list.
     */
    public void addComponent(Component component) {
        this.components.add(component);
    }

    /**
     * Get the spec version.
     */
    public int getSpecVersion() {
        return this.specVersion;
    }

    /**
     * Set the spec version.
     */
    public void setSpecVersion(int value) {
        this.specVersion = value;
    }
}
