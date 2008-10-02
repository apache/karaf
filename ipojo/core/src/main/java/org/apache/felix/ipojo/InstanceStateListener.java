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

/**
 * This class defines instance state listeners.
 * An instance state listener is notified of instance state changes. It needs to be
 * registered on the instance by invoking the ({@link ComponentInstance#addInstanceStateListener(InstanceStateListener)}
 * method. Once registered, the listener can track instance state changes. 
 * Received states are:
 * <li>{@link ComponentInstance#VALID}</li>
 * <li>{@link ComponentInstance#INVALID}</li>
 * <li>{@link ComponentInstance#STOPPED}</li>
 * <li>{@link ComponentInstance#DISPOSED}</li> 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface InstanceStateListener {
    
    /**
     * State change listener callback method.
     * Every time that a monitored instance's state changes,
     * this method is called with the instance and the new state.
     * @param instance the changing instance
     * @param newState the new instance state
     */
    void stateChanged(ComponentInstance instance, int newState);
}
