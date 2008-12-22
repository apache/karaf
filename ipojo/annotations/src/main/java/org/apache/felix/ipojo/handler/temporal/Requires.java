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
package org.apache.felix.ipojo.handler.temporal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Temporal dependency annotation.
 * Allows specifying a temporal dependency.
 * Be aware that despite is it provided in the annotations jar, 
 * it refers to an external handler.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target(ElementType.FIELD)
public @interface Requires {
    
    /**
     * Set the LDAP filter of the dependency.
     * Default : no filter
     */
    String filter() default "";
   
    /**
     * Timeout of the dependency.
     * Default : true
     */
    long timeout() default 3000;
    
    /**
     * Set the on timeout action.
     * Supports null, nullable, empty, and default-implementation.
     * In this latter case, you must specify the qualified class name
     * of the default-implementation (instead of default-implementation).
     * Default: no action (i.e throws a runtime exception)
     */
    String onTimeout() default "";
    
    /**
     * Set the service specification (for Collection fields).
     * This attribute is mandatory for Collections.
     */
    String specification() default "";
    
    /**
     * Inject a proxy instead of the real object.
     * This allows passing this reference to collaborators.
     * Default: false
     */
    boolean proxy() default false;

}
