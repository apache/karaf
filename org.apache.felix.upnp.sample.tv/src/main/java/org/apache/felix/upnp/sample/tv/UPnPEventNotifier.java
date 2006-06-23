/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.upnp.sample.tv;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

/**
 * @author Francesco Furfari
 */

public class UPnPEventNotifier implements PropertyChangeListener,ServiceListener {
	BundleContext context;
	UPnPDevice device;
	UPnPService service;
	EventSource source;
	
	Properties UPnPTargetListener;
	String deviceId;
	String serviceId;
	Vector upnpListeners = new Vector();
	
	public UPnPEventNotifier(BundleContext context,UPnPDevice device,UPnPService service,EventSource source){
		this.context=context;
		this.device=device;
		this.service=service;
		this.source=source;
		this.serviceId=service.getId();
		setupUPnPListenerHouseKeeping(device);
	}
	
	/**
	 * @param deviceId
	 */
	private void setupUPnPListenerHouseKeeping(UPnPDevice device) {
		UPnPTargetListener = new Properties();
		Dictionary dict = device.getDescriptions(null);
		deviceId = (String) dict.get(UPnPDevice.ID);
		UPnPTargetListener.put(UPnPDevice.ID,deviceId);
		UPnPTargetListener.put(UPnPService.ID,serviceId);
		UPnPTargetListener.put(UPnPDevice.TYPE,dict.get(UPnPDevice.TYPE));
		UPnPTargetListener.put(UPnPService.TYPE,service.getType());
		String ANY_UPnPEventListener = "("+Constants.OBJECTCLASS+"="+UPnPEventListener.class.getName()+")";
		
		ServiceReference[] listeners = null; 
		try {
			listeners = context.getServiceReferences(UPnPEventListener.class.getName(),null);
			if (listeners != null){
				for (int i = 0;i<listeners.length;i++){
					ServiceReference sr = listeners[i];
					Filter filter = (Filter) sr.getProperty(UPnPEventListener.UPNP_FILTER);
					if (filter == null) addNewListener(sr);
					else {				
						if (filter.match(UPnPTargetListener))
							addNewListener(sr);
					}
				}
			}
		} catch (Exception ex) {
			System.out.println(ex);
		}
		
	    try {
	    	//String filter = "(&" + ANY_UPnPEventListener + deviceId_Filter + ")";
	    	String filter = ANY_UPnPEventListener;
			context.addServiceListener(this,filter);
		} catch (Exception ex) {
			System.out.println(ex);
		}
		
		if (source!=null){
			UPnPStateVariable[] vars = service.getStateVariables();
			if (vars != null){
				for (int i=0;i<vars.length;i++)
					if(vars[i].sendsEvents())
						source.addPropertyChangeListener(vars[i].getName(),this);
			}
		}
		
		
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		Iterator list = upnpListeners.iterator();
		String property = evt.getPropertyName();
		Object value = evt.getNewValue();
		String valueString = value.toString();
		final Properties events = new Properties();
		events.put(property,valueString);
		while (list.hasNext()){
			final ServiceReference sr = (ServiceReference)list.next();
			String[] props =sr.getPropertyKeys();
			new Thread(null,null,"Notifier"){
				public void run(){
					try {
					    UPnPEventListener listener = (UPnPEventListener) context.getService(sr);
						listener.notifyUPnPEvent(deviceId,serviceId,events);
						context.ungetService(sr);
					} catch (Exception ex){
						System.out.println("TV UPnPEventNotifier Err: " +ex);
						System.out.println("context: " +context);
						System.out.println("listener: " +context.getService(sr));
						System.out.println("sr: " +sr);
					}
				}
			}.start();						
		}
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent e) {
		switch(e.getType()){
			case ServiceEvent.REGISTERED:{
				ServiceReference sr = e.getServiceReference();
				Filter filter = (Filter) sr.getProperty(UPnPEventListener.UPNP_FILTER);
				if (filter == null) addNewListener(sr);
				else {				
					if (filter.match(UPnPTargetListener))
						addNewListener(sr);
				}
			};break;
			
			case ServiceEvent.MODIFIED:{				
	               ServiceReference sr = e.getServiceReference();
	               Filter filter = (Filter)	sr.getProperty(UPnPEventListener.UPNP_FILTER);
	               removeListener(sr);
	               if (filter == null)
	                   addNewListener(sr);
	               else {
	                   if (filter.match(UPnPTargetListener))
	                       addNewListener(sr);
	               }
			};break;
			
			case ServiceEvent.UNREGISTERING:{	
				removeListener(e.getServiceReference());
			};break;
				
		}
		
	}

	/**
	 * @param reference
	 */
	private void removeListener(ServiceReference reference) {
		upnpListeners.remove(reference);		
	}

	/**
	 * @param reference
	 */
	private void addNewListener(ServiceReference reference) {
		upnpListeners.add(reference);	
		UPnPStateVariable[] vars = service.getStateVariables();
		if (vars != null){
			for (int i=0;i<vars.length;i++)
				if(vars[i].sendsEvents()){
				    Object value = ((UPnPStateVariableDescriptor) vars[i]).getValue();
				    propertyChange(new PropertyChangeEvent(this,vars[i].getName(),value,value));
				}
		}
	}
	
	public void destroy(){
		context.removeServiceListener(this);
		upnpListeners.removeAllElements();
	}
}
