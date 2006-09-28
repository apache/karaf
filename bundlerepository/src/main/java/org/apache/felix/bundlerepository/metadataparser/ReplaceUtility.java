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

import java.util.Map;

/**
 * This class provides methods to replace ${var} substring by values stored in a map
 */

public class ReplaceUtility {

	/**
	 * This method replaces ${var} substring by values stored in a map.
	 * @return resulted string
	 */
	public static String replace(String str, Map values) {

		int len = str.length();
		StringBuffer sb = new StringBuffer(len);

		int prev = 0;
		int start = str.indexOf("${");
		int end = str.indexOf("}", start);
		while (start != -1 && end != -1) {
			String key = str.substring(start + 2, end);
			Object value = values.get(key);
			if (value != null) {
				sb.append(str.substring(prev, start));
				sb.append(value);
			} else {
				sb.append(str.substring(prev, end + 1));
			}
			prev = end + 1;
			if (prev >= str.length())
				break;

			start = str.indexOf("${", prev);
			if (start != -1)
				end = str.indexOf("}", start);
		}

		sb.append(str.substring(prev));

		return sb.toString();
	}

	//	public static void main(String[] args){
	//		Map map=new HashMap();
	//		map.put("foo","FOO");
	//		map.put("bar","BAR");
	//		map.put("map",map);
	//				
	//		String str;
	//		if(args.length==0) str=""; else str=args[0];
	//		
	//		System.out.println(replace(str,map));
	//		
	//	}
}