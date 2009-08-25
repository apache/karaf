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

package org.osgi.framework.hooks.service;

import java.util.Collection;

import org.osgi.framework.BundleContext;

/**
 * OSGi Framework Service Find Hook Service.
 * 
 * <p>
 * Bundles registering this service will be called during framework service find
 * (get service references) operations.
 * 
 * @ThreadSafe
 * @version $Revision: 6967 $
 */

public interface FindHook {
	/**
	 * Find hook method. This method is called during the service find operation
	 * (for example, {@link BundleContext#getServiceReferences(String, String)}
	 * ). This method can filter the result of the find operation.
	 * 
	 * @param context The bundle context of the bundle performing the find
	 *        operation.
	 * @param name The class name of the services to find or <code>null</code>
	 *        to find all services.
	 * @param filter The filter criteria of the services to find or
	 *        <code>null</code> for no filter criteria.
	 * @param allServices <code>true</code> if the find operation is the result
	 *        of a call to
	 *        {@link BundleContext#getAllServiceReferences(String, String)}
	 * @param references A <code>Collection</code> of Service References to be
	 *        returned as a result of the find operation. The implementation of
	 *        this method may remove service references from the collection to
	 *        prevent the references from being returned to the bundle
	 *        performing the find operation. The collection supports all the
	 *        optional <code>Collection</code> operations except
	 *        <code>add</code> and <code>addAll</code>. Attempting to add to the
	 *        collection will result in an
	 *        <code>UnsupportedOperationException</code>. The collection is not
	 *        synchronized.
	 */
	void find(BundleContext context, String name, String filter,
			boolean allServices,
			Collection/* <ServiceReference> */references);
}
