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
package org.osgi.service.metatype;

/**
 * Provides access to metatypes.
 * 
 * @version $Revision: 5673 $
 */
public interface MetaTypeProvider {
	/**
	 * Returns an object class definition for the specified id localized to the
	 * specified locale.
	 * 
	 * <p>
	 * The locale parameter must be a name that consists of <code>language</code>[
	 * "_" <code>country</code>[ "_" <code>variation</code>] ] as is customary in
	 * the <code>Locale</code> class. This <code>Locale</code> class is not used
	 * because certain profiles do not contain it.
	 * 
	 * @param id The ID of the requested object class. This can be a pid or
	 *        factory pid returned by getPids or getFactoryPids.
	 * @param locale The locale of the definition or <code>null</code> for default
	 *        locale.
	 * @return A <code>ObjectClassDefinition</code> object.
	 * @throws IllegalArgumentException If the id or locale arguments are not
	 *         valid
	 */
	public ObjectClassDefinition getObjectClassDefinition(String id, String locale);

	/**
	 * Return a list of available locales.
	 * 
	 * The results must be names that consists of language [ _ country [ _
	 * variation ]] as is customary in the <code>Locale</code> class.
	 * 
	 * @return An array of locale strings or <code>null</code> if there is no
	 *         locale specific localization can be found.
	 *  
	 */
	public String[] getLocales();
}
