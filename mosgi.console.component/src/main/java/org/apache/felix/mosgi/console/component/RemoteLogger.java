/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
*/
package org.apache.felix.mosgi.console.component;

import org.apache.felix.mosgi.console.ifc.CommonPlugin;
import org.apache.felix.mosgi.console.ifc.Plugin;

import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import javax.swing.ListSelectionModel;

import java.util.Hashtable;
import java.util.StringTokenizer;

import java.beans.PropertyChangeEvent;

public class RemoteLogger extends DefaultTableModel implements CommonPlugin, NotificationListener{
  private JTable logList;
  private JPanel jp;
  private Hashtable nodes=new Hashtable();

  public RemoteLogger (){
    super(new String[]{"Src", "Level", "Id", "Text"},1);
System.out.println("Remote logger v1");
    this.jp=new JPanel();
    
    logList = new JTable(this);
    logList.setPreferredScrollableViewportSize(new java.awt.Dimension(600, 70));
    
    logList.getColumnModel().getColumn(0).setPreferredWidth(130);
    logList.getColumnModel().getColumn(1).setPreferredWidth(50);
    logList.getColumnModel().getColumn(2).setPreferredWidth(10);
    logList.getColumnModel().getColumn(3).setPreferredWidth(300);
    

    logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    logList.getTableHeader().setReorderingAllowed(false);
    jp.add(new JScrollPane(logList));
    
  }

  /////////////////////////////////////
  //  Plugin Interface ////////////////
  /////////////////////////////////////
  public String getName(){ return "Remote Logger";}
  public Component getGUI(){return this.jp;}

/* a supprimer si on enleve l'heritage CommonPlugin -> Plugin */
  public String pluginLocation(){
    return null;
  }
  public void registerServicePlugin(){}
  public void unregisterServicePlugin(){}
/* fin a supprimer */

  
  public void propertyChange(PropertyChangeEvent e){
    if (e.getPropertyName().equals(Plugin.NEW_NODE_CONNECTION)){
      try{
        MBeanServerConnection mbs=(MBeanServerConnection)e.getNewValue();
        if (nodes.get(mbs)==null){
System.out.println("Ajout d'un listener " +mbs);
          ((MBeanServerConnection)e.getNewValue()).addNotificationListener(new ObjectName("OSGI:name=Remote Logger"), this, null, e.getOldValue());
          nodes.put(mbs, "ok");
        }
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    //int row=this.getRowCount();
    StringTokenizer st=new StringTokenizer(notification.getMessage(), ":");
    Object [] event=new Object []{handback, st.nextToken(), st.nextToken(), st.nextToken()};
    this.insertRow(0,event);
    this.fireTableRowsInserted(0, 0);
  }
  
}
