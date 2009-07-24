/*
 * Copyright (c) OSGi Alliance (2001, 2008). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.prefs;

/**
 * The Preferences Service.
 * 
 * <p>
 * Each bundle using this service has its own set of preference trees: one for
 * system preferences, and one for each user.
 * 
 * <p>
 * A <code>PreferencesService</code> object is specific to the bundle which
 * obtained it from the service registry. If a bundle wishes to allow another
 * bundle to access its preferences, it should pass its
 * <code>PreferencesService</code> object to that bundle.
 *  
 */
public interface PreferencesService {
	/**
	 * Returns the root system node for the calling bundle.
	 * 
	 * @return The root system node for the calling bundle.
	 */
	public Preferences getSystemPreferences();

	/**
	 * Returns the root node for the specified user and the calling bundle.
	 * 
	 * @param name The user for which to return the preference root node. 
	 * @return The root node for the specified user and the calling bundle.
	 */
	public Preferences getUserPreferences(String name);

	/**
	 * Returns the names of users for which node trees exist.
	 * 
	 * @return The names of users for which node trees exist.
	 */
	public String[] getUsers();
}
