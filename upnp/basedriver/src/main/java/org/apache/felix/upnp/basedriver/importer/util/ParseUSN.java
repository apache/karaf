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

package org.apache.felix.upnp.basedriver.importer.util;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class ParseUSN {
	boolean service;
	boolean device;
	String udn;
	String serviceType;
	
	public ParseUSN(String usn) {
		String [] splited=StringSplitter.split(usn, ':');
		if(splited.length==5||splited.length==2){
			udn="uuid:"+splited[1];
			device=true;
			service=false;
		}else if(splited.length==8){
			udn="uuid:"+splited[1];
			if(splited[5].equals("device")){
				device=true;
				service=false;
			}else{
				serviceType=splited[3]+":"+splited[4]+":"+splited[5]+":"+splited[6]+":"+splited[7];
				device=false;
				service=true;
			}
		}
	}	

	public boolean isService() {
		return service;
	}
	public boolean isDevice() {
		return device;
	}
	public String getUDN() {
		return udn;
	}
	public String getServiceType() {
		return serviceType;
	}
	
}
