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

package org.apache.felix.upnp.basedriver.importer.core;


import org.osgi.framework.ServiceRegistration;

import org.apache.felix.upnp.basedriver.importer.core.upnp.UPnPDeviceImpl;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class OSGiDeviceInfo {
	private UPnPDeviceImpl osgidevice;
	private ServiceRegistration registration;
	
	/**
	 * @param osgidevice UPnPDeviceImpl a Service
	 * @param registration Serviceregistration of the UPnPDevice service
	 */
	public OSGiDeviceInfo(UPnPDeviceImpl osgidevice,
			ServiceRegistration registration) {
		this.osgidevice = osgidevice;
		this.registration = registration;
	}
	public UPnPDeviceImpl getOSGiDevice(){
		return osgidevice;
	}
	public ServiceRegistration getRegistration(){
		return registration;
	}
}
