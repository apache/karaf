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

import org.osgi.framework.ServiceReference;

/**
 * A <code>Driver</code> service object must be registered by each Driver bundle
 * wishing to attach to Device services provided by other drivers. For each
 * newly discovered {@link Device} object, the device manager enters a bidding
 * phase. The <code>Driver</code> object whose {@link #match} method bids the
 * highest for a particular <code>Device</code> object will be instructed by the
 * device manager to attach to the <code>Device</code> object.
 * 
 * @version $Revision: 5654 $
 * @see Device
 * @see DriverLocator
 * @ThreadSafe
 */
public interface Driver {
	/**
	 * Checks whether this Driver service can be attached to the Device service.
	 * 
	 * The Device service is represented by the given {@link ServiceReference}
	 * and returns a value indicating how well this driver can support the given
	 * Device service, or {@link Device#MATCH_NONE} if it cannot support the
	 * given Device service at all.
	 * 
	 * <p>
	 * The return value must be one of the possible match values defined in the
	 * device category definition for the given Device service, or
	 * <code>Device.MATCH_NONE</code> if the category of the Device service is
	 * not recognized.
	 * 
	 * <p>
	 * In order to make its decision, this Driver service may examine the
	 * properties associated with the given Device service, or may get the
	 * referenced service object (representing the actual physical device) to
	 * talk to it, as long as it ungets the service and returns the physical
	 * device to a normal state before this method returns.
	 * 
	 * <p>
	 * A Driver service must always return the same match code whenever it is
	 * presented with the same Device service.
	 * 
	 * <p>
	 * The match function is called by the device manager during the matching
	 * process.
	 * 
	 * @param reference the <code>ServiceReference</code> object of the device
	 *        to match
	 * 
	 * @return value indicating how well this driver can support the given
	 *         Device service, or <code>Device.MATCH_NONE</code> if it cannot
	 *         support the Device service at all
	 * 
	 * @throws java.lang.Exception if this Driver service cannot examine the
	 *         Device service
	 */
	public int match(ServiceReference reference) throws Exception;

	/**
	 * Attaches this Driver service to the Device service represented by the
	 * given <code>ServiceReference</code> object.
	 * 
	 * <p>
	 * A return value of <code>null</code> indicates that this Driver service
	 * has successfully attached to the given Device service. If this Driver
	 * service is unable to attach to the given Device service, but knows of a
	 * more suitable Driver service, it must return the <code>DRIVER_ID</code>
	 * of that Driver service. This allows for the implementation of referring
	 * drivers whose only purpose is to refer to other drivers capable of
	 * handling a given Device service.
	 * 
	 * <p>
	 * After having attached to the Device service, this driver may register the
	 * underlying device as a new service exposing driver-specific
	 * functionality.
	 * 
	 * <p>
	 * This method is called by the device manager.
	 * 
	 * @param reference the <code>ServiceReference</code> object of the device
	 *        to attach to
	 * 
	 * @return <code>null</code> if this Driver service has successfully
	 *         attached to the given Device service, or the
	 *         <code>DRIVER_ID</code> of a more suitable driver
	 * 
	 * @throws java.lang.Exception if the driver cannot attach to the given
	 *         device and does not know of a more suitable driver
	 */
	public String attach(ServiceReference reference) throws Exception;
}
