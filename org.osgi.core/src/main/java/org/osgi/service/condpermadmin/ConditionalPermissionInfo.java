/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/ConditionalPermissionInfo.java,v 1.11 2006/06/16 16:31:37 hargrave Exp $
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

package org.osgi.service.condpermadmin;

import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * A binding of a set of Conditions to a set of Permissions. Instances of this
 * interface are obtained from the Conditional Permission Admin service.
 * 
 * @version $Revision: 1.11 $
 */
public interface ConditionalPermissionInfo {
	/**
	 * Returns the Condition Infos for the Conditions that must be satisfied to
	 * enable the Permissions.
	 * 
	 * @return The Condition Infos for the Conditions in this Conditional
	 *         Permission Info.
	 */
	public ConditionInfo[] getConditionInfos();

	/**
	 * Returns the Permission Infos for the Permission in this Conditional
	 * Permission Info.
	 * 
	 * @return The Permission Infos for the Permission in this Conditional
	 *         Permission Info.
	 */
	public PermissionInfo[] getPermissionInfos();

	/**
	 * Removes this Conditional Permission Info from the Conditional Permission
	 * Admin.
	 * 
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 */
	public void delete();

	/**
	 * Returns the name of this Conditional Permission Info.
	 * 
	 * @return The name of this Conditional Permission Info.
	 */
	public String getName();
}
