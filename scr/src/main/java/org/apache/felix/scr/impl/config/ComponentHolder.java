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
package org.apache.felix.scr.impl.config;


import java.util.Dictionary;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;


/**
 * The <code>ComponentHolder</code> interface provides the API for supporting
 * component instances configured through either singleton configurations (or
 * no configuration at all) and factory configurations.
 * <p>
 * Instances of this interface are managed by the {@link ConfigurationSupport}
 * class on behalf of the
 * {@link org.apache.felix.scr.impl.BundleComponentActivator} and the
 * {@link org.apache.felix.scr.impl.ComponentRegistry}.
 */
public interface ComponentHolder
{

    /**
     * Returns the {@link BundleComponentActivator} owning this component
     * holder.
     */
    BundleComponentActivator getActivator();


    /**
     * Returns the {@link ComponentMetadata} describing and declaring this
     * component.
     */
    ComponentMetadata getComponentMetadata();


    /**
     * The configuration with the given PID has been deleted from the
     * Configuration Admin service.
     *
     * @param pid The PID of the deleted configuration
     */
    void configurationDeleted( String pid );


    /**
     * Configure a component with configuration from the given PID.
     *
     * @param pid The PID of the configuration used to configure the component
     */
    void configurationUpdated( String pid, Dictionary props );


    /**
     * Enables all components of this holder.
     */
    void enableComponents();


    /**
     * Disables all components of this holder.
     */
    void disableComponents();


    /**
     * Disposes off all components of this holder.
     * @param reason
     */
    void disposeComponents( int reason );


    /**
     * Informs the holder that the component has been disposed as a result of
     * calling the dispose method.
     */
    void disposed( ImmediateComponentManager component );
}
