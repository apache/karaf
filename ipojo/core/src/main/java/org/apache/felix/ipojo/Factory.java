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
import java.util.List;

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * Component Type Factory Service. This service is exposed by a instance manager factory, and allows the dynamic creation of component instance.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Factory {

    /**
     * Factory State. A valid factory is a factory where all required handlers are available.
     */
    int VALID = 1;

    /**
     * Factory State. An invalid factory is a factory where at least one required handler is unavailable. Creating an instance with an invalid factory failed.
     */
    int INVALID = 0;

    /**
     * Create an instance manager (i.e. component type instance).
     * @param configuration : the configuration properties for this component.
     * @return the created instance manager.
     * @throws UnacceptableConfiguration : when a given configuration is not valid.
     * @throws MissingHandlerException : when an handler is missing.
     * @throws ConfigurationException : when the instance configuration failed.
     */
    ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException;

    /**
     * Create an instance manager (i.e. component type instance). This has these service interaction in the scope given in argument.
     * @param configuration : the configuration properties for this component.
     * @param serviceContext : the service context of the component.
     * @return the created instance manager.
     * @throws UnacceptableConfiguration : when the given configuration is not valid.
     * @throws MissingHandlerException : when an handler is missing.
     * @throws ConfigurationException : when the instance configuration failed.
     */
    ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException;

    /**
     * Get the component type information containing provided service, configuration properties ...
     * @return the component type information.
     */
    Element getDescription();

    /**
     * Get the component type description.
     * @return the component type description object
     */
    ComponentTypeDescription getComponentDescription();

    /**
     * Check if the given configuration is acceptable as a configuration of a component instance.
     * @param conf : the configuration to test
     * @return true if the configuration is acceptable
     */
    boolean isAcceptable(Dictionary conf);

    /**
     * Return the factory name.
     * @return the name of the factory.
     */
    String getName();

    /**
     * Reconfigure an instance already created. This configuration need to have the name property to identify the instance.
     * @param conf : the configuration to reconfigure the instance.
     * @throws UnacceptableConfiguration : if the given configuration is not consistent for the targeted instance.
     * @throws MissingHandlerException : when an handler is missing.
     */
    void reconfigure(Dictionary conf) throws UnacceptableConfiguration, MissingHandlerException;

    /**
     * Add a factory state listener on the current factory.
     * @param listener : the listener to add
     */
    void addFactoryStateListener(FactoryStateListener listener);

    /**
     * Remove the given factory state listener from the listener list.
     * @param listener : the listener to remove
     */
    void removeFactoryStateListener(FactoryStateListener listener);

    /**
     * Get the list of missing handlers.
     * @return the list containing the name of missing handlers (Name : namespace:name)
     */
    List getMissingHandlers();

    /**
     * Get the list of required handlers.
     * @return the list containing the name of required handlers (Name : namespace:name)
     */
    List getRequiredHandlers();

    /**
     * Return the classname of the component type.
     * For factories which does not contains a class, return "composite"
     * @return the class name of the component type or "composite"
     */
    String getClassName();

    /**
     * Return the state of the factory.
     * @return the state of the factory
     */
    int getState();

    /**
     * Get the bundle context of the factory.
     * @return the bundle context of the factory.
     */
    BundleContext getBundleContext();

}
