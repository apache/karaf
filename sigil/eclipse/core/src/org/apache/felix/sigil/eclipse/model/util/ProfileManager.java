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

package org.apache.felix.sigil.eclipse.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.utils.GlobCompiler;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class ProfileManager {
	private static final Pattern[] BOOT_DELEGATION_PATTERNS = new Pattern[] {
		GlobCompiler.compile("org.ietf.jgss"),
		GlobCompiler.compile("org.omg.*"),
		GlobCompiler.compile("org.w3c.*"),
		GlobCompiler.compile("org.xml.*"),
		GlobCompiler.compile("sun.*"),
		GlobCompiler.compile("com.sun.*"),
	};
	
	private static HashMap<String, Properties> profiles;

	public static boolean isBootDelegate(ISigilProjectModel project, String packageName) {
		if ( packageName.startsWith( "java." ) ) {
			return true;
		}
		
		for ( Pattern p : BOOT_DELEGATION_PATTERNS ) {
			if ( p.matcher(packageName).matches()) {
				return true;
			}
		}
		return false;
	}
	
	public static Properties findProfileForVersion(String javaVersion) {
		Map<String, Properties> profiles = loadProfiles();
		
		if ( "1.5.0".equals( javaVersion ) ) {
			return profiles.get( "J2SE-1.5" );
		}
		else if ( "1.6.0".equals( javaVersion ) ) {
			return profiles.get( "J2SE-1.6" );
		}
		
		return null;
	}
	
	private synchronized static Map<String, Properties> loadProfiles() {
		if ( profiles == null ) {
			profiles = new HashMap<String, Properties>();
			
			Bundle b = Platform.getBundle("org.eclipse.osgi");
			
			for ( String profile : loadProfiles( b )) {
				if ( profile.trim().length() > 0 ) {
					URL url = findURL(profile, b);
					if ( url != null ) {
						try {
							Properties p = loadProperties(url);
							String name = p.getProperty("osgi.java.profile.name");
							if ( name != null ) {
								profiles.put(name, p);
							}
							else {
								SigilCore.error( "Invalid profile definition, no name specified: " + url);
							}
						} catch (IOException e) {
							SigilCore.error( "Failed to load java profile", e );
						}
					}
					else {
						SigilCore.error( "Unknown profile **" + profile + "**" );
					}
				}
				// else ignore empty values
			}
		}
		return profiles;
	}

	private static String[] loadProfiles(Bundle b) {
		URL url = findURL("profile.list", b);

		if ( url != null ) {
			try {
				Properties p = loadProperties(url);
				String s = p.getProperty("java.profiles");
				return s == null ? new String[] {} : s.split(",");
			} catch (IOException e) {
				SigilCore.error( "Failed to load java profile list", e );
			}
		}
		else {
			SigilCore.error( "Failed to find java profile list" );
		}
		
		// fine no properties found
		return new String[] {};
	}

	@SuppressWarnings("unchecked")
	private static URL findURL(String file, Bundle b) {
		Enumeration e = b.findEntries("/", file, false);
		return e == null ? null : (URL) (e.hasMoreElements() ? e.nextElement() : null);
	}


	private static Properties loadProperties(URL url) throws IOException {
		Properties p = new Properties();
		
		InputStream in = null;
		
		try {
			in = url.openStream();
			p.load(in);
		}
		finally {
			if ( in != null ) {
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return p;
	}




}
