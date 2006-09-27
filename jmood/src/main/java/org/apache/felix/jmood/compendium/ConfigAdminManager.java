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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.apache.felix.jmood.AgentContext;
import org.apache.felix.jmood.utils.ObjectNames;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

//import es.upm.dit.osgi.management.agent.AgentConstants;
/**
 * This is the main class of the config admin module. As such, it is responsible for controlling all the issues related to it. This class implements
 * the ConfigAdminManagerMXBean which defines its management interface. It creates a ConfigurationDelegate object for each available 
 * Configuration  object.
 *
 */
public class ConfigAdminManager extends NotificationBroadcasterSupport
	implements MBeanRegistration, ConfigAdminManagerMBean{
	/*NOTE: The spec says that ConfigurationException's should be used by management systems
	 * in order to inform a human manager in a suitable way. However, this is not possible unless we implement the service
	 * because nowhere in the spec is there a way of accesing those exceptions (unless the implementation of the cm chooses to log the exception, which is not even suggested in the spec) 
	*/
	private MBeanServer server;
    private AgentContext ac;
    private ConfigurationAdmin cadmin;
    public ConfigAdminManager(AgentContext ac) {
        this.ac=ac;

    }
	/** 
	 * This is called before the module is loaded. It initializes the module. 
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	public ObjectName preRegister(MBeanServer server, ObjectName name)
		throws Exception {
		this.server = server;
		return name;
	}

	/** 
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	public void postRegister(Boolean registrationDone) {}
	/**
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	public void preDeregister() throws Exception {
		//Remove the service
		//and remove all mbeans from this module...
        unregisterMBeans();
        }
	/**
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	public void postDeregister() {}

	/**
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#listConfigurations(java.lang.String)
	 */

	public String[] listConfigurations(String filter) throws Exception {
		ConfigurationAdmin cad=ac.getConfigurationAdmin();
		Configuration[] confs = null;
		if (cad!= null) {
			confs = cad.listConfigurations(filter);
            refresh();
			if (confs == null)
				return null;
		}
		String[] result = new String[confs.length];
		for (int i = 0; i < confs.length; i++)
			result[i] = this.getObjectName(confs[i]);
		return result;
	}

	/**
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#getConfiguration(java.lang.String)
	 */
	public String getConfiguration(String pid) throws Exception {
        //FIXME this should not be invoked
        //if created, the configuration is attached to the management agent's location
		ac.debug("ConfigAdmin, getting config for pid: "+pid);
		if (pid.contains(":")) throw new IllegalArgumentException("pid not compliant with jmx. Please remove ':' from the pid");
		ConfigurationAdmin cad=ac.getConfigurationAdmin();
		if (cad != null) {
			Configuration config = cad.getConfiguration(pid);
			refresh();
			return this.getObjectName(config);
		
		} else
			return null;
	}

	/**
	 *  This method gets a configuration object related to a pid and a bundle location
	 * @param pid Persistent ID
	 * @param location Bundle location of the service 
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#getConfiguration(java.lang.String, java.lang.String)
	 */
	public String getConfiguration(String pid, String location)
		throws Exception {
		//":" is reserved in objectnames, as a work around we do not permit pids containing it
			if (pid.contains(":")) throw new IllegalArgumentException("pid not compliant with jmx. Please remove ':' from the pid");
			ConfigurationAdmin cad=ac.getConfigurationAdmin();
		if (cad != null) {
			Configuration config = cad.getConfiguration(pid, location);
			refresh();
			return this.getObjectName(config);
		} else
			return null;
	}
	/**
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#createFactoryConfiguration(java.lang.String)
	 */
	public String createFactoryConfiguration(String pid) throws Exception {
		ConfigurationAdmin cad=ac.getConfigurationAdmin();
		if (cad != null) {
			Configuration conf = cad.createFactoryConfiguration(pid);
			refresh();
			return this.getObjectName(conf);
		} else
			return null;
	}

	/**
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#createFactoryConfiguration(java.lang.String, java.lang.String)
	 */
	public String createFactoryConfiguration(String pid, String location)
		throws Exception {
		ConfigurationAdmin cad=ac.getConfigurationAdmin();
		if (cad != null) {
			Configuration conf = cad.createFactoryConfiguration(pid, location);
			refresh();
			return this.getObjectName(conf);
		} else
			return null;
	}
	/** 
	 *  Delete the configurations identified by the LDAP filter
	 * @param filter LDAP String representing the configurations that want to be deleted 
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#deleteConfigurations(java.lang.String)
	 */
	public void deleteConfigurations(String filter) throws Exception {
		ConfigurationAdmin cad=ac.getConfigurationAdmin();
		Configuration[] confs = null;
		if (cad!= null) {
			confs = cad.listConfigurations(filter);
		}
		if (confs != null)
			for (int i = 0; i < confs.length; i++) {
				confs[i].delete();
			}
		refresh();
	}

	/**
	 * Removes a property from all the configurations selected by an LDAP expression 
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#removePropertyFromConfigurations(java.lang.String, java.lang.String)
	 */
	public void removePropertyFromConfigurations(String filter, String name)
		throws Exception {
		ConfigurationAdmin cad=ac.getConfigurationAdmin();
		Configuration[] confs = null;
		if (cad != null) {
			confs = cad.listConfigurations(filter);
		}
		if (confs != null)
			for (int i = 0; i < confs.length; i++) {
				Dictionary dic = confs[i].getProperties();
				Enumeration keys = dic.keys();
				while (keys.hasMoreElements()) {
					String key = (String) keys.nextElement();
					if (key.equals(name)) {
						dic.remove(key);
						try {
							confs[i].update(dic);
						} catch (IOException e) {
						    ac.error("Unexpected exception", (Exception)e);
						}
					}
				}
			}
	}

	/** 
	 * Updates or adds a property to configurations selected by an LDAP expression
	 * Arrays and vectors not supported
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#addPropertyToConfigurations(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void addPropertyToConfigurations(
		String filter,
		String name,
		String value,
		String type)
		throws Exception {
		if (isValidType(type)) {
			ConfigurationAdmin cad=ac.getConfigurationAdmin();
			Configuration[] confs = null;
			if (cad != null) {
				confs = cad.listConfigurations(filter);
			}
			if (confs != null)
				for (int i = 0; i < confs.length; i++) {
					Dictionary dic = confs[i].getProperties();
					dic.put(name, castValueToType(type, value));
					try {
						confs[i].update(dic);
					} catch (IOException e) {
                        ac.error("Unexpected exception", (Exception)e);
					}
				}
		}
	}

	/**
	 *  
	 * Validate that the value type is supported
	 * @param type
	 */
	protected static boolean isValidType(String type) {
		String[] validTypes =
			{
				"String",
				"Integer",
				"Long",
				"Float",
				"Double",
				"Byte",
				"Short",
				"Character",
				"Boolean",
				"BigInteger",
				"BigDecimal" };
		for (int i = 0; i < validTypes.length; i++) {
			if (validTypes[i].equalsIgnoreCase(type))
				return true;
		}
		return false;
	}
	protected static Object castValueToType(String type, String value) {
		value = value.equals("") ? null : value;
		if (type.equals("String")) {
			return value == null ? new String() : new String(value);
		} else if (type.equals("Integer")) {
			return value == null ? new Integer(0) : new Integer(value);
		} else if (type.equals("Long")) {
			return value == null ? new Long(0) : new Long(value);
		} else if (type.equals("Float")) {
			return value == null ? new Float(0) : new Float(value);
		} else if (type.equals("Double")) {
			return value == null ? new Double(0) : new Double(value);
		} else if (type.equals("Byte")) {
			return value == null ? new Byte("0") : new Byte(value);
		} else if (type.equals("Short")) {
			return value == null ? new Short("0") : new Short(value);
		} else if (type.equals("BigInteger")) {
			return value == null ? new BigInteger("0") : new BigInteger(value);
		} else if (type.equals("BigDecimal")) {
			return value == null ? new BigDecimal(0) : new BigDecimal(value);
		} else if (type.equals("Character")) {
			return value == null
				? new Character('a')
				: new Character(value.charAt(0));
		} else if (type.equals("Boolean")) {
			return value == null ? new Boolean(false) : new Boolean(value);
		} else {
			// Unsupported type
			return null;
		}
	}
    private void registerMBeans() throws Exception{
        ConfigurationAdmin cad=ac.getConfigurationAdmin();
        if (cad==null) {
            ac.debug("could not add any conf mbean, conf admin not available");
            return;
        }
            ac.debug("creating mbeans for existing config objects");
            Configuration[] confs = null;
            //confs contains the new config objects
            //The old ones are in configObjects, whose key is the object name.
            confs = cad.listConfigurations(null);
            if (confs!=null) {
                ac.debug("Existing conf objects: ");
            for(int i=0;i<confs.length;i++) {
                ac.debug("\t"+confs[i].getPid());
                //now we add the new ones 
                String oname = this.getObjectName(confs[i]);
                server.registerMBean(
                        new ConfigurationDelegate(confs[i], ac),
                        new ObjectName(oname));
                ac.debug("Succesfully registered? "+!server.queryMBeans(new ObjectName(oname), null).isEmpty());
                }
            }
                
    }
	public synchronized void refresh() throws Exception {
        //Extremely innefficient but KISS
        unregisterMBeans();
        registerMBeans();
	}
    public boolean isAvailable() {
        return ac.getConfigurationAdmin()==null?false:true;
    }
    private void unregisterMBeans() throws MalformedObjectNameException, NullPointerException, InstanceNotFoundException, MBeanRegistrationException{
        Set set =
            server.queryNames(
                new ObjectName(ObjectNames.ALL_CM_OBJECT),
                null);
        Iterator it = set.iterator();
        while (it.hasNext()) {
            ObjectName oname=(ObjectName) it.next();
            ac.debug("Unregistering config mbean: "+oname);
            server.unregisterMBean(oname);
        }
    }

    private String getObjectName(Configuration configuration) {
		StringBuffer posfix = new StringBuffer();
		posfix.append("pid=" + configuration.getPid());
		if (configuration.getFactoryPid() != null)
			posfix.append(
				",isFactory=true,FactoryPid=" + configuration.getFactoryPid());
		else
			posfix.append(",isFactory=false");
		String oname = ObjectNames.CM_OBJECT + posfix.toString();
		return oname;
	}
}
