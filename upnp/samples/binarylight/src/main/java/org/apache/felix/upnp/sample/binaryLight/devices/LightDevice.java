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

package org.apache.felix.upnp.sample.binaryLight.devices;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPService;

import org.apache.felix.upnp.extra.util.UPnPEventNotifier;
import org.apache.felix.upnp.sample.binaryLight.LightModel;
import org.apache.felix.upnp.sample.binaryLight.LightUI;
import org.apache.felix.upnp.sample.binaryLight.icons.LightIcon;
import org.apache.felix.upnp.sample.binaryLight.services.PowerSwitchService;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class LightDevice implements UPnPDevice {

	final private String DEVICE_ID = "uuid:Felix-BinaryLight";
	private BundleContext context;
	private LightModel model;
	private LightUI ui;
	private PowerSwitchService powerSwitch;
	private UPnPService[] services;
	private Dictionary dictionary;
	private UPnPEventNotifier notifier;
	
	public LightDevice(BundleContext context) {
		this.context=context;
		model = new LightModel();
		ui = new LightUI(model);
		powerSwitch = new PowerSwitchService(model);
		services = new UPnPService[]{powerSwitch};
		setupDeviceProperties();
		buildEventNotifyer();
	}

	public LightModel getModel(){
		return model;
	}
	/**
	 * 
	 */
	private void buildEventNotifyer() {
		notifier = new UPnPEventNotifier(context,this,powerSwitch,model);
	}

	private void setupDeviceProperties(){
		dictionary = new Properties();
		dictionary.put(UPnPDevice.UPNP_EXPORT,"");
		dictionary.put(
		        org.osgi.service
		        	.device.Constants.DEVICE_CATEGORY,
	        	new String[]{UPnPDevice.DEVICE_CATEGORY}
	        );
		//dictionary.put(UPnPDevice.DEVICE_CATEGORY,new String[]{UPnPDevice.DEVICE_CATEGORY});
		dictionary.put(UPnPDevice.FRIENDLY_NAME,"Felix OSGi-UPnP BinaryLight");
		dictionary.put(UPnPDevice.MANUFACTURER,"Apache Software Foundation");
		dictionary.put(UPnPDevice.MANUFACTURER_URL,"http://felix.apache.org");
		dictionary.put(UPnPDevice.MODEL_DESCRIPTION,"A BinaryLight device to test OSGi to UPnP service export");
		dictionary.put(UPnPDevice.MODEL_NAME,"Lucciola");
		dictionary.put(UPnPDevice.MODEL_NUMBER,"1.0");
		dictionary.put(UPnPDevice.MODEL_URL,"http://felix.apache.org/site/upnp-example-binarylight.html");
		String port = context.getProperty("org.osgi.service.http.port");
        InetAddress inet;
		try {
			inet = InetAddress.getLocalHost();
	        String hostname = inet.getHostName();
            //String hostname = inet.getHostAddress();
		dictionary.put(UPnPDevice.PRESENTATION_URL,"http://"+hostname + ":"+port+"/upnp/binaryLight/");
		} catch (UnknownHostException e) {
			System.out.println("Warning: enable to cacth localhost name");
		}
		dictionary.put(UPnPDevice.SERIAL_NUMBER,"123456789");
		dictionary.put(UPnPDevice.TYPE,"urn:schemas-upnp-org:device:BinaryLight:1");
		dictionary.put(UPnPDevice.UDN,DEVICE_ID);
		//dictionary.put(UPnPDevice.ID,dictionary.get(UPnPDevice.UDN));
		dictionary.put(UPnPDevice.UPC,"1213456789");
	}
	
	
	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPDevice#getService(java.lang.String)
	 */
	public UPnPService getService(String serviceId) {
		if  (serviceId.equals(powerSwitch.getId())) return powerSwitch;
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
		UPnPIcon icon = new LightIcon();
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
	public void close() {
		ui.dispose();
		notifier.destroy();
	}
	
}
