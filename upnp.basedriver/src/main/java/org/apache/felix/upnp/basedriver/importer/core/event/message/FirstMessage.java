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

package org.apache.felix.upnp.basedriver.importer.core.event.message;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;

import org.osgi.service.upnp.UPnPEventListener;

/**
 * This class rappresent a message that is equeued for the Suscriber.<br>
 * This is message is related to a registration of listener for a 
 * CyberLink Service during the registering of the UPnP Event Listener
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
 

public class FirstMessage {
	private Service service;
	private UPnPEventListener listener;
	private String sid;
	private Device device;

	public FirstMessage(Service service, UPnPEventListener listener) {
		this.service = service;
		this.listener = listener;
		this.sid = "";
		this.device = service.getDevice();
	}
	public Service getService() {
		return service;
	}
	public UPnPEventListener getListener() {
		return listener;
	}
	public void setSid(String sid) {
		this.sid = sid;
	}
	public String getSid() {
		return sid;
	}
	public Device getDevice() {
		return device;
	}
	public String getDeviceID(){
		return device.getUDN();
	}
	public String getServiceID(){
		return service.getServiceID();	
	}
}
