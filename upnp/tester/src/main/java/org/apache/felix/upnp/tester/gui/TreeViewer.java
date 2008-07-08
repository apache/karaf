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


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.tester.Activator;
import org.apache.felix.upnp.tester.Mediator;
import org.apache.felix.upnp.tester.discovery.DeviceNode;
import org.apache.felix.upnp.tester.discovery.DeviceNodeListener;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class TreeViewer extends JPanel 	implements DeviceNodeListener 
{
	
	private UPnPDeviceTreeNode root;
	private DefaultTreeModel treeModel;
	private JTree tree;
    final TreePopup popup ;
	public TreeViewer(){
		super(new BorderLayout());
		Mediator.setTreeViewer(this);
		root = new UPnPDeviceTreeNode("UPnP Devices");
		treeModel= new DefaultTreeModel(root);
		tree = new JTree(treeModel);
		Mediator.setUPnPDeviceTree(tree);
		tree.setCellRenderer(new TreeNodeCellRenderer() );
		tree.putClientProperty("JTree.lineStyle", "Angled");
        add(new JScrollPane(tree));
		addTreeSelectionListener();
        
        
        popup = new TreePopup(tree);
        popup.setEnabled(false);
        tree.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                if (SwingUtilities.isRightMouseButton(e)){
                    TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                    popup.show(tree, e.getX(), e.getY());
                }
            }
        });
        ToolTipManager.sharedInstance().registerComponent(tree);
         

	}
	
    public void setPopupMenuEnabled(boolean driverControllerAvailable){
        popup.getComponent(0).setEnabled(driverControllerAvailable);
    }
	public void deviceDetected(DeviceNode node) {
		root.add(new UPnPDeviceTreeNode(node,Activator.context));
		treeModel.nodeStructureChanged(root);
	}
	
	public void rootDeviceUnplugged(String udn){
		Enumeration list = root.children();
		while (list.hasMoreElements()){
			UPnPDeviceTreeNode node= (UPnPDeviceTreeNode)list.nextElement();
			DeviceNode device = (DeviceNode)node.getUserObject();
			if (udn.equals(device.toString())) {
				node.removeFromParent();
				treeModel.nodeStructureChanged(root);
			}
		}
	}

	private void addTreeSelectionListener(){
		tree.addTreeSelectionListener(new TreeSelectionListener(){
			public void valueChanged(TreeSelectionEvent e){
				UPnPDeviceTreeNode selectedNode = (UPnPDeviceTreeNode)tree.getLastSelectedPathComponent();				
				doNodeAction(selectedNode);			
			}		
		});
	}
	
	private void doNodeAction(UPnPDeviceTreeNode node){
		if (node == null) {
			clearPropertiesViewer();
			return;
		}
		if (node.category.equals(UPnPDeviceTreeNode.ACTION))
			Mediator.getPropertiesViewer().showActionPanel(true);
		else
			Mediator.getPropertiesViewer().showActionPanel(false);

		if (node.category.equals(UPnPDeviceTreeNode.SERVICE))
			Mediator.getPropertiesViewer().showSubscriptionPanel(true);
		else
			Mediator.getPropertiesViewer().showSubscriptionPanel(false);

		if ( node.category.equals(UPnPDeviceTreeNode.DEVICE)
			||node.category.equals(UPnPDeviceTreeNode.ROOT_DEVICE)){
			DeviceNode device = (DeviceNode) node.getUserObject();
			UPnPDevice upnpDevice = device.getDevice(Activator.context);
			makeProperties(upnpDevice);
		} 
		else if (node.category.equals(UPnPDeviceTreeNode.SERVICE)){
			UPnPService service = (UPnPService) node.getUserObject();			
			makeProperties(service);
		}
		else if (node.category.equals(UPnPDeviceTreeNode.ACTION)){
			UPnPAction action = (UPnPAction) node.getUserObject();
			makeProperties(action);
			Mediator.getPropertiesViewer().setAction(action);
		}
		else if (node.category.equals(UPnPDeviceTreeNode.STATE)
		        ||node.category.equals(UPnPDeviceTreeNode.EVENTED_STATE)
		        ||node.category.equals(UPnPDeviceTreeNode.SUBSCRIBED_STATE)){
			UPnPStateVariable state = (UPnPStateVariable) node.getUserObject();
			makeProperties(state);
		}
		
	}
	
	private void clearPropertiesViewer(){
		String[] names = new String[]{};
		String[] values = new String[]{};
		PropertiesViewer viewer = Mediator.getPropertiesViewer();
		viewer.setProperties(names,values);
		viewer.showActionPanel(false);
		viewer.showSubscriptionPanel(false);
	}
	
	private void makeProperties(UPnPDevice upnpDevice){
		Dictionary dict = upnpDevice.getDescriptions(null);
		int size = dict.size();
		String[] names = new String[size];
		String[] values = new String[size];
		Enumeration keys = dict.keys();
		for (int i=0;i<size;i++){
			names[i]= (String) keys.nextElement();
			values[i]= Util.justString(dict.get(names[i]));
		}
		Mediator.getPropertiesViewer().setProperties(names,values);
	}
	
	private void makeProperties(UPnPService service){
		String[] names = new String[]{"Id","Type","Version"};
		String[] values = new String[]{service.getId(),service.getType(),service.getType()};
		Mediator.getPropertiesViewer().setProperties(names,values);
	}
	
	private void makeProperties(UPnPAction action){
		ArrayList names = new ArrayList();
		ArrayList values = new ArrayList();
		names.add("Name");
		values.add(action.getName());
		
		String returnName = action.getReturnArgumentName();
		if (returnName != null){
			names.add("Return value name");
			values.add(returnName);
		}
		String[] inArg = action.getInputArgumentNames();
		if (inArg != null){
			for (int i = 0; i<inArg.length;i++){
				names.add("Input arg["+ (i+1)+"]");
				values.add(inArg[i]);			
			}
		}
		String[] outArg = action.getOutputArgumentNames();
		if (outArg != null){
			for (int i = 0; i<outArg.length;i++){
				names.add("Output arg["+ (i+1)+"]");
				values.add(outArg[i]);			
			}
		}
		
		Mediator.getPropertiesViewer().setProperties(
				(String[])names.toArray(new String[]{}),
				(String[])values.toArray(new String[]{})
		);
		
	}
	
	private void makeProperties(UPnPStateVariable state){
		ArrayList names = new ArrayList();
		ArrayList values = new ArrayList();
		names.add("Name");
		values.add(state.getName());
		names.add("Evented");
		values.add(state.sendsEvents()? "yes":"no");
		names.add("Default Value");
		values.add(Util.justString(state.getDefaultValue()));
		names.add("Java Data Type");
		values.add(state.getJavaDataType().getName());
		names.add("Java UPnP Type");
		values.add(state.getUPnPDataType());
		names.add("Minimum");
		values.add(Util.justString(state.getMinimum()));
		names.add("Maximum");
		values.add(Util.justString(state.getMaximum()));
		names.add("Step");
		values.add(Util.justString(state.getStep()));		
		String[] allowed = state.getAllowedValues();
		if (allowed!=null){
			for (int i=0;i<allowed.length;i++){
				names.add("Allowed value["+i+1+"]");
				values.add(allowed[i]);		
			}			
		}
		Mediator.getPropertiesViewer().setProperties(
				(String[])names.toArray(new String[]{}),
				(String[])values.toArray(new String[]{})
		);
	}
	
	
}

class TreePopup extends JPopupMenu implements PopupMenuListener {
    JTree tree;
    JMenuItem item;

    public TreePopup(final JTree tree){
        super();
        this.tree = tree;
        (item = add(new AbstractAction(){
            public void actionPerformed(ActionEvent e){
                UPnPDeviceTreeNode selectedNode = (UPnPDeviceTreeNode)tree.getLastSelectedPathComponent();   
                String url = "";
                if (selectedNode.category.equals(UPnPDeviceTreeNode.DEVICE)){
                    UPnPDeviceTreeNode parent =  (UPnPDeviceTreeNode)selectedNode.getParent();
                    while (parent.category!=UPnPDeviceTreeNode.ROOT_DEVICE)
                         parent =  (UPnPDeviceTreeNode)parent.getParent();
                    DeviceNode device =  (DeviceNode) parent.getUserObject();
                    String udn = (String)device.getReference().getProperty(UPnPDevice.UDN);
                    url = Mediator.getDriverProxy().getDeviceDescriptionURI(udn);
                }
                        
                else if (selectedNode.category.equals(UPnPDeviceTreeNode.ROOT_DEVICE))
                {
                    DeviceNode node =  (DeviceNode) selectedNode.getUserObject();
                    String udn = (String)node.getReference().getProperty(UPnPDevice.UDN);
                    url = Mediator.getDriverProxy().getDeviceDescriptionURI(udn);
                }
                else if (selectedNode.category.equals(UPnPDeviceTreeNode.SERVICE))
                {
                    UPnPDeviceTreeNode parent =  (UPnPDeviceTreeNode)selectedNode.getParent();
                    while (parent.category!=UPnPDeviceTreeNode.ROOT_DEVICE)
                         parent =  (UPnPDeviceTreeNode)parent.getParent();
                    DeviceNode device =  (DeviceNode) parent.getUserObject();
                    String udn = (String)device.getReference().getProperty(UPnPDevice.UDN);
                    UPnPService service =  (UPnPService) selectedNode.getUserObject();
                    url = Mediator.getDriverProxy().getServiceDescriptionURI(udn,service.getId());
                }                    
                Util.openUrl(url);   
            }
        })).setText("Show Description");
        addPopupMenuListener(this);

    }
    
    public void popupMenuCanceled(PopupMenuEvent e){}
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e){}
    public void popupMenuWillBecomeVisible(PopupMenuEvent e){
        if (Mediator.getDriverProxy().isDriverAvailable()){
        UPnPDeviceTreeNode selectedNode = (UPnPDeviceTreeNode)tree.getLastSelectedPathComponent();              
            if (selectedNode.category.equals(UPnPDeviceTreeNode.DEVICE)
                ||selectedNode.category.equals(UPnPDeviceTreeNode.ROOT_DEVICE)
                ||selectedNode.category.equals(UPnPDeviceTreeNode.SERVICE))
            {
                item.setEnabled(true);
            } 
            else
                item.setEnabled(false);
        }
        else
            item.setEnabled(false);
           
    }
}
