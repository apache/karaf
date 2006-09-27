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
package org.apache.felix.jmood;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class RMIHandler {
	private int port;
	private Registry rmiRegistry;
	private AgentContext ac;
	private JMXConnectorServer connectorServer;
	private MBeanServer server;
    private static final String connectorServerOname="RemotingService:type=ConnectorServer, subtype=RMIConnectorServer, provider=JRE";

 public RMIHandler(int port, AgentContext ac, MBeanServer server){
	 this.port=port;
	 this.ac=ac;
	 this.server=server;
 }
 public void start() throws IOException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException, NullPointerException, InstanceNotFoundException {
 	Thread t=new Thread(){
 		public void run() {
 	        try {
 	        	rmiRegistry=LocateRegistry.createRegistry(port);
 	            } catch (RemoteException e) {
 	                ac.warning(e.getMessage()+"\n"+"Possibly some other framework already running, skipping RMI registry creation");
 	              return;
 	           }
 		}
 	};
 	t.setName("RMIREGISTRY");
 	t.setContextClassLoader(this.getClass().getClassLoader());
 	t.start();
     InetAddress[] addresses=InetAddress.getAllByName(InetAddress.getLocalHost().getCanonicalHostName());
     //Do not attach the agent to the loopback address
     InetAddress address=null;
     for (int i = 0; i < addresses.length; i++) {
         if (!addresses[i].isLoopbackAddress()) {                    
             address=addresses[i];
             break;
         }
     }
     if (address==null){
     	StringBuffer msg=new StringBuffer("java.net.InetAddress could not find non-localhost IP. \n");
     		msg.append(" Is there any network interface available? Are you using Linux?. \n")
     		   .append("If you are using debian-based distros, try editing the /etc/hosts file so that it does not contain")
     		   .append(" something like '127.0.0.1 ${hostname}'");
     	this.ac.warning(msg.toString());
     	address=InetAddress.getLocalHost();
     }
     JMXServiceURL url = new JMXServiceURL(
             "service:jmx:rmi:///jndi/rmi://"+address.getHostName()+":"
                     + this.port + "/server");
             this.ac.debug(url.toString());                        
     this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
             url, null,this.server);

     this.server.registerMBean(this.connectorServer, new ObjectName(connectorServerOname));
     this.connectorServer.start();
 }
 public void stop() throws Exception {
     this.connectorServer.stop();
     UnicastRemoteObject.unexportObject(this.rmiRegistry, true);
     this.server.unregisterMBean(new ObjectName(connectorServerOname));
 }

}
