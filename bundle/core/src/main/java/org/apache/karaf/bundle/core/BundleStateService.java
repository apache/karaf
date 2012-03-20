/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.bundle.core;

import org.osgi.framework.Bundle;

/**
 * SPI to track an extended bundle state for injection frameworks like blueprint that
 * also reports on  dependencies and status on the injection container level
 */
public interface BundleStateService {
    public final static String NAME_BLUEPRINT = "Blueprint";
    public final static String NAME_SPRING_DM = "Spring DM";
    public final static String NAME_DS = "Declarative Services";

    /**
     * Name of the framework the implementation supports.
     * Should return one of the NAME_ constants above if it matches
     *  
     * @return name of the framework
     */
    String getName();

    /**
     * Give a textual report about the details of a bundle status
     * like missing namespace handlers or service dependencies.
     * Should also give the details if there are config errors
     * 
     * @param bundle
     * @return diagnostic details
     */
    String getDiag(Bundle bundle);
    
    /**
     * Report the bundle state from the framework point of view. 
     * If the framework is not active it should return Unknown.
     * 
     * @param bundle
     * @return status
     */
    BundleState getState(Bundle bundle);

}
