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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class MBeanProxyManager {
	//FIXME: mixing up Server and MBeanServerConnections is dirty and error-prone
	private List<Server> servers = new ArrayList<Server>();

	private List<Object> objects = new ArrayList<Object>();

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public void addRMIServer(String host, String path) throws Exception {
		addRMIServer(host, path, 1099);
	}

	public void addRMIServer(String host, String path, int port)
			throws Exception {
		addRemoteServer("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + path);
	}

	public void addRemoteServer(String url) throws Exception {
		JMXServiceURL u = new JMXServiceURL(url);
		MBeanServerConnection s = JMXConnectorFactory.connect(u)
				.getMBeanServerConnection();
		add(url, s);
	}

	public void addLocalServer() throws Exception {
			MBeanServerConnection s=ManagementFactory.getPlatformMBeanServer();
			add(s.getDefaultDomain(), s);
	}

	private void add(String id, MBeanServerConnection s) throws Exception {
		Server server=new Server(s,id);
		servers.add(server);
		Set<ObjectName> onames = s.queryNames(ObjectName.WILDCARD, null);
		MBeanProxyFactory introspector = new MBeanProxyFactory(server);
		for (ObjectName name : onames) {
			try {
				objects.add(introspector.newProxyInstance(name
						.toString()));
			} catch (javassist.NotFoundException nfe) {
				
				logger.warning("ERROR"+nfe);
				continue;
			}
		}
	}

	public void removeServer(Server server) {
				servers.remove(server);
				for (Object o : objects) {
					if (((MBean)o).getMBeanServer().equals(server)) objects.remove(o);
				}
	}
	///////////
	//Finders//
	///////////
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
