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

import java.util.Collection;
import java.util.List;

/**
 * Metadata for a service to be registered by the Blueprint Container when
 * enabled.
 * 
 * <p>
 * This is specified by the <code>service</code> element.
 * 
 * @ThreadSafe
 * @version $Revision: 7563 $
 */
public interface ServiceMetadata extends ComponentMetadata {

	/**
	 * Do not auto-detect types for advertised service interfaces
	 * 
	 * @see #getAutoExport()
	 */
	static final int		AUTO_EXPORT_DISABLED		= 1;

	/**
	 * Advertise all Java interfaces implemented by the component instance type
	 * as service interfaces.
	 * 
	 * @see #getAutoExport()
	 */
	static final int		AUTO_EXPORT_INTERFACES		= 2;

	/**
	 * Advertise all Java classes in the hierarchy of the component instance
	 * type as service interfaces.
	 * 
	 * @see #getAutoExport()
	 */
	static final int		AUTO_EXPORT_CLASS_HIERARCHY	= 3;

	/**
	 * Advertise all Java classes and interfaces in the component instance type
	 * as service interfaces.
	 * 
	 * @see #getAutoExport()
	 */
	static final int	AUTO_EXPORT_ALL_CLASSES		= 4;

	/**
	 * Return the Metadata for the component to be exported as a service.
	 * 
	 * This is specified inline or via the <code>ref</code> attribute of the
	 * service.
	 * 
	 * @return The Metadata for the component to be exported as a service.
	 */
	Target getServiceComponent();

	/**
	 * Return the type names of the interfaces that the service should be
	 * advertised as supporting.
	 * 
	 * This is specified in the <code>interface</code> attribute or child
	 * <code>interfaces</code> element of the service.
	 * 
	 * @return An immutable List of <code>String</code> for the type names of
	 *         the interfaces that the service should be advertised as
	 *         supporting. The List is empty if using <code>auto-export</code>
	 *         or no interface names are specified for the service.
	 */
	List/* <String> */getInterfaces();

	/**
	 * Return the auto-export mode for the service.
	 * 
	 * This is specified by the <code>auto-export</code> attribute of the
	 * service.
	 * 
	 * @return The auto-export mode for the service.
	 * @see #AUTO_EXPORT_DISABLED
	 * @see #AUTO_EXPORT_INTERFACES
	 * @see #AUTO_EXPORT_CLASS_HIERARCHY
	 * @see #AUTO_EXPORT_ALL_CLASSES
	 */
	int getAutoExport();

	/**
	 * Return the user declared properties to be advertised with the service.
	 * 
	 * This is specified by the <code>service-properties</code> element of the
	 * service.
	 * 
	 * @return An immutable List of {@link MapEntry} objects for the user
	 *         declared properties to be advertised with the service. The List
	 *         is empty if no service properties are specified for the service.
	 */
	List/* <MapEntry> */getServiceProperties();

	/**
	 * Return the ranking value to use when advertising the service. If the
	 * ranking value is zero, the service must be registered without a
	 * <code>service.ranking</code> service property.
	 * 
	 * This is specified by the <code>ranking</code> attribute of the service.
	 * 
	 * @return The ranking value to use when advertising the service.
	 */
	int getRanking();

	/**
	 * Return the registration listeners to be notified when the service is
	 * registered and unregistered with the framework.
	 * 
	 * This is specified by the <code>registration-listener</code> elements of
	 * the service.
	 * 
	 * @return An immutable Collection of {@link RegistrationListener} objects
	 *         to be notified when the service is registered and unregistered
	 *         with the framework. The Collection is empty if no registration
	 *         listeners are specified for the service.
	 */
	Collection /* <RegistrationListener> */getRegistrationListeners();
}
