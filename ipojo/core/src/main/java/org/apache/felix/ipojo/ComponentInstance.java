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
package org.apache.felix.ipojo;

import java.util.Dictionary;

import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.osgi.framework.BundleContext;

/**
 * This class defines the iPOJO's component instance concept.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentInstance {

    /**
     * Component Instance State : DISPOSED. The component instance was disposed.
     */
    int DISPOSED = -1;
    
    /**
     * Component Instance State : STOPPED. The component instance is not
     * started.
     */
    int STOPPED = 0;

    /**
     * Component Instance State : INVALID. The component instance is invalid when it
     * starts or when a component dependency is invalid.
     */
    int INVALID = 1;

    /**
     * Component Instance State : VALID. The component instance is resolved when it is
     * running and all its attached handlers are valid.
     */
    int VALID = 2;

    /**
     * Starts the component instance.
     */
    void start();

    /**
     * Stops the component instance.
     * A stopped instance can be re-started.
     */
    void stop();
    
    /**
     * Disposes the component instance.
     * A disposed instance cannot be re-started.
     */
    void dispose();

    /**
     * Returns the actual state of the instance. 
     * @return the actual state of the component instance.
     */
    int getState();

    /**
     * Returns the instance description.
     * @return the instance description of the current instance
     */
    InstanceDescription getInstanceDescription();

    /**
     * Returns the factory who created this instance.
     * @return the factory of the component instance.
     */
    ComponentFactory getFactory();

    /**
     * Returns the bundle context of this instance.
     * @return the context of the component instance
     */
    BundleContext getContext();

    /**
     * Returns the name of the instance.
     * @return the name of the component instance
     */
    String getInstanceName();

    /**
     * Checks if the instance is started.
     * @return <code>true</code> if {@link ComponentInstance#getState()} 
     * returns {@link ComponentInstance#INVALID} or {@link ComponentInstance#VALID}.
     */
    boolean isStarted();

    /**
     * Re-configures an instance. Do nothing if the instance does not support
     * dynamic reconfiguration. The reconfiguration does not stop the instance.
     * @param configuration the new configuration.
     */
    void reconfigure(Dictionary configuration);
    
    /**
     * Adds an instance state listener on the current instance.
     * @param listener the listener to add.
     */
    void addInstanceStateListener(InstanceStateListener listener);
    
    /**
     * Removes an instance state listener on the current instance.
     * @param listener the listener to remove.
     */
    void removeInstanceStateListener(InstanceStateListener listener);

}
