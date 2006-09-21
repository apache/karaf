/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.mosgi.console.component;

import org.apache.felix.mosgi.console.ifc.CommonPlugin;
import org.apache.felix.mosgi.console.ifc.Plugin;
//import org.apache.felix.mosgi.console.component.JtreeCellRenderer;
import org.apache.felix.mosgi.console.component.MyTree;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;

import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import javax.swing.JButton;
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

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;

import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.JFileChooser;
		
import java.util.Date;
import java.text.DateFormat;
//import java.text.SimpleDateFormat;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.JFrame;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

//import org.osgi.service.prefs.Preferences;

public class RemoteLogger_jtree extends DefaultTreeModel implements CommonPlugin, NotificationListener{

  private MyTree logTree;
  private JPanel jp;
  private Hashtable nodes=new Hashtable();
  private DefaultMutableTreeNode rootNode=new DefaultMutableTreeNode("root");
  private Hashtable eventName=new Hashtable();

  public RemoteLogger_jtree (BundleContext bdlCtx){
    super(null);
    setRoot(rootNode);
    System.out.println("JTree Remote logger"); 

    this.jp=new JPanel();
    this.jp.setLayout(new BorderLayout());
   
    this.logTree=new MyTree(this);
    JtreeCellRenderer treeCellRenderer=new JtreeCellRenderer(bdlCtx);
    this.logTree.setCellRenderer(treeCellRenderer);
    this.logTree.setLargeModel(true);
    this.logTree.setToggleClickCount(1); 
    this.logTree.setRootVisible(false);
    // this create an invisible tree even if I use *expand* so...
    // I use expand after the first insert into the tree
  
    jp.add(new JScrollPane(logTree), BorderLayout.CENTER);    
    
    eventName.put(new Integer(Bundle.ACTIVE),     "ACTIVE     ");
    eventName.put(new Integer(Bundle.INSTALLED),  "INSTALLED  ");
    eventName.put(new Integer(Bundle.RESOLVED),   "RESOLVED   ");
    eventName.put(new Integer(Bundle.STARTING),   "STARTING   ");
    eventName.put(new Integer(Bundle.STOPPING),   "STOPPING   ");
    eventName.put(new Integer(Bundle.UNINSTALLED),"UNINSTALLED");
  }

  /////////////////////////////////////
  //  Plugin Interface ////////////////
  /////////////////////////////////////
  public String getName(){ return "JTree Remote Logger";}
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

  private DefaultMutableTreeNode createIfNeed(String nodeToCreateAndGet, DefaultMutableTreeNode parent){
    int childNumber=this.getChildCount(parent);
    DefaultMutableTreeNode theNode=null;
    for (int i=0 ; i<childNumber ; i++){ // is node even exist ?
      String string_pool=((DefaultMutableTreeNode)(this.getChild(parent, i))).toString();
      if (string_pool.equals(nodeToCreateAndGet)){
        theNode=(DefaultMutableTreeNode) (this.getChild(parent, i));
          break;
      }
    }
    if (theNode==null){ // create the node
      theNode=new DefaultMutableTreeNode(nodeToCreateAndGet);	
      // Unable to set tree expand whithout a first node
      if (rootNode.getChildCount()==0){
        this.insertNodeInto(theNode, parent, 0);
        logTree.expandPath(new TreePath(rootNode.getPath()));
      }else{
        this.insertNodeInto(theNode, parent, 0);
      }
    }
  return theNode;
  }

  public void handleNotification(Notification notification, Object handback) {
    StringTokenizer st=new StringTokenizer(handback.toString(),":");
    String ip=st.nextToken();
    String ref=st.nextToken();
    
    st = new StringTokenizer(notification.getMessage(),":");
    Date timeDate=new Date(notification.getTimeStamp());
    //DateFormat dateFormat = new SimpleDateFormat("hh'h'mm dd-MM-yy");
    DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM); // use local date format 
    DateFormat df2 = DateFormat.getDateInstance(DateFormat.SHORT);
    String time=df.format(timeDate);
    String date=df2.format(timeDate);

    String id=st.nextToken();
    String name=st.nextToken();
    String idname=new String(id+" : "+name);
    String state=""+eventName.get(new Integer((int) Integer.parseInt(st.nextToken())));
    String lvl=st.nextToken();
    String msg=st.nextToken();

    // Get and maybe create parents nodes : ip, ref, idname
    DefaultMutableTreeNode dmtn_ip=createIfNeed(ip, rootNode);
    DefaultMutableTreeNode dmtn_ref=createIfNeed(ref, dmtn_ip);
    DefaultMutableTreeNode dmtn_idname=createIfNeed(idname, dmtn_ref);

    // insert the leaf with message under id/ref/idname
    DefaultMutableTreeNode dmtn=new DefaultMutableTreeNode(date+" | "+time+" | "+state+" | "+lvl+" | "+msg,false); 
    this.insertNodeInto(dmtn, dmtn_idname, 0);

    this.reload(dmtn_idname);
    }
  
}
