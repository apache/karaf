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
package org.apache.felix.jmood.utils;
import java.util.*;
import javax.management.openmbean.*;
import org.osgi.service.useradmin.*;
import org.osgi.service.log.*;
import org.osgi.framework.*;

/**
 * This class's task is to be in charge of all needed type conversions
 * inside the management agent. This involves translating osgi-defined types
 * to jmx's open types. It implements methods for obtaining open instances. 
 * This class implements the singleton pattern.
 * 
 */
//TODO this class is only used by log and useradmin mbeans, should be just remove it?
public class OSGi2JMXCodec {
	public static CompositeData encodeBundleEvent(BundleEvent event) throws Exception{
		if(event==null) return null;
		String[] itemNames=CompositeDataItemNames.BUNDLE_EVENT;
		Object[] itemValues=new Object [3];
		itemValues[0]=new Integer((int)event.getBundle().getBundleId());
		itemValues[1]=event.getBundle().getLocation();
		itemValues[2]=new Integer(event.getType());
		return new CompositeDataSupport(
			OSGiTypes.BUNDLEEVENT,
			itemNames,
			itemValues);
	}
	public static CompositeData encodeServiceEvent(ServiceEvent event) throws Exception{
		if(event==null) return null;
		String[] itemNames=CompositeDataItemNames.SERVICE_EVENT;
		Object[] itemValues=new Object[2];
		itemValues[0]=encodeService(event.getServiceReference());
		itemValues[1]=new Integer(event.getType());
		return new CompositeDataSupport(
						OSGiTypes.SERVICEEVENT,
						itemNames,
						itemValues);
		
	}
	public static CompositeData encodeFrameworkEvent(FrameworkEvent event) throws Exception{
		if(event==null) return null;
		String[] itemNames=CompositeDataItemNames.FRAMEWORK_EVENT;
		Object[] itemValues=new Object[4];
		itemValues[0]=new Integer((int)event.getBundle().getBundleId());
		itemValues[1]=event.getBundle().getLocation();
				if (event.getThrowable()==null) itemValues[2]=null;
				else itemValues[2]=encodeException(event.getThrowable());
				itemValues[3]=new Integer(event.getType());
			return new CompositeDataSupport(
						OSGiTypes.FRAMEWORKEVENT,
						itemNames,
						itemValues);

	}
	public static CompositeData encodeUserAdminEvent(UserAdminEvent event) throws Exception{
		//type: better as a String or as a number? for the moment number.
		//FUTURE WORK Enable some bulk methods in most used parts: optimization issue
		if(event==null) return null;
		String [] itemNames =CompositeDataItemNames.USER_EVENT;
		Object[] itemValues=new Object[3];
		itemValues[0]=new Integer(event.getType());
		itemValues[1]=encodeRole(event.getRole());
		itemValues[2]=encodeService(event.getServiceReference());
		return new CompositeDataSupport(
			OSGiTypes.USERADMINEVENT,
			itemNames,
			itemValues);
		}
	public static CompositeData[] encodeLog(Enumeration enumeration) throws Exception{
		if(enumeration==null) return null;
			Vector vector=new Vector();
			while(enumeration.hasMoreElements()){
				vector.add(encodeLogEntry((LogEntry)enumeration.nextElement()));
			}
			CompositeData[] value=new CompositeData[vector.size()];
			vector.copyInto(value);
			return value;
	}
	public static CompositeData encodeUser(User user) throws Exception {
		if(user==null) return null;
			String[] itemNames = CompositeDataItemNames.USER;
			Object[] itemValues = new Object[2];
			itemValues[0] = encodeRole((Role)user);
			itemValues[1] =
				OSGi2JMXCodec.encodeUserCredentials(user.getCredentials());
			return new CompositeDataSupport(
				OSGiTypes.USER,
				itemNames,
				itemValues);
	}
	public static CompositeData encodeRole(Role role) throws Exception {
		if (role==null) return null;
		Object[] itemValues = new Object[3];
		itemValues[0] = role.getName();
		itemValues[1] = new Integer(role.getType());
		itemValues[2] =
			OSGi2JMXCodec.encodeRoleProperties(role.getProperties());
		CompositeData cdata =
			new CompositeDataSupport(
				OSGiTypes.ROLE,
		 CompositeDataItemNames.ROLE,
		 		itemValues);
		return cdata;
	}
	public static CompositeData encodeGroup(Group group) throws Exception {
		if(group==null) return null;
		String[] itemNames = CompositeDataItemNames.GROUP;
		Object[] itemValues = new Object[3];
		itemValues[0] = encodeUser((User)group);
		Role[] members = group.getMembers();
		String[] membersNames;
		if (members!=null){		
				membersNames = new String[members.length];
		for (int i = 0; i < members.length; i++)
			membersNames[i] = members[i].getName();
		}
		else{ 
		 membersNames=new String[0];
		}
		itemValues[1] = membersNames;
		Role[] requiredMembers = group.getRequiredMembers();
		String [] requiredMembersNames;
		if(requiredMembers!=null){
			requiredMembersNames = new String[requiredMembers.length];
			for (int i = 0; i < requiredMembers.length; i++)
				requiredMembersNames[i] = requiredMembers[i].getName();
		}else requiredMembersNames=new String[0];
		itemValues[2] = requiredMembersNames;
		return new CompositeDataSupport(OSGiTypes.GROUP, itemNames, itemValues);
	}
	public static CompositeData encodeAuthorization(Authorization authorization)
		throws Exception {
			if(authorization==null) return null;
		Object[] itemValues = new Object[2];
		String[] itemNames = CompositeDataItemNames.AUTHORIZATION;
		itemValues[0] = authorization.getName();
		itemValues[1] = authorization.getRoles();
		return new CompositeDataSupport(
			OSGiTypes.AUTHORIZATION,
			itemNames,
			itemValues);
	}
	public static CompositeData encodeLogEntry(LogEntry entry)
		throws Exception {
			if (entry==null) return null;
		String[] itemNames = CompositeDataItemNames.LOG_ENTRY;
		Object[] itemValues = new Object[7];
		itemValues[0] = new Integer((int) entry.getBundle().getBundleId());
		itemValues[1] =entry.getBundle().getLocation();
		itemValues[2] = OSGi2JMXCodec.encodeException(entry.getException());
		itemValues[3] = new Integer(entry.getLevel());
		itemValues[4] = entry.getMessage();
		itemValues[5] =
			OSGi2JMXCodec.encodeService(entry.getServiceReference());
		itemValues[6] = new Integer((int) entry.getTime());
		return new CompositeDataSupport(
			OSGiTypes.LOGENTRY,
			itemNames,
			itemValues);
	}
	public static CompositeData encodeRoleProperties(Dictionary RoleProperties)
		throws Exception {
		if (RoleProperties==null) return null;
		if(RoleProperties.isEmpty()) return null;
		Enumeration propkeys= RoleProperties.keys();
		Vector  byteKeys=new Vector();
		Vector byteValues=new Vector();
		Vector StringKeys=new Vector();
		Vector StringValues=new Vector();
		while(propkeys.hasMoreElements()){
			Object key=propkeys.nextElement();
			Object value= RoleProperties.get(key);
			if ( value instanceof byte[] ) {
				byteKeys.add(key);
				byteValues.add(value);
				}
				else if (value instanceof String) {
					 StringKeys.add(key);
					 StringValues.add(value);
			}
		}
		Byte[][] bvalues=new Byte[byteValues.size()][];
		for(int i=0; i<byteValues.size();i++){
			byte[] array=(byte[])byteValues.elementAt(i);
			bvalues[i]=OSGi2JMXCodec.byte2Byte(array);
		}
		Object[] propsItemValues = new Object[4];
		String[] bkeys = new String[byteKeys.size()];
		byteKeys.copyInto(bkeys);
		//byteValues.copyInto(bvalues);
		String[] skeys = new String[StringKeys.size()];
		StringKeys.copyInto(skeys);
		String[] svalues = new String[StringValues.size()];
		StringValues.copyInto(svalues);
		propsItemValues[1] = bkeys;
		propsItemValues[3] = bvalues;
		propsItemValues[0] =skeys;
		propsItemValues[2]=svalues;
		return new CompositeDataSupport(
			OSGiTypes.ROLEPROPERTIES,
			CompositeDataItemNames.ROLE_PROPERTIES,
			propsItemValues);
	}
	public static CompositeData encodeUserCredentials(Dictionary credentials)
		throws Exception {
			if (credentials==null) return null;
			if(credentials.isEmpty())return null;
		String[] itemNames = CompositeDataItemNames.USER_CREDENTIALS;
		//For the moment, user credentials and role properties have the same structure...
		CompositeData cdata=OSGi2JMXCodec.encodeRoleProperties(credentials);
		Object[] values=cdata.getAll(CompositeDataItemNames.USER_CREDENTIALS);
		return new CompositeDataSupport(
			OSGiTypes.USERCREDENTIALS,
			itemNames,
			values);
	}
	public static CompositeData encodeService(ServiceReference service)
		throws Exception {
			if(service==null) return null;
		Object[] itemValues = new Object[3];
		String[] itemNames = CompositeDataItemNames.SERVICE;
		int id;
		String[] objectClass;
		String BundleLocation;
		if(service==null){
			id=-1;
			objectClass=new String[1];
			objectClass[0]="No service related to this log entry";
			BundleLocation="none";
		}else {
			id=(int) service.getBundle().getBundleId();
			objectClass= (String[])service.getProperty(itemNames[1]);
			BundleLocation=service.getBundle().getLocation();
		}
		itemValues[0] = new Integer(id);
		itemValues[1] =BundleLocation;
		itemValues[2] =objectClass;
		return new CompositeDataSupport(
			OSGiTypes.SERVICE,
			itemNames,
			itemValues);
	}
	public static CompositeData encodeException(Throwable throwable)
		throws Exception {
		if (throwable==null) return null;
		Object[] itemValues = new Object[2];
		String[] itemNames = CompositeDataItemNames.EXCEPTION;
		String message;
		if (throwable==null) message="This log entry has not an associated exception"; 
		else message= throwable.getMessage();
		itemValues[0] =message;
		StackTraceElement[] stack=throwable.getStackTrace();
		if(stack==null) itemValues[1]=null;
		else{
		CompositeData[] cstack=new CompositeData[stack.length];
		for (int i=0;i<stack.length;i++) cstack[i]=encodeStackTraceElement(stack[i]);
		itemValues[1]=cstack;
		} 
		return new CompositeDataSupport(
			OSGiTypes.EXCEPTION,
			itemNames,
			itemValues);
	}
	public static CompositeData encodeStackTraceElement(StackTraceElement element) throws Exception{
		Object[] itemValues = new Object[5];
		String[] itemNames = CompositeDataItemNames.STACK_TRACE_ELEMENT;
		itemValues[0]=element.getClassName();
		itemValues[1]=element.getFileName();
		itemValues[2]=new Integer(element.getLineNumber());
		itemValues[3]=element.getMethodName();
		itemValues[4]=new Boolean(element.isNativeMethod());
		return new CompositeDataSupport(
			OSGiTypes.STACKTRACE_ELEMENT,
			itemNames,
			itemValues);
	}
	public static byte[] Byte2byte(Byte[] bytes){
		byte[] result=new byte[ bytes.length];
		for(int i=0;i<bytes.length;i++) result[i]=bytes[i].byteValue();
		return result;
	}
	public static Byte[] byte2Byte(byte[] bytes){
		Byte[] result=new Byte[ bytes.length];
		for(int i=0;i<bytes.length;i++) result[i]=new Byte(bytes[i]);
		return result;

	}
}