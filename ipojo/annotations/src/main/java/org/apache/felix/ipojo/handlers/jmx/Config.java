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
package org.apache.felix.ipojo.handlers.jmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * JMX Handler annotation.
 * Allows exposing the instances as a JMX MBean.
 * Be aware that despite is it provided in the annotations jar, 
 * it refers to an external handler.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target(ElementType.TYPE)
public @interface Config {
    
    /**
     * Enables or Disables MOSGi usage.
     * If MOSGi is used, MBeans are exposed with the MOSGi mechanism.
     * Otherwise the MBean Platform Server is used.
     * Default : false
     */
    boolean usesMOSGi() default false;
    
    /**
     * Sets the MBean object name.
     * Default : 'package-name:facotry-name:instance-name'.
     */
    String objectname() default "";
    
    /**
     * Sets the MBean domain.
     * Default : 'package-name'
     */
    String domain() default "";
    
    /**
     * Sets the MBean name.
     * Default : 'instance-name'
     */
    String name() default "";
    

}
