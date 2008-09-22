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


/**
 * Context Source service interface.
 * A context source advertises of context changes.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ContextSource {
    
    /**
     * Gets the current value of the given property.
     * @param property property name
     * @return the property value (<code>null</code> if unknown)
     */
    Object getProperty(String property);
    
    /**
     * Gets the entire context.
     * @return the dictionary [Property, Value]
     */
    Dictionary getContext();
    
    /**
     * Registers a context listener on the given set of properties.
     * The listener will be notified of every changes made on monitored properties.
     * @param listener the context listener to register.
     * @param properties property set monitored by the listener.
     */
    void registerContextListener(ContextListener listener, String[] properties);
    
    /**
     * Unregisters the given context listener.
     * @param listener the listener to unregister.
     */
    void unregisterContextListener(ContextListener listener);

}
