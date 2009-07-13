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

package org.cauldron.bld.core.util;

import java.util.ArrayList;

public class QuoteUtil {
	public static String[] split(String str) {
		ArrayList<String> split = new ArrayList<String>();
		boolean quote = false;
		StringBuffer buf = new StringBuffer(str.length());
		
		for ( int i = 0; i < str.length(); i++ ) {
			char c = str.charAt(i);
			switch ( c ) {
			case '"':
				quote = !quote;
				break;
			case ',':
				if ( !quote ) {
					split.add( buf.toString().trim() );
					buf.setLength(0);
					break;
				}
				// else fall through on purpose
			default:
				buf.append( c );
			}
		}
		
		if ( buf.length() > 0 ) {
			split.add( buf.toString().trim() );
		}
		return split.toArray( new String[split.size()] );
	}	
}
