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
package org.apache.felix.jmood.compendium;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.jmood.AgentContext;
import org.osgi.service.cm.Configuration;


public class ConfigurationDelegate implements  MBeanRegistration, ConfigurationDelegateMBean {
	private Configuration configuration;
	private MBeanServer server;
	private ObjectName oname;
    private AgentContext ac;
	public ConfigurationDelegate(Configuration configuration, AgentContext ac){
		this.configuration=configuration;
        this.ac=ac;
	}

	/**
	 * @see org.osgi.service.cm.Configuration#getPid()
	 */
	public String getPid() {

		return configuration.getPid();
	}

	/**
	 * @see org.osgi.service.cm.Configuration#getProperties()
	 */
	public Hashtable getProperties() {
		Dictionary dic=configuration.getProperties();
		Enumeration keys=dic.keys();
		Hashtable properties=new Hashtable();
		while(keys.hasMoreElements()) {
			Object key=keys.nextElement();
			properties.put(key, dic.get(key));
		}
		return properties;
	}

	/**
	 * @see org.osgi.service.cm.Configuration#update(java.util.Dictionary)
	 *hashtable is a dictionary!
	 */
	public void update(Hashtable properties) throws IOException {
		configuration.update(properties);
	}

	/**
	 * @see org.osgi.service.cm.Configuration#delete()
	 */
	public void delete() throws Exception {
		server.unregisterMBean(oname);
		configuration.delete();
	}

	/**
	 * @see org.osgi.service.cm.Configuration#getFactoryPid()
	 */
	public String getFactoryPid() {
		return configuration.getFactoryPid();
	}

	/**
	 * @see org.osgi.service.cm.Configuration#update()
	 */
	public void update() throws IOException {
		configuration.update();
	}

	/**
	 * @see org.osgi.service.cm.Configuration#setBundleLocation(java.lang.String)
	 */
	public void setBundleLocation(String bundleLocation) {
		configuration.setBundleLocation(bundleLocation);
	}

	/**
	 * @see org.osgi.service.cm.Configuration#getBundleLocation()
	 */
	public String getBundleLocation() {
		return configuration.getBundleLocation();
	}
	/**
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	public void postDeregister() {
	}

	/**
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	public void postRegister(Boolean registrationDone) {
	}

	/**
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	public void preDeregister() throws Exception {
	}

	/**
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	public ObjectName preRegister(MBeanServer server, ObjectName name)
		throws Exception {
			this.oname=name;
			this.server=server;
		return name;
	}

	/**
	 * @see org.apache.felix.jmood.compendium.ConfigurationDelegateMBean#getProperty(java.lang.String)
	 */
	public String getProperty(String key) throws Exception {
		Object result= configuration.getProperties().get(key);
		if (result==null) return null;
		if (result instanceof String) return (String) result;
		else return result.toString();
	}

	/**
	 * @see org.apache.felix.jmood.compendium.ConfigurationDelegateMBean#setProperty(java.lang.String, java.lang.String)
	 */
	public void setProperty(String key, String value, String type) throws Exception {
		if (ConfigAdminManager.isValidType(type)) {
					Dictionary dic = configuration.getProperties();
					dic.put(key, ConfigAdminManager.castValueToType(type, value));
					try {
						configuration.update(dic);
					} catch (IOException e) {
                        ac.error("Unexpected exception", (Exception)e);
					}
				}
	}
	public void deleteProperty(String key) throws Exception{
				Dictionary dic = configuration.getProperties();
				Enumeration keys = dic.keys();
				while (keys.hasMoreElements()) {
					String k = (String) keys.nextElement();
					if (k.equals(key)) {
						dic.remove(k);
							configuration.update(dic);
					}
				}
			}
}