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


import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.IconList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;

import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPService;

import org.apache.felix.upnp.basedriver.importer.core.upnp.UPnPIconImpl;
import org.apache.felix.upnp.basedriver.importer.core.upnp.UPnPServiceImpl;
import org.apache.felix.upnp.basedriver.util.Constants;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class DeviceSetup {

	public static void deviceSetup(Dictionary properties, Device dev,Vector icons,Hashtable services) {
		//TODO if I don't have any device, the size of devlist is 0 
		DeviceList devList = dev.getDeviceList();
		/* childrenUDN property */
		if (devList.size() > 0) {
			String[] childrenUDN = new String[devList.size()];
			for (int i = 0; i < devList.size(); i++) {
				childrenUDN[i] = devList.getDevice(i).getUDN();
			}
			properties.put(UPnPDevice.CHILDREN_UDN, childrenUDN);
		}
		/* DEVICE CATEGORY */
		properties.put(
		        org.osgi.service
		        	.device.Constants.DEVICE_CATEGORY,
	        	new String[]{UPnPDevice.DEVICE_CATEGORY}
	        );

		/*UPNP_IMPORT*/
		properties.put(Constants.UPNP_IMPORT, "http://felix.apache.org");
		
		/* FRIENDLY_NAME */
		//check the implementation fo getFriendlyName made by CyberLink
		properties.put(UPnPDevice.FRIENDLY_NAME, dev.getFriendlyName());
		/* MANUFACTURER */
		properties.put(UPnPDevice.MANUFACTURER, dev.getManufacture());
		/* MANUFACTURER_URL */
		properties.put(UPnPDevice.MANUFACTURER_URL, dev.getManufactureURL());
		/* MODEL_DESCRIPTION */
		properties.put(UPnPDevice.MODEL_DESCRIPTION, dev.getModelDescription());
		/* MODEL_NAME */
		properties.put(UPnPDevice.MODEL_NAME, dev.getModelName());
		/* MODEL_NUMBER */
		properties.put(UPnPDevice.MODEL_NUMBER, dev.getModelNumber());
		/* MODEL_URL */
		properties.put(UPnPDevice.MODEL_URL, dev.getModelURL());
		/* PARENT_UDN */
		if (!dev.isRootDevice()) {
			Device parent = dev.getParentDevice();
			/*Device root = dev.getRootDevice();
			if (root == null) {
				System.out.println("il device " + dev.getFriendlyName()
						+ "non ha root !!!");
			}*/
			properties.put(UPnPDevice.PARENT_UDN, parent.getUDN());
		}
		/* PRESENTATION_URL */
		properties.put(UPnPDevice.PRESENTATION_URL, dev.getPresentationURL());
		/* SERIAL_NUMBER */
		properties.put(UPnPDevice.SERIAL_NUMBER, dev.getSerialNumber());
		/* TYPE */
		properties.put(UPnPDevice.TYPE, dev.getDeviceType());
		/* UDN */
		properties.put(UPnPDevice.UDN, dev.getUDN());
		/* UPC */
		properties.put(UPnPDevice.UPC, dev.getUPC());

		IconList iconsList = dev.getIconList();
		if (iconsList.size() != 0) {
			for (int i = 0; i < iconsList.size(); i++) {
				UPnPIcon icon = new UPnPIconImpl(iconsList.getIcon(i),dev);
				icons.add(icon);
			}
		}
		/* 
		 * service of this device
		 */ 
		ServiceList serviceList = dev.getServiceList();
		/*
		 * if dev contain no service I'll get an empty SserviceList object
		 */
		String[] servicesIDProperty = new String[serviceList.size()];
		String[] servicesTypeProperty;
		HashSet serTypeSet = new HashSet();
		for (int i = 0; i < serviceList.size(); i++) {
			Service service = serviceList.getService(i);
			UPnPServiceImpl serviceImpl = new UPnPServiceImpl(service);
			services.put(service.getServiceID(), serviceImpl);
			servicesIDProperty[i] = serviceImpl.getId();
			serTypeSet.add(serviceImpl.getType());
		}
		servicesTypeProperty = new String[serTypeSet.size()];
		Iterator iter = serTypeSet.iterator();
		int i = 0;
		while (iter.hasNext()) {
			servicesTypeProperty[i] = (String) iter.next();
			i++;
		}
		properties.put(UPnPService.ID, servicesIDProperty);
		properties.put(UPnPService.TYPE, servicesTypeProperty);

	
	}

}
