/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
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

package org.osgi.service.component;

/**
 * A ComponentInstance encapsulates a component instance of an activated
 * component configuration. ComponentInstances are created whenever a component
 * configuration is activated.
 * 
 * <p>
 * ComponentInstances are never reused. A new ComponentInstance object will be
 * created when the component configuration is activated again.
 * 
 * @ThreadSafe
 * @version $Revision: 5654 $
 */
public interface ComponentInstance {
	/**
	 * Dispose of the component configuration for this component instance. The
	 * component configuration will be deactivated. If the component
	 * configuration has already been deactivated, this method does nothing.
	 */
	public void dispose();

	/**
	 * Returns the component instance of the activated component configuration.
	 * 
	 * @return The component instance or <code>null</code> if the component
	 *         configuration has been deactivated.
	 */
	public Object getInstance();
}
