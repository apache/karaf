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
package org.apache.felix.mosgi.console.gui;

import java.awt.GridLayout;
import java.util.Hashtable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.osgi.framework.BundleContext;

import org.apache.felix.mosgi.console.ifc.Plugin;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.NotificationListener;
import javax.management.Notification;
import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectionNotification;

import org.apache.felix.framework.cache.BundleCache;


public class NodesTree extends JPanel implements TreeSelectionListener, NotificationListener {
  static String TOP_NAME="Servers";
  static int POOLING_TIME=10000;

	protected JTree tree;
  private DefaultTreeModel dtm;
	private Hashtable connectedNodes = new Hashtable();
	private Hashtable jmxConnectors = new Hashtable();
	private DefaultMutableTreeNode top = null;
  protected boolean cont=true;
  
	private static boolean useSystemLookAndFeel = false;

  private Activator a;
  private BundleContext bc;
  private PoolingThread pt;
  protected Thread poolThread;
  private NodeCellRenderer ncr;
  private Hashtable nodes=new Hashtable();


	public NodesTree(Activator a,BundleContext bc) {
		super(new GridLayout(1, 0));
    this.a=a;
    this.bc=bc;
    this.pt=new PoolingThread();
    
		System.setProperty("java.security.policy", "client.policy");
		top = new DefaultMutableTreeNode(NodesTree.TOP_NAME);
    dtm=new DefaultTreeModel(top);
		tree = new JTree(dtm);
    tree.setCellRenderer(ncr=new NodeCellRenderer(bc,this));
		tree.addTreeSelectionListener(this);
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);

		JScrollPane treeView = new JScrollPane(tree);
		add(treeView);
	}

  public void runDiscovery(){
	this.createDefaultNodes(true);
    poolThread=new Thread(this.pt);
    poolThread.start();
  }

  //////////////////////////////////////////////////////
  //               TreeSelectionListener //
  //////////////////////////////////////////////////////
	public void valueChanged(TreeSelectionEvent e) {
//    System.out.println("coucou, value changed");
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
	  String selectedNode=(String)node.getUserObject();
    if (!selectedNode.equals(NodesTree.TOP_NAME)){
      Object mbsc=connectedNodes.get(selectedNode);
      if (mbsc!=null){
        a.firePropertyChangedEvent(Plugin.NEW_NODE_SELECTED, selectedNode, mbsc);
      }else{
        a.firePropertyChangedEvent(Plugin.EMPTY_NODE, null, "null");
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
    }
  }
  
  ////////////////////////////////////////////////////
  //            Friend methods //
  ///////////////////////////////////////////////////
  void stop(){
    this.cont=false;
  }

  boolean isNodeConnected(String s){
    if (this.connectedNodes.get(s)==null){
      return false;
    }else{
      return true;
    }
  }

  ///////////////////////////////////////////////////
  //           Private part //
  ///////////////////////////////////////////////////
  protected void createDefaultNodes(boolean createTreeNode) {
    String profile;
    String rmiPort;
    String ip;
    
    int i=1;
    profile=bc.getProperty("insa.jmxconsole.profile" + i);

    while (profile!=null){
      rmiPort=bc.getProperty("insa.jmxconsole.rmiport."+profile);
      if (rmiPort==null){rmiPort="1099";}
      String virtual=bc.getProperty("insa.jmxconsole.core."+profile);
      ip=bc.getProperty("insa.jmxconsole.ip" + i);
      String connString=ip+":"+rmiPort+"/"+profile;
      if (createTreeNode){
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
      this.connectToNode(connString);
      i++;
      profile=bc.getProperty("insa.jmxconsole.profile" + i);
    }
      
    if (i==1){
      try{
        System.out.println("No property insa.jmxconsole.profile, managing local profile");
		String prof=bc.getProperty(BundleCache.CACHE_PROFILE_PROP);
    	if (prof==null){
	      prof=System.getProperty(BundleCache.CACHE_PROFILE_PROP);
		}
        String connString=java.net.InetAddress.getLocalHost().getHostAddress()+":1099/"+prof;
        if (createTreeNode){
          top.add(new DefaultMutableTreeNode(connString));
        }
//        this.connectToNode(connString);
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  private void disconnectFromNode(String connString){
    JMXConnector jmxc=(JMXConnector)this.jmxConnectors.remove(connString);
    MBeanServerConnection mbscl=(MBeanServerConnection)this.connectedNodes.remove(connString);
    try{
      jmxc.removeConnectionNotificationListener(this);
      //mbscl.removeNotificationListener(new ObjectName("OSGI:name=OSGi
      // Server"), this); // Does not work since the connexion is lost !!
      jmxc.close();
      a.firePropertyChangedEvent(Plugin.EMPTY_NODE, null, "null");
    }catch (Exception e){
      e.printStackTrace();
    }
    jmxc=null;
    mbscl=null;
  }
     
  private void connectToNode(String connString){
    Object ls=this.connectedNodes.get(connString);
    if (ls==null){
      JMXConnector jmxc = null;
      try {

        JMXServiceURL surl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + connString);
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
          mbsc.addNotificationListener(new ObjectName("TabUI:name=OsgiProbes"), this, null, null); //Needed,

          this.connectedNodes.put(connString, mbsc);
          this.jmxConnectors.put(connString, jmxc);
          a.firePropertyChangedEvent(Plugin.NEW_NODE_CONNECTION, connString, mbsc);
 System.out.println("Node "+connString+" connected");
        }else {
System.out.println("The Remote Logger of "+connString+" is not started");
        }
      }catch(java.io.IOException ex){
System.out.println("Impossible to connect to "+connString);
      }catch(MalformedObjectNameException e){
        e.printStackTrace();
      }catch(Exception e){
        e.printStackTrace();
      }
    }else{
      a.firePropertyChangedEvent(Plugin.NEW_NODE_CONNECTION, connString, ls);
    }
  }

  protected class PoolingThread implements Runnable{
    public void run(){
      while (cont){
        createDefaultNodes(false);
        tree.treeDidChange();
        try{
          Thread.sleep(POOLING_TIME);
        }catch(InterruptedException e){
          e.printStackTrace();
        }
      }
    }
  }
}
