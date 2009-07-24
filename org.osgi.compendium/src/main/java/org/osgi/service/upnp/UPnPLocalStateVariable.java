/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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
/**
 * To keep the current values getting from subscribed UPnPDevices. 
 * 
 * The actual values of the UPnPStateVaraible are passed as Java object type. 
 * 
 * @since 1.1
 **/
package org.osgi.service.upnp;

/**
 * A local UPnP state variable which allows the value of the state variable to
 * be queried.
 * 
 * @since 1.1
 * 
 * @version $Revision: 5673 $
 */
public interface UPnPLocalStateVariable extends UPnPStateVariable {
	/**
	 * This method will keep the current values of UPnPStateVariables of a
	 * UPnPDevice whenever UPnPStateVariable's value is changed , this method
	 * must be called.
	 * 
	 * @return <code>Object</code> current value of UPnPStateVariable. if the
	 *         current value is initialized with the default value defined UPnP
	 *         service description.
	 */
	public Object getCurrentValue();
}
