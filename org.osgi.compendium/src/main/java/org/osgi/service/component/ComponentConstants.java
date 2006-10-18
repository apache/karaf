/*
 * $Header: /cvshome/build/org.osgi.service.component/src/org/osgi/service/component/ComponentConstants.java,v 1.14 2006/06/16 16:31:26 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2004, 2006). All Rights Reserved.
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

package org.osgi.service.component;

/**
 * Defines standard names for Service Component constants.
 * 
 * @version $Revision: 1.14 $
 */
public interface ComponentConstants {
	/**
	 * Manifest header (named &quot;Service-Component&quot;) specifying the XML
	 * documents within a bundle that contain the bundle's Service Component
	 * descriptions.
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	SERVICE_COMPONENT		= "Service-Component";

	/**
	 * A component property for a component configuration that contains the name
	 * of the component as specified in the <code>name</code> attribute of the
	 * <code>component</code> element. The type of this property must be
	 * <code>String</code>.
	 */
	public final static String	COMPONENT_NAME			= "component.name";

	/**
	 * A component property that contains the generated id for a component
	 * configuration. The type of this property must be <code>Long</code>.
	 * 
	 * <p>
	 * The value of this property is assigned by the Service Component Runtime
	 * when a component configuration is created. The Service Component Runtime
	 * assigns a unique value that is larger than all previously assigned values
	 * since the Service Component Runtime was started. These values are NOT
	 * persistent across restarts of the Service Component Runtime.
	 */
	public final static String	COMPONENT_ID			= "component.id";

	/**
	 * A service registration property for a Component Factory that contains the
	 * value of the <code>factory</code> attribute. The type of this property
	 * must be <code>String</code>.
	 */
	public final static String	COMPONENT_FACTORY		= "component.factory";

	/**
	 * The suffix for reference target properties. These properties contain the
	 * filter to select the target services for a reference. The type of this
	 * property must be <code>String</code>.
	 */
	public final static String	REFERENCE_TARGET_SUFFIX	= ".target";
}
