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

import org.apache.felix.servicebinder.InstanceMetadata;

/**
 *
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public interface Instance
{
    public static final int INSTANCE_CREATED = 0;
    public static final int INSTANCE_VALID = 1;
    public static final int INSTANCE_INVALID = 2;
    public static final int INSTANCE_DESTROYED = 3;

    /**
     * Get the state of the instance
     *
     * @return an integer representing the state of the instance
    **/
    public int getState();

    /**
     * Get the bundle
     *
     * @return an integer with the bundle id
    **/
     public long getBundleId();

    /**
     * Get a list of depenencies
     *
     * @return a List containing all of the dependencies
    **/
    public Dependency[] getDependencies();

    /**
     * Get a list of child instances in case this instance is a factory
     *
     * @return a List containing all of the child instances
    **/
    public Instance[] getChildInstances();

    /**
     * Get the instance metadata
     *
     * @return the isntance metadata
    **/
    public InstanceMetadata getInstanceMetadata();
}
