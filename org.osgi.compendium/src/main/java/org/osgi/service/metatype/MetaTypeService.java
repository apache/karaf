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
 * The MetaType Service can be used to obtain meta type information for a
 * bundle. The MetaType Service will examine the specified bundle for meta type
 * documents to create the returned <code>MetaTypeInformation</code> object.
 * 
 * <p>
 * If the specified bundle does not contain any meta type documents, then a
 * <code>MetaTypeInformation</code> object will be returned that wrappers any
 * <code>ManagedService</code> or <code>ManagedServiceFactory</code>
 * services registered by the specified bundle that implement
 * <code>MetaTypeProvider</code>. Thus the MetaType Service can be used to
 * retrieve meta type information for bundles which contain a meta type
 * documents or which provide their own <code>MetaTypeProvider</code> objects.
 * 
 * @version $Revision: 5673 $
 * @since 1.1
 */
public interface MetaTypeService {
	/**
	 * Return the MetaType information for the specified bundle.
	 * 
	 * @param bundle The bundle for which meta type information is requested.
	 * @return A MetaTypeInformation object for the specified bundle.
	 */
	public MetaTypeInformation getMetaTypeInformation(Bundle bundle);

	/**
	 * Location of meta type documents. The MetaType Service will process each
	 * entry in the meta type documents directory.
	 */
	public final static String	METATYPE_DOCUMENTS_LOCATION	= "OSGI-INF/metatype";
}
