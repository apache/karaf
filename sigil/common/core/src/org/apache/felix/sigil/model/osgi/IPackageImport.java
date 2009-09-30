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

package org.apache.felix.sigil.model.osgi;


import org.apache.felix.sigil.model.IRequirementModelElement;


public interface IPackageImport extends IPackageModelElement, IVersionRangeModelElement, IRequirementModelElement,
    Comparable<IPackageImport>
{
    /**
     * indicates whether import is needed at compile-time.
     * Default true. Used in conjunction with OSGiHeader.ALWAYS,
     * to add an OSGI import, without creating a dependency.
     */
    boolean isDependency();


    void setDependency( boolean dependency );


    /**
     * indicates whether import should be added to OSGi Package-Import header.
     * Default: AUTO.
     */
    OSGiImport getOSGiImport();


    void setOSGiImport( OSGiImport osgiImport );

    enum OSGiImport
    {
        /**
         * only add to OSGi header, if it appears to be needed.
         */
        AUTO,

        /**
         * always add to OSGi header, even if it appears unnecessary.
         */
        ALWAYS,

        /**
         * never add to OSGi header.
         */
        NEVER
    }
}