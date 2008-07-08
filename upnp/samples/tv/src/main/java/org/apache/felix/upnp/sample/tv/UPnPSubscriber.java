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

package org.apache.felix.upnp.sample.tv;

import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class UPnPSubscriber {
	ServiceRegistration registration = null;
	BundleContext context;
	UPnPEventListener listener;
	
	public UPnPSubscriber(BundleContext context,UPnPEventListener listener){
		this.context = context;
		this.listener = listener;
	}
	
	public void subscribe(String deviceType, String serviceType){
		String keys = "(&(" + UPnPDevice.TYPE + "="+ deviceType + ")(" + UPnPService.TYPE + "=" + serviceType + "))";
		try {
			Filter filter = context.createFilter(keys);
			Properties props = new Properties();
			props.put(UPnPEventListener.UPNP_FILTER, filter);
			registration = context.registerService(UPnPEventListener.class.getName(), listener, props);
		}catch (Exception ex){
			System.out.println(ex);
		}
	}
	
	public void unsubscribe(){
		registration.unregister();
		registration = null;
	}
	

}
