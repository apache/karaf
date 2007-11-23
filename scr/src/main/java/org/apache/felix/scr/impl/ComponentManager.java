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
package org.apache.felix.scr.impl;

import org.apache.felix.scr.Component;


/**
 * This interface is provided so that there can be multiple implementations of
 * managers that are responsible for managing component's lifecycle.
 *
 */
public interface ComponentManager extends Component {

	/**
	 * Enable the component
	 */
	public void enable();

    /**
     * Reconfigure the component with configuration data newly retrieved from
     * the Configuration Admin Service.
     */
    public void reconfigure();

    /**
     * Disable the component. After disabling the component may be re-enabled
     * by calling the {@link #enable()} method.
     */
    public void disable();

	/**
	 * Dispose the component. After disposing the component manager it must not
     * be used anymore.
	 */
	public void dispose();

	/**
	 * Get the component information
	 *
	 * @return a ComponentMetadata object
	 */
	public ComponentMetadata getComponentMetadata();
}
