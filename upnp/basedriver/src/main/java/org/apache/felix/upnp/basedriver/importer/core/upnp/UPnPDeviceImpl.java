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

package org.apache.felix.upnp.basedriver.importer.core.upnp;


import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.cybergarage.upnp.Device;

import org.osgi.framework.BundleContext;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPService;

import org.apache.felix.upnp.basedriver.importer.util.DeviceSetup;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class UPnPDeviceImpl implements UPnPDevice {

	/**
	 * <code>properties</code> Dictionary that contains Device properties
	 */
	private Dictionary properties;
	private Vector icons;
	private Hashtable services;
	/**
	 * @param dev
	 *            Device the cyberLink Device used to rappresent the real UPnP
	 *            Device
	 */
	public UPnPDeviceImpl(Device dev, BundleContext context) {
		properties = new Hashtable();
		this.services=new Hashtable();
		this.icons=new Vector();
		DeviceSetup.deviceSetup(properties,dev,icons,services);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPDevice#getService(java.lang.String)
	 */
	public UPnPService getService(String serviceId) {
		return (UPnPServiceImpl) services.get(serviceId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPDevice#getServices()
	 */
	public UPnPService[] getServices() {
		Enumeration e = services.elements();
		if (e == null) {
			//TODO should I return null or an empty array? The specification seems to said to return null
			return null;
		}
		UPnPService[] uPnPser = new UPnPService[services.size()];
		int i = 0;
		while (e.hasMoreElements()) {
			uPnPser[i] = (UPnPServiceImpl) e.nextElement();
			i++;
		}
		return uPnPser;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPDevice#getIcons(java.lang.String)
	 */
	public UPnPIcon[] getIcons(String locale) {
		if (locale != null) {
			return null;
		}
		if(icons.size()==0){
			return null;
		}
		return (UPnPIcon[]) icons.toArray(new UPnPIcon[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPDevice#getDescriptions(java.lang.String)
	 */

	public Dictionary getDescriptions(String locale) {
		//TODO Sent the right localized version of Description if are available
		if (locale != null) {
			return null;
		}
		return properties;
	}

	/**
	 * @param serviceType
	 * @return true if device contains the serviceType
	 */
	public boolean existServiceType(String serviceType) {
		String[] services = (String[]) properties.get(UPnPService.TYPE);
		if (services != null) {
			for (int i = 0; i < services.length; i++) {
				if (services[i].equals(serviceType)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param property
	 * @param obj
	 */
	public void setProperty(String property, Object obj) {
		properties.remove(property);
		properties.put(property, obj);

	}

}
