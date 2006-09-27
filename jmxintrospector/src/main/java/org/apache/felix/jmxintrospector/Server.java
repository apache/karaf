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

import java.io.IOException;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

public class Server implements MBeanServerConnection {
	private MBeanServerConnection server;
	private String id;
	public Server(MBeanServerConnection server, String id){
	 this.server=server;
	 this.id=id;
	}
	
	//Delegates
	public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
		server.addNotificationListener(name, listener, filter, handback);
	}
	public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
		server.addNotificationListener(name, listener, filter, handback);
	}
	public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
		return server.createMBean(className, name, params, signature);
	}
	public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
		return server.createMBean(className, name, loaderName, params, signature);
	}
	public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
		return server.createMBean(className, name, loaderName);
	}
	public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
		return server.createMBean(className, name);
	}
	public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
		return server.getAttribute(name, attribute);
	}
	public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
		return server.getAttributes(name, attributes);
	}
	public String getDefaultDomain() throws IOException {
		return server.getDefaultDomain();
	}
	public String[] getDomains() throws IOException {
		return server.getDomains();
	}
	public Integer getMBeanCount() throws IOException {
		return server.getMBeanCount();
	}
	public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		return server.getMBeanInfo(name);
	}
	public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
		return server.getObjectInstance(name);
	}
	public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
		return server.invoke(name, operationName, params, signature);
	}
	public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
		return server.isInstanceOf(name, className);
	}
	public boolean isRegistered(ObjectName name) throws IOException {
		return server.isRegistered(name);
	}
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
		return server.queryMBeans(name, query);
	}
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
		return server.queryNames(name, query);
	}
	public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		server.removeNotificationListener(name, listener, filter, handback);
	}
	public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		server.removeNotificationListener(name, listener);
	}
	public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		server.removeNotificationListener(name, listener, filter, handback);
	}
	public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		server.removeNotificationListener(name, listener);
	}
	public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
		server.setAttribute(name, attribute);
	}
	public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
		return server.setAttributes(name, attributes);
	}
	public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
		server.unregisterMBean(name);
	}
}