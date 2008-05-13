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
 * Service interface published by handler factory. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface HandlerFactory extends Factory {

    /**
     * iPOJO Default Namespace.
     */
    String IPOJO_NAMESPACE = "org.apache.felix.ipojo";

    /**
     * Gets the namespace associated with this handler factory.
     * @return the namespace used by this handler
     */
    String getNamespace();

    /**
     * Gets the name associated with this handler factory.
     * @return the name used by this handler
     */
    String getHandlerName();

    /**
     * Gets the type of created handler.
     * The handler can only be plugged on instance container with the same type.
     * Basically, types are primitive and composite.
     * @return the types of the handler
     */
    String getType();

    /**
     * Gets the start level of the handlers created by this factory.
     * Handlers with a low start level are configured and started before 
     * handlers with an higher start level. Moreover, these handlers are
     * stopped and disposed after.
     * @return the handler's start level
     */
    int getStartLevel();

}
