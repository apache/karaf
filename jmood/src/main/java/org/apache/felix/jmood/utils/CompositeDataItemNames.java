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

public interface CompositeDataItemNames {
	public static final String BUNDLE_ID="BundleId";
	public static final String BUNDLE_LOCATION="BundleLocation";
	public static final String EVENT_TYPE="Type";
	public static final String ENCODED_SERVICE="Service";
	public static final String ENCODED_EXCEPTION="Throwable";
	public static final String ENCODED_ROLE="Role";
	public static final String OBJECT_CLASS="objectClass";
	public static final String CLASSNAME="ClassName";
	public static final String FILENAME="FileName";
	public static final String LINE_NUMBER="LineNumber";
	public static final String METHOD_NAME="MethodName";
	public static final String IS_NATIVE_METHOD="isNativeMethod";
	public static final String EXCEPTION_MESSAGE="Message";
	public static final String STACK_TRACE="StackTrace";
	public static final String KEYS_FOR_STRING_VALUES="KeysForStringValues";
	public static final String KEYS_FOR_BYTEARRAY_VALUES="KeysForByteArrayValues";
	public static final String STRING_VALUES="StringValues";
	public static final String BYTEARRAY_VALUES="ByteArrayValues";
	public static final String ENCODED_CREDENTIALS="credentials";
	public static final String ROLE_NAME="name";
	public static final String ROLE_TYPE="type";
	public static final String ROLE_ENCODED_PROPERTIES="properties";
	public static final String ENCODED_USER="User";
	public static final String GROUP_MEMBERS="members";
	public static final String GROUP_REQUIRED_MEMBERS="requiredMembers";
	public static final String USER_NAME="UserName";
	public static final String ROLE_NAMES="RoleNames";
	public static final String LOG_LEVEL="Level";
	public static final String LOG_MESSAGE="Message";
	public static final String LOG_TIME="Time";
	public static final String[] AUTHORIZATION={USER_NAME,ROLE_NAMES};
	public static final String[] BUNDLE_EVENT={BUNDLE_ID,BUNDLE_LOCATION, EVENT_TYPE};
	public static final String[] SERVICE_EVENT={ENCODED_SERVICE, EVENT_TYPE};
	public static final String[] USER_EVENT={EVENT_TYPE, ENCODED_ROLE, ENCODED_SERVICE};
	public static final String[] FRAMEWORK_EVENT={BUNDLE_ID,BUNDLE_LOCATION, ENCODED_EXCEPTION,EVENT_TYPE };
	public static final String[] SERVICE={BUNDLE_ID,BUNDLE_LOCATION,OBJECT_CLASS};
	public static final String[] STACK_TRACE_ELEMENT={CLASSNAME,FILENAME,LINE_NUMBER,METHOD_NAME,IS_NATIVE_METHOD};
	public static final String[] EXCEPTION={EXCEPTION_MESSAGE,STACK_TRACE};
	public static final String[] ROLE_PROPERTIES={KEYS_FOR_STRING_VALUES,KEYS_FOR_BYTEARRAY_VALUES,STRING_VALUES,BYTEARRAY_VALUES};
	public static final String[] USER_CREDENTIALS=ROLE_PROPERTIES;
	public static final String[] USER={ENCODED_ROLE,ENCODED_CREDENTIALS};
	public static final String[] ROLE={ROLE_NAME,ROLE_TYPE,ROLE_ENCODED_PROPERTIES};
	public static final String[] GROUP={ENCODED_USER,GROUP_MEMBERS,GROUP_REQUIRED_MEMBERS};
	public static final String[] LOG_ENTRY={BUNDLE_ID,BUNDLE_LOCATION, ENCODED_EXCEPTION,LOG_LEVEL,LOG_MESSAGE,ENCODED_SERVICE,LOG_TIME};
}
