/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.bundlerepository.metadataparser;

/**
 * This class provides methods to process class name
 */

public class ClassUtility {

	/**
	 * This method capitalizes the first character in the provided string.
	 * @return resulted string
	 */
	public static String capitalize(String name) {

		int len=name.length();
		StringBuffer sb=new StringBuffer(len);
		boolean setCap=true;
		for(int i=0; i<len; i++){
			char c=name.charAt(i);
			if(c=='-' || c=='_') {
				setCap=true;			
			} else {
				if(setCap){
					sb.append(Character.toUpperCase(c));
					setCap=false;
				} else {
					sb.append(c);
				}
			}
		} 
 
		return sb.toString();
	}

	/**
	 * This method minusculizes all characters in the provided string.
	 * @return resulted string
	 */
	public static String toLowerCase(String name) {
		int len=name.length();
		StringBuffer sb=new StringBuffer(len);
		for(int i=0; i<len; i++){
			char c=name.charAt(i);
			sb.append(Character.toLowerCase(c));
		}  
		return sb.toString();
	}


	/**
	 * This method capitalizes all characters in the provided string.
	 * @return resulted string
	 */
	public static String finalstaticOf(String membername) {
		int len=membername.length();
		StringBuffer sb=new StringBuffer(len+2);
		for(int i=0; i<len; i++){
			char c=membername.charAt(i);
			if(Character.isLowerCase(c) ) {
				sb.append(Character.toUpperCase(c));
			} else if(Character.isUpperCase(c) ) {
				sb.append('_').append(c);
			} else {
				sb.append(c);				
			}
		} 
 
		return sb.toString();
	}
	
	/**
	 * This method returns the package name in a full class name
	 * @return resulted string
	 */
	public static String packageOf(String fullclassname) {
		int index=fullclassname.lastIndexOf(".");
		if(index>0) {
			return fullclassname.substring(0,index);
		} else {
			return "";	
		}
	}

	/**
	 * This method returns the package name in a full class name
	 * @return resulted string
	 */
	public static String classOf(String fullclassname) {
		int index=fullclassname.lastIndexOf(".");
		if(index>0) {
			return fullclassname.substring(index+1);
		} else {
			return fullclassname;	
		}
	}
}