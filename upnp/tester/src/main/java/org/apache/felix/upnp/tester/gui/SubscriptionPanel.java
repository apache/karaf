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

package org.apache.felix.upnp.tester.gui;



import java.awt.event.ActionEvent;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;

import org.apache.felix.upnp.tester.Activator;
import org.apache.felix.upnp.tester.Mediator;
import org.apache.felix.upnp.tester.UPnPSubscriber;
import org.apache.felix.upnp.tester.discovery.DeviceNode;
/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class SubscriptionPanel extends JPanel implements UPnPEventListener{
	public SubscriptionPanel() {
		super();
		buildButtonPanel();
	}

	private void buildButtonPanel(){
		JButton subscribeBtn = new JButton("Subscribe");
		subscribeBtn.addActionListener(new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
			    //System.out.println("subscribing ...");
			    doSubscribe();
			}
		});		
		JButton unsubscribeBtn = new JButton("Unsubscribe");
		unsubscribeBtn.addActionListener(new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
			    //System.out.println("unsubscribing ...");
			    doUnsubscribe();
				}
		});		
	    add(subscribeBtn);
	    add(unsubscribeBtn);
	}
	
	UPnPSubscriber subscriber;
	public void doSubscribe()
	{
		if (subscriber == null)
		    subscriber = new UPnPSubscriber(Activator.context,this);
		
		UPnPDeviceTreeNode selectedNode = getSelectedNode();
		String serviceId = getServiceId(selectedNode);
		String parentId = getDeviceId(selectedNode);
		LogPanel.log("subscribing ... "+ "ServiceId ["+serviceId+"] of DeviceId ["+parentId +"]");
		subscriber.subscribe(parentId,serviceId);
		setSubscribedVariableOf(selectedNode);
	}
	
	public void doUnsubscribe(){
		UPnPDeviceTreeNode selectedNode = getSelectedNode();
		String serviceId = getServiceId(selectedNode);
		String parentId = getDeviceId(selectedNode);
		LogPanel.log("unsubscribing ... "+ "ServiceId ["+serviceId+"] of DeviceId ["+parentId +"]");
		subscriber.unsubscribe(parentId,serviceId);
		setUnubscribedVariableOf(selectedNode);
		}
	
	public void notifyUPnPEvent(final String deviceId, final String serviceId, final Dictionary events) {
        // UPnP base driver notify are synchronous !!
		Runnable doShowMsg = new Runnable() {
       	 public void run() {
			LogPanel.log("notifyUPnPEvent::[DeviceID "+deviceId+"][ServiceId "+serviceId+"]");
			Enumeration elements = events.keys();
			while (elements.hasMoreElements()){
			    Object key = elements.nextElement();
			    Object value = events.get(key);
			    LogPanel.log("["+key+"][value "+value+"]");
			}
       	 }
        };
        SwingUtilities.invokeLater(doShowMsg);
		
	}
	
	private void setSubscribedVariableOf(UPnPDeviceTreeNode selectedNode){
	    Enumeration list = selectedNode.children();
	    while (list.hasMoreElements()){
	        UPnPDeviceTreeNode node =  (UPnPDeviceTreeNode) list.nextElement();
	        if (node.category == UPnPDeviceTreeNode.EVENTED_STATE)
	            node.category = UPnPDeviceTreeNode.SUBSCRIBED_STATE;
	    }
	    JTree tree = Mediator.getUPnPDeviceTree();
	    tree.validate();
	    tree.repaint();
	}
	
	private void setUnubscribedVariableOf(UPnPDeviceTreeNode selectedNode){
	    Enumeration list = selectedNode.children();
	    while (list.hasMoreElements()){
	        UPnPDeviceTreeNode node =  (UPnPDeviceTreeNode) list.nextElement();
	        if (node.category == UPnPDeviceTreeNode.SUBSCRIBED_STATE)
	            node.category = UPnPDeviceTreeNode.EVENTED_STATE;
	    }
	    JTree tree = Mediator.getUPnPDeviceTree();
	    tree.validate();
	    tree.repaint();
	}
	
	private  UPnPDeviceTreeNode getSelectedNode(){
		JTree tree = Mediator.getUPnPDeviceTree();
		UPnPDeviceTreeNode selectedNode = (UPnPDeviceTreeNode)tree.getLastSelectedPathComponent();
	    return selectedNode;
	}
	private  String getServiceId (UPnPDeviceTreeNode selectedNode){
		Object userObj = selectedNode.getUserObject();
	    String serviceId = ((UPnPService) userObj).getId();
	    return serviceId;
	}
	private String getDeviceId (UPnPDeviceTreeNode selectedNode){
	     UPnPDeviceTreeNode parent = (UPnPDeviceTreeNode)selectedNode.getParent();
	     DeviceNode node =(DeviceNode)parent.getUserObject();
	     String parentId = (String) node.getReference().getProperty(UPnPDevice.ID);
	    return parentId;
	}
	
	
	
}


