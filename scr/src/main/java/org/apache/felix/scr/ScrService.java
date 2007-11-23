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
package org.apache.felix.scr;


import org.osgi.framework.Bundle;


/**
 * The <code>ScrService</code> represents the Declarative Services main
 * controller also known as the Service Component Runtime or SCR for short.
 * It provides access to the components managed the SCR.
 */
public interface ScrService
{

    /**
     * Returns an array of all components managed by this SCR instance. The
     * components are returned in ascending order of their component.id. If
     * there are no components currently managed by the SCR, <code>null</code>
     * is returned.
     *
     * @return The components or <code>null</code> if there are none.
     */
    Component[] getComponents();


    /**
     * Returns the component whose component.id matches the given
     * <code>componentId</code> or <code>null</code> if no component with the
     * given id is currently managed.
     *
     * @param componentId The ID of the component to return
     *
     * @return The indicated component or <code>null</code> if no such
     *      component exists.
     */
    Component getComponent( long componentId );


    /**
     * Reuturns an array of all components managed by this SCR instance on
     * behalf of the given bundle. The components are returned in ascending
     * order of their component.id. If there are no components managed by the
     * SCR for the given bundle, <code>null</code> is returned.
     *
     * @param bundle The <code>Bundle</code> whose components are to be
     *      returned.
     *
     * @return The bundle's components or <code>null</code> if the bundle
     *      has none.
     */
    Component[] getComponents( Bundle bundle );

}
