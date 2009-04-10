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

import java.awt.Component;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPService;

import org.apache.felix.upnp.extra.util.UPnPEventNotifier;
import org.apache.felix.upnp.extra.util.UPnPSubscriber;

public class TvDevice implements UPnPDevice,UPnPEventListener,ServiceListener  {
	
	final private String DEVICE_ID = "uuid:Felix-TV+" +Integer.toHexString(new Random(System.currentTimeMillis()).nextInt());
	private final static String CLOCK_DEVICE_TYPE = "urn:schemas-upnp-org:device:clock:1";
	private final static String TIME_SERVICE_TYPE = "urn:schemas-upnp-org:service:timer:1";
	
	private final static String LIGHT_DEVICE_TYPE = "urn:schemas-upnp-org:device:light:1";
	private final static String POWER_SERVICE_TYPE = "urn:schemas-upnp-org:service:power:1";

	private final static String AIRCON_DEVICE_TYPE = "urn:schemas-upnp-org:device:aircon:1";
	private final static String TEMP_SERVICE_TYPE = "urn:schemas-upnp-org:service:temp:1";
	
	private final static String WASHER_DEVICE_TYPE = "urn:schemas-upnp-org:device:washer:1";
	private final static String STATUS_SERVICE_TYPE = "urn:schemas-upnp-org:service:state:1";

	private final String devicesFilter = 
		"(&"+
			"("+Constants.OBJECTCLASS+"="+UPnPDevice.class.getName()+"))";
			/*"(|("+UPnPDevice.TYPE+"="+ CLOCK_SERVICE_TYPE+")"+
				"("+UPnPDevice.TYPE+"="+ LIGHT_SERVICE_TYPE+")"+
				"("+UPnPDevice.TYPE+"="+ AIRCON_SERVICE_TYPE+")"+
				"("+UPnPDevice.TYPE+"="+ WASHER_SERVICE_TYPE+")))";*/

	private BundleContext context;
	private PowerService powerService;
	private UPnPService[] services;
	private Dictionary dictionary;
	private UPnPEventNotifier notifier;
	private PowerStateVariable powerState;

	public TvDevice() {
		powerService = new PowerService();
		services = new UPnPService[]{powerService};
		powerState = (PowerStateVariable) powerService.getStateVariable("Power");
		setupDeviceProperties();
		buildEventNotifyer();
		try {
			Activator.context.addServiceListener(this,devicesFilter);
		} catch (InvalidSyntaxException e) {
			System.out.println(e);		
		}
	}

	/**
	 * 
	 */
	private void buildEventNotifyer() {
		notifier = new UPnPEventNotifier(Activator.context,this,powerService);
		powerState.setNotifier(notifier);
	}

	private void setupDeviceProperties(){
		dictionary = new Properties();
		dictionary.put(UPnPDevice.UPNP_EXPORT,"");
		dictionary.put(
	        org.osgi.service.device.Constants.DEVICE_CATEGORY,
        	new String[]{UPnPDevice.DEVICE_CATEGORY}
        );
		dictionary.put(UPnPDevice.FRIENDLY_NAME,"Felix Sample Tv");
		dictionary.put(UPnPDevice.MANUFACTURER,"Apache Software Foundation");
		dictionary.put(UPnPDevice.MANUFACTURER_URL,"http://felix.apache.org");
		dictionary.put(UPnPDevice.MODEL_DESCRIPTION,"A CyberLink Tv device clone to test OSGi to UPnP service import");
		dictionary.put(UPnPDevice.MODEL_NAME,"BimbiTv");
		dictionary.put(UPnPDevice.MODEL_NUMBER,"1.0");
		dictionary.put(UPnPDevice.MODEL_URL,"http://felix.apache.org/site/upnp-examples.html");
		dictionary.put(UPnPDevice.SERIAL_NUMBER,"123456789");
		dictionary.put(UPnPDevice.TYPE,"urn:schemas-upnp-org:device:tv:1");
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
		if  (serviceId.equals(powerService.getId())) return powerService;
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
		UPnPIcon icon = new TvIcon();
		return new UPnPIcon[]{icon} ;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPDevice#getDescriptions(java.lang.String)
	 */
	public Dictionary getDescriptions(String locale) {
		return dictionary;
	}



	////////////////////////////////////////////////
	//	Component
	////////////////////////////////////////////////

	private Component comp;
	
	public void setComponent(Component comp)
	{
		this.comp = comp;	
	}
	
	public Component getComponent()
	{
		return comp;
	}
	
	////////////////////////////////////////////////
	//	on/off
	////////////////////////////////////////////////

	private boolean onFlag = false;
	
	public void on()
	{
		powerState.setPower(Boolean.TRUE);
		doSubscribe();
	}

	public boolean isOn()
	{ 
		return powerState.getCurrentPower().booleanValue();
	}
	public void off()
	{
		powerState.setPower(Boolean.FALSE);
		undoSubscribe();
	}


	////////////////////////////////////////////////
	//	Clock
	////////////////////////////////////////////////

	private String clockTime = ""; 
	
	public String getClockTime()
	{
		return clockTime;	
	}
	
	////////////////////////////////////////////////
	//	Aircon
	////////////////////////////////////////////////

	private String airconTemp = ""; 
	
	public String getAirconTempture()
	{
		return airconTemp;	
	}

	////////////////////////////////////////////////
	//	Message
	////////////////////////////////////////////////

	private String message = ""; 
	
	public void setMessage(String msg)
	{
		message = msg;
	}
	
	public String getMessage()
	{
		return message;
	}
	

	////////////////////////////////////////////////
	//	Subscribe
	////////////////////////////////////////////////
	
	private UPnPSubscriber subscriber;
	
	public void doSubscribe()
	{
		subscriber = new UPnPSubscriber(Activator.context,this);
		subscriber.subscribeEveryServiceType(CLOCK_DEVICE_TYPE, TIME_SERVICE_TYPE);
		subscriber.subscribeEveryServiceType(AIRCON_DEVICE_TYPE, TEMP_SERVICE_TYPE);
		subscriber.subscribeEveryServiceType(LIGHT_DEVICE_TYPE, POWER_SERVICE_TYPE);
		subscriber.subscribeEveryServiceType(WASHER_DEVICE_TYPE, STATUS_SERVICE_TYPE);
	}
	
	public void undoSubscribe(){
		subscriber.unsubscribeAll();
	}
	
	ArrayList LinkedDevices = new ArrayList();
	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPEventListener#notifyUPnPEvent(java.lang.String, java.lang.String, java.util.Dictionary)
	 */
	public void notifyUPnPEvent(String deviceId, String serviceId, Dictionary events) {
		if( !LinkedDevices.contains(deviceId))
			LinkedDevices.add(deviceId);
		if (deviceId.indexOf("Clock") != -1){
				Date date = (Date) events.get("Time");
				clockTime = date.toString();				
		}
		else if (deviceId.indexOf("AirCon") != -1)
				airconTemp = (String) events.get("Temp");
		else if (deviceId.indexOf("Washer") != -1)
				message = (String) events.get("State");
		else if (deviceId.indexOf("Light") != -1)
				message = (String) events.get("Power");
 
		comp.repaint();
	}


	////////////////////////////////////////////////
	//	start/stop
	////////////////////////////////////////////////
	
	public void start()
	{
		on();
	}

	public void stop()
	{
		((PowerStateVariable) powerService.getStateVariable("Power")).setNotifier(null);
		notifier.destroy();
		off();
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent event) {
		switch(event.getType()){
			case ServiceEvent.REGISTERED:{
			};break;
			
			case ServiceEvent.MODIFIED:{				
			};break;
			
			case ServiceEvent.UNREGISTERING:{	
				ServiceReference sr = event.getServiceReference();
				String UDN = (String)sr.getProperty(UPnPDevice.ID);
				if (UDN != null){
					if (LinkedDevices.contains(UDN)) {
						if (UDN.indexOf("Clock") != -1)
								clockTime = "";
						else if (UDN.indexOf("AirCon") != -1)
								airconTemp = "";
						else if (UDN.indexOf("Washer") != -1)
								message = "";
						else if (UDN.indexOf("Light") != -1)
								message = "";
					}
				}
				comp.repaint();
		 	};break;
		}
	}

}

