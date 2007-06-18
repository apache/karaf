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

import java.util.Dictionary;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class StateChanged {
	private String sid;
	private Dictionary dic;
	private long seq;
	private Service service;
	private Device device;
	/**
	 * @param sid
	 * @param dic
	 * @param varName
	 * @param varValue
	 */
	public StateChanged(String sid, long seq, Dictionary dic, Device device,
			Service service) {
		super();
		this.sid = sid;
		/*
		 * this.varName = varName; this.varValue = varValue;
		 */
		this.dic = dic;
		/* dic.put(this.varName, this.varValue); */
		//this.service=service;
		this.seq = seq;
		this.device = device;
		this.service = service;
	}

	public Dictionary getDictionary() {
		return dic;
	}
	public String getSid() {
		return sid;
	}
	/*
	 * public String getVarName() { return varName; } public String
	 * getVarValue() { return varValue; }
	 */
	public long getSeq() {
		return seq;
	}
	/*
	 * public Service getService(){ return service; }
	 *  
	 */
	public String getDeviceID() {
		return device.getUDN();
	}
	public String getServiceID() {
		return service.getServiceID();
	}
}
