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

package org.cauldron.sigil.utils;

import java.util.regex.Pattern;

public class GlobCompiler {
	public static final Pattern compile(String glob) {
		char[] chars = glob.toCharArray();
		if ( chars.length > 0 ) {
			StringBuilder builder = new StringBuilder(chars.length + 5);
	
			builder.append('^');
			
			for (char c : chars) {
				switch ( c ) {
				case '*':
					builder.append(".*");
					break;
				case '.':
					builder.append("\\.");
					break;
				case '$':
					builder.append( "\\$" );
					break;
				default:
					builder.append( c );
				}
			}
	
			return Pattern.compile(builder.toString());
		}
		else {
			return Pattern.compile(glob);
		}
	}
}
