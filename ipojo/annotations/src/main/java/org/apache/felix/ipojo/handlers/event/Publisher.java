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
package org.apache.felix.ipojo.handlers.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/**
 * Event Admin Publisher handler.
 * Be aware that despite is it provided in the annotations jar, 
 * it refers to an external handler.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target(ElementType.FIELD)
public @interface Publisher {
    
    /**
     * Sets the publisher name.
     */
    String name();
    
    /**
     * Sets topics on which event are sent.
     * The topics are separated by a ',' such as in
     * "foo, bar".
     * Default : no topics (configured in the instance configuration)
     */
    String topics() default "";
    
    /**
     * Enables/Disables synchronous sending.
     * Default : false (asynchronous) 
     */
    boolean synchronous() default false;
    
    /**
     * Sets the data key in which the data is
     * put.
     * Default : user.data
     */
    String data_key() default "user.data";

}
