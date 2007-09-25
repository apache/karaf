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
 * The component instance class manages one instance of a component type.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentInstance {

    /**
     * Component Instance State : DISPOSED. The instance was destroyed.
     */
    int DISPOSED = -1;
    
    /**
     * Component Instance State : STOPPED. The component instance is not
     * started.
     */
    int STOPPED = 0;

    /**
     * Component Instance State : INVALID. The component is invalid when it
     * start or when a component dependency is invalid.
     */
    int INVALID = 1;

    /**
     * Component Instance State : VALID. The component is resolved when it is
     * running and all its component dependencies are valid.
     */
    int VALID = 2;

    /**
     * Start the component instance.
     */
    void start();

    /**
     * Stop the component instance.
     * A stopped instance can be re-started.
     */
    void stop();
    
    /**
     * Dispose the component instance.
     * A disposed instance cannot be re-started.
     */
    void dispose();

    /**
     * Return the actual state of the instance. 
     * @return the actual state of the component instance.
     */
    int getState();

    /**
     * Return the instance description.
     * @return the instance description of the current instance
     */
    InstanceDescription getInstanceDescription();

    /**
     * Return the factory which create this instance.
     * @return the factory of the component instance.
     */
    ComponentFactory getFactory();

    /**
     * Return the bundle context of this instance.
     * @return the context of the component instance
     */
    BundleContext getContext();

    /**
     * Return the name of the instance.
     * @return the name of the component instance
     */
    String getInstanceName();

    /**
     * Check if the instance is started.
     * @return true if getState returns INVALID or VALID.
     */
    boolean isStarted();

    /**
     * Re-configure an instance. Do nothing if the instance does not support
     * dynamic reconfiguration. The reconfiguration does not stop the instance.
     * 
     * @param configuration : the new configuration.
     */
    void reconfigure(Dictionary configuration);
    
    /**
     * Add an instance state listener on the current instance.
     * @param listener : the listener to add.
     */
    void addInstanceStateListener(InstanceStateListener listener);
    
    /**
     * Remove an instance state listener on the current instance.
     * @param listener : the listener to remove.
     */
    void removeInstanceStateListener(InstanceStateListener listener);

}
