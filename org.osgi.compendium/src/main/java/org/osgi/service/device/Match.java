/*
 * Copyright (c) OSGi Alliance (2001, 2008). All Rights Reserved.
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
package org.osgi.service.device;

import org.osgi.framework.ServiceReference;

/**
 * Instances of <code>Match</code> are used in the {@link DriverSelector#select}
 * method to identify Driver services matching a Device service.
 * 
 * @version $Revision: 5654 $
 * @since 1.1
 * @see DriverSelector
 * @ThreadSafe
 */
public interface Match {
	/**
	 * Return the reference to a Driver service.
	 * 
	 * @return <code>ServiceReference</code> object to a Driver service.
	 */
	public ServiceReference getDriver();

	/**
	 * Return the match value of this object.
	 * 
	 * @return the match value returned by this Driver service.
	 */
	public int getMatchValue();
}
