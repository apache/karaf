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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;


import org.apache.felix.jmood.core.CoreController;
import org.apache.felix.jmood.core.Framework;
import org.apache.felix.jmood.utils.ObjectNames;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
public class Activator implements BundleActivator{
    private Properties props;
    private String agentPropsPath="/agent.properties";
    private static final String IS_POLICY_EMBEDDED="policy.embedded";
    private static final String JAVA_SECURITY_POLICY="java.security.policy";
    private MBeanServer server;
    private JMXConnectorServer connectorServer;
    private int rmiRegistryPort = 1199;
    private AgentContext ac;
    private static final String connectoServerOname="RemotingService:type=ConnectorServer, subtype=RMIConnectorServer, provider=JRE";
    private CompendiumController compendium;
    private Registry rmiRegistry;
    

    public void start(BundleContext context) throws Exception {
        this.ac=new AgentContext(context);
        this.ac.debug("starting");
        try{
        this.server=this.getMBeanServer();
        }
        catch (Exception e){
        	this.ac.error("unexpected error", e);
        	throw e;
        }
        this.ac.debug("got platform mbeanserver");
        this.compendium = new CompendiumController(this.server,this.ac);
        this.props = this.loadProperties();
        this.ac.debug("props loaded");
        //TODO Enable this when Felix has security support.RMI Serialization doesn't work well without it. In Equinox, it works fine
        //this.setSecurityManager();
        //ac.debug("security manager set");
         this.ac.debug("registering mbeans");
            this.registerMBeans();
            this.initRMIConn();
            this.ac.debug("rmi connection initialised and mbeans registered");
            this.ac.debug("agent started");
    }
    public void stop(BundleContext context) throws Exception {
        this.ac.debug("stopping");
        this.stopRMIConn();
        this.ac.closeTrackers();
        this.unregisterMBeans();
        this.ac.debug("done");
    }
    private void setSecurityManager() throws Exception{
        //TODO check this when we add permission admin support to the bundle
        //It caused StackOverFlow the second time the framework was run(?)
    	
        if (System.getSecurityManager() != null) {
			return;
		}
		try {
			this.ac.debug("Security manager does not exist");
            if (this.props.getProperty(IS_POLICY_EMBEDDED).equalsIgnoreCase("true")){
                this.ac.debug("Policy is embedded, copying it to filesystem...");
                String policyName=this.props.getProperty(JAVA_SECURITY_POLICY);
                //The policy is in the file system and should be copied...
                File file=this.ac.getBundleContext().getDataFile(policyName);
                if (file.exists()) {
                    this.ac.debug("trying to delete file...");
                    boolean deleted=file.delete();
                    if(!deleted) {
						this.ac.error("Could not delete existing policy file");
					} else {
						this.ac.debug("successfully deleted");
					}
                    file=this.ac.getBundleContext().getDataFile(policyName);
                    file.createNewFile();
                    this.ac.debug("new file created");
                }

                FileOutputStream o=new FileOutputStream (file);
                InputStream i=this.ac.getBundleContext().getBundle().getResource("/"+policyName).openStream();
                byte [] buffer=new byte [1024];
                while (i.read(buffer)!=-1){
                   o.write(buffer);
                }
                i.close();
                o.flush();
                o.close();
                
                System.setProperty(JAVA_SECURITY_POLICY, file.getAbsolutePath());
            }
            else{
         System.setProperty(JAVA_SECURITY_POLICY, this.props.getProperty(JAVA_SECURITY_POLICY));
            }
         System.setSecurityManager(new SecurityManager());

        }catch(Exception e){
            this.ac.error("Unexpected exception", e);
            }
        this.ac.debug("Security policy: "+System.getProperty(JAVA_SECURITY_POLICY));
        this.ac.debug("Security manager toString(): "+System.getSecurityManager().toString());

        }
        
    private Properties loadProperties() throws Exception{
        Properties props = new Properties();
        URL u=this.ac.getBundleContext().getBundle().getResource(this.agentPropsPath);
        props.load(u.openStream());
        return props;
    }

    /**
     * A getter method for retrieving the context of this bundle
     * @return BundleContext the bundle context for this bundle
     * @throws InstanceNotFoundException 
     */

    private void initRMIConn() throws IOException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException, NullPointerException, InstanceNotFoundException {
    	Thread t=new Thread(){
    		public void run() {
    	        try {
    	        	rmiRegistry=LocateRegistry.createRegistry(rmiRegistryPort);
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
                        + this.rmiRegistryPort + "/server");
                this.ac.debug(url.toString());                        
        this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
                url, null,this.server);

        this.server.registerMBean(this.connectorServer, new ObjectName(connectoServerOname));
        this.connectorServer.start();
    }
    private void stopRMIConn() throws Exception {
        this.connectorServer.stop();
        UnicastRemoteObject.unexportObject(this.rmiRegistry, true);
        this.server.unregisterMBean(new ObjectName(connectoServerOname));
        
    }
    
    private void registerMBeans() throws Exception{
        this.server.registerMBean(new CoreController(this.ac), new ObjectName(ObjectNames.CORE_CONTROLLER));
        this.server.registerMBean(new Framework(this.ac), new ObjectName(ObjectNames.FRAMEWORK));
        this.compendium.initController();
        this.ac.debug("mbeans registered");

        
    }
    private void unregisterMBeans() throws Exception{
        this.server.unregisterMBean(new ObjectName(ObjectNames.CORE_CONTROLLER));
        this.server.unregisterMBean(new ObjectName(ObjectNames.FRAMEWORK));
        this.compendium.dispose();
    }
    private MBeanServer getMBeanServer() throws Exception{
    	String jvm=System.getProperty("java.version"); //1.5.0 or higher
    	this.ac.debug("java version is: "+jvm);
    	String[] s=jvm.split("\\.");
    	if(Integer.parseInt(s[1])<5){//In this way it should also work with Mustang
				return MBeanServerFactory.createMBeanServer();
		} else {
				Class clazz =
					Class.forName("java.lang.management.ManagementFactory");
				Method m=clazz.getDeclaredMethod("getPlatformMBeanServer", new Class[0]);
						return (MBeanServer) m.invoke(null,(Object[]) null);
	}
    }

}
