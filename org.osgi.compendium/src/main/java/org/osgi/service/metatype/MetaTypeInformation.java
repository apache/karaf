/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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

import org.osgi.framework.Bundle;

/**
 * A MetaType Information object is created by the MetaTypeService to return
 * meta type information for a specific bundle.
 * 
 * @version $Revision: 5673 $
 * @since 1.1
 */
public interface MetaTypeInformation extends MetaTypeProvider {
	/**
	 * Return the PIDs (for ManagedServices) for which ObjectClassDefinition
	 * information is available.
	 * 
	 * @return Array of PIDs.
	 */
	public String[] getPids();

	/**
	 * Return the Factory PIDs (for ManagedServiceFactories) for which
	 * ObjectClassDefinition information is available.
	 * 
	 * @return Array of Factory PIDs.
	 */
	public String[] getFactoryPids();

	/**
	 * Return the bundle for which this object provides meta type information.
	 * 
	 * @return Bundle for which this object provides meta type information.
	 */
	public Bundle getBundle();
}
