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

package org.apache.felix.upnp.basedriver.export;


import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.felix.upnp.basedriver.Activator;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.ServiceList;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class ThreadExporter implements Runnable,ServiceListener {

	private boolean end;

	private RootDeviceExportingQueue queueRootDevice;

//	private String basePath; twa: redundant

//	private File baseFile; twa: redundant

	private Hashtable exportedDevices;
	
	private boolean listening;
	
	private class ExportedDeviceInfo{
		private Device device;
		private ServiceRegistration serviceRegistration;
		//private DeviceNode deviceNode;
		
			
		/**
		 * @param device
		 * @param serviceRegistration
		 * @param deviceNode
		 */
		private ExportedDeviceInfo(Device device,
				ServiceRegistration serviceRegistration,
				DeviceNode deviceNode) {
			super();
			this.device = device;
			this.serviceRegistration = serviceRegistration;
			//this.deviceNode = deviceNode;
		}
		
		
		private Device getDevice() {
			return this.device;
		}
		private ServiceRegistration getServiceRegistration() {
			return this.serviceRegistration;
		}
		/*private DeviceNode getDeviceNode(){
			return this.deviceNode;
		}*/
        
		
	}
	
	/**
	 *  
	 */
	public ThreadExporter(RootDeviceExportingQueue queue) throws InvalidSyntaxException {
	    end=false;
	    queueRootDevice=queue;
		this.exportedDevices=new Hashtable();
		setListening(false);
	}
    
	public void run() {
		
		File osgiRoot = Activator.bc.getDataFile("");
		if (osgiRoot == null) {
			Activator.logger.ERROR("Unable to use filesystem");
			while (true) {
				try {
					Activator.bc.getBundle().stop();
					break;
				} catch (BundleException e) {
					e.printStackTrace();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}
			return;
		}
		
		ServiceReference rootDevice = null;
		while (!shouldEnd()) {
			DeviceNode dn = queueRootDevice.getRootDevice();
			if (dn == null)
				continue;
			rootDevice = dn.getReference();
			if(!getListening()) 
				setListen();
			Activator.logger.INFO("[Exporter] Exporting device "+ rootDevice.getProperty(UPnPDevice.FRIENDLY_NAME));
			
            /*
			 * I don't know if the exporting should be make default language of the framework
			 * or without any lanuguages
			Root r = new Root(rootDevice, context, context
					.getProperty(Constants.FRAMEWORK_LANGUAGE));
			*/
            
			synchronized (this) {
				Device d = BuildDevice.createCyberLinkDevice(dn.getReference());
				if (d != null) {
					if(!bindInvokes(d,rootDevice)){
						Activator.logger.DEBUG("Unable to find all the sub device or to set action listener");
						continue;
					}
					ServiceRegistration listenReg = bindSubscribe(d);
					if(listenReg==null){
						Activator.logger.DEBUG("Unable to set action listener event listener");
						continue;
					}			
					//makeIcons(r.getRootDevice(),xml.getAbsolutePath());
					d.start();
					exportedDevices.put(
							rootDevice.getProperty(UPnPDevice.UDN),
							new ExportedDeviceInfo(d,listenReg,dn)
					);
				}
			}
		}
	}

	/**
	 * 
	 */
	private void setListen() {
		{
			try {
				Activator.bc.addServiceListener(
						this,
						// fixed by Matteo and Francesco 21/9/04
						"(&("+Constants.OBJECTCLASS+"="+UPnPDevice.class.getName()+")" 
						+ "(" + UPnPDevice.UPNP_EXPORT +"=*))"
				);
			} catch (InvalidSyntaxException ingnore) {}
		}
	}
	/*
	/**
	 * @param upnpDev
	 * @param refDev
	 *
	private void makeIcons(
			org.apache.felix.upnpbase.export.xml.Device upnpDev, 
			String path) {
		Icon[] icons = upnpDev.getIcons();
		if(icons!=null){
			byte[] buf = new byte[512];
			for (int i = 0; i < icons.length; i++) {
				try {
					String icoPath = path+icons[i].getUrl().replace('/',File.separatorChar);
					InputStream is = icons[i].getInputStream();
					Converter.makeParentPath(icoPath);
					FileOutputStream fos = new FileOutputStream(icoPath);
					int n=is.read(buf,0,buf.length);
					while(n>0){
						fos.write(buf,0,n);
						n=is.read(buf,0,buf.length);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		org.apache.felix.upnpbase.export.xml.Device[] devs = upnpDev.getDevices();
		if(devs==null)
			return;
		for (int i = 0; i < devs.length; i++) {
			makeIcons(devs[i],path);
		}
	}*/
	/**
	 * This method is used to connect all the Action that are shown to UPnP world
	 * by CyberLink UPnP Device to the real implementation that is conatined iniside
	 * the OSGi service.
	 * This method will connect even all the subdevice of te given OSGi device.
	 * 
	 * @param d CyberLink Device that will be used associated to the OSGi Device
	 * @param rootDevice ServiceReference to the OSGi Device that will be used as 
	 * 	implementation of the CyberLink Device
	 * @return true if and only if the binding off all the action of all the children
	 * 		device is done succesfully
	 */
	private boolean bindInvokes(Device d, ServiceReference rootDevice) {
		bindInvoke(d,rootDevice);
		ServiceReference[] childs = null;
		try {
			childs = Activator.bc.getServiceReferences(
				UPnPDevice.class.getName(),
				"("+UPnPDevice.PARENT_UDN+"="+rootDevice.getProperty(UPnPDevice.UDN)+")"
			);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();				
		}		
		String[] childsUDN = (String[]) rootDevice.getProperty(UPnPDevice.CHILDREN_UDN);
		if((childs==null)&&(childsUDN==null)){
			return true;			
		}else if((childs==null)||(childsUDN==null)){
			return false;
		}else if(childs.length==childsUDN.length){
            DeviceList dl = d.getDeviceList();
            for (int i = 0; i < childs.length; i++) {
                Device dev = (Device)dl.elementAt(i);
                if(!bindInvokes(dev,childs[i]))
                    return false;
            }

			return true;
		}else{
			return false;
		}
			
	}
    
	/**
	 * This method add an UPnPEventListener Service to the OSGi Framework so that
	 * the Base Driver can notify all the event listener registered on the CyberLink
	 * UPnP device from the UPnP World. 
	 * 
	 * @param d Device of CyberLink that will be notified by the changing of the StateVariable
	 * 		that happen on the OSGi World  
	 * @return ServiceRegistration of the new registered service.
	 */
	private ServiceRegistration bindSubscribe(Device d) {
		ExporterUPnPEventListener eventer = new ExporterUPnPEventListener(d);
		Properties p = new Properties();

		StringBuffer sb = new StringBuffer("(|");
		Vector v = new Vector();
		v.add(d);
		Device current;
		while (v.size() != 0) {
			current = (Device) v.elementAt(0);
			v.remove(0);
			DeviceList dl = current.getDeviceList();
			for (int i = 0; i < dl.size(); i++) {
				v.add(dl.elementAt(i));
			}
			sb.append("(").append(UPnPDevice.ID).append("=").append(
					current.getUDN()).append(")");
		}
		sb.append(")");
		Filter f = null;
		try {
			f = Activator.bc.createFilter(sb.toString());
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return null;
		}
		if (f != null) p.put(UPnPEventListener.UPNP_FILTER, f);
		
		return Activator.bc.registerService(UPnPEventListener.class.getName(), eventer, p);
	}

	/**
	 * This method do the real connection between OSGi UPnP Service action and 
	 * CyberLink UPnP Device action
	 * 
	 * @param upnpDev the CyberLink UPnP Device object that will be connected
	 * @param osgiDev the ServiceReference to OSGi UPnP Service that will be connected to
	 * 		CyberLink UPnP as implementation of the Action
	 * @return true if and only if the binding off all the action is done succesfully 
	 */
	private boolean bindInvoke(Device upnpDev,ServiceReference osgiDev) {
		ServiceList sl = upnpDev.getServiceList();
		int l=sl.size();
		for (int i = 0; i < l; i++) {
			sl.getService(i).setActionListener(
				new GeneralActionListener(
					osgiDev,
					sl.getService(i).getServiceID()
				)
			);			
		}
		return true;
	}

	/**
	 *  
	 */
	public synchronized void cleanUp() {
		Activator.logger.INFO("Cleaning...");
		
		Enumeration keys;
		
		Activator.logger.INFO("Removing temporary listener....");
		keys=exportedDevices.keys();
		while (keys.hasMoreElements()) {
			ServiceRegistration sr = ((ExportedDeviceInfo) 
					exportedDevices.get(keys.nextElement())).getServiceRegistration();
			sr.unregister();
		}		
		Activator.logger.INFO("Done");
		
		Activator.logger.INFO("Removing device....");
		keys=exportedDevices.keys();
		while (keys.hasMoreElements()) {
			Device dev = ((ExportedDeviceInfo) 
					exportedDevices.get(keys.nextElement())).getDevice();
			dev.stop();
		}		
		Activator.logger.INFO("Done");
	}

	private synchronized boolean shouldEnd() {
		return end;
	}

	public synchronized void end() {
		end = true;
		queueRootDevice.addRootDevice(null);
	}
	/**
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent event) {
		switch(event.getType()){
			case ServiceEvent.REGISTERED:break;
			
			case ServiceEvent.MODIFIED:
			case ServiceEvent.UNREGISTERING:{
				this.unexportDevice(event.getServiceReference());
				if(exportedDevices.size()==0){
					Activator.bc.removeServiceListener(this);
					setListening(false);
				}
			}break;
		}
	}
	/**
	 * @param b
	 */
	private synchronized void setListening(boolean b) {
		listening=b;	
	}
	
	private synchronized boolean getListening(){
		return listening;
	}
	
	/**
	 * @param property
	 */
	private synchronized void unexportDevice(ServiceReference dev) {
		String udn=(String) dev.getProperty(UPnPDevice.PARENT_UDN);
		if(udn==null){
			ExportedDeviceInfo edi =
				(ExportedDeviceInfo) exportedDevices.get(
					dev.getProperty(UPnPDevice.UDN)
				);
			Device d = edi.getDevice();
			if(d!=null) {
                Activator.logger.INFO("[Exporter] removing device:" +d.getFriendlyName());
				d.stop();
				exportedDevices.remove(d.getUDN());
			}
			ServiceRegistration srListener=edi.getServiceRegistration();
			if(srListener!=null) srListener.unregister();
			
		}else{
			ServiceReference[] servs=null;
			try {
				servs = Activator.bc.getServiceReferences(
						UPnPDevice.class.getName(),
						"("+UPnPDevice.UDN+"="+udn+")"
				);
			} catch (InvalidSyntaxException ignored) {}
			if(servs==null) return;
			this.unexportDevice(servs[0]);
		}
	}
}
