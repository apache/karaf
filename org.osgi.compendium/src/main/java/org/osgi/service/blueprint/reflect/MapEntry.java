/*
 * Copyright (c) OSGi Alliance (2008, 2009). All Rights Reserved.
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

package org.osgi.service.blueprint.reflect;

/**
 * Metadata for a map entry.
 * 
 * This type is used by {@link MapMetadata}, {@link PropsMetadata} and
 * {@link ServiceMetadata}.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */
public interface MapEntry {
	/**
	 * Return the Metadata for the key of the map entry.
	 * 
	 * This is specified by the <code>key</code> attribute or element.
	 * 
	 * @return The Metadata for the key of the map entry. This must not be
	 *         <code>null</code>.
	 */
	NonNullMetadata getKey();

	/**
	 * Return the Metadata for the value of the map entry.
	 * 
	 * This is specified by the <code>value</code> attribute or element.
	 * 
	 * @return The Metadata for the value of the map entry. This must not be
	 *         <code>null</code>.
	 */
	Metadata getValue();
}
