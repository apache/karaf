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
 * $Header: /cvshome/build/org.osgi.service.obr/src/org/osgi/service/obr/Requirement.java,v 1.4 2006/03/16 14:56:17 hargrave Exp $
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

/**
 * A named requirement specifies the need for certain capabilities with the same
 * name.
 *
 * A requirement is said to be satisfied by a capability if and only if:
 * <ul>
 *   <li>they have the same nsame</li>
 *   <li>the filter matches the capability properties</li>
 * </ul>
 * 
 * @version $Revision: 1.4 $
 */
public interface Requirement
{

    /**
     * Return the name of the requirement.
     */
    String getName();

    /**
     * Return the filter.
     */
    String getFilter();

    boolean isMultiple();

    boolean isOptional();

    boolean isExtend();

    String getComment();

    /**
     * Check if the given capability satisfied this requirement.
     *
     * @param capability the capability to check
     * @return <code>true</code> is the capability satisfies this requirement, <code>false</code> otherwise
     */
    boolean isSatisfied(Capability capability);

}