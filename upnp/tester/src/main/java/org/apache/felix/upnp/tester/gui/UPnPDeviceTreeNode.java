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

import java.awt.Color;
import java.awt.Component;
import java.awt.image.ImageObserver;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.osgi.framework.BundleContext;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.tester.discovery.DeviceNode;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class UPnPDeviceTreeNode extends DefaultMutableTreeNode {
	public final static String ROOT_DEVICE = "RootDeviceNode";
	public final static String DEVICE = "DeviceNode";
	public final static String SERVICE = "UPnPService";
	public final static String ACTION = "UPnPAction";
	public final static String STATE = "UPnPStateVariable";
	public final static String EVENTED_STATE = "EventedStateVariable";
	public final static String SUBSCRIBED_STATE = "SubscribedStateVariable";
	
	protected String category;
	public UPnPDeviceTreeNode(String obj) {
		super(obj);
		category = obj.getClass().getName();
	}
	
	public UPnPDeviceTreeNode(DeviceNode obj, BundleContext ctx) {
		super(obj);
		
		if (obj.isRoot()) category = ROOT_DEVICE;
		else category = DEVICE;
		
		UPnPDevice device = (UPnPDevice)ctx.getService(obj.getReference());
		UPnPService[] services = device.getServices();
		
		Collection nodeChildren = obj.getChildren();
		if (nodeChildren != null){		
			Iterator list = nodeChildren.iterator();
			while (list.hasNext()){
				DeviceNode node = (DeviceNode)list.next();
				this.add(new UPnPDeviceTreeNode(node,ctx));
			}
		}
		if (services != null){
			for (int i=0;i<services.length;i++)
				this.add(new UPnPDeviceTreeNode(services[i]));
		}
		
	}
	
	public UPnPDeviceTreeNode(UPnPService obj) {
		super(obj);
		category = SERVICE;
		UPnPStateVariable[] variables = obj.getStateVariables();
		if (variables != null){
			for (int i=0;i<variables.length;i++)
				this.add(new UPnPDeviceTreeNode(variables[i]));
		}
		UPnPAction[] actions = obj.getActions();
		if (actions != null){
			for (int i=0;i<actions.length;i++)
				this.add(new UPnPDeviceTreeNode(actions[i]));
		}
	}
	public UPnPDeviceTreeNode(UPnPAction obj) {
		super(obj);
		category = ACTION;
	}
	
	public UPnPDeviceTreeNode(UPnPStateVariable obj) {
		super(obj);
		if (obj.sendsEvents()) category = EVENTED_STATE;
		else category = STATE;
	}
	
	public String toString() {
		if (category.equals(DEVICE)||category.equals(ROOT_DEVICE)){
			DeviceNode node =  (DeviceNode) getUserObject();
			return node.toString();
		}
		else if (category.equals(SERVICE)){
			UPnPService node =  (UPnPService) getUserObject();
			return node.getType();
		}
		else if (category.equals(ACTION)){
			UPnPAction node =  (UPnPAction) getUserObject();
			return node.getName();
		}
		else if (category.equals(STATE) ||category.equals(EVENTED_STATE)||category.equals(SUBSCRIBED_STATE)){
			UPnPStateVariable node =  (UPnPStateVariable) getUserObject();
			return node.getName();
		}
		else
			return getUserObject().toString();
	}
}

//   local class for JTree icon renderer
class TreeNodeCellRenderer extends DefaultTreeCellRenderer implements ImageObserver{
	
	private HashMap icons ;
	ImageIcon image;
	public TreeNodeCellRenderer() {
		super();
		icons = new HashMap();
		try {
			icons.put(UPnPDeviceTreeNode.EVENTED_STATE, loadIcon(UPnPDeviceTreeNode.EVENTED_STATE));
			image =  loadIcon(UPnPDeviceTreeNode.SUBSCRIBED_STATE);
			// to use animate gif
			//image.setImageObserver(this);
 			icons.put(UPnPDeviceTreeNode.SUBSCRIBED_STATE, image);

 			icons.put(UPnPDeviceTreeNode.ROOT_DEVICE, loadIcon(UPnPDeviceTreeNode.ROOT_DEVICE));
			icons.put(UPnPDeviceTreeNode.DEVICE, loadIcon(UPnPDeviceTreeNode.DEVICE));
			icons.put(UPnPDeviceTreeNode.SERVICE, loadIcon(UPnPDeviceTreeNode.SERVICE));
			icons.put(UPnPDeviceTreeNode.ACTION, loadIcon(UPnPDeviceTreeNode.ACTION));
			icons.put(UPnPDeviceTreeNode.STATE, loadIcon(UPnPDeviceTreeNode.STATE));
		} catch (Exception ex) {
			System.out.println(ex);
		}

	}
    
    public JToolTip createToolTip() {
        JToolTip tip = super.createToolTip();
        tip.setBackground(Color.yellow);
        return tip;
    }

	//test to display animated gif
	/* 
	 public boolean imageUpdate(Image img, int infoflags,
		       int x, int y, int width, int height){
	       	//System.out.println("image update");
	        Mediator.getUPnPDeviceTree().validate();
	        Mediator.getUPnPDeviceTree().repaint();
	        return true;
	   }
	 */

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		Icon icon = selectIcon((UPnPDeviceTreeNode) value);
        setToolTip((UPnPDeviceTreeNode) value);
		if (icon != null) {
			setOpenIcon(icon);
			setClosedIcon(icon);
			setLeafIcon(icon);
		} else {
			setOpenIcon(getDefaultOpenIcon());
			setClosedIcon(getDefaultClosedIcon());
			setLeafIcon(getDefaultLeafIcon());
		}
		return super.getTreeCellRendererComponent(tree, value, sel, expanded,
				leaf, row, hasFocus);
	}
	
    public Icon selectIcon(UPnPDeviceTreeNode node) {
        Icon icon = null;
        try {
                String tag = node.category;
                icon = (Icon) icons.get(tag);
        } catch (Exception ex) {
            System.out.println("getTreeCellRendererComponent Exception:" + ex);
        }
        return icon;
    }
    
    public void setToolTip(UPnPDeviceTreeNode node) {
        String tag = node.category;
        if (tag.equals(UPnPDeviceTreeNode.ROOT_DEVICE)
             ||tag.equals(UPnPDeviceTreeNode.DEVICE))
        {
            DeviceNode device = (DeviceNode) node.getUserObject();
            setToolTipText("<html><TABLE BORDER='0' CELLPADDING='0' CELLSPACING='0' ><TR BGCOLOR='#F9FF79' ><TD>" 
                    + device.getReference().getProperty(UPnPDevice.FRIENDLY_NAME).toString()
                    +"</TD></TR></TABLE ></html>");
        }
        else
            setToolTipText(null);
     }
	
    public  static ImageIcon loadIcon(String name)
    {
        try {
            /*
            System.out.println("loading image ..."+name);
            System.out.println("from "+"IMAGES/" + name + ".gif");
            */
            URL eventIconUrl = UPnPDeviceTreeNode.class.getResource("IMAGES/" + name + ".gif");           
            return new ImageIcon(eventIconUrl,name);
        }
        catch (Exception ex){
		        System.out.println("Resource:" + name + " not found : " + ex.toString());
            return null;
        }
    }

}
