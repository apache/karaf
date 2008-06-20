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
  private static final String DEFAULT_JMXSURL = JMX_SERVICE+"rmi:///jndi/rmi://127.0.0.1:1099/core";

  protected static Hashtable HT_GATEWAY = new Hashtable();

  private JMXConnector jmxc;
  private MBeanServerConnection mbsc;

  private String nickname;
  private Gateway parentGateway;
  private String toolTipText;
  private boolean isConnected;
  
  public String getNickname() {
    return this.nickname;
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
  private Gateway(String nickname, String surl, Gateway parent) throws MalformedURLException {
    super(surl);
    this.nickname = nickname;
    this.parentGateway = parent;
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
    if ( HT_GATEWAY.containsKey(nickname) ) {
      throw new Exception("Gateway nickname \""+nickname+"\" even exist.");
    }
    if ( !serviceURL.startsWith(JMX_SERVICE) ) {
      serviceURL = JMX_SERVICE+serviceURL;
    }
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

    return new Gateway(nickname, serviceURL, vosgiGateway);
  }

  // Creation of Gateways from the config properties file
  public static Gateway[] newGateways(BundleContext bc) {
    String str_jmxsurl, nickname, str_parent;
    Vector v_gateways = new Vector();

    int i = 1;
    str_jmxsurl = bc.getProperty("mosgi.jmxconsole.id."+i+".jmxsurl");
    if ( str_jmxsurl == null ) { str_jmxsurl = DEFAULT_JMXSURL; }

    while ( str_jmxsurl != null) {
      nickname = bc.getProperty("mosgi.jmxconsole.id."+i+".nickname");
      if (nickname == null ) { nickname = ""+i; };
      str_parent = bc.getProperty("mosgi.jmxconsole.id."+i+".parent");
      Gateway g = null;
      try {
        g = Gateway.newGateway(nickname, str_jmxsurl, str_parent);
        if ( g != null ) {
          v_gateways.addElement(g);
        }
      } catch (Exception exep) {
        System.out.println("Gateway creation error:\n "+exep.toString());
      }
      str_jmxsurl = bc.getProperty("mosgi.jmxconsole.id."+(++i)+".jmxsurl");
    }

    Gateway[] gateways=new Gateway[v_gateways.size()];
    v_gateways.toArray(gateways);
    return gateways;
  }

  // GUI gateway creator
  public static Gateway newGateway(Gateway ref) throws Exception {
    String nickname = JOptionPane.showInputDialog("Profil nickname", "");
    if ( nickname == null) return null;
    if ( HT_GATEWAY.containsKey(nickname) ) {
      throw new Exception("Gateway nickname \""+nickname+"\" even exist.");
    }
    String gateway_ref = ref!=null?ref+"":"";
    String str_jmxsurl = JOptionPane.showInputDialog("JMX service URL", gateway_ref);
    if ( str_jmxsurl == null) return null;
    Object gateway_list[] = HT_GATEWAY.keySet().toArray();
    Object gateway_list2[] = new Object[gateway_list.length+1];
    System.arraycopy(gateway_list, 0, gateway_list2, 1, gateway_list.length);
    gateway_list2[0] = "None";
    java.util.Arrays.sort(gateway_list);
    Object val = JOptionPane.showInputDialog(new javax.swing.JFrame(), "Link to another gateway ?", "Gateway association", JOptionPane.QUESTION_MESSAGE, null, gateway_list2, gateway_list2[0]);
    if ( val == null) return null;
    if ( val.toString().equals("None") ) {
      val = new String("");
    }
    String str_parent = val.toString();
    return Gateway.newGateway(nickname, str_jmxsurl, str_parent);
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
