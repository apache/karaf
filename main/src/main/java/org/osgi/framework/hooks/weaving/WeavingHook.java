/*
 * Copyright (c) OSGi Alliance (2010). All Rights Reserved.
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

package org.osgi.framework.hooks.weaving;

/**
 * OSGi Framework Weaving Hook Service.
 * 
 * <p>
 * Bundles registering this service will be called during framework class
 * loading operations. Weaving hook services are called when a class is being
 * loaded by the framework and have an opportunity to transform the class file
 * bytes that represents the class being loaded. Weaving hooks may also ask the
 * framework to wire in additional dynamic imports to the bundle.
 * 
 * <p>
 * When a class is being loaded, the framework will create a {@link WovenClass}
 * object for the class and pass it to each registered weaving hook service for
 * possible modification. The first weaving hook called will see the original
 * class file bytes. Subsequently called weaving hooks will see the class file
 * bytes as modified by previously called weaving hooks.
 * 
 * @ThreadSafe
 * @version $Id: d1985029024baba2db1c56aab1e06ee953fd6365 $
 */

public interface WeavingHook {
	/**
	 * Weaving hook method.
	 * 
	 * This method can modify the specified woven class object to weave the
	 * class being defined.
	 * 
	 * <p>
	 * If this method throws any exception, the framework must log the exception
	 * and fail the class load in progress. This weaving hook service must be
	 * blacklisted by the framework and must not be called again. The
	 * blacklisting of this weaving hook service must expire when this weaving
	 * hook service is unregistered. However, this method can throw a
	 * {@link WeavingException} to deliberately fail the class load in progress
	 * without being blacklisted by the framework.
	 * 
	 * @param wovenClass The {@link WovenClass} object that represents the data
	 *        that will be used to define the class.
	 * @throws WeavingException If this weaving hook wants to deliberately fail
	 *         the class load in progress without being blacklisted by the
	 *         framework
	 */
	public void weave(WovenClass wovenClass);
}
