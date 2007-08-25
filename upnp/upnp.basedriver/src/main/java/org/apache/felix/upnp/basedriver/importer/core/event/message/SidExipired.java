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

import org.cybergarage.upnp.Service;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class SidExipired {
private String sid;
private Service service;
/**
	 * @param sid
	 */
	public SidExipired(String sid,Service service) {
		super();
		this.sid = sid;
		this.service=service;
	}
	public String getSid(){
		return sid;
	}
	public Service getService(){
		return service;
	}
}
