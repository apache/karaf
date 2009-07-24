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
 * Metadata for a list of service references.
 * 
 * <p>
 * This is specified by the <code>reference-list</code> element.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */
public interface ReferenceListMetadata extends ServiceReferenceMetadata {

	/**
	 * Reference list values must be proxies to the actual service objects.
	 * 
	 * @see #getMemberType()
	 */
	 static final int	USE_SERVICE_OBJECT		= 1;

	/**
	 * Reference list values must be <code>ServiceReference</code> objects.
	 * 
	 * @see #getMemberType()
	 */
	 static final int	USE_SERVICE_REFERENCE	= 2;

	/**
	 * Return whether the List will contain service object proxies or
	 * <code>ServiceReference</code> objects.
	 * 
	 * This is specified by the <code>member-type</code> attribute of the
	 * reference list.
	 * 
	 * @return Whether the List will contain service object proxies or
	 *         <code>ServiceReference</code> objects.
	 * @see #USE_SERVICE_OBJECT
	 * @see #USE_SERVICE_REFERENCE
	 */
	int getMemberType();
}
