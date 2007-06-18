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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;

import org.apache.felix.upnp.basedriver.Activator;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class RootDeviceListener implements ServiceListener {

	private RootDeviceExportingQueue queue;

	private ArrayList devices;

	public RootDeviceListener(RootDeviceExportingQueue queue) {
		this.queue = queue;
		devices = new ArrayList();
	}

	public synchronized void addDevice(ServiceReference sr) {
		DeviceNode node = new DeviceNode(sr);
		
	if(node.isRoot() && node.isLeaf()){
		//Obiovsly
		queue.addRootDevice(node);
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
				if(node.isRoot() && node.isComplete()){
					queue.addRootDevice(node);
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
					queue.addRootDevice(tree);
				}
				return;
			}
		}		
	}
	
	devices.add(node);

	}

	/**
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent e) {
		switch (e.getType()) {
			case ServiceEvent.REGISTERED: {
				addDevice(e.getServiceReference());
			}break;

			case ServiceEvent.MODIFIED: 
			break;

			case ServiceEvent.UNREGISTERING: {
				removeDevice(e.getServiceReference());
			}break;
		}
	}

	/**
	 * @param serviceReference
	 */
	public synchronized void removeDevice(ServiceReference sr) {
		String udn = (String) sr.getProperty(UPnPDevice.UDN);
		DeviceNode removeNode = null;
		/*
		 * I look in the queue of complete device that will be exported
		 */
		
		DeviceNode root=this.queue.removeRootDevice(udn);
		if(root!=null) removeNode=root.dethatch(udn);

		/*
		 * I look in the list that conatin unqueue,so no-complete Device 
		 */
		Iterator i = this.devices.iterator();
		while (i.hasNext()) {
			root = (DeviceNode) i.next();
			removeNode = root.dethatch(udn);
		}
		
		/*
		 * if I found the device removeNode != null
		 */
		if(removeNode==null) return;
		/*
		 * I get all the already attached subdevice and add to 
		 * unttached device list
		 */
		Collection c=removeNode.getChildren();
		if(c==null) return;		
		this.devices.addAll(c);
	}

	/**
	 * Register this object to listen to all "well registered" UPnPDevice <br>
	 * that should be Exported <br>
	 * And look for all the already registered UPnPDevice to be exported
	 *  
	 */
	public void activate() {
		/*
		 * I listen for the UPnPDevice service that are Root that should be
		 * exported to UPnP Network.
		 * 
		 * Please Note: The following filter disable listining for bad registerd
		 * UPnPDevice
		 */
		try {
			Activator.bc.addServiceListener(this, 
					"(&" + 
					"(" + Constants.OBJECTCLASS	+ "=" + UPnPDevice.class.getName() + ")" + 
					"("	+ UPnPDevice.UPNP_EXPORT + "=*)" + 
					"(DEVICE_CATEGORY=UPnP)" + 
					"(" + UPnPDevice.UDN + "=*)" + 
					"("	+ UPnPDevice.FRIENDLY_NAME + "=*)" +
					"("	+ UPnPDevice.MANUFACTURER + "=*)" + 
					"("	+ UPnPDevice.MODEL_NAME + "=*)" + 
					"(" + UPnPDevice.TYPE + "=*)" + 
					")");
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		ServiceReference[] roots = null;
		try {
			roots = Activator.bc.getServiceReferences(UPnPDevice.class.getName(),
					"(&" + "(" + UPnPDevice.UPNP_EXPORT + "=*)"	+ 
							"(DEVICE_CATEGORY=UPnP)" + 
							"(" + UPnPDevice.UDN + "=*)" + 
							"(" + UPnPDevice.FRIENDLY_NAME + "=*)" +  
							"(" + UPnPDevice.MANUFACTURER + "=*)" + 
							"("	+ UPnPDevice.MODEL_NAME + "=*)" + 
							"("	+ UPnPDevice.TYPE + "=*)" + 
							"(!("+ UPnPDevice.PARENT_UDN + "=*))" + 
							")");
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		if (roots != null) {
			for (int i = 0; i < roots.length; i++) {
				addDevice(roots[i]);
			}
		}
	}	

	/**
	 *  
	 */
	public void deactive() {
		Activator.bc.removeServiceListener(this);
	}

}
