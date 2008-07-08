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



import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;

import org.apache.felix.upnp.tester.Mediator;
import org.apache.felix.upnp.tester.discovery.DeviceNode;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class PropertiesViewer extends JPanel {

    ActionPanel actionsPanel;
	SubscriptionPanel subscriptionPanel;
	DataModel dataModel;
	JTable table;
	
	public PropertiesViewer(){
		super(new GridBagLayout());
		buildTable();
		actionsPanel = new ActionPanel();
		subscriptionPanel = new SubscriptionPanel();
		
		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(500,200));
		//actionsPanel.setPreferredSize(new Dimension(500,100)); twa
		actionsPanel.setPreferredSize(new Dimension(500,200));
		add(scroll,Util.setConstrains(0,0,6,2,100,50));
		add(actionsPanel,Util.setConstrains(0,2,6,1,100,20));
		add(subscriptionPanel,Util.setConstrains(0,3,6,1,100,5));
		showActionPanel(false);
		showSubscriptionPanel(false);
         
        table.addMouseMotionListener(new MouseMotionListener(){
           private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
           
           public void mouseDragged(MouseEvent e) {}
           public void mouseMoved(MouseEvent e) {
                String link = getDataLink(e.getPoint());
                if (link != null)
                    table.setCursor(handCursor);                       
                else
                    table.setCursor(Cursor.getDefaultCursor());
           }

        });
        
        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                String link = getDataLink(e.getPoint());
                if (link != null)
                    Util.openUrl(link);              
            }
        });

    }   
        
    public String getDataLink(Point p) {
        int col = table.columnAtPoint(p);
        if (col != 1) 
            return null;
        else {
            int row = table.rowAtPoint(p);
            String data = (String) dataModel.getValueAt(row,col);
            if (data == null) return null;
            if (data.length()<4) return null;
            String begin = data.substring(0,4);
            if (begin.equalsIgnoreCase("http"))
                return data;
            else {
                String propertyName= (String) dataModel.getValueAt(row,0);
                if (propertyName.equalsIgnoreCase(UPnPDevice.PRESENTATION_URL)
                    ||propertyName.equalsIgnoreCase(UPnPDevice.MANUFACTURER_URL)
                    ||propertyName.equalsIgnoreCase(UPnPDevice.MODEL_URL) )
                    {
                    JTree tree = Mediator.getUPnPDeviceTree();
                    UPnPDeviceTreeNode selectedNode = (UPnPDeviceTreeNode)tree.getLastSelectedPathComponent();   
                    String url = "";
                    if (selectedNode.category.equals(UPnPDeviceTreeNode.DEVICE)){
                        UPnPDeviceTreeNode parent =  (UPnPDeviceTreeNode)selectedNode.getParent();
                        while (parent.category!=UPnPDeviceTreeNode.ROOT_DEVICE)
                             parent =  (UPnPDeviceTreeNode)parent.getParent();
                        DeviceNode device =  (DeviceNode) parent.getUserObject();
                        String udn = (String)device.getReference().getProperty(UPnPDevice.UDN);
                        url = Mediator.getDriverProxy().resolveRelativeUrl(udn,data);
                        return url;
                    }                           
                    else if (selectedNode.category.equals(UPnPDeviceTreeNode.ROOT_DEVICE))
                    {
                        DeviceNode node =  (DeviceNode) selectedNode.getUserObject();
                        String udn = (String)node.getReference().getProperty(UPnPDevice.UDN);
                        url = Mediator.getDriverProxy().resolveRelativeUrl(udn,data);
                        return url;
                    }
                }
                return null;     
            }
        }
	}
	
	public void setProperties(String[]name,String[]values){
		dataModel.setData(name,values);
	}
	
	public void setAction(UPnPAction action){
		actionsPanel.setArguments(action);
		
	}
	
	public void showActionPanel(boolean show){
		actionsPanel.setVisible(show);
	}
	
	public void showSubscriptionPanel(boolean show){
	    subscriptionPanel.setVisible(show);
	}
	
	private void buildTable(){
		dataModel = new DataModel();
	    table = new JTable(dataModel);
        table.setDefaultRenderer(table.getColumnClass(1), new LinkCellRenderer());
	}


}

class DataModel extends  AbstractTableModel {
	int size = 0;
	String[] names = new String[]{""};
	String[] values = new String[]{""};
    String[] header = new String[]{"property key","value"};
    
    public String getColumnName(int col) { return header[col]; }
	public int getColumnCount() { return 2; }
    public int getRowCount() { return size;}
    public Object getValueAt(int row, int col) { 
    	if (col==0) return names[row];
    	else return values[row];
    }
    
    /*
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    */
    
    public void setData(String[]names,String[]values){
    	this.names=names;
    	this.values=values;
    	size = names.length;
		this.fireTableChanged(new TableModelEvent(this));
    }
    
}

class LinkCellRenderer extends DefaultTableCellRenderer
{
    public LinkCellRenderer(){
        super();
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        if (column==1){
            String propertyName = (String)table.getModel().getValueAt(row,0);
            if (propertyName.equalsIgnoreCase(UPnPDevice.PRESENTATION_URL)
                ||propertyName.equalsIgnoreCase(UPnPDevice.MODEL_URL)
                ||propertyName.equalsIgnoreCase(UPnPDevice.MANUFACTURER_URL))
            {
                if (!value.equals(""))
                    setValue("<html><a href=''>"+value+"</a></html>");
                return this;
            }
            if (((String) value).length()<4) return this;
            String begin = ((String) value).substring(0,4);
            if (begin.equalsIgnoreCase("http"))
                setValue("<html><a href=''>"+value+"</a></html>");
        }
        return this;
    }
 
}

