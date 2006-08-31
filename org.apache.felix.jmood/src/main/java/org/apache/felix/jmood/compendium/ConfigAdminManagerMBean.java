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


public interface ConfigAdminManagerMBean {
	/**
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#listConfigurations(java.lang.String)
	 */
	public abstract String[] listConfigurations(String filter)
		throws Exception;
	/**
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#getConfiguration(java.lang.String)
	 */
	public abstract String getConfiguration(String pid) throws Exception;
	/**
	 *  This method gets a configuration object related to a pid and a bundle location
	 * @param pid Persistent ID
	 * @param location Bundle location of the service 
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#getConfiguration(java.lang.String, java.lang.String)
	 */
	public abstract String getConfiguration(String pid, String location)
		throws Exception;
	/**
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#createFactoryConfiguration(java.lang.String)
	 */
	public abstract String createFactoryConfiguration(String pid)
		throws Exception;
	/**
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#createFactoryConfiguration(java.lang.String, java.lang.String)
	 */
	public abstract String createFactoryConfiguration(
		String pid,
		String location)
		throws Exception;
	/** 
	 *  Delete the configurations identified by the LDAP filter
	 * @param filter LDAP String representing the configurations that want to be deleted 
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#deleteConfigurations(java.lang.String)
	 */
	public abstract void deleteConfigurations(String filter) throws Exception;
	/**
	 * Removes a property from all the configurations selected by an LDAP expression 
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#removePropertyFromConfigurations(java.lang.String, java.lang.String)
	 */
	public abstract void removePropertyFromConfigurations(
		String filter,
		String name)
		throws Exception;
	/** 
	 * Updates or adds a property to configurations selected by an LDAP expression
	 * Arrays and vectors not supported
	 * @see org.apache.felix.jmood.compendium.ConfigAdminManagerMBean#addPropertyToConfigurations(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public abstract void addPropertyToConfigurations(
		String filter,
		String name,
		String value,
		String type)
		throws Exception;
	public abstract void refresh() throws Exception;
    public abstract boolean isAvailable() throws Exception;
}