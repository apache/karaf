/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.servicebinder.architecture;

import org.apache.felix.servicebinder.DependencyMetadata;

/**
 * Interface for a dependency
 *
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public interface Dependency
{
    /**
     * get the dependency state
     *
     * @return the state of the dependency
    **/
    public int getDependencyState();

    /**
     * get the dependency metadata
     *
     * @return the metadata of the dependency
    **/
    public DependencyMetadata getDependencyMetadata();

    /**
     * get the bound service objects
     *
     * @return the bound Instances
    **/
    public Instance []getBoundInstances();
}
