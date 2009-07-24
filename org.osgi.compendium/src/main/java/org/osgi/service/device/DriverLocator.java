/*
 * Copyright (c) OSGi Alliance (2000, 2008). All Rights Reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

/**
 * A Driver Locator service can find and load device driver bundles given a
 * property set. Each driver is represented by a unique <code>DRIVER_ID</code>.
 * <p>
 * Driver Locator services provide the mechanism for dynamically downloading new
 * device driver bundles into an OSGi environment. They are supplied by
 * providers and encapsulate all provider-specific details related to the
 * location and acquisition of driver bundles.
 * 
 * @version $Revision: 5654 $
 * @see Driver
 * @ThreadSafe
 */
public interface DriverLocator {
	/**
	 * Returns an array of <code>DRIVER_ID</code> strings of drivers capable of
	 * attaching to a device with the given properties.
	 * 
	 * <p>
	 * The property keys in the specified <code>Dictionary</code> objects are
	 * case-insensitive.
	 * 
	 * @param props the properties of the device for which a driver is sought
	 * @return array of driver <code>DRIVER_ID</code> strings of drivers capable
	 *         of attaching to a Device service with the given properties, or
	 *         <code>null</code> if this Driver Locator service does not know of
	 *         any such drivers
	 */
	public String[] findDrivers(Dictionary props);

	/**
	 * Get an <code>InputStream</code> from which the driver bundle providing a
	 * driver with the giving <code>DRIVER_ID</code> can be installed.
	 * 
	 * @param id the <code>DRIVER_ID</code> of the driver that needs to be
	 *        installed.
	 * @return An <code>InputStream</code> object from which the driver bundle
	 *         can be installed or <code>null</code> if the driver with the
	 *         given ID cannot be located
	 * @throws java.io.IOException the input stream for the bundle cannot be
	 *         created
	 */
	public InputStream loadDriver(String id) throws IOException;
}
