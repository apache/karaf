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
 * When the device manager detects a new Device service, it calls all registered
 * Driver services to determine if anyone matches the Device service. If at
 * least one Driver service matches, the device manager must choose one. If
 * there is a Driver Selector service registered with the Framework, the device
 * manager will ask it to make the selection. If there is no Driver Selector
 * service, or if it returns an invalid result, or throws an
 * <code>Exception</code>, the device manager uses the default selection
 * strategy.
 * 
 * @version $Revision: 5654 $
 * @since 1.1
 * @ThreadSafe
 */
public interface DriverSelector {
	/**
	 * Return value from <code>DriverSelector.select</code>, if no Driver
	 * service should be attached to the Device service. The value is -1.
	 */
	public static final int	SELECT_NONE	= -1;

	/**
	 * Select one of the matching Driver services. The device manager calls this
	 * method if there is at least one driver bidding for a device. Only Driver
	 * services that have responded with nonzero (not {@link Device#MATCH_NONE})
	 * <code></code> match values will be included in the list.
	 * 
	 * @param reference the <code>ServiceReference</code> object of the Device
	 *        service.
	 * @param matches the array of all non-zero matches.
	 * @return index into the array of <code>Match</code> objects, or
	 *         <code>SELECT_NONE</code> if no Driver service should be attached
	 */
	public int select(ServiceReference reference, Match[] matches);
}
