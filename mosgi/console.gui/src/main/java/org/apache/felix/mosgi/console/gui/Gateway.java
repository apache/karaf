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

import org.osgi.framework.BundleContext;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.management.remote.JMXServiceURL;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationListener;
import java.net.MalformedURLException;

public class Gateway extends JMXServiceURL {

  private static final String JMX_SERVICE = "service:jmx:";

  private static final String DEFAULT_PROFILE  = "core";
  private static final String DEFAULT_HOST     = "127.0.0.1";
  private static final String DEFAULT_PROTOCOL = "rmi";
  private static final String DEFAULT_PORT     = "1099";

  private static Hashtable HT_GATEWAY = new Hashtable(); // used to manage virtuals gateways

  private JMXConnector jmxc;
  private MBeanServerConnection mbsc;

  private String nickname; // used to manage virtuals gateways
  private String name; // name for the GUI = this.nickname+" : "+this.toString()
  private Gateway parentGateway;
  private String toolTipText;
  private boolean isConnected;
  
  public String getName() {
    return this.name;
  }

  public MBeanServerConnection getMbsc() {
    return this.mbsc;
  }

  public boolean isConnected() {
    return this.isConnected;
  }

  public String getToolTipText() {
    return this.toolTipText;
  }

  public Gateway getParent() {
    return parentGateway;
  }

  // The constructor (private)
  private Gateway(String nickname, String surl, Gateway vosgi) throws MalformedURLException {
    super(surl);
    this.nickname = nickname;
    this.name = nickname+" : \""+this.toString().substring(JMX_SERVICE.length())+"\"";
    this.parentGateway = vosgi;
    this.isConnected = false;
    this.toolTipText = "<html><B>\""+nickname+"\" JMXServiceURL =<br>"+
      "- protocol=</B>"+this.getProtocol()+"<br><B>"+
      "- host=</B>"+this.getHost()+"<br><B>"+
      "- port=</B>"+this.getPort()+"<br><B>"+
      "- URLPath=</B>"+this.getURLPath()+"</html>";
    HT_GATEWAY.put(nickname, this);
  }

  // Intermediate private Gateway creator
  private static Gateway newGateway(String nickname, String serviceURL, String parent_gateway) throws Exception {
    if ( serviceURL.contains("null") ) {
      throw new Exception("Invalid service URL \""+serviceURL+"\"");
    }
    
    Gateway vosgiGateway = null;
    if ( parent_gateway != null && !parent_gateway.equals("") && !parent_gateway.equals("null") ) {
      vosgiGateway = (Gateway) HT_GATEWAY.get(parent_gateway);
      if ( vosgiGateway == null ) {
        throw new Exception("Parent gateway \""+parent_gateway+"\" doesn't exist.");
      }
    }

    Gateway newGateway = new Gateway(nickname, serviceURL, vosgiGateway);
    return newGateway;
  }

  // Creation of Gateways from the config properties file
  public static Gateway[] newGateways(BundleContext bc) {
    Vector v_gateways = new Vector();
    String protocol, host, profile, port, nickname, parent_gateway;
    int port_int;

    int i = 1;
    profile = bc.getProperty("mosgi.jmxconsole.id."+i+".profile");
    if ( profile == null) { profile = DEFAULT_PROFILE; }
  
    while ( profile != null ) {
      host = bc.getProperty("mosgi.jmxconsole.id."+i+".host");
      if ( host == null ) { host = DEFAULT_HOST; }
      protocol = bc.getProperty("mosgi.jmxconsole.id."+i+".protocol");
      if ( protocol == null ) { protocol = DEFAULT_PROTOCOL; }
      port = bc.getProperty("mosgi.jmxconsole.id."+i+".port");
      if ( port == null ) { port = DEFAULT_PORT; }
      try {
        port_int = Integer.parseInt(port);
      } catch (Exception exep) { try { port_int = Integer.parseInt(DEFAULT_PORT); } catch (Exception e) {} }
      String serviceURL = JMX_SERVICE+protocol+":///jndi/"+"rmi"+"://"+host+":"+port+"/"+profile;
      parent_gateway = bc.getProperty("mosgi.jmxconsole.id."+i+".virtual");
      Gateway g = null;
      nickname =  bc.getProperty("mosgi.jmxconsole.id."+i+".nickname");
      if (nickname == null ) { nickname = ""+i; };
      try {
        g = Gateway.newGateway(nickname, serviceURL, parent_gateway);
        if ( g != null ) {
          v_gateways.addElement(g);
        }
      } catch (Exception exep) {
        System.out.println(""+exep);
      }
      i++;
      profile = bc.getProperty("mosgi.jmxconsole.id."+i+".profile");
    }
    Gateway[] gateways=new Gateway[v_gateways.size()];
    v_gateways.toArray(gateways);
    return gateways;
  }

  // GUI gateway creator
  public static Gateway newGateway() {
    String nickname = JOptionPane.showInputDialog("Profil nickname", "");
    if ( nickname == null) return null; // should check nickname is unique
    String host = JOptionPane.showInputDialog("Host", DEFAULT_HOST);
    if ( host == null ) return null;
    String protocol = JOptionPane.showInputDialog("Protocol", DEFAULT_PROTOCOL);
    if ( protocol==null ) return null;
    String port = JOptionPane.showInputDialog("Port", DEFAULT_PORT);
    try { Integer.parseInt(port); } catch (Exception ex) { return null; }
    String profile = JOptionPane.showInputDialog("OSGi profil name", "");
    if (profile==null) return null;
    Object gateway_list[] = HT_GATEWAY.keySet().toArray();
    Object gateway_list2[] = new Object[gateway_list.length+1];
    System.arraycopy(gateway_list, 0, gateway_list2, 1, gateway_list.length);
    gateway_list2[0] = "None";
    java.util.Arrays.sort(gateway_list);
    int val = JOptionPane.showOptionDialog(new javax.swing.JFrame(), "Virtual of", "Is it a virtual gateway ?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, gateway_list2, gateway_list2[0]);
    String virtual = "";
    if ( val != JOptionPane.CLOSED_OPTION ) { virtual = ""+gateway_list2[val]; }
    if ( val == 0 ) { virtual = ""; }
    String serviceURL = JMX_SERVICE+protocol+":///jndi/"+protocol+"://"+host+":"+port+"/"+profile;
    Gateway g = null;
    try { 
      g = Gateway.newGateway(nickname, serviceURL, virtual);
    } catch (Exception exep) {
      System.out.println(""+exep);
      return null;
    }
    return g;
  }

  public boolean connect(NotificationListener notificationListener) {
    if ( this.isConnected ) {
      return true;
    } else {
      try {
        //System.out.println("\nconnectToNode("+this+")...");
        java.util.HashMap env=new java.util.HashMap();
        env.put("jmx.remote.protocol.provider.class.loader", this.getClass().getClassLoader());
        env.put("jmx.remote.protocol.provider.pkgs", NodesTree.getProtocolPackageProvider(this.getProtocol())); // efficacite partielle... Le reste etant assure par du codage en dur dans les classes du protocol murmi
        this.jmxc = JMXConnectorFactory.newJMXConnector(this, env);
          //System.out.println("jmxc = "+jmxc);
        this.jmxc.connect(env);
          //System.out.println("jmxc connected = "+jmxc);
        this.mbsc = jmxc.getMBeanServerConnection();
        if ( this.mbsc.isRegistered(new ObjectName("OSGI:name=Remote Logger")) ) {
	  jmxc.addConnectionNotificationListener(notificationListener, null, this);
	  //mbsc.addNotificationListener(new ObjectName("TabUI:name=OsgiProbes"), notificationListener, null, null); //Needed ? Osgiprobes is a NotficationBroadcasterSupport but do nothing with... ?
          this.isConnected = true;
          System.out.println("Node \""+this.toString()+"\" connected");
          return true;
        } else {
          System.out.println("The Remote Logger of \""+this.toString()+"\" is not started");
          return false;
        }
      } catch(java.io.IOException ex){
        //System.out.println("Impossible to connect to "+this.toString());
      } catch(MalformedObjectNameException e){
        //
      } catch(Exception e){
        //use one thread per node to avoid being freeze by a timeOutConnection
        //System.out.println("gui.NodesTree.connectToNode("+this.toString()+") : "+e);
        //System.out.println("  => Delete this node ? to implement... ?");
      }
    }
    return false;
  }

  public void disconnect(NotificationListener nl) {
    //System.out.println(this+".disconnect() ...)");
    if ( this.isConnected ) {
      try {
        jmxc.removeConnectionNotificationListener(nl);
        this.jmxc.close();
      } catch (Exception e){
        //e.printStackTrace();
      }
      this.jmxc = null;
    }
    this.mbsc = null;
    this.isConnected = false;
  }

}
