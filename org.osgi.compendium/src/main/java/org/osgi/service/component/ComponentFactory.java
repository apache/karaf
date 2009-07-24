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

import java.util.Dictionary;

/**
 * When a component is declared with the <code>factory</code> attribute on its
 * <code>component</code> element, the Service Component Runtime will register
 * a Component Factory service to allow new component configurations to be
 * created and activated rather than automatically creating and activating
 * component configuration as necessary.
 * 
 * @ThreadSafe
 * @version $Revision: 5654 $
 */
public interface ComponentFactory {
	/**
	 * Create and activate a new component configuration. Additional properties
	 * may be provided for the component configuration.
	 * 
	 * @param properties Additional properties for the component configuration
	 *        or <code>null</code> if there are no additional properties.
	 * @return A <code>ComponentInstance</code> object encapsulating the
	 *         component instance of the component configuration. The component
	 *         configuration has been activated and, if the component specifies
	 *         a <code>service</code> element, the component instance has been
	 *         registered as a service.
	 * @throws ComponentException If the Service Component Runtime is unable to
	 *         activate the component configuration.
	 */
	public ComponentInstance newInstance(Dictionary properties);
}
