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

import org.osgi.framework.BundleContext;

/**
 * The component manager class manages one instance of a component type.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public interface ComponentManager {

    /**
     * Component State : INVALID.
     * The component is invalid when it start or when a component dependency is unvalid.
     */
    int INVALID = 1;

    /**
     * Component State : VALID.
     * The component is resolved when it is running and all its component dependencies are valid.
     */
    int VALID = 2;

    /**
     * Start the component manager.
     */
    void start();

    /**
     * Stop the component manager.
     */
    void stop();

    /**
     * @return the actual state of the component.
     */
    int getState();

    /**
     * @return the component type information.
     */
    ComponentInfo getComponentInfo();

    /**
     * @return the component metadata.
     */
    ComponentMetadata getComponentMetatada();

    /**
     * @return the factory of the component
     */
    ComponentManagerFactory getFactory();
    
    /**
     * @return the context of the component manager
     */
    BundleContext getContext();
    
    /**
     * @return the name of the component instance
     */
    String getName();


}
