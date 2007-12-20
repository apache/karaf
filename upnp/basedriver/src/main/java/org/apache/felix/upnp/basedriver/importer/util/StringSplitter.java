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
package org.apache.felix.upnp.basedriver.importer.util;

import java.util.ArrayList;

/** 
* The class is used only for JDK1.3 backporting purpose
* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
* @since 0.3
*/
public class StringSplitter {
	
	public static String [] split (String tosplit, char ch){
		ArrayList result = new ArrayList();
		int  pos = -1;
		while ( (pos = tosplit.indexOf(ch)) != -1) {
			result.add(new String (tosplit.substring(0, pos)));
			tosplit = tosplit.substring(pos + 1);
		}
		if (!tosplit.equals("")) result.add(new String(tosplit));
		return (String [])result.toArray(new String [0]);
	}

}
