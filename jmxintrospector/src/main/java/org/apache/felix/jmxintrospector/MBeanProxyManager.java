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

package org.apache.felix.jmxintrospector;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.NotificationBroadcaster;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * This class is the main entry point to the jmxintrospector library. 
 * It uses the {@link MBeanProxyFactory#newProxyInstance(String)} to create
 * proxies for the mbeans found in the mbean servers. 
 * MBean servers can be added through the addXXServer methods. Some helper methods
 * for browsing through the MBeans are also provided. 
 *
 */
public class MBeanProxyManager {
	//FIXME: mixing up Server and MBeanServerConnections is dirty and error-prone
	/*
	 *Currently, we wrap the MBeanProxy Server with a Server in order to add identification
	 *this is quite dirty.
	 */
	private List<Server> servers = new ArrayList<Server>();

	private List<Object> objects = new ArrayList<Object>();

	private Logger logger = Logger.getLogger(this.getClass().getName());

	
	/**
	 * Adds a remote MBean server that has a RMI connector at host:1099/path
	 * Same as {@link MBeanProxyManager}{@link #addRMIServer(host, path, 1099)}
	 * @param host
	 * @param path
	 * @throws Exception
	 */
	public void addRMIServer(String host, String path) throws Exception {
		addRMIServer(host, path, 1099);
	}
	/**
	 * Adds a remote MBean server that has a RMI connector running at
	 * {@literal service:jmx:rmi:///jndi/rmi://host:port/path}
	 * Same as {@link MBeanProxyManager#addRemoteServer(service:jmx:rmi:///jndi/rmi://host:port/path)} 
	 * @param host
	 * @param path
	 * @param port
	 * @throws Exception
	 */
	public void addRMIServer(String host, String path, int port)
			throws Exception {
		addRemoteServer("service:jmx:rmi:///jndi/rmi://" + host + ":" + port +"/" +path);
	}

	public void addRemoteServer(String url) throws Exception {
		JMXServiceURL u = new JMXServiceURL(url);
		MBeanServerConnection s = JMXConnectorFactory.connect(u)
				.getMBeanServerConnection();
		add(url, s);
	}
	/**
	 * The same for the local server. Note that this will make an MBeanServer if it was not already available.
	 * @throws Exception
	 */
	public void addLocalServer() throws Exception {
			MBeanServerConnection s=ManagementFactory.getPlatformMBeanServer();
			add(s.getDefaultDomain(), s);
	}

	private void add(String id, MBeanServerConnection s) throws Exception {
		Server server=new Server(s,id);
		servers.add(server);
		Set onames = s.queryNames(ObjectName.getInstance("*:*"), null);
		MBeanProxyFactory introspector = new MBeanProxyFactory(server);
		for (Object o : onames) {
			try {
				ObjectName name=(ObjectName) o;
				objects.add(introspector.newProxyInstance(name
						.toString()));
			} catch (Exception e) {
				e.printStackTrace();
				logger.warning("ERROR: "+e);
				continue;
			}
		}
	}

	/**
	 * it removes the specified server
	 * @param server
	 */
	public void removeServer(Server server) {
				servers.remove(server);
				for (Object o : objects) {
					if (((MBean)o).getMBeanServer().equals(server)) objects.remove(o);
				}
	}
	///////////
	//Finders//
	///////////

	/**
	 * Returns all the created proxies.
	 * Remember that each proxy object can implement up to 3 interfaces:
	 * <ol>
	 * <li>{@link MBean}. Always implemented</li>
	 * <li>The implicit interface (dynamically generated) of the remote MBean. This can only be called through reflection
	 * or using a dynamic language on top of Java</li>
	 * <li>{@link NotificationBroadcaster} if the underlying mbean broadcasts notifications. 
	 * </li>
	 * </ol>
	 */
	public List<Object> getObjects() {
		return objects;
	}
	public Object findFirst(String substring){
		return findFirst(substring, null);
	}
	public Object findFirst(String substring, Server server){
		for (Object o :objects){
			MBean bean=(MBean)o;
			if (isTheServer(bean, server)&&bean.getObjectName().contains(substring)){
				return o;
			}
		}
		return null;
	}
	private boolean isTheServer(MBean bean, MBeanServerConnection server){
		return server==null?true:bean.getMBeanServer().equals(server);
	}
	public List<Object> findAll(String substring){
		return findAll(substring, null);
	}
	
	public List<Object> findAll(String substring, MBeanServerConnection server){
		List<Object> matches=new ArrayList<Object>();
		for (Object o :objects){
			MBean bean=(MBean)o;
			if (isTheServer(bean, server)&&bean.getObjectName().contains(substring)){
				matches.add(o);
			}
		}
		return matches;
	}
	
	public List<Object> findMatches(String regex){
		List<Object> matches=new ArrayList<Object>();
		for (Object o :objects){
			if (((MBean)o).getObjectName().matches(regex)){
				matches.add(o);
			}
		}
		return matches;
	}
		
	public List<Server> getServers() {
		return servers;
	}
}
