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
package org.apache.felix.scr;


import java.util.Dictionary;

import org.osgi.framework.Bundle;


/**
 * The <code>Component</code> interface represents a single component managed
 * by the Service Component Runtime. Management agents may access the Component
 * instances through the {@link ScrService}.
 */
public interface Component
{

    /**
     * The Component has just been created and is still disabled or it has
     * been disabled by calling the {@link #disable()} method (value is 1).
     */
    static final int STATE_DISABLED = 1;

    /**
     * The Component has been enabled and is now going to be activated (value
     * is 2).
     */
    static final int STATE_ENABLED = 2;

    /**
     * The Component activation failed because any dependency is not satisfied
     * (value is 4).
     */
    static final int STATE_UNSATISFIED = 4;

    /**
     * The Component is currently being activated either because it has been
     * enabled or because any dependency which was previously unsatisfied has
     * become satisfied (value is 8).
     */
    static final int STATE_ACTIVATING = 8;

    /**
     * The Component has successfully been activated and is fully functional
     * (value is 16). This is the state of immediate components after
     * successfull activation. Delayed and Service Factory Components enter
     * this state when the service instance has actually be instantiated because
     * the service has been acquired.
     */
    static final int STATE_ACTIVE = 16;

    /**
     * The Component has successfully been activated but is a Delayed or Service
     * Factory Component pending instantiation on first use (value is 32).
     */
    static final int STATE_REGISTERED = 32;

    /**
     * The Component is a Component Factory ready to manage Component instances
     * from configuration data received from the Configuration Admin Service
     * (value is 64).
     */
    static final int STATE_FACTORY = 64;

    /**
     * The Component is being deactivated either because it is being disabled
     * or because a dependency is not satisfied any more (value is 128). After
     * deactivation the Component enters the {@link #STATE_UNSATISFIED} state.
     */
    static final int STATE_DEACTIVATING = 128;

    /**
     * The Component has been destroyed and cannot be used any more (value is
     * 256). This state is only used when the bundle declaring the component
     * is being stopped and all components have to be removed.
     */
    static final int STATE_DESTROYED = 256;


    /**
     * Returns the component ID of this component. This ID is managed by the
     * SCR.
     */
    long getId();


    /**
     * Returns the name of the component, which is also used as the service PID.
     * This method provides access to the <code>name</code> attribute of the
     * <code>component</code> element.
     */
    String getName();


    /**
     * Returns the current state of the Component, which is one of the
     * <code>STATE_*</code> constants defined in this interface.
     */
    int getState();


    /**
     * Returns the <code>Bundle</code> declaring this component.
     */
    Bundle getBundle();


    /**
     * Returns the component factory name or <code>null</code> if this component
     * is not defined as a component factory. This method provides access to
     * the <code>factory</code> attribute of the <code>component</code>
     * element.
     */
    String getFactory();


    /**
     * Returns <code>true</code> if this component is a service factory. This
     * method returns the value of the <code>serviceFactory</code> attribute of
     * the <code>service</code> element. If the component has no service
     * element, this method returns <code>false</code>.
     */
    boolean isServiceFactory();


    /**
     * Returns the class name of the Component implementation. This method
     * provides access to the <code>class</code> attribute of the
     * <code>implementation</code> element.
     */
    String getClassName();


    /**
     * Returns whether the Component is declared to be enabled initially. This
     * method provides access to the <code>enabled</code> attribute of the
     * <code>component</code> element.
     */
    boolean isDefaultEnabled();


    /**
     * Returns whether the Component is an Immediate or a Delayed Component.
     * This method provides access to the <code>immediate</code> attribute of
     * the <code>component</code> element.
     */
    boolean isImmediate();


    /**
     * Returns an array of service names provided by this Component or
     * <code>null</code> if the Component is not registered as a service. This
     * method provides access to the <code>interface</code> attributes of the
     * <code>provide</code> elements.
     */
    String[] getServices();


    /**
     * Returns the properties of the Component. The Dictionary returned is a
     * private copy of the actual properties and contains the same entries as
     * are used to register the Component as a service and are returned by
     * the <code>ComponentContext.getProperties()</code> method.
     */
    Dictionary getProperties();


    /**
     * Returns an array of {@link Reference} instances representing the service
     * references (or dependencies) of this Component. If the Component has no
     * references, <code>null</code> is returned.
     */
    Reference[] getReferences();


    /**
     * Enables this Component if it is disabled. If the Component is not
     * currently {@link #STATE_DISABLED disabled} this method has no effect. If
     * the Component is {@link #STATE_DESTROYED destroyed}, this method throws
     * an <code>IllegalStateException</code>.
     *
     * @throws IllegalStateException If the Component is destroyed.
     */
    void enable();


    /**
     * Disables this Component if it is enabled. If the Component is already
     * {@link #STATE_DISABLED disabled} this method has no effect. If the
     * Component is {@link #STATE_DESTROYED destroyed}, this method throws an
     * <code>IllegalStateException</code>.
     *
     * @throws IllegalStateException If the Component is destroyed.
     */
    void disable();

}
