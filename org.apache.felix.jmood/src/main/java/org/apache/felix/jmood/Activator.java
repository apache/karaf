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

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.felix.jmood.core.CoreController;
import org.apache.felix.jmood.core.Framework;
import org.apache.felix.jmood.utils.ObjectNames;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
public class Activator implements BundleActivator{
    private Properties props;
    private String agentPropsPath="/agent.properties";
    private static final String RMI_PORT="rmi.registry.port";
    private MBeanServer server;
    private AgentContext ac;
    private CompendiumHandler compendiumHandler;
    private RMIHandler rmiHandler;
    private SecurityManagerHandler securityHandler;
    

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
        this.compendiumHandler = new CompendiumHandler(this.server,this.ac);
        this.props = this.loadProperties();
        this.ac.debug("props loaded");
        //TODO Enable this when Felix has security support.RMI Serialization doesn't work well without it. In Equinox, it works fine
		//String policyName=this.props.getProperty(SecurityManagerHandler.JAVA_SECURITY_POLICY);
        //boolean policyEmbedded=this.props.getProperty(SecurityManagerHandler.IS_POLICY_EMBEDDED).equalsIgnoreCase("true");
        //securityHandler=new SecurityManagerHandler(ac, policyEmbedded, policyName);
        //securityHandler.setSecurityManager();
        //ac.debug("security manager set");
         this.ac.debug("registering mbeans");
         this.registerMBeans();
         int port=Integer.parseInt(props.getProperty(RMI_PORT));
         rmiHandler=new RMIHandler(port, ac, server);
         rmiHandler.start();
         this.ac.debug("rmi connection initialised and mbeans registered");
         this.ac.debug("agent started");
    }
    public void stop(BundleContext context) throws Exception {
        this.ac.debug("stopping");
        rmiHandler.stop();
        this.ac.closeTrackers();
        this.unregisterMBeans();
        this.ac.debug("done");
    }
        
    private Properties loadProperties() throws Exception{
        Properties props = new Properties();
        URL u=this.ac.getBundleContext().getBundle().getResource(this.agentPropsPath);
        props.load(u.openStream());
        return props;
    }

    private void registerMBeans() throws Exception{
        this.server.registerMBean(new CoreController(this.ac), new ObjectName(ObjectNames.CORE_CONTROLLER));
        this.server.registerMBean(new Framework(this.ac), new ObjectName(ObjectNames.FRAMEWORK));
        this.compendiumHandler.initController();
        this.ac.debug("mbeans registered");

        
    }
    private void unregisterMBeans() throws Exception{
        this.server.unregisterMBean(new ObjectName(ObjectNames.CORE_CONTROLLER));
        this.server.unregisterMBean(new ObjectName(ObjectNames.FRAMEWORK));
        this.compendiumHandler.dispose();
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
