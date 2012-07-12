/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.util;

/**
 * Contains various methods for helping with layout no commands
 */
public class CommandUtils {

	/**
	 * The message is either enlarged or trimmed to the given size. 
	 * 
	 * @param message - the message to be trimmed or enlarged
	 * @param length - the length of the message text
	 * @return the optimized message
	 */
	public static String trimToSize(String message, int length) {
		while (message.length() < length) {
			message += " ";
		} 
		if (message.length() > length) {
			message = message.substring(0, length);
		}
		
		return message;
	}
	
}
