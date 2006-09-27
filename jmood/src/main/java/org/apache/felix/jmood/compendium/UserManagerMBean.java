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

import java.util.Hashtable;

import javax.management.openmbean.CompositeData;

public interface UserManagerMBean {
	/**
	 * Creates a role of the specified type, case insensitive, with the specified name
	 * @param name
	 * @param type
	 * @throws Exception
	 */
	public abstract void createRole(String name, String type) throws Exception;
	public abstract CompositeData getRole(String name) throws Exception;
	public abstract CompositeData getGroup(String groupname) throws Exception;
	public abstract CompositeData getUser(String username) throws Exception;
	public abstract CompositeData getAuthorization(String user) throws Exception;
	public abstract String[] getRoles(String filter) throws Exception;
	public abstract String getUser(String key, String value) throws Exception;
	public abstract boolean removeRole(String name) throws Exception;
	public abstract String[] getRoles() throws Exception;
	public abstract String[] getGroups() throws Exception;
	public abstract String[] getUsers() throws Exception;
	public abstract String[] getMembers(String groupname) throws Exception;
	public abstract String[] getRequiredMembers(String groupname) throws Exception;
	public abstract boolean addMember(String groupname, String rolename) throws Exception;
	public abstract boolean addRequiredMember(
		String groupname,
		String rolename) throws Exception;
	public abstract boolean removeMember(String groupname, String rolename) throws Exception;
	public abstract String[] getImpliedRoles(String username) throws Exception;
	public abstract void addProperty(String key, Object value, String rolename) throws Exception;
	public abstract void removeProperty(String key, String rolename) throws Exception;
	public abstract void addCredential(String key,Object value, String username) throws Exception;
	public abstract void removeCredential(String key, String username) throws Exception;
	public abstract Hashtable getProperties(String rolename) throws Exception;
	public abstract Hashtable getCredentials(String username) throws Exception;
}