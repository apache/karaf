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
package org.osgi.service.obr;

import java.util.Map;

/**
 * A named set of properties representing some capability that is provided by
 * its owner.
 * 
 * @version $Revision: 1.3 $
 */
public interface Capability
{
    /**
     * Return the name of the capability.
     * 
     */
    String getName();

    /**
     * Return the set of properties.
     * 
     * Notice that the value of the properties is a list of values.
     * 
     * @return a Map<String,List>
     */
    Map getProperties();
}