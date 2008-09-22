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
 * A factory state listener received notification about monitored factory state changes.
 * This listener allows anyone to be notified when the listened factory state changes. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface FactoryStateListener {
    
    /**
     * Notification listener.
     * Each time an instance state changes, this method is called
     * with the new factory state.
     * @param factory the changing factory
     * @param newState the new factory state
     */
    void stateChanged(Factory factory, int newState);
}
