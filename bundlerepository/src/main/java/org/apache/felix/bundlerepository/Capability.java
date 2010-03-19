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
/*
 * $Header: /cvshome/build/org.osgi.service.obr/src/org/osgi/service/obr/Capability.java,v 1.3 2006/03/16 14:56:17 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2006). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This document is an experimental draft to enable interoperability
// between bundle repositories. There is currently no commitment to 
// turn this draft into an official specification.  
package org.apache.felix.bundlerepository;

import java.util.Map;

/**
 * A named set of properties representing some capability that is provided by
 * its owner.
 * 
 * @version $Revision: 1.3 $
 */
public interface Capability
{

    String BUNDLE = "bundle";
    String FRAGMENT = "fragment";
    String PACKAGE = "package";
    String SERVICE = "service";
    String EXECUTIONENVIRONMENT = "ee";

    /**
     * Return the name of the capability.
     * 
     */
    String getName();

    /**
     * Return the properties of this capability
     * 
     * @return
     */
    Property[] getProperties();

    /**
     * Return the map of properties.
     * 
     * @return a Map<String,Object>
     */
    Map getPropertiesAsMap();



}