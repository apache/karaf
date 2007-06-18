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
package org.apache.felix.servicebinder;

/**
 * This interface creates a level of indirection for the objects
 * created by a factory. This is necessary because it might not
 * be possible for a factory to create the actual object instance
 * at the time of the call to <tt>Factory.createInstance()</tt>
 * due to unfulfilled dependencies. In such a scenario, this
 * interface can be used to listen for the object instance to
 * become available.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a> 
 */
public interface InstanceReference
{
    public static final String INSTANCE_STATE = "INSTANCE_STATE";
    public static final String INSTANCE_METADATA = "INSTANCE_METADATA";
    public static final String INSTANCE_BUNDLE = "INSTANCE_BUNDLE";
    public static final String INSTANCE_DEPENDENCIES ="INSTANCE_DEPENDENCIES";

    /**
     * Get a property associated with this instance. For classes
     * implementing this method, special care must be taken for
     * values implementing <tt>InstanceReference.ValueHolder</tt>.
     * In such cases, the value itself should not be returned, but
     * the value of <tt>InstanceReference.ValueHolder.get()</tt>
     * should be returned instead. This may be used to defer
     * creating value objects in cases where creating the value
     * object is expensive.
     * @param name the name of the property to retrieve.
     * @return the value of the associated property or <tt>null</tt>.
    **/
    public Object get(String name);

    /**
     * Associate a property with this instance. For classes
     * implementing this method, special care must be taken for
     * values implementing <tt>InstanceReference.ValueHolder</tt>.
     * In such cases, the value itself should not be returned, but
     * the value of <tt>InstanceReference.ValueHolder.get()</tt>
     * should be returned instead. This may be used to defer
     * creating value objects in cases where creating the value
     * object is expensive.
     * @param name the name of the property to add.
     * @param value the value of the property.
    **/
    public void put(String name, Object value);

    /**
     * Gets the actual object associated with this instance refernce.
     * @return the object associated with this reference or <tt>null</tt>
     *         if the reference is not currently valid.
    **/
    public Object getObject();

    /**
     * Adds an instance reference listener to listen for changes to
     * the availability of the underlying object associated with this
     * instance reference.
     * @param l the listener to add.
    **/
    public void addInstanceReferenceListener(InstanceReferenceListener l);

    /**
     * Removes an instance reference listener.
     * @param l the listener to remove.
    **/
    public void removeInstanceReferenceListener(InstanceReferenceListener l);

    /**
     * A simple interface that enabled deferred value creation for
     * the <tt>InstanceReference.get()</tt> and <tt>InstanceReference.put()</tt>
     * methods.
    **/
    public static interface ValueHolder
    {
        /**
         * Returns the associated value.
         * @return the associated value or <tt>null</tt>.
        **/
        public Object get(InstanceReference ir);
    }
}
