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
package org.apache.felix.mosgi.console.gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.JButton;

import java.util.Hashtable;
import java.util.Enumeration;
import org.osgi.framework.BundleContext;
import org.apache.felix.mosgi.console.ifc.Plugin;
import org.apache.felix.mosgi.console.ifc.CommonPlugin;
import org.apache.felix.framework.cache.BundleCache;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.NotificationListener;
import javax.management.Notification;
import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectionNotification;

//import javax.jmdns.ServiceInfo;

public class NodesTree extends JPanel implements TreeSelectionListener, NotificationListener, ActionListener, PropertyChangeListener {

  static String TOP_NAME="Servers";
  static int POOLING_TIME=2000;
  static String JMX_SERVICE_URL="service:jmx:rmi:///jndi/rmi://";
  private static boolean useSystemLookAndFeel = false;
  
  private Activator a;
  private BundleContext bc;
  
  protected JTree tree;
  private DefaultTreeModel dtm;
  private DefaultMutableTreeNode top = null;
  private NodeCellRenderer ncr;
  private JButton addNodeButton;
  private JButton removeNodeButton;

  private Hashtable connectedNodes = new Hashtable();
  private Hashtable jmxConnectors = new Hashtable();
  private Hashtable nodes=new Hashtable();
 
  protected boolean cont=true; 
  private PoolingThread pt;
  protected Thread poolThread;
  
  private boolean isAllNodesConnected=false;

  public NodesTree(Activator a,BundleContext bc) {
    super(new BorderLayout());
    this.a=a;
    this.bc=bc;
    this.pt=new PoolingThread();
    
    System.setProperty("java.security.policy", "client.policy");
    top=new DefaultMutableTreeNode(NodesTree.TOP_NAME);
    dtm=new DefaultTreeModel(top);
    tree=new JTree(dtm);
    tree.setCellRenderer(ncr=new NodeCellRenderer(bc,this));
    tree.addTreeSelectionListener(this);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    JScrollPane treeView=new JScrollPane(tree);
    add(createButtonPanel(), BorderLayout.NORTH);
    add(treeView, BorderLayout.CENTER);
  }

  public void runDiscovery(){
    this.createDefaultNodes();
    poolThread=new Thread(this.pt);
    poolThread.start();
  }

  //////////////////////////////////////////////////
  //          PropertyChangeListener              //
  //////////////////////////////////////////////////
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(CommonPlugin.COMMON_PLUGIN_ADDED)) {
      Enumeration enu=connectedNodes.keys();
      while (enu.hasMoreElements()) {
        // Common plugin added after a gateway connection so firePCE(Plugin.NEW_NODE_CONNECTION, connString , mbsc) again :
        String key=(String) enu.nextElement();
        System.out.println("   "+key+"="+connectedNodes.get(key));
        a.firePropertyChangedEvent(Plugin.NEW_NODE_CONNECTION, key, connectedNodes.get(key));
      }
    }
  }

  //////////////////////////////////////////////////////
  //               TreeSelectionListener              //
  //////////////////////////////////////////////////////
  public void valueChanged(TreeSelectionEvent e) {
    //System.out.println("Value changed : e="+e);
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
    if (node!=null) {
      String selectedNode=(String)node.getUserObject();
      if (!selectedNode.equals(NodesTree.TOP_NAME)){
        Object mbsc=connectedNodes.get(selectedNode);
        if (mbsc!=null){
	  //tryToConnectAllNodes();
	  //createDefaultNodes(false);
          a.firePropertyChangedEvent(Plugin.NEW_NODE_SELECTED, selectedNode, mbsc);
        }else{
          a.firePropertyChangedEvent(Plugin.EMPTY_NODE, null, "null");
        }
      }
    }
  }

  ////////////////////////////////////////////////////
  //            NotificationListener                //
  ////////////////////////////////////////////////////
  public void handleNotification(Notification notification, Object handback) {
    //    System.out.println("Connection ==> "+notification.getMessage()+":"+((JMXConnectionNotification)notification).getConnectionId()+":"+handback);
    if ( !JMXConnectionNotification.OPENED.equals(notification.getType()) ){
      this.disconnectFromNode((String)handback);
      tree.treeDidChange();
      isAllNodesConnected=false;
    }
  }
  
  ////////////////////////////////////////////////////
  //            Friend methods                      //
  ////////////////////////////////////////////////////
  void stop(){ //Never used ... ???
    this.cont=false;
  }

  boolean isNodeConnected(String s){//Never used ... ???
    if (this.connectedNodes.get(s)==null){
      return false;
    }else{
      return true;
    }
  }

  public void actionPerformed(ActionEvent e) {
    // TODO : 
    Object object = e.getSource();
    if (object==addNodeButton) { // Add a new node into tree
      String connString = JOptionPane.showInputDialog("Please input a connection string : ", "127.0.0.1:1099/vosgi");
      TreePath tp=tree.getSelectionPath();
      if (connString!=null) {
	createTreeNode(connString,null,null);
	dtm.reload(top);
	isAllNodesConnected=false;
	tree.setSelectionPath(tp);
      }
    } else if (object==removeNodeButton) { // Remove a nod from tree
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
      String connString=(String) node.getUserObject();
      if (connString!=null){
        if (!node.equals(top)){
	  if(JOptionPane.showConfirmDialog(null,"Sure we remove \""+connString+"\" node ?")==JOptionPane.YES_OPTION) {
	    if (top.getChildCount()>1){
              disconnectFromNode(connString);
	      dtm.removeNodeFromParent(node);
	      System.out.println("Remove node : "+(String) (node.getUserObject()));
	    }
          }
	}
      }
    }
  }

  ///////////////////////////////////////////////////
  //           Private part                        //
  ///////////////////////////////////////////////////
  protected void tryToConnectAllNodes(){
    //boolean oldTreeState=isAllNodesConnected;
    isAllNodesConnected=true;
    String connString="";
    Enumeration enu=top.breadthFirstEnumeration();
    while (enu.hasMoreElements()) {
      DefaultMutableTreeNode dmtn_tmp=(DefaultMutableTreeNode) enu.nextElement();
      if (dmtn_tmp!=top) {
        connString=(String) dmtn_tmp.getUserObject();
        if (!this.connectToNode(connString)) {
          isAllNodesConnected=false;
        }
      }
    }
    /*Enumeration dmtns_std=top.children();
    while (dmtns_std.hasMoreElements()){
      DefaultMutableTreeNode dmtn_std=(DefaultMutableTreeNode) dmtns_std.nextElement();
      connString=(String) dmtn_std.getUserObject();
      //System.out.println("   -connectToNode("+connString+")");
      if (!this.connectToNode(connString)) {
        isAllNodesConnected=false;
      }
      Enumeration dmtns_virtual=dmtn_std.children();
      while (dmtns_virtual.hasMoreElements()){
        DefaultMutableTreeNode dmtn_virtual=(DefaultMutableTreeNode) dmtns_virtual.nextElement();
        connString=(String) dmtn_virtual.getUserObject();
	//System.out.println("      -connectToNode("+connString+")");
        if (!this.connectToNode(connString)) {
          isAllNodesConnected=false;
        }
      }
    }*/
    //if(oldTreeState!=isAllNodesConnected){
    //  System.out.println("AllNodesConnected="+isAllNodesConnected);
    //}
  }

  private boolean connectToNode(String connString){
    Object ls=this.connectedNodes.get(connString);
    if (ls==null){
      JMXConnector jmxc=null;
      try {
        JMXServiceURL surl=new JMXServiceURL(NodesTree.JMX_SERVICE_URL+connString);
/*
Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
System.out.println("=====> "+java.util.logging.Logger.getLogger("javax.management.remote.misc"));
java.util.logging.Logger.getLogger("javax.management.remote.misc").setLevel(java.util.logging.Level.FINEST);
java.util.logging.Logger.getLogger("javax.management.remote.rmi").setLevel(java.util.logging.Level.FINEST);

java.util.logging.ConsoleHandler ch=new java.util.logging.ConsoleHandler();
ch.setLevel(java.util.logging.Level.FINEST);
java.util.logging.Logger.getLogger("javax.management.remote.misc").addHandler(ch);
java.util.logging.Logger.getLogger("javax.management.remote.rmi").addHandler(ch);
*/				
        jmxc = JMXConnectorFactory.connect(surl);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        if (mbsc.isRegistered(new ObjectName("OSGI:name=Remote Logger"))){
          jmxc.addConnectionNotificationListener(this, null, connString);
          mbsc.addNotificationListener(new ObjectName("TabUI:name=OsgiProbes"), this, null, null); //Needed ???
          this.connectedNodes.put(connString, mbsc);
          this.jmxConnectors.put(connString, jmxc);
          a.firePropertyChangedEvent(Plugin.NEW_NODE_CONNECTION, connString, mbsc);
	  // If new connected is the selected one then create a false valueChanged in order to load the NodePanel :
	  DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent(); 
	  if (node!=null) {
  	    if (connString.equals((String)node.getUserObject())){
	      this.valueChanged(null);
            }
	  }
	  System.out.println("Node "+connString+" connected");
	  return true;
        }else {
          System.out.println("The Remote Logger of "+connString+" is not started");
	  return false;
        }
      }catch(java.io.IOException ex){
        //System.out.println("Impossible to connect to "+connString);
      }catch(MalformedObjectNameException e){
        e.printStackTrace();
      }catch(Exception e){
	//use one thread per node to avoid being freeze by a timeOutConnection
	System.out.println("gui.NodesTree.connectToNode("+connString+") : "+e);
	System.out.println("  => Delete this node ? to implement... ?");

      }
    }else{
      return true;
    }
  return false;
  }

  protected class PoolingThread implements Runnable{
    public void run(){
      while (cont){
        if (!isAllNodesConnected) {
	  tryToConnectAllNodes();
          tree.treeDidChange();
        } 
	try{
          Thread.sleep(POOLING_TIME);
        }catch(InterruptedException e){
          e.printStackTrace();
        }
      }
    }
  }

  private void createTreeNode(String connString, String ip, String virtual){
    System.out.println("Add a gateway : ip="+ip+" connString="+connString+" virtual="+virtual);
    DefaultMutableTreeNode dmtn=new DefaultMutableTreeNode(connString);
    DefaultMutableTreeNode parent=top;
    if (virtual==null){
      nodes.put(connString, dmtn);
    }else{
      String parentString=ip+":"+virtual;
      parent=(DefaultMutableTreeNode)nodes.get(parentString);
    }
    parent.add(dmtn);
  }

  protected void createDefaultNodes() {
    String profile;
    String rmiPort;
    String ip;
    int i=1;
    profile=bc.getProperty("mosgi.jmxconsole.profile." + i);

    while (profile!=null){
      rmiPort=bc.getProperty("mosgi.jmxconsole.rmiport."+profile);
      if (rmiPort==null) {rmiPort="1099";}
      String virtual=bc.getProperty("mosgi.jmxconsole.core."+profile);
      ip=bc.getProperty("mosgi.jmxconsole.ip."+i);
      String connString=ip+":"+rmiPort+"/"+profile;
      createTreeNode(connString, ip, virtual);
      i++;
      profile=bc.getProperty("mosgi.jmxconsole.profile." + i);
    }

    // Kesako ???
    if (i==1){
      try{
        System.out.println("No property mosgi.jmxconsole.profile., managing local profile");
	String prof=bc.getProperty(BundleCache.CACHE_PROFILE_PROP);
    	if (prof==null){
	  prof=System.getProperty(BundleCache.CACHE_PROFILE_PROP);
	}
        String connString=java.net.InetAddress.getLocalHost().getHostAddress()+":1099/"+prof;
        top.add(new DefaultMutableTreeNode(connString));
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  private void disconnectFromNode(String connString){
    JMXConnector jmxc=(JMXConnector)this.jmxConnectors.remove(connString);
    MBeanServerConnection mbscl=(MBeanServerConnection)this.connectedNodes.remove(connString);
    System.out.println("mbscl="+mbscl);
    if (jmxc!=null) {
      try{
        jmxc.removeConnectionNotificationListener(this);
        //mbscl.removeNotificationListener(new ObjectName("OSGI:name=OSGi
        // Server"), this); // Does not work since the connexion is lost !!
        jmxc.close();

        // firePCE Plugin.EMPTY_NODE only if it's the selected one
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent(); 
        if (node!=null) {
	  if (connString.equals((String)node.getUserObject())){
            a.firePropertyChangedEvent(Plugin.EMPTY_NODE, null, "null");
	  }
        }
      }catch (Exception e){
        e.printStackTrace();
      }
      jmxc=null;
    }
    mbscl=null;
  }
     
  private JPanel createButtonPanel(){
    JPanel buttonPanel=new JPanel(new FlowLayout(FlowLayout.CENTER,2,2));
    //border...
    addNodeButton=new MyButton('a', " + ", this, buttonPanel);
    removeNodeButton=new MyButton('d', " - ", this, buttonPanel);
    return buttonPanel;
  }

  static class MyButton extends JButton {
    private int W =55;
    private int H = 25;

    public MyButton(char c, String name, NodesTree listener, JPanel panel) {
      super.setText(name);
      super.setMnemonic(c);
      super.setMinimumSize(new Dimension(W, H));
      super.setPreferredSize(new Dimension(W, H));
      super.setMaximumSize(new Dimension(W, H));
      super.addActionListener(listener);
      panel.add(this);
    }
  }

}
