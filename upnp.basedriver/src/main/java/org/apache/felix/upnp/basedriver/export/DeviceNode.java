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
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class DeviceNode {
	
	private ServiceReference sr;
	private boolean isRootNode;
	private String udn ;
	private boolean hasChild;
	private int numberOfSons;
	private ArrayList children;
	private DeviceNode parent;
	
	public DeviceNode(ServiceReference sr){
		//PRE: argument is always UPnPDevice service reference
		if (sr == null) 
			throw new IllegalArgumentException("null is not a valid arg in DeviceNode constructor");
		this.sr = sr;
		udn = (String) sr.getProperty(UPnPDevice.UDN);
		parent=null;
		isRootNode = (sr.getProperty(UPnPDevice.PARENT_UDN) == null);
		String[] sons = ((String[]) sr.getProperty(UPnPDevice.CHILDREN_UDN));
		hasChild = (sons != null);
		if (hasChild) {
			children = new ArrayList();
			numberOfSons = sons.length;
		}
	}
	
	public ServiceReference getReference(){
		return sr;
	}
	public UPnPDevice getDevice(BundleContext ctx){
		return (UPnPDevice)ctx.getService(sr);
	}
	
	public void attach(DeviceNode node){
		if (node == null) 
			throw new IllegalArgumentException("null is not a valid arg in DeviceNode.attach() method");
		node.parent = this;
		children.add(node);
	}
	
	public DeviceNode dethatch(String name){
		DeviceNode dn = this.search(name);
		if(dn==null) 
			return null;
		
		if(dn.parent!=null){
			Iterator list = dn.parent.children.iterator();
			while (list.hasNext()) {
				DeviceNode x = (DeviceNode) list.next();
				if(x.udn.equals(name)){
					list.remove();
					break;
				}
			}
		}
		dn.parent=null;
		return dn;
	}
	
	public Collection getAllChildren(){
		if((this.children==null)||(this.children.size()==0)) 
			return null;
		
		Vector v = new Vector(this.children);
		Iterator list = this.children.iterator();
		while (list.hasNext()) {
			DeviceNode x = (DeviceNode) list.next();
			Collection c = x.getAllChildren();
			if(c==null) continue;
			v.addAll(c);
		}
		return v;
	}

	public Collection getChildren(){
		if((this.children==null)||(this.children.size()==0)) 
			return null;
		return this.children;
	}	
	
	/**
	 * @param name <code>String</code> that contain the UDN to look for
	 * @return return a <code>DeviceNode</code> that have the UDN equals to name and <br>
	 * 		if there is any <code>DeviceNode</code> with the proper UDN value return <code>null</code>
	 */
	public DeviceNode search(String name){
		if (name == null) 
			throw new IllegalArgumentException("null is not a valid arg in DeviceNode.search() method");
		if (name.equals(udn))
			return this;
		else if (hasChild){
			Iterator list = children.iterator();
			while (list.hasNext()){
				DeviceNode child = (DeviceNode)list.next();
				DeviceNode node = child.search(name);
				if (node != null) return node;				
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param udn
	 * @return <code>true</code> if and only if this <code>DeviceNode</code>
	 * 		contains a DeviceNode with UDN equals to passed argument or if
	 * 		its USN is equal to passed argument
	 */
	public boolean contains(String udn){
		return this.search(udn)!=null;
	}
	
	public boolean isComplete(){
		if (! hasChild) return true;
		if (numberOfSons != children.size())return false;
		Iterator list = children.iterator();
		while (list.hasNext()){
			DeviceNode child = (DeviceNode)list.next();
			if (! child.isComplete()) return false;
		}
		return true;
	}
	
	public DeviceNode isAttachable(DeviceNode node){
		if (node == null) 
			throw new IllegalArgumentException("null is not a valid arg in DeviceNode.isAttachable() method");
		String parentUDN=(String) node.getReference().getProperty(UPnPDevice.PARENT_UDN);
		if(parentUDN==null) return null;
		return search(parentUDN);		
	}
		
	public boolean isRoot(){
		return isRootNode;		
	}
	
	public boolean equals(String udn){
		return this.udn.equals(udn);
	}
	
	public String toString(){
		return udn;
	}

	/**
	 * @return true if and only 
	 * 		if the Device doesn't have embedded Device
	 */
	public boolean isLeaf() {
		return !hasChild;
	}		
}
