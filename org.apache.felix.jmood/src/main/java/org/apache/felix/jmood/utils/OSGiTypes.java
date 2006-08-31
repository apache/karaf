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
import javax.management.openmbean.*;
/**
 * This class defines open types in a static way for the management agent. An open type instance defines the structure of an open data.
 * For more information on open types and open data see jmx specification.
 *
 */
public class OSGiTypes {
	public final static CompositeType USER;
	public final static CompositeType ROLE;
	public final static CompositeType GROUP;
	public final static CompositeType USERADMINEVENT;
	public final static CompositeType AUTHORIZATION;
	public final static CompositeType LOGENTRY;
	public final static CompositeType ROLEPROPERTIES;
	public final static CompositeType USERCREDENTIALS;
	public final static CompositeType SERVICE;
	public final static CompositeType EXCEPTION;
	public final static CompositeType BUNDLEEVENT;
	public final static CompositeType SERVICEEVENT;
	public final static CompositeType FRAMEWORKEVENT;
	public final static CompositeType STACKTRACE_ELEMENT;
	protected static OpenType[] DICTIONARYITEMTYPES = null;
	//protected static String[] RoleItemNames = null;
	//protected static String[] RolePropertiesItemNames = null;

	static {
		try {
			DICTIONARYITEMTYPES = new OpenType[4];
			DICTIONARYITEMTYPES[0] = new ArrayType(1, SimpleType.STRING);
			DICTIONARYITEMTYPES[1] = new ArrayType(1, SimpleType.STRING);
			DICTIONARYITEMTYPES[2] = new ArrayType(1, SimpleType.STRING);
			DICTIONARYITEMTYPES[3] = new ArrayType(2, SimpleType.BYTE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		STACKTRACE_ELEMENT=createStackTraceElementType();	
		ROLEPROPERTIES = createRolePropertiesType();
		USERCREDENTIALS = createUserCredentialsType();
		SERVICE = createServiceType();
		EXCEPTION = createExceptionType();
		ROLE = createRoleType();
		USER = createUserType();
		GROUP = createGroupType();
		AUTHORIZATION = createAuthorizationType();
		LOGENTRY = createLogEntryType();
		USERADMINEVENT=createUserAdminEventType();
		BUNDLEEVENT=createBundleEventType();
		SERVICEEVENT=createServiceEventType();
		FRAMEWORKEVENT=createFrameworkEventType();
	}
		private static CompositeType createBundleEventType(){
			String description="This type encapsulates OSGi bundle events";
			String[] itemNames=CompositeDataItemNames.BUNDLE_EVENT;
			OpenType[] itemTypes=new OpenType[3];
			String[] itemDescriptions=new String[3];
		/*	itemNames[0]="BundleId";
			itemNames[1]="BundleLocation";
			itemNames[2]="Type";*/
			itemTypes[0]=SimpleType.INTEGER;
			itemTypes[1]=SimpleType.STRING;
			itemTypes[2]=SimpleType.INTEGER;
			itemDescriptions[0]="The ID of the bundle that generated this event";
			itemDescriptions[1]="The location of the bundle that generated this event";
			itemDescriptions[2]="The type of the event: {INSTALLED=1, STARTED=2, STOPPED=4, UPDATED=8, UNINSTALLED=16}";
			try {
				return new CompositeType(
					"BundleEvent",
					description,
					itemNames,
					itemDescriptions,
					itemTypes);
			} catch (OpenDataException e) {
				e.printStackTrace();
				return null;
			}
			
		}
		private static CompositeType createServiceEventType(){
						String description="This type encapsulates OSGi service events";
						String[] itemNames=CompositeDataItemNames.SERVICE_EVENT;
						OpenType[] itemTypes=new OpenType[2];
						String[] itemDescriptions=new String[2];
						/*itemNames[0]="Service";
						itemNames[1]="Type";
						*/
						itemTypes[0]=SERVICE;
						itemTypes[1]=SimpleType.INTEGER;
						itemDescriptions[0]="The service associated with this event";
						itemDescriptions[1]="The type of the event: {REGISTERED=1, MODIFIED=2 UNREGISTERING=3}";
						try {
							return new CompositeType(
								"ServiceEvent",
								description,
								itemNames,
								itemDescriptions,
								itemTypes);
						} catch (OpenDataException e) {
							e.printStackTrace();
							return null;
						}
			
		}
		private static CompositeType createFrameworkEventType(){
			String description="This type encapsulates OSGi framework events";
			String[] itemNames=CompositeDataItemNames.FRAMEWORK_EVENT;
			OpenType[] itemTypes=new OpenType[4];
			String[] itemDescriptions=new String[4];
			/*itemNames[0]="BundleId";
			itemNames[1] ="BundleLocation";
			itemNames[2]="Throwable";
			itemNames[3]="Type";
			*/
			itemTypes[0]=SimpleType.INTEGER;
			itemTypes[1]=SimpleType.STRING;
			itemTypes[2]= EXCEPTION;
			itemTypes[3]=SimpleType.INTEGER;
			itemDescriptions[0]="The id of the bundle that os related to this event";
			itemDescriptions[1]="The location of the bundle that os related to this event";
			itemDescriptions[2]="The associated exception";
			itemDescriptions[3]="The type of the event: {STARTED=1, ERROR=2, PACKAGES_REFRESHED=4, STARTLEVEL_CHANGED=8}";
			try {
				return new CompositeType(
					"FrameworkEvent",
					description,
					itemNames,
					itemDescriptions,
					itemTypes);
			} catch (OpenDataException e) {
				e.printStackTrace();
				return null;
			}
		}
		private static CompositeType createUserAdminEventType(){
			String description="This type encapsulates OSGi user admin events";
			String[] itemNames=CompositeDataItemNames.USER_EVENT;
			OpenType[] itemTypes=new OpenType[3];
			String[] itemDescriptions=new String[3];
			/*itemNames[0]="Type";
			itemNames[1]="Role";
			itemNames[2]="Service";
			*/
			itemTypes[0]=SimpleType.INTEGER;
			itemTypes[1]=ROLE;
			itemTypes[2]=SERVICE;
			itemDescriptions[0]="The type of the event: {ROLE_CREATED= 1, ROLE_CHANGED=2, ROLE_REMOVED=4}";
			itemDescriptions[1]="The role associated with this event";
			itemDescriptions[2]="The UserAdmin service associated with this event. In some gateways, there might be more than one service available.";
			try {
				return new CompositeType(
					"UserAdminEvent",
					description,
					itemNames,
					itemDescriptions,
					itemTypes);
			} catch (OpenDataException e) {
				e.printStackTrace();
				return null;
			}
		}
		private static CompositeType createServiceType() {
		String description =
			"The mapping of the service information for log entries";
		String[] ServiceItemNames = CompositeDataItemNames.SERVICE;
		/*ServiceItemNames[0] = "BundleId";
		ServiceItemNames[1] = "BundleLocation";
		ServiceItemNames[2] = "objectClass";*/
		String[] itemDescriptions = new String[3];
		itemDescriptions[0] =
			"The id of the bundle which registered the service";
		itemDescriptions[1] ="The location of the bundle that registered the service";
		itemDescriptions[2] =
			"An string array containing the interfaces under which the service has been registered";
		OpenType[] itemTypes = new OpenType[3];
		itemTypes[0] = SimpleType.INTEGER;
		itemTypes[1] =SimpleType.STRING;
		try {
			itemTypes[2] = new ArrayType(1, SimpleType.STRING);
			return new CompositeType(
				"Service",
				description,
				ServiceItemNames,
				itemDescriptions,
				itemTypes);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static CompositeType createStackTraceElementType() {
		//classname,filename,int linenumber,string method name,boolean isNativeMethod	
		String description = "The mapping for of stack trace's elements";
		String[] itemNames =CompositeDataItemNames.STACK_TRACE_ELEMENT;
		/*itemNames[0] = "ClassName";
		itemNames[1]="FileName";
		itemNames[2]="LineNumber";
		itemNames[3]="MethodName";
		itemNames[4]="isNativeMethod";*/
		String[] itemDescriptions = new String[5];
		itemDescriptions[0] = "the class where occured";
		itemDescriptions[1]="the file name";
		itemDescriptions[2] = "the line number";
		itemDescriptions[3] = "the method name";
		itemDescriptions[4] = "True if it is a native method";
		OpenType[] itemTypes = new OpenType[5];
		itemTypes[0] = SimpleType.STRING;
		itemTypes[1] = SimpleType.STRING;
		itemTypes[2] = SimpleType.INTEGER;
		itemTypes[3] = SimpleType.STRING;
		itemTypes[4] = SimpleType.BOOLEAN;
		try {
			return new CompositeType(
				"Exception",
				description,
				itemNames,
				itemDescriptions,
				itemTypes);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static CompositeType createExceptionType() {
		String description = "The exception mapping for logging purposes";
		String[] itemNames = CompositeDataItemNames.EXCEPTION;
		/*itemNames[0] = "Message";
		itemNames[1]="StackTrace";*/
		String[] itemDescriptions = new String[2];
		itemDescriptions[0] = "The exception's message";
		itemDescriptions[1]="The exception's stack trace";
		OpenType[] itemTypes = new OpenType[2];
		itemTypes[0] = SimpleType.STRING;
		try {
			itemTypes[1] = new ArrayType(1, STACKTRACE_ELEMENT);
			return new CompositeType(
				"Exception",
				description,
				itemNames,
				itemDescriptions,
				itemTypes);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}

	}
	private static CompositeType createRolePropertiesType() {
		String description =
			"The properties dictionary mapping of a Role. It has four fields: KeysForStringValues, KeysForByteArrayValues, StringValues, ByteArrayValues";
		String[] RolePropertiesItemNames = CompositeDataItemNames.ROLE_PROPERTIES;
		/*RolePropertiesItemNames[0] = "KeysForStringValues";
		RolePropertiesItemNames[1] = "KeysForByteArrayValues";
		RolePropertiesItemNames[3] = "ByteArrayValues";
		RolePropertiesItemNames[2] = "StringValues";*/
		String[] itemDescriptions = new String[4];
		itemDescriptions[0] =
			"A string array containing the keys for the properties which are Strings";
		itemDescriptions[1] =
			"A string array containing the keys for the properties which are byte[]";
		itemDescriptions[2] =
			"A string array containing the values of the properties which are Strings";
		itemDescriptions[3] =
			"A 2D-array containing the values of the properties which are byte[]. byte[i] is the i-nth Byte[], whose key is at KeysForByteArrayValues[i]";
		try {
			return new CompositeType(
				"RoleProperties",
				description,
				RolePropertiesItemNames,
				itemDescriptions,
				DICTIONARYITEMTYPES);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static CompositeType createUserCredentialsType() {
		//BUG possibly problems here with indexes which is which. review
		String description = "The credentials for a user";
		String[] itemNames = CompositeDataItemNames.USER_CREDENTIALS;
		/*itemNames[0] = "KeysForStringValues";
		itemNames[1] = "KeysForByteArrayValues";
		itemNames[3] = "ByteArrayValues";
		itemNames[2] = "StringValues";
		*/
		String[] itemDescriptions = new String[4];
		itemDescriptions[0] =
			"A string array containing the keys for the credentials which are Strings";
		itemDescriptions[1] =
			"A string array containing the keys for the credentials which are byte[]";
		itemDescriptions[2] =
			"A string array containing the values of the credentials which are Strings";
		itemDescriptions[3] =
			"A 2D-array containing the values of the credentials which are byte[]. byte[i] is the i-nth Byte[], whose key is at KeysForByteArrayValues[i]";
		try {

		return new CompositeType(
				"UserCredentials",
				description,
				itemNames,
				itemDescriptions,
				OSGiTypes.DICTIONARYITEMTYPES);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static CompositeType createUserType() {
		//public CompositeType(String typeName, String description, String[] itemNames, String[] itemDescriptions, OpenType[] itemTypes)		
		String description =
			"Mapping of org.osgi.service.useradmin.User for remote management purposes. User extends Role";
		String[] itemNames = CompositeDataItemNames.USER;
		/*itemNames[0] = "Role";
		itemNames[1] = "credentials";*/
		String[] itemDescriptions = new String[2];
		itemDescriptions[0] = "The role object that is extended by this user object";
		itemDescriptions[1] = "The credentials for this user";
		OpenType[] itemTypes = new OpenType[2];
		itemTypes[0] = ROLE;
		itemTypes[1] = USERCREDENTIALS;
		try {

			return new CompositeType(
				"User",
				description,
				itemNames,
				itemDescriptions,
				itemTypes);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static CompositeType createRoleType() {
		createRolePropertiesType();
		String description =
			"Mapping of org.osgi.service.useradmin.Role for remote management purposes. User and Group extend Role";
		String []RoleItemNames = CompositeDataItemNames.ROLE;
		/*RoleItemNames[0] = "name";
		RoleItemNames[1] = "type";
		RoleItemNames[2] = "properties";*/
		String[] itemDescriptions = new String[3];
		itemDescriptions[0] =
			"The name of the role. Can be either a group or a user";
		itemDescriptions[1] =
			"An integer representing type of the role: {0=Role,1=user,2=group}";
		itemDescriptions[2] =
			"A properties list as defined by org.osgi.service.useradmin.Role";
		OpenType[] itemTypes = new OpenType[3];
		itemTypes[0] = SimpleType.STRING;
		itemTypes[1] = SimpleType.INTEGER;
		itemTypes[2] = ROLEPROPERTIES;
		try {

			return new CompositeType(
				"Role",
				description,
				RoleItemNames,
				itemDescriptions,
				itemTypes);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static CompositeType createGroupType() {
		String description =
			"Mapping of org.osgi.service.useradmin.Group for remote management purposes. Group extends User which in turn extends Role";
		String[] itemNames = CompositeDataItemNames.GROUP;
		/*itemNames[0] = "User";
		itemNames[1] = "members";
		itemNames[2] = "requiredMembers";
		*/
		String[] itemDescriptions = new String[3];
		itemDescriptions[0] = "The user object that is extended by this group object";
		itemDescriptions[1] = "The members of this group";
		itemDescriptions[2] = "The required members for this group";
		OpenType[] itemTypes = new OpenType[3];
		itemTypes[0] = USER;
		try {
			itemTypes[1] = new ArrayType(1, SimpleType.STRING);
			itemTypes[2] = new ArrayType(1, SimpleType.STRING);
			return new CompositeType(
				"Group",
				description,
				itemNames,
				itemDescriptions,
				itemTypes);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static CompositeType createAuthorizationType() {
		String description =
			"An authorization object defines which roles has a user got";
		String[] itemNames = CompositeDataItemNames.AUTHORIZATION;
		/*itemNames[0] = "UserName";
		itemNames[1] = "RoleNames";*/
		String[] itemDescriptions = new String[2];
		itemDescriptions[0] = "The user name for this authorization object";
		itemDescriptions[1] =
			"The names of the roles encapsulated by this auth object";
		OpenType[] itemTypes = new OpenType[2];
		itemTypes[0] = SimpleType.STRING;
		try {
			itemTypes[1] = new ArrayType(1, SimpleType.STRING);
			return new CompositeType(
				"Authorization",
				description,
				itemNames,
				itemDescriptions,
				itemTypes);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static CompositeType createLogEntryType() {
		String description =
			"A log entry type encapsulates a org.osgi.service.log.LogEntry";
		String[] itemNames = CompositeDataItemNames.LOG_ENTRY;
		/*itemNames[0] = "BundleId";
		itemNames[1] = "BundleLocation";
		itemNames[2] = "Throwable";
		itemNames[3] = "Level";
		itemNames[4] = "Message";
		itemNames[5] = "ServiceReference";
		itemNames[6] = "Time";*/
		String[] itemDescriptions = new String[7];
		itemDescriptions[0] =
			"The id for the bundle that generated the log entry";
		itemDescriptions[1] =
					"The location of the bundle that generated the log entry";
		itemDescriptions[2] =
			"The ExceptionType that caused the error, or null if there was none";
		itemDescriptions[3] = "The level of entry: DEBUG, INFO, ERROR, WARNING";
		itemDescriptions[4] = "The log message";
		itemDescriptions[5] =
			"The service that generated the log entry, or null if not available";
		itemDescriptions[6] = "The time at which the exception occurred";
		OpenType[] itemTypes = new OpenType[7];
		itemTypes[0] = SimpleType.INTEGER;
		itemTypes[1]=SimpleType.STRING;
		itemTypes[2] = EXCEPTION;
		itemTypes[3] = SimpleType.INTEGER;
		itemTypes[4] = SimpleType.STRING;
		
		try {
			itemTypes[5] = SERVICE;
			itemTypes[6] = SimpleType.INTEGER;
			return new CompositeType(
				"LogEntry",
				description,
				itemNames,
				itemDescriptions,
				itemTypes);
		} catch (OpenDataException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
