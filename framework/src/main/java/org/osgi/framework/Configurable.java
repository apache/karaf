/*
 * Copyright (c) OSGi Alliance (2000, 2009). All Rights Reserved.
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

package org.osgi.framework;

/**
 * Supports a configuration object.
 * 
 * <p>
 * <code>Configurable</code> is an interface that should be used by a bundle
 * developer in support of a configurable service. Bundles that need to
 * configure a service may test to determine if the service object is an
 * <code>instanceof Configurable</code>.
 * 
 * @deprecated As of 1.2. Please use Configuration Admin service.
 * @version $Revision: 6361 $
 */
public interface Configurable {
	/**
	 * Returns this service's configuration object.
	 * 
	 * <p>
	 * Services implementing <code>Configurable</code> should take care when
	 * returning a service configuration object since this object is probably
	 * sensitive.
	 * <p>
	 * If the Java Runtime Environment supports permissions, it is recommended
	 * that the caller is checked for some appropriate permission before
	 * returning the configuration object.
	 * 
	 * @return The configuration object for this service.
	 * @throws SecurityException If the caller does not have an
	 *         appropriate permission and the Java Runtime Environment supports
	 *         permissions.
	 * @deprecated As of 1.2. Please use Configuration Admin service.
	 */
	public Object getConfigurationObject();
}
