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

import java.util.List;

/**
 * Metadata for a collection based value. Values of the collection are defined
 * by Metadata objects. This Collection Metadata can constrain the values of the
 * collection to a specific type.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */

public interface CollectionMetadata extends NonNullMetadata {

	/**
	 * Return the type of the collection.
	 * 
	 * The possible types are: array (<code>Object[]</code>), <code>Set</code>,
	 * and <code>List</code>. This information is specified in the element name.
	 * 
	 * @return The type of the collection. <code>Object[]</code> is returned to
	 *         indicate an array.
	 */
	Class/* <?> */getCollectionClass();

	/**
	 * Return the type specified for the values of the collection.
	 * 
	 * The <code>value-type</code> attribute specified this information.
	 * 
	 * @return The type specified for the values of the collection.
	 */
	String getValueType();

	/**
	 * Return Metadata for the values of the collection.
	 * 
	 * @return A List of Metadata for the values of the collection.
	 */
	List /* <Metadata> */getValues();
}
