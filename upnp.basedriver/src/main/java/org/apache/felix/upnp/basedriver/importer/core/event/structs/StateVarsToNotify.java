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

package org.apache.felix.upnp.basedriver.importer.core.event.structs;


import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.felix.upnp.basedriver.importer.core.event.message.StateChanged;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class StateVarsToNotify {
	private Dictionary stateVars;
	private String sid;
	private String deviceID;
	private String serviceID;
    
    //public StateVarsToNotify(String sid, String deviceID, String serviceID,Dictionary dic) {
    public StateVarsToNotify(StateChanged msg) {
		this.stateVars= msg.getDictionary();
		this.sid = msg.getSid();
		this.deviceID = msg.getDeviceID();
		this.serviceID = msg.getServiceID();
	}


	public synchronized Dictionary getDictionary() {
			return stateVars;
	}
    public synchronized String getSid() {
		return sid;
	}
	public synchronized String getDeviceID() {
		return deviceID;
	}
	public synchronized String getServiceID() {
		return serviceID;
	}
    
    
	public void updateDic(Dictionary dic){
		Enumeration e=dic.keys();
		while(e.hasMoreElements()){
			String varName=(String)e.nextElement();
			Object varValue=dic.get(varName);
			stateVars.put(varName,varValue);
		}
	}
}
