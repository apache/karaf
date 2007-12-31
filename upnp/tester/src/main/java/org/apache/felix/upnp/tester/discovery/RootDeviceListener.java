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

package org.apache.felix.upnp.tester.discovery;


import java.util.ArrayList;
import java.util.Iterator;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;

import org.apache.felix.upnp.tester.Activator;
import org.apache.felix.upnp.tester.gui.LogPanel;
import org.apache.felix.upnp.tester.gui.Util;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class RootDeviceListener implements ServiceListener {
		
	//This list contains all partial tree of UPnP Device
	//when a device is complete it will be removed from the list
    private ArrayList devices;
	
	private DeviceNodeListener listener;

	public RootDeviceListener(){
		devices = new ArrayList();

	}
	
	public void setDeviceNodeListener(DeviceNodeListener listener){
		this.listener = listener;
	}
	
	public synchronized void addNewDevice(ServiceReference sr){
		LogPanel.log("adding device:"+sr.getProperty(UPnPDevice.FRIENDLY_NAME));
		DeviceNode node = new DeviceNode(sr);
		//node.print();	
		if(node.isRoot() && node.isLeaf()){
			//Obiovsly
			listener.deviceDetected(node);
			return;
		}

		if(!node.isLeaf()){
			//I look if there is some partial tree that is child 
			//of my new node. This operation may complete the tree 
			Iterator list = devices.iterator();
			DeviceNode handle = null;
			while(list.hasNext()){
				DeviceNode tree = (DeviceNode) list.next();
				if((handle = node.isAttachable(tree)) != null){
					handle.attach(tree);
					list.remove();
                    monitorDevices();
					if(node.isRoot() && node.isComplete()){
						listener.deviceDetected(node);
//						removeChildren(node);
						return;
					}
				}
			}
		}
		
		if(!node.isRoot()){
			//I look if there is some partial tree that should own
			//my new node. This operation may complete the tree			
			Iterator list = devices.iterator();
			DeviceNode handle = null;
			while(list.hasNext()){
				DeviceNode tree = (DeviceNode) list.next();
				if((handle = tree.isAttachable(node)) != null){
					handle.attach(node);
					if(tree.isRoot() && tree.isComplete()){
						list.remove();
                        monitorDevices();
						listener.deviceDetected(tree);
//						removeChildren(tree);
					}
					return;
				}
			}		
		}
		
		devices.add(node);
        monitorDevices();
	}

	private void monitorDevices() {
        LogPanel.status("Pending Devices: " + devices.size() );      
    }

    public void removeDevice(ServiceReference sr){
		DeviceNode node = new DeviceNode(sr);
		if (node.isRoot()) {
			LogPanel.log("removing root device ..."+sr.getProperty(UPnPDevice.FRIENDLY_NAME));
			listener.rootDeviceUnplugged(node.toString());
			return;
		}

	}
	/**
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent e) {
		switch(e.getType()){
			case ServiceEvent.REGISTERED:{
				addNewDevice(e.getServiceReference());
			};break;
			
			case ServiceEvent.MODIFIED:{				
			};break;
			
			case ServiceEvent.UNREGISTERING:{	
				removeDevice(e.getServiceReference());
			};break;
				
		}
	}


	/**
	 * Register this object to listen to all "well registered" UPnPDevice<br> 
	 * that should be Exported<br>
	 * And look for all the already registered UPnPDevice to be exported
	 * 
	 */
	public void activate() {
		/*
		 * I listen for the UPnPDevice service that are Root 
		 * that should be exported to UPnP Network.
		 */
	    try {
			Activator.context.addServiceListener(this,
				"(&"+
					"("+Constants.OBJECTCLASS+"="+UPnPDevice.class.getName()+")"+
					"("+UPnPDevice.UDN+"=*)"+
				")"
				);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		ServiceReference[] roots = null; 
		try {
			roots = Activator.context.getServiceReferences(
					UPnPDevice.class.getName(),
					"(&"+
					"("+Constants.OBJECTCLASS+"="+UPnPDevice.class.getName()+")"+
					"("+UPnPDevice.UDN+"=*)"+
					")"
				);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		if(roots!=null){
			for (int i = 0; i < roots.length; i++) {
				addNewDevice(roots[i]);
			}
		}
	}

	/**
	 * 
	 */
	public void deactive() {
	    Activator.context.removeServiceListener(this);
	}
    
    
    
    
    
    
    public void checkIncompleteDevice() {
        if (devices.size() == 0){
            LogPanel.log("[check incomplete device] no pending devices !"  );
            return;
        }
        Iterator list = devices.iterator();
        while(list.hasNext()){
            LogPanel.log("[check incomplete device] "  );           
            LogPanel.log("------- device -------- "  );           
            DeviceNode tree = (DeviceNode) list.next();
            printProperties(tree.getReference());
        }
        
    }
    
    public static void checkErrataDevice() {
        ServiceReference[] allUPnPDevice = null; 
        ServiceReference[] UPnPbaseDriverDevice = null;
        try {
            allUPnPDevice = Activator.context.getServiceReferences(
                    UPnPDevice.class.getName(),
                    "(&" + "(" + UPnPDevice.UPNP_EXPORT + "=*)" 
                         + "("+Constants.OBJECTCLASS+"="+UPnPDevice.class.getName()+")"
                    + ")"
                );
            // filter used by UPnP base Driver
            UPnPbaseDriverDevice = Activator.context.getServiceReferences(UPnPDevice.class.getName(),
                        "(&" + "(" + UPnPDevice.UPNP_EXPORT + "=*)" + 
                                "(DEVICE_CATEGORY=UPnP)" + 
                                "(" + UPnPDevice.UDN + "=*)" + 
                                "(" + UPnPDevice.FRIENDLY_NAME + "=*)" +  
                                "(" + UPnPDevice.MANUFACTURER + "=*)" + 
                                "(" + UPnPDevice.MODEL_NAME + "=*)" + 
                                "(" + UPnPDevice.TYPE + "=*)" + 
                                "(!("+ UPnPDevice.PARENT_UDN + "=*))" + 
            ")");

        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        if (allUPnPDevice!=null){
            if (UPnPbaseDriverDevice!= null){
                if (allUPnPDevice.length == UPnPbaseDriverDevice.length){                   
                    LogPanel.log("---- Check Errata Device ----");
                    LogPanel.log("All registered Devices have mandatory properties specified");
                    LogPanel.log("--------  End Check  --------");
                }
                else if (allUPnPDevice.length > UPnPbaseDriverDevice.length)
                    printErrataDevice(allUPnPDevice,UPnPbaseDriverDevice) ;               
            }
            else {
                printErrataDevice(allUPnPDevice,UPnPbaseDriverDevice) ;               
            }
        }
        
    }
    
    public static void  printErrataDevice(ServiceReference[] allUPnPDevice,ServiceReference[] UPnPbaseDriverDevice) {
        LogPanel.log("---- Check Errata Device ----");
        for (int i =0;i< allUPnPDevice.length;i++){
             if (! isPresent(allUPnPDevice[i],UPnPbaseDriverDevice)){
                 LogPanel.log("---- Device ----");
                 printProperties(allUPnPDevice[i]);
             }
         }
        LogPanel.log("--------  End Check  --------");
    }
    
    public static boolean isPresent(ServiceReference sr, ServiceReference[] list) {
        Object s = Activator.context.getService(sr);
        for(int i = 0; i<list.length;i++){
            Object item =Activator.context.getService(list[i]);
            if (s == item) {
                Activator.context.ungetService(sr);
                Activator.context.ungetService(list[i]);
                return true;
            }
            Activator.context.ungetService(list[i]);           
        }
        Activator.context.ungetService(sr);
        return false;
    }
    
    public static void printProperties(ServiceReference service){
        String[] properties = service.getPropertyKeys();
        if (properties == null)
            LogPanel.log("properties are null");
        else {
            for(int j =0;j<properties.length;j++)
                LogPanel.log(properties[j] + "=" + Util.justString(service.getProperty(properties[j])));
        }
        
        if ( service.getProperty(UPnPDevice.UDN)== null) 
            LogPanel.log("[Warning] missing property: "+UPnPDevice.UDN);
        if ( service.getProperty(UPnPDevice.FRIENDLY_NAME)== null) 
            LogPanel.log("[Warning] missing property: "+UPnPDevice.FRIENDLY_NAME);
        if ( service.getProperty(UPnPDevice.MANUFACTURER)== null) 
            LogPanel.log("[Warning] missing property: "+UPnPDevice.MANUFACTURER);
        if ( service.getProperty(UPnPDevice.MODEL_NAME)== null) 
            LogPanel.log("[Warning] missing property: "+UPnPDevice.MODEL_NAME);
        if ( service.getProperty(UPnPDevice.TYPE)== null) 
            LogPanel.log("[Warning] missing property: "+UPnPDevice.TYPE);

    }
    

}
