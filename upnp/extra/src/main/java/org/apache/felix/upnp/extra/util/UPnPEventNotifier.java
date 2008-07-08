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

package org.apache.felix.upnp.extra.util;

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
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class UPnPEventNotifier implements PropertyChangeListener, ServiceListener {
	private BundleContext context;
	private UPnPDevice device;
	private UPnPService service;
	private String deviceId;
	private String serviceId;
	
	private Properties UPnPTargetListener;
	private Vector upnpListeners = new Vector();
	final private Object LOCK = new Object();
	
	
	private class NotifierThread extends Thread {
		private ServiceReference sr;
		private Properties events;

		public NotifierThread(ServiceReference sr, Properties events){
			super("UPnPEventNotifier");
			this.sr = sr;
			this.events = events;
		}
		
		public void run(){
			try {
				UPnPEventListener listener = (UPnPEventListener) context.getService(sr);
				listener.notifyUPnPEvent(deviceId,serviceId,events);
				context.ungetService(sr);
			} catch (Exception ignored){
			}
		}
	}

	public UPnPEventNotifier(BundleContext context, UPnPDevice device, UPnPService service, EventSource source){
		this(context,device,service);
		
		if (source != null){
			UPnPStateVariable[] vars = service.getStateVariables();
			if (vars != null){
				for (int i=0;i<vars.length;i++){
					if(vars[i].sendsEvents()){
						source.addPropertyChangeListener(vars[i].getName(),this);
					}
				}
			}
		}

	}
	
	public UPnPEventNotifier(BundleContext context, UPnPDevice device, UPnPService service){
		this.context=context;
		this.device=device;
		this.service=service;
		setupUPnPListenerWatcher();
	}
	
	/**
	 * @param deviceId
	 */
	private void setupUPnPListenerWatcher() {
		Dictionary dict = device.getDescriptions(null);
		this.deviceId = (String) dict.get(UPnPDevice.ID);
		this.serviceId=service.getId();
		UPnPTargetListener = new Properties();
		UPnPTargetListener.put(UPnPDevice.ID,deviceId);
		UPnPTargetListener.put(UPnPService.ID,serviceId);
		UPnPTargetListener.put(UPnPDevice.TYPE,dict.get(UPnPDevice.TYPE));
		UPnPTargetListener.put(UPnPService.TYPE,service.getType());
		String ANY_UPnPEventListener = "("+Constants.OBJECTCLASS+"="+UPnPEventListener.class.getName()+")";
		
		synchronized (LOCK) {
		    try {
		    	String filter = ANY_UPnPEventListener;
				context.addServiceListener(this,filter);
			} catch (Exception ex) {
				System.out.println(ex);
			}
						
			ServiceReference[] listeners = null; 
			try {
				listeners = context.getServiceReferences(UPnPEventListener.class.getName(),null);
				if (listeners != null){
					for (int i = 0;i<listeners.length;i++){
						ServiceReference sr = listeners[i];
						Filter filter = (Filter) sr.getProperty(UPnPEventListener.UPNP_FILTER);
						if (filter == null) upnpListeners.add(sr);
						else {				
							if (filter.match(UPnPTargetListener))
								addNewListener(sr);
						}
					}
				}
			} catch (Exception ex) {
				System.out.println(ex);
			}
			
			Properties status = getCurrentStatus();
			if (! status.isEmpty())
					doNotify(status);
		
		} // end LOCK
	
	}

	private Properties getCurrentStatus() {
		Properties status = new Properties();
		UPnPStateVariable[] vars = service.getStateVariables();
		if (vars != null){
			for (int i=0;i<vars.length;i++){
				if(vars[i].sendsEvents()){
					if (vars[i] instanceof UPnPLocalStateVariable){
						UPnPLocalStateVariable localVar = (UPnPLocalStateVariable) vars[i];
						String property = localVar.getName();
						Object value = localVar.getCurrentValue();
						status.put(property,value);						
					}
					else {
						System.out.println("[Warning] The StateVariable (" +vars[i].getName() +")"
								+ " of device (ID:" + deviceId 	+ ") "
								+ "should implement the interface UPnPLocalStateVariable");
					}
				}
			} // end for
		}
		return status;
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
        String property = evt.getPropertyName();
		Object value = evt.getNewValue();
		Properties events = new Properties();		
        events.put(property,value);
		doNotify(events);
	}

	private void doNotify(Properties events){
				
		synchronized (LOCK) {			
			Iterator list = upnpListeners.iterator();
			while (list.hasNext()){
				final ServiceReference sr = (ServiceReference)list.next();
				new NotifierThread(sr,events).start();						
			}
		}		
	}
	
	private void doNotify(ServiceReference sr) {
		Properties status = getCurrentStatus();
		if (! status.isEmpty())
			new NotifierThread(sr,status).start();						
	}

	
	/* (non-Javadoc)
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent e) {
		ServiceReference sr = e.getServiceReference();		
		synchronized (LOCK) {			
			switch(e.getType()){
				case ServiceEvent.REGISTERED:{
					Filter filter = (Filter) sr.getProperty(UPnPEventListener.UPNP_FILTER);
					if (filter == null) {
						addNewListener(sr);
						doNotify(sr);
					}
					else {				
						if (filter.match(UPnPTargetListener)){
							addNewListener(sr);
							doNotify(sr);
						}
					}
				};break;
				
				case ServiceEvent.MODIFIED:{				
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
					removeListener(sr);
				};break;					
			}
		}//end LOCK
		
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
	}
	
	public void destroy(){
		context.removeServiceListener(this);
		upnpListeners.removeAllElements();
	}
}
