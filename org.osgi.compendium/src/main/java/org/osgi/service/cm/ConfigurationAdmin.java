/*
 * $Header: /cvshome/build/org.osgi.service.cm/src/org/osgi/service/cm/ConfigurationAdmin.java,v 1.17 2006/09/26 13:33:09 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2001, 2006). All Rights Reserved.
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
package org.osgi.service.cm;

import java.io.IOException;
import java.util.Dictionary;

import org.osgi.framework.InvalidSyntaxException;

/**
 * Service for administering configuration data.
 * 
 * <p>
 * The main purpose of this interface is to store bundle configuration data
 * persistently. This information is represented in <code>Configuration</code>
 * objects. The actual configuration data is a <code>Dictionary</code> of
 * properties inside a <code>Configuration</code> object.
 * 
 * <p>
 * There are two principally different ways to manage configurations. First
 * there is the concept of a Managed Service, where configuration data is
 * uniquely associated with an object registered with the service registry.
 * 
 * <p>
 * Next, there is the concept of a factory where the Configuration Admin service
 * will maintain 0 or more <code>Configuration</code> objects for a Managed
 * Service Factory that is registered with the Framework.
 * 
 * <p>
 * The first concept is intended for configuration data about "things/services"
 * whose existence is defined externally, e.g. a specific printer. Factories are
 * intended for "things/services" that can be created any number of times, e.g.
 * a configuration for a DHCP server for different networks.
 * 
 * <p>
 * Bundles that require configuration should register a Managed Service or a
 * Managed Service Factory in the service registry. A registration property
 * named <code>service.pid</code> (persistent identifier or PID) must be used
 * to identify this Managed Service or Managed Service Factory to the
 * Configuration Admin service.
 * 
 * <p>
 * When the ConfigurationAdmin detects the registration of a Managed Service, it
 * checks its persistent storage for a configuration object whose PID matches
 * the PID registration property (<code>service.pid</code>) of the Managed
 * Service. If found, it calls {@link ManagedService#updated} method with the
 * new properties. The implementation of a Configuration Admin service must run
 * these call-backs asynchronously to allow proper synchronization.
 * 
 * <p>
 * When the Configuration Admin service detects a Managed Service Factory
 * registration, it checks its storage for configuration objects whose
 * <code>factoryPid</code> matches the PID of the Managed Service Factory. For
 * each such <code>Configuration</code> objects, it calls the
 * <code>ManagedServiceFactory.updated</code> method asynchronously with the
 * new properties. The calls to the <code>updated</code> method of a
 * <code>ManagedServiceFactory</code> must be executed sequentially and not
 * overlap in time.
 * 
 * <p>
 * In general, bundles having permission to use the Configuration Admin service
 * can only access and modify their own configuration information. Accessing or
 * modifying the configuration of another bundle requires
 * <code>ConfigurationPermission[*,CONFIGURE]</code>.
 * 
 * <p>
 * <code>Configuration</code> objects can be <i>bound </i> to a specified
 * bundle location. In this case, if a matching Managed Service or Managed
 * Service Factory is registered by a bundle with a different location, then the
 * Configuration Admin service must not do the normal callback, and it should
 * log an error. In the case where a <code>Configuration</code> object is not
 * bound, its location field is <code>null</code>, the Configuration Admin
 * service will bind it to the location of the bundle that registers the first
 * Managed Service or Managed Service Factory that has a corresponding PID
 * property. When a <code>Configuration</code> object is bound to a bundle
 * location in this manner, the Confguration Admin service must detect if the
 * bundle corresponding to the location is uninstalled. If this occurs, the
 * <code>Configuration</code> object is unbound, that is its location field is
 * set back to <code>null</code>.
 * 
 * <p>
 * The method descriptions of this class refer to a concept of "the calling
 * bundle". This is a loose way of referring to the bundle which obtained the
 * Configuration Admin service from the service registry. Implementations of
 * <code>ConfigurationAdmin</code> must use a
 * {@link org.osgi.framework.ServiceFactory} to support this concept.
 * 
 * @version $Revision: 1.17 $
 */
public interface ConfigurationAdmin {
	/**
	 * Service property naming the Factory PID in the configuration dictionary.
	 * The property's value is of type <code>String</code>.
	 * 
	 * @since 1.1
	 */
	public final static String	SERVICE_FACTORYPID		= "service.factoryPid";
	/**
	 * Service property naming the location of the bundle that is associated
	 * with a a <code>Configuration</code> object. This property can be
	 * searched for but must not appear in the configuration dictionary for
	 * security reason. The property's value is of type <code>String</code>.
	 * 
	 * @since 1.1
	 */
	public final static String	SERVICE_BUNDLELOCATION	= "service.bundleLocation";

	/**
	 * Create a new factory <code>Configuration</code> object with a new PID.
	 * 
	 * The properties of the new <code>Configuration</code> object are
	 * <code>null</code> until the first time that its
	 * {@link Configuration#update(Dictionary)} method is called.
	 * 
	 * <p>
	 * It is not required that the <code>factoryPid</code> maps to a
	 * registered Managed Service Factory.
	 * <p>
	 * The <code>Configuration</code> object is bound to the location of the
	 * calling bundle.
	 * 
	 * @param factoryPid PID of factory (not <code>null</code>).
	 * @return A new <code>Configuration</code> object.
	 * @throws IOException if access to persistent storage fails.
	 * @throws SecurityException if caller does not have <code>ConfigurationPermission[*,CONFIGURE]</code> and <code>factoryPid</code> is bound to another bundle.
	 */
	public Configuration createFactoryConfiguration(String factoryPid)
			throws IOException;

	/**
	 * Create a new factory <code>Configuration</code> object with a new PID.
	 * 
	 * The properties of the new <code>Configuration</code> object are
	 * <code>null</code> until the first time that its
	 * {@link Configuration#update(Dictionary)} method is called.
	 * 
	 * <p>
	 * It is not required that the <code>factoryPid</code> maps to a
	 * registered Managed Service Factory.
	 * 
	 * <p>
	 * The <code>Configuration</code> is bound to the location specified. If
	 * this location is <code>null</code> it will be bound to the location of
	 * the first bundle that registers a Managed Service Factory with a
	 * corresponding PID.
	 * 
	 * @param factoryPid PID of factory (not <code>null</code>).
	 * @param location A bundle location string, or <code>null</code>.
	 * @return a new <code>Configuration</code> object.
	 * @throws IOException if access to persistent storage fails.
	 * @throws SecurityException if caller does not have <code>ConfigurationPermission[*,CONFIGURE]</code>.
	 */
	public Configuration createFactoryConfiguration(String factoryPid, String location)
			throws IOException;

	/**
	 * Get an existing <code>Configuration</code> object from the persistent
	 * store, or create a new <code>Configuration</code> object.
	 * 
	 * <p>
	 * If a <code>Configuration</code> with this PID already exists in
	 * Configuration Admin service return it. The location parameter is ignored
	 * in this case.
	 * 
	 * <p>
	 * Else, return a new <code>Configuration</code> object. This new object
	 * is bound to the location and the properties are set to <code>null</code>.
	 * If the location parameter is <code>null</code>, it will be set when a
	 * Managed Service with the corresponding PID is registered for the first
	 * time.
	 * 
	 * @param pid Persistent identifier.
	 * @param location The bundle location string, or <code>null</code>.
	 * @return An existing or new <code>Configuration</code> object.
	 * @throws IOException if access to persistent storage fails.
	 * @throws SecurityException if the caller does not have <code>ConfigurationPermission[*,CONFIGURE]</code>.
	 */
	public Configuration getConfiguration(String pid, String location)
			throws IOException;

	/**
	 * Get an existing or new <code>Configuration</code> object from the
	 * persistent store.
	 * 
	 * If the <code>Configuration</code> object for this PID does not exist,
	 * create a new <code>Configuration</code> object for that PID, where
	 * properties are <code>null</code>. Bind its location to the calling
	 * bundle's location.
	 * 
	 * <p>
	 * Otherwise, if the location of the existing <code>Configuration</code> object
	 * is <code>null</code>, set it to the calling bundle's location.
	 * 
	 * @param pid persistent identifier.
	 * @return an existing or new <code>Configuration</code> matching the PID.
	 * @throws IOException if access to persistent storage fails.
	 * @throws SecurityException if the <code>Configuration</code> object is bound to a location different from that of the calling bundle and it has no <code>ConfigurationPermission[*,CONFIGURE]</code>.
	 */
	public Configuration getConfiguration(String pid) throws IOException;

	/**
	 * List the current <code>Configuration</code> objects which match the
	 * filter.
	 * 
	 * <p>
	 * Only <code>Configuration</code> objects with non- <code>null</code>
	 * properties are considered current. That is,
	 * <code>Configuration.getProperties()</code> is guaranteed not to return
	 * <code>null</code> for each of the returned <code>Configuration</code>
	 * objects.
	 * 
	 * <p>
	 * Normally only <code>Configuration</code> objects that are bound to the
	 * location of the calling bundle are returned, or all if the caller has 
	 * <code>ConfigurationPermission[*,CONFIGURE]</code>.
	 * 
	 * <p>
	 * The syntax of the filter string is as defined in the {@link org.osgi.framework.Filter}
	 * class. The filter can test any configuration parameters including the
	 * following system properties:
	 * <ul>
	 * <li><code>service.pid</code>-<code>String</code>- the PID under
	 * which this is registered</li>
	 * <li><code>service.factoryPid</code>-<code>String</code>- the
	 * factory if applicable</li>
	 * <li><code>service.bundleLocation</code>-<code>String</code>- the
	 * bundle location</li>
	 * </ul>
	 * The filter can also be <code>null</code>, meaning that all
	 * <code>Configuration</code> objects should be returned.
	 * 
	 * @param filter A filter string, or <code>null</code> to
	 *        retrieve all <code>Configuration</code> objects.
	 * @return All matching <code>Configuration</code> objects, or
	 *         <code>null</code> if there aren't any.
	 * @throws IOException if access to persistent storage fails
	 * @throws InvalidSyntaxException if the filter string is invalid
	 */
	public Configuration[] listConfigurations(String filter) throws IOException,
			InvalidSyntaxException;
}
