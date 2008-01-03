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

package org.apache.felix.upnp.basedriver.importer.core;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.cybergarage.http.HTTPRequest;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;
import org.cybergarage.upnp.ServiceStateTable;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.device.NotifyListener;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.event.NotifyRequest;
import org.cybergarage.upnp.event.Property;
import org.cybergarage.upnp.event.PropertyList;
import org.cybergarage.upnp.ssdp.SSDPPacket;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.basedriver.Activator;
import org.apache.felix.upnp.basedriver.importer.core.event.message.FirstMessage;
import org.apache.felix.upnp.basedriver.importer.core.event.message.ListenerModified;
import org.apache.felix.upnp.basedriver.importer.core.event.message.ListenerUnRegistration;
import org.apache.felix.upnp.basedriver.importer.core.event.message.StateChanged;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.NotifierQueue;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.SubscriptionQueue;
import org.apache.felix.upnp.basedriver.importer.core.upnp.UPnPDeviceImpl;
import org.apache.felix.upnp.basedriver.importer.core.upnp.UPnPServiceImpl;
import org.apache.felix.upnp.basedriver.importer.util.ParseUSN;
import org.apache.felix.upnp.basedriver.util.Converter;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class MyCtrlPoint extends ControlPoint
		implements
			NotifyListener,
			SearchResponseListener,
			ServiceListener
{
	private BundleContext context;   
    private Hashtable devices;//key UDN value OsgideviceInfo(Osgi)
	private SubscriptionQueue subQueue;
	private NotifierQueue notifierQueue;
    
    private final String UPNP_EVENT_LISTENER_FLTR =
        "(" + Constants.OBJECTCLASS + "=" + UPnPEventListener.class.getName() + ")";
    private final String UPNP_DEVICE_FLTR =
        "(" + Constants.OBJECTCLASS + "=" + UPnPDevice.class.getName() + ")";
    private final String EXPORT_FLTR =
        "(" + UPnPDevice.UPNP_EXPORT + "=*" + ")";
    private final String IMPORT_FLTR =
        "(" + org.apache.felix.upnp.basedriver.util.Constants.UPNP_IMPORT + "=*" + ")";

    
    public MyCtrlPoint(BundleContext context, SubscriptionQueue subQueue,
			NotifierQueue notifierQueue) {
		super();
		this.context = context;
        devices = new Hashtable();
		addNotifyListener(this);
		addSearchResponseListener(this);
		try {
			context.addServiceListener(this, UPNP_EVENT_LISTENER_FLTR);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		this.subQueue = subQueue;
		this.notifierQueue = notifierQueue;
	}

	public void httpRequestRecieved(HTTPRequest httpReq) {
        Activator.logger.DEBUG("[Importer] httpRequestRecieved event");
        Activator.logger.PACKET(httpReq.toString());

        if (httpReq.isNotifyRequest() == true) {
            Activator.logger.DEBUG("[Importer] Notify Request");
			NotifyRequest notifyReq = new NotifyRequest(httpReq);
			String uuid = notifyReq.getSID();
			long seq = notifyReq.getSEQ();
			PropertyList props = notifyReq.getPropertyList();
//			int propCnt = props.size();
//			Hashtable hash = new Hashtable();
//			for (int n = 0; n < propCnt; n++) {
//				Property prop = props.getProperty(n);
//				String varName = prop.getName();
//				String varValue = prop.getValue();
//				hash.put(varName, varValue);
//			}
            newEventArrived(uuid, seq, props);
			httpReq.returnOK();
			return;
		}

        Activator.logger.DEBUG("BAD Request");
		httpReq.returnBadRequest();

	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cybergarage.upnp.ControlPoint#removeExpiredDevices()
	 *  
	 */
	public void removeExpiredDevices() {
		DeviceList devList = getDeviceList();
		int devCnt = devList.size();
		for (int n = 0; n < devCnt; n++) {
			Device dev = devList.getDevice(n);
			if (dev.isExpired() == true) {
                Activator.logger.DEBUG("[Importer] Expired device:"+ dev.getFriendlyName());
				removeDevice(dev);
				removeOSGiExpireDevice(dev);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cybergarage.upnp.device.NotifyListener#deviceNotifyReceived(org.cybergarage.upnp.ssdp.SSDPPacket)
	 */
	public void deviceNotifyReceived(SSDPPacket ssdpPacket) {
        Activator.logger.DEBUG("[Importer] deviceNotifyReceived");
        Activator.logger.PACKET(ssdpPacket.toString());
		/*
		 * if the packet is 
		 * 		NOTIFY or ISALIVE or *new* ROOT	then create and register the UPnPDevice and 
		 * 										all the embeeded device too
		 * 		DEVICE or SERVICE	then if they already exist in OSGi do nothing otherwise I'll create and 
		 * 							register all the UPnPDevice need starting from the root device
		 * 		*root* BYEBYE		then I'll unregister it and all its children from OSGi Framework 
		 * 		*service* BYEBYE	then I'll re-register the UPnPDevice that contain the service with the updated
		 * 							properties 
		 * 		*device* BYEBYE		then I'll re-register the UPnPDevice that contain the device with the updated
		 * 							properties and also unregister the UPnPDevice that has left
		 */
		String usn = ssdpPacket.getUSN();
		ParseUSN parseUSN = new ParseUSN(usn);
		String udn = parseUSN.getUDN();
        
		ServiceReference[] refs = null;
		String filter = "(&" + UPNP_DEVICE_FLTR + EXPORT_FLTR+ ")";
		try {
			refs = context.getServiceReferences(UPnPDevice.class.getName(),	filter);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		if (refs != null) {
			for (int i = 0; i < refs.length; i++) {
				UPnPDevice dev = (UPnPDevice) context.getService(refs[i]);
				Dictionary dic = dev.getDescriptions(null);
				if (((String) dic.get(UPnPDevice.UDN)).equals(udn)) {
					return;
				}
			}
		}

		if (ssdpPacket.isAlive()) {
			
            Activator.logger.DEBUG("[Importer] ssdpPacket.isAlive");
			if (devices.containsKey(udn)) {
                Activator.logger.DEBUG("[Importer] Device already discovered");
				if (parseUSN.isService()) {
                    doServiceUpdating(udn,parseUSN.getServiceType());
				}
			} else {
                doDeviceRegistration(udn);
			}

		} else if (ssdpPacket.isByeBye()) {
            Activator.logger.DEBUG("[Importer] ssdpPacket.isByeBye");

            synchronized (devices) {		

				if (devices.containsKey(udn)) {
					if (parseUSN.isDevice()) {
	                    Activator.logger.DEBUG("[Importer] parseUSN.isDevice ...unregistering all the children devices ");
	                    
						//unregistering all the children devices 
						UPnPDeviceImpl dev = ((OSGiDeviceInfo) devices.get(udn)).getOSGiDevice();
						removeOSGiandUPnPDeviceHierarchy(dev);
	
					} else if (parseUSN.isService()) {
	                    Activator.logger.DEBUG("[Importer] parseUSN.isService ...registering modified device again ");
						/* 
						 * I have to unregister the UPnPDevice and register it again 
						 * with the updated properties  
						 */
						UPnPDeviceImpl device = 
	                        ((OSGiDeviceInfo) devices.get(udn)).getOSGiDevice();
						ServiceRegistration registar = 
	                        ((OSGiDeviceInfo) devices.get(udn)).getRegistration();
						String[] oldServicesID = 
	                        (String[]) (device.getDescriptions(null).get(UPnPService.ID));
						String[] oldServiceType = 
	                        (String[]) (device.getDescriptions(null).get(UPnPService.TYPE));
	                    
						Device cyberDevice = findDeviceCtrl(this, udn);
						Vector vec = new Vector();
						for (int i = 0; i < oldServiceType.length; i++) {
							Service ser = cyberDevice.getService(oldServicesID[i]);
							if (!(ser.getServiceType().equals(parseUSN.getServiceType()))) 
	                        {
								vec.add(oldServicesID[i]);
							}
						}
	
	                    //new serviceID
						String[] actualServicesID = new String[vec.size()];
						actualServicesID = (String[]) vec.toArray(new String[]{});
	
	                    //new serviceType
						String[] actualServiceType = new String[oldServiceType.length - 1];
						vec.clear();
						for (int i = 0; i < oldServiceType.length; i++) {
							if (!(oldServiceType[i].equals(parseUSN.getServiceType()))) 
	                        {
								vec.add(oldServiceType[i]);
							}
						}
						actualServiceType = (String[]) vec.toArray(new String[]{});
	
	                    //unrigistering and registering again with the new properties
						unregisterUPnPDevice(registar);
						device.setProperty(UPnPService.ID, actualServicesID);
						device.setProperty(UPnPService.TYPE, actualServiceType);
						registerUPnPDevice(null, device, device.getDescriptions(null));
						searchForListener(cyberDevice);
					}
				}
				
			}//synchronized(devices)
		} else {
			/*
			 * if it is a service means that it has deleted when the 
			 * owner was unregister so I can skip this bye-bye
			 * 
			 * //TODO Understand the comment
			 *
			 */
		}
	}
    
	public synchronized void removeOSGiandUPnPDeviceHierarchy(final UPnPDeviceImpl dev) 
    {
		/*
		 * remove all the UPnPDevice from the struct of local device recursively
		 */
		final String udn = (String) dev.getDescriptions(null).get(UPnPDevice.UDN);
		
		if(devices.containsKey(udn) == false){
			Activator.logger.INFO("Device "+dev.getDescriptions(null).get(UPnPDevice.FRIENDLY_NAME)+"("+udn+") already removed");
			return;
		}		
		
		String[] childrenUDN = (String[]) dev.getDescriptions(null).get(
				UPnPDevice.CHILDREN_UDN);

		if (childrenUDN == null) {
			//no children			
			unregisterUPnPDevice(((OSGiDeviceInfo) devices.get(udn)).getRegistration());
			Activator.logger.INFO("Device "+dev.getDescriptions(null).get(UPnPDevice.FRIENDLY_NAME)+"("+udn+") deleted");
			devices.remove(udn);
			return;
		} else {
			for (int i = 0; i < childrenUDN.length; i++) {
				if (devices.get(childrenUDN[i]) != null) {
					removeOSGiandUPnPDeviceHierarchy(((OSGiDeviceInfo) devices.get(childrenUDN[i])).getOSGiDevice());
				}
			}
			unregisterUPnPDevice(((OSGiDeviceInfo) devices.get(udn)).getRegistration());
			Activator.logger.INFO("Device "+dev.getDescriptions(null).get(UPnPDevice.FRIENDLY_NAME)+"("+udn+") deleted");
			devices.remove(udn);
		}
	}

	public synchronized void removeOSGiExpireDevice(Device dev) {
		/*
		 * unregistering root device with all its children device from OSGi 
		 * deleting root device and all its children from struct that conatin 
		 * a list of local device
		 */
		removeOSGiandUPnPDeviceHierarchy(((OSGiDeviceInfo) devices.get(dev
				.getUDN())).getOSGiDevice());
	}

	public void registerUPnPDevice(Device dev, UPnPDeviceImpl upnpDev,
			Dictionary prop) {
		/*
		 * registering the new Device as OSGi UPnPDevice and then add 
		 * ServiceRegistration and UPnPDevice reference to the hashtable
		 * that contains local devices
		 */
		if (prop == null && upnpDev == null) {
			UPnPDeviceImpl newDevice = new UPnPDeviceImpl(dev, context);
			ServiceRegistration registration = 
                context.registerService(UPnPDevice.class.getName(), 
                                        newDevice, 
                                        newDevice.getDescriptions(null));
			OSGiDeviceInfo deviceInfo = 
                new OSGiDeviceInfo(newDevice,registration);
            
            String udn = (String) ((newDevice.getDescriptions(null)).get(UPnPDevice.UDN));
			devices.put(udn, deviceInfo);
		} else {
			ServiceRegistration registration = 
                context.registerService(UPnPDevice.class.getName(), upnpDev, prop);
			OSGiDeviceInfo deviceInfo = 
                new OSGiDeviceInfo(upnpDev,	registration);
			devices.put(upnpDev.getDescriptions(null).get(UPnPDevice.UDN),deviceInfo);
		}
	}
    
    
	public void unregisterUPnPDevice(ServiceRegistration registration) {
		registration.unregister();

	}

	public Device findDeviceCtrl(ControlPoint ctrl, String udn) {
		/* 
		 * this return the device looking in all the struct
		 */
		
		DeviceList devList = getDeviceList();
		Device dev = null;
		int i = 0;
		while (i < devList.size() && (dev == null)) {
			if (devList.getDevice(i).getUDN().equals(udn)) {
				dev = devList.getDevice(i);
				return dev;
			}
			dev = findDevice(udn, devList.getDevice(i));
			i++;	
		}
		return dev;
	}
    
	public Device findDevice(String udn, Device dev) {
		/*
		 * look for the device if it exist, starting from the root on
		 * cyberlink struct
		 */
		DeviceList devList = dev.getDeviceList();
		Device aux = null;
		for (int i = 0; i < devList.size(); i++) {
			if (devList.getDevice(i).getUDN().equals(udn)) {
				return devList.getDevice(i);
			} else {
				if((aux = findDevice(udn, devList.getDevice(i))) != null)
					return aux;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cybergarage.upnp.device.SearchResponseListener#deviceSearchResponseReceived(org.cybergarage.upnp.ssdp.SSDPPacket)
	 */
	public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
        Activator.logger.DEBUG("[Importer] deviceSearchResponseReceived");
        Activator.logger.PACKET(ssdpPacket.toString());

		String usn = ssdpPacket.getUSN();
		ParseUSN parseUSN = new ParseUSN(usn);
		String udn = parseUSN.getUDN();

		ServiceReference[] refs = null;
        
        String filter = "(&" + UPNP_DEVICE_FLTR + EXPORT_FLTR + ")";
       
		try {
			refs = context.getServiceReferences(UPnPDevice.class.getName(),
					filter);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
        
		if (refs != null) {
			for (int i = 0; i < refs.length; i++) {
				UPnPDevice dev = (UPnPDevice) context.getService(refs[i]);
				Dictionary dic = dev.getDescriptions(null);
				if (((String) dic.get(UPnPDevice.UDN)).equals(udn)) {
					return;
				}
			}
		}

		if (devices.containsKey(udn)) {			
            Activator.logger.DEBUG("[Importer] Device already discovered");
			/*
			 * Exist the registered device either in OSGi and 
			 * hashtable of local device
			 */
			if (parseUSN.isService()) {
                doServiceUpdating(udn,parseUSN.getServiceType());
			}
		} else {
            doDeviceRegistration(udn);
		}

	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent event) {
        Activator.logger.DEBUG("[Importer] serviceChanged");
        Activator.logger.DEBUG("Event::"+event.toString());

		if (event.getType() == ServiceEvent.REGISTERED) {
			/* check new listener registration */
			ServiceReference serRef = event.getServiceReference();
			Object obj = serRef.getProperty(UPnPEventListener.UPNP_FILTER);
			/* obtain interested devices for the listener */
			ServiceReference[] devicesRefs = null;
			if (obj != null) {
				Filter filter = (Filter) obj;
				String filtra = filter.toString();
				/*
				 * Avoid to implement the notification for device 
				 * that are not been created by BaseDriver
				 */ 
				String newfilter = "(&" + filtra +  IMPORT_FLTR + ")";
                //String newfilter = "(&" + filtra + "(!" + EXPORT_FLTR + ")" + ")";
				//System.out.println(newfilter);
				try {
					devicesRefs = context.getServiceReferences(UPnPDevice.class
							.getName(), newfilter);
				} catch (InvalidSyntaxException e) {
					e.printStackTrace();
				}

				if (devicesRefs != null) {/*
										   * 
										   * only if there is a compatibile device
										   */
					Dictionary dic = new Hashtable();
					for (int i = 0; i < devicesRefs.length; i++) {
						UPnPDevice device = (UPnPDevice) context.getService(devicesRefs[i]);
						dic.put(UPnPDevice.ID, device.getDescriptions(null).get(UPnPDevice.UDN));
						dic.put(UPnPDevice.TYPE, device.getDescriptions(null).get(UPnPDevice.TYPE));
						UPnPService[] services = device.getServices();
						if (services != null) {
							for (int j = 0; j < services.length; j++) {
								dic.put(UPnPService.ID, services[j].getId());
								dic.put(UPnPService.TYPE, services[j].getType());
								//TODO add method boolean serviceEvented() so we can remove the below cycle
								UPnPStateVariable[] stateVars = services[j].getStateVariables();
								boolean hasEventedVars = false;
								for (int k = 0; k < stateVars.length && ! hasEventedVars; k++) {
									hasEventedVars = stateVars[k].sendsEvents();
									if (hasEventedVars) {
										if(filter.match(dic)){
											UPnPEventListener listener = 
		                                        (UPnPEventListener) context.getService(serRef);
											FirstMessage msg = new FirstMessage(
													((UPnPServiceImpl) services[j]).getCyberService(),
													listener);
											subQueue.enqueue(msg);											
										}
									}
								}
							}
						}
                        context.ungetService(devicesRefs[i]);
					}
				}
			} else {/* obj==null (interested in all devices) */
				try {
					String newfilter = "(!" + EXPORT_FLTR+ ")";
					devicesRefs = context.getServiceReferences(UPnPDevice.class.getName(), newfilter);
				} catch (InvalidSyntaxException e) {
					e.printStackTrace();
				}
				if (devicesRefs != null) {/*
										   * 
										   * only if there is a device
										   */

					for (int i = 0; i < devicesRefs.length; i++) {
						UPnPDevice device = (UPnPDevice) context
								.getService(devicesRefs[i]);
						UPnPService[] services = device.getServices();
						if (services != null) {
							for (int j = 0; j < services.length; j++) {
								UPnPStateVariable[] stateVars = services[j]
										.getStateVariables();
								boolean bool = false;								
								for (int k = 0; k < stateVars.length; k++) {
									bool = stateVars[k].sendsEvents();
									if (bool) {
										break;
									}
								}
								if (bool) {
									UPnPEventListener listener = 
                                        (UPnPEventListener) context.getService(serRef);
									FirstMessage msg = new FirstMessage(
											((UPnPServiceImpl) services[j]).getCyberService(),
											listener);
									subQueue.enqueue(msg);
								}
							}
						}
                        context.ungetService(devicesRefs[i]);
					}
				}
			}

		} else if (event.getType() == ServiceEvent.MODIFIED) {
			Vector newServices = new Vector();
			ServiceReference serRef = event.getServiceReference();
			Filter filter = (Filter) serRef.getProperty(UPnPEventListener.UPNP_FILTER);
			UPnPEventListener listener = (UPnPEventListener) context.getService(serRef);
			ServiceReference[] devicesRefs = null;

			if (filter != null) {
				try {
					String filtra = filter.toString();
                    String newfilter = "(&" + filtra + "(!" + EXPORT_FLTR + ")" + ")";
					devicesRefs = context.getServiceReferences(UPnPDevice.class.getName(), newfilter);
				} catch (InvalidSyntaxException e) {
					e.printStackTrace();
				}
				if (devicesRefs != null) {/*
										   * 
										   * only if there is a compatibile device
										   */
					Dictionary dic = new Hashtable();
					/* 
					 * look for the service that match
					 */
					for (int i = 0; i < devicesRefs.length; i++) {
						UPnPDevice device = (UPnPDevice) context
								.getService(devicesRefs[i]);
						dic.put(UPnPDevice.ID, device.getDescriptions(null)
								.get(UPnPDevice.UDN));
						dic.put(UPnPDevice.TYPE, device.getDescriptions(null)
								.get(UPnPDevice.TYPE));
						UPnPService[] services = device.getServices();

						if (services != null) {
							for (int j = 0; j < services.length; j++) {
								dic.put(UPnPService.ID, services[j].getId());
								dic.put(UPnPService.TYPE, services[j].getType());

								UPnPStateVariable[] stateVars = services[j]
										.getStateVariables();
								boolean hasEventedVars = false;
								for (int k = 0; k < stateVars.length; k++) {
									hasEventedVars = stateVars[k].sendsEvents();
									if (hasEventedVars) {
										break;
									}
								}
								if (!hasEventedVars) {
									continue;
								}

								boolean bool = filter.match(dic);
								if (bool) {
									newServices
											.add(((UPnPServiceImpl) services[j])
													.getCyberService());
								}
							}//for services
						}//services ==null
						context.ungetService(devicesRefs[i]);
					}//for devicesRefs
					ListenerModified msg = new ListenerModified(newServices,
							listener);
					subQueue.enqueue(msg);
				}//devicesrefs !=null
			} else {//interrested in all devices
				try {

					String newfilter = "(!(" + UPnPDevice.UPNP_EXPORT + "=*"
							+ ")" + ")";
					devicesRefs = context.getServiceReferences(UPnPDevice.class
							.getName(), newfilter);
				} catch (InvalidSyntaxException e) {
					e.printStackTrace();
				}
				if (devicesRefs != null) {/*
										   * only if there is a device
										   */

					for (int i = 0; i < devicesRefs.length; i++) {
						UPnPDevice device = (UPnPDevice) context
								.getService(devicesRefs[i]);
						UPnPService[] services = device.getServices();
						if (services != null) {
							for (int j = 0; j < services.length; j++) {
								UPnPStateVariable[] stateVars = services[j]
										.getStateVariables();
								boolean hasEventedVars = false;
								for (int k = 0; k < stateVars.length; k++) {
									hasEventedVars = stateVars[k].sendsEvents();
									if (hasEventedVars) {
										break;
									}
								}
								if (hasEventedVars) {
									newServices
											.add(((UPnPServiceImpl) services[j])
													.getCyberService());
								}//hasEventedvars
							}//for services
						}//services !=null
                        context.ungetService(devicesRefs[i]);
					}//for devicesRefs
					subQueue
							.enqueue(new ListenerModified(newServices, listener));
				}//devicesRefs !=null

			}

		} else if (event.getType() == ServiceEvent.UNREGISTERING) {
			UPnPEventListener listener = (UPnPEventListener) context
					.getService(event.getServiceReference());
			if (listener != null) {
				ListenerUnRegistration msg = new ListenerUnRegistration(
						listener);
				subQueue.enqueue(msg);
			}
			context.ungetService(event.getServiceReference());
		}
	} /*
	   * (non-Javadoc)
	   * 
	   * @see org.cybergarage.upnp.event.EventListener#eventNotifyReceived(java.lang.String,
	   *      long, java.lang.String, java.lang.String)
	   */
	/*
	 * public void eventNotifyReceived(String uuid, long seq, String varName,
	 * String value) { // TODO Auto-generated method stub StateChanged msg = new
	 * StateChanged(uuid, varName, value,seq);,serviceFromSid(uuid)
	 * notifierQueue.enqueue(msg); }
	 */

	public Service serviceFromSid(String sid) {
		Enumeration e = devices.elements();
		Service cyberService = null;
		while (e.hasMoreElements()) {
			OSGiDeviceInfo deviceinfo = (OSGiDeviceInfo) e.nextElement();
			UPnPDevice device = deviceinfo.getOSGiDevice();
			UPnPService[] services = (UPnPService[]) device.getServices();
			UPnPServiceImpl[] servicesImpl = new UPnPServiceImpl[services.length];
			for (int i = 0; i < servicesImpl.length; i++) {
				servicesImpl[i] = (UPnPServiceImpl) services[i];
			}
			for (int i = 0; i < servicesImpl.length; i++) {
				cyberService = servicesImpl[i].getCyberService();
				boolean bool = cyberService.isSubscribed();
				if (bool) {
					if (cyberService.getSID().equals(sid)) {
						return cyberService;
					}
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.upnpbase.importer.MyEventListener#newEventArrived(java.lang.String,
	 *      long, java.util.Dictionary)
	 */
	public void newEventArrived(String uuid, long seq, PropertyList props) {
        Activator.logger.DEBUG("[Importer] newEventArrived");
		Service service = serviceFromSid(uuid);
		if (service != null) {
            int size = props.size();
            Hashtable hash = new Hashtable();
            for (int i = 0; i < size; i++) {
                Property prop = props.getProperty(i);
                String varName = prop.getName();
                String varValue = prop.getValue();
                String upnpType = service.getStateVariable(varName).getDataType();
                Object valueObj;
                try {
                    valueObj = Converter.parseString(varValue,upnpType);
                } catch (Exception e) {
                    Activator.logger.ERROR("[Importer] Bad data value in Notify event: "
                            +"var name="+varName +" value="+varValue +" type="+upnpType + "\n"+e);
                    return;
                }
                hash.put(varName, valueObj);
            }
           
			Device device = service.getDevice();
			StateChanged msg = new StateChanged(uuid, seq, hash, device, service);
			notifierQueue.enqueue(msg);
		}
	}
    
    public void doServiceUpdating(String udn,String serviceType){
        Activator.logger.DEBUG("[Importer] check for service updating");
        OSGiDeviceInfo deviceinfo = (OSGiDeviceInfo) devices.get(udn);
        UPnPDeviceImpl device = deviceinfo.getOSGiDevice();
        boolean isSerPresent = device.existServiceType(serviceType);
        if (!isSerPresent) {
            /*
             * The serivice doesn't exist so it's new.
             * Find the udn of owner device and re-register the owner
             */
            ServiceRegistration registar = 
                ((OSGiDeviceInfo) devices.remove(udn)).getRegistration();
            String[] oldServicesID = 
                (String[]) device.getDescriptions(null).get(UPnPServiceImpl.ID);
            String[] oldServicesType = 
                (String[]) device.getDescriptions(null).get(UPnPServiceImpl.TYPE);
            
            //to handle multiple instance of a serivice of the same type
            Device cyberDevice = findDeviceCtrl(this, udn);
            ServiceList serviceList = cyberDevice.getServiceList();
            ArrayList newServicesID = new ArrayList();

            for (int i = 0; i < serviceList.size(); i++) {
                if (serviceList.getService(i).getServiceType()
                        .equals(serviceType)) 
                {
                    newServicesID.add(serviceList.getService(i).getServiceID());
                }
            }
            
            //adding the new servicesID 
            String[] currentServicesID = 
                new String[(oldServicesID.length + newServicesID.size())];
            int endOld = 1;
            for (int i = 0; i < oldServicesID.length; i++) {
                currentServicesID[i] = oldServicesID[i];
                endOld++;
            }
            int j = 0;
            for (; endOld < currentServicesID.length; endOld++) {
                currentServicesID[endOld] = (String) newServicesID.get(j);
                j++;
            }
            
            //adding the new ServiceType
            String[] currentServicesType = new String[oldServicesType.length + 1];
            for (int i = 0; i < oldServicesType.length; i++) {
                currentServicesType[i] = oldServicesType[i];
            }
            currentServicesType[currentServicesType.length - 1] = serviceType;
            
            
            // unregistring the OSGi Device
            // and setting new properties
            unregisterUPnPDevice(registar);
            device.setProperty(UPnPService.ID, currentServicesID);
            device.setProperty(UPnPServiceImpl.TYPE,currentServicesType);
            
            //registering the service with the updated properties
            //TODO Check if null to the first paramaters is correct or it requires the reference to the cyberdomo upnp device
            registerUPnPDevice(null, device, device.getDescriptions(null));
            searchForListener(cyberDevice);
        }   
    }
    
    public void doDeviceRegistration(String udn,boolean checkDouble){
    	if(checkDouble){    		
    		try {
    			ServiceReference[] refs =
    				Activator.bc.getServiceReferences(
						UPnPDevice.class.getName(),
						"(" + UPnPDevice.UDN + "=" + udn + ")"
    				);
    			if(refs!=null)
    				return;
			} catch (InvalidSyntaxException ignored) {
			}
    	}
    	doDeviceRegistration(udn);
    }
    
    public synchronized void doDeviceRegistration(String udn){
        /*
         * registering the new device either if it is new root device or
         * a new embedded device 
         */
        Device dev = findDeviceCtrl(this, udn);
        if (dev == null) {
        	/*
        	 * In this case the UPnP SDK notifies us that a ssdp:alive has arrived,
        	 * but,  because the no root device ssdp:alive packet has recieved by the UPnP SDK
        	 * no Device is present in the UPnP SDK device list. 
        	 */
            Activator.logger.INFO("Cyberlink notified packet from UDN:" +udn+ ", but Device instance doesn't exist in Cyberlink structs! It will be Ignored");            
        }else if(devices.containsKey(udn) == false) {
            Activator.logger.INFO("[Importer] registering UPnPDevice:"+dev.getFriendlyName()+"("+dev.getUDN()+")" );
            registerUPnPDevice(dev, null, null);
            searchForListener(dev);
            /*
             * now we can register all the device embedded device and service without
             * recieving the NOTIFY 
             */
            //XXX Think about this choice
            for (Iterator i = dev.getDeviceList().iterator(); i.hasNext();) {
				Device d = (Device) i.next();
				doDeviceRegistration(d.getUDN(),true);
			}
        }else if(devices.containsKey(udn) == true) {
        	Activator.logger.INFO("[Importer] UPnPDevice UDN::"+dev.getFriendlyName()+"("+dev.getUDN()+") already registered Skipping");
        }
    }
    
	public void searchForListener(Device device) {
        Activator.logger.DEBUG("[Importer] searching for UPnPEventListener");
		ServiceReference[] listeners = null;
		try {
			listeners = context.getServiceReferences(UPnPEventListener.class.getName(), null);
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (listeners != null) {
			String deviceID = device.getUDN();
			String serviceID;
			String deviceType = device.getDeviceType();
			String serviceType;
			Hashtable hash = new Hashtable();
			hash.put(UPnPDevice.ID, deviceID);
			hash.put(UPnPDevice.TYPE, deviceType);
			ServiceList services = device.getServiceList();
			Vector eventedSers = new Vector();

			for (int i = 0; i < services.size(); i++) {
				Service service = (Service) services.elementAt(i);
				ServiceStateTable vars = service.getServiceStateTable();
				for (int j = 0; j < vars.size(); j++) {
					StateVariable var = (StateVariable) vars.elementAt(j);
					if (var.isSendEvents()) {
						eventedSers.add(service);
						break;
					}
				}
			}

			for (int i = 0; i < listeners.length; i++) {
				UPnPEventListener listener = (UPnPEventListener) context
						.getService(listeners[i]);
				Filter filter = (Filter) listeners[i]
						.getProperty(UPnPEventListener.UPNP_FILTER);
				if (filter == null) {
					for (int j = 0; j < eventedSers.size(); j++) {
						Service ser = (Service) eventedSers.elementAt(j);
						subQueue.enqueue(new FirstMessage(ser, listener));
					}
				} else {
					for (int j = 0; j < eventedSers.size(); j++) {
						Service ser = (Service) eventedSers.elementAt(j);
						serviceID = ser.getServiceID();
						serviceType = ser.getServiceType();
						hash.put(UPnPService.ID, serviceID);
						hash.put(UPnPService.TYPE, serviceType);
						boolean bool = filter.match(hash);
						if (bool) {
							subQueue.enqueue(new FirstMessage(ser, listener));
						}

					}
				}

			}

		}
	}
}
