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
 * Context Source Listener interface.
 * A context source listener is notified when a monitored context 
 * property value changed.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ContextListener {
    
    /**
     * A monitored value has been modified.
     * @param source the context source containing the property
     * @param property the modified property name
     * @param value the new value of the property
     */
    void update(ContextSource source, String property, Object value);

}
