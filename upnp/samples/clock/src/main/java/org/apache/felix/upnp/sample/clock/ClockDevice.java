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

package org.apache.felix.upnp.sample.clock;


import java.beans.PropertyChangeEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;

import org.osgi.framework.BundleContext;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.extra.util.UPnPEventNotifier;

public class ClockDevice implements UPnPDevice {

	final private String DEVICE_ID = "uuid:Felix-Clock+"+Integer.toHexString(new Random(System.currentTimeMillis()).nextInt());
	private BundleContext context;
	private TimerService timerService;
	private UPnPService[] services;
	private Dictionary dictionary;
	public static UPnPEventNotifier notifier = null;
	
	public ClockDevice(BundleContext context) {
		this.context=context;
		timerService = new TimerService();
		services = new UPnPService[]{timerService};
		setupDeviceProperties();
		buildEventNotifyer();
	}

	/**
	 * 
	 */
	private void buildEventNotifyer() {
		 notifier = new UPnPEventNotifier(context,this,timerService);
	}

	private void setupDeviceProperties(){
		dictionary = new Properties();
		dictionary.put(UPnPDevice.UPNP_EXPORT,"");
		dictionary.put(
		        org.osgi.service.device.Constants.DEVICE_CATEGORY,
	        	new String[]{UPnPDevice.DEVICE_CATEGORY}
	        );
		
		dictionary.put(UPnPDevice.FRIENDLY_NAME,"Felix OSGi-UPnP Clock");
		dictionary.put(UPnPDevice.MANUFACTURER,"Apache Software Foundation");
		dictionary.put(UPnPDevice.MANUFACTURER_URL,"http://felix.apache.org");
		dictionary.put(UPnPDevice.MODEL_DESCRIPTION,"A CyberLink Clock device clone to test OSGi to UPnP service export");
		dictionary.put(UPnPDevice.MODEL_NAME,"DolceDormire");
		dictionary.put(UPnPDevice.MODEL_NUMBER,"1.0");
		dictionary.put(UPnPDevice.MODEL_URL,"http://felix.apache.org/site/upnp-examples.html");
		dictionary.put(UPnPDevice.SERIAL_NUMBER,"123456789");
		dictionary.put(UPnPDevice.TYPE,"urn:schemas-upnp-org:device:clock:1");
		dictionary.put(UPnPDevice.UDN,DEVICE_ID);
		dictionary.put(UPnPDevice.UPC,"1213456789");
		
		HashSet types = new HashSet(services.length+5);
		String[] ids = new String[services.length];
		for (int i = 0; i < services.length; i++) {
			ids[i]=services[i].getId();
			types.add(services[i].getType());
		}
		
		dictionary.put(UPnPService.TYPE, types.toArray(new String[]{}));
		dictionary.put(UPnPService.ID, ids);
	}
	
	
	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPDevice#getService(java.lang.String)
	 */
	public UPnPService getService(String serviceId) {
		if  (serviceId.equals(timerService.getId())) return timerService;
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPDevice#getServices()
	 */
	public UPnPService[] getServices() {
		return services;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPDevice#getIcons(java.lang.String)
	 */
	public UPnPIcon[] getIcons(String locale) {
		UPnPIcon icon = new ClockIcon();
		return new UPnPIcon[]{icon} ;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPDevice#getDescriptions(java.lang.String)
	 */
	public Dictionary getDescriptions(String locale) {
		return dictionary;
	}

	/**
	 * 
	 */
	public void start() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 */
	public void stop() {
		notifier.destroy();	
	}

	/**
	 * 
	 */
	public void update() {
		Clock clock = Clock.getInstance();
		Calendar cal = clock.getCalendar();
        Date date = cal.getTime();
        UPnPStateVariable variable =  timerService.getStateVariable("Time");
		notifier.propertyChange(new PropertyChangeEvent(variable,"Time",null,date));
	}
	
}
