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

package org.osgi.service.condpermadmin;

import java.util.List;

/**
 * Update the Conditional Permission Table. There may be many update objects in
 * the system at one time. If commit is called and the Conditional Permission
 * Table has been modified since this update was created, then the call to
 * commit will fail and this object should be discarded.
 * 
 * @ThreadSafe
 * @version $Revision: 6492 $
 * @since 1.1
 */
public interface ConditionalPermissionUpdate {
	/**
	 * This method returns the list of {@link ConditionalPermissionInfo}s for
	 * this update. This list is originally based on the Conditional Permission
	 * Table at the time this update was created. The list returned by this
	 * method will be replace the Conditional Permission Table if commit is
	 * called and is successful.
	 * <p>
	 * The {@link ConditionalPermissionInfo#delete delete} method of the
	 * ConditionalPermissionInfos in the list must throw
	 * UnsupportedOperationException.
	 * <p>
	 * The list returned by this method is ordered and the most significant
	 * table entry is the first entry in the list.
	 * 
	 * @return A <code>List</code> of the {@link ConditionalPermissionInfo}s
	 *         which represent the Conditional Permissions maintained by this
	 *         update. Modifications to this list will not affect the
	 *         Conditional Permission Table until successfully committed. The
	 *         list may be empty if the Conditional Permission Table was empty
	 *         when this update was created.
	 */
	List /* <ConditionalPermissionInfo> */getConditionalPermissionInfos();

	/**
	 * Commit this update. If no changes have been made to the Conditional
	 * Permission Table since this update was created, then this method will
	 * replace the Conditional Permission Table with this update's Conditional
	 * Permissions. This method may only be successfully called once on this
	 * object.
	 * <p>
	 * If any of the {@link ConditionalPermissionInfo}s in the update list has
	 * <code>null</code> as a name it will be replaced with a new
	 * {@link ConditionalPermissionInfo} object that has a generated name which
	 * is unique within the list.
	 * <p>
	 * No two entries in this update's Conditional Permissions may have the same
	 * name. Other consistency checks may also be performed. If this update's
	 * Conditional Permissions are determined to be inconsistent in some way
	 * then an <code>IllegalStateException</code> will be thrown.
	 * <p>
	 * This method returns <code>false</code> if the commit did not occur
	 * because the Conditional Permission Table has been modified since the
	 * creation of this update.
	 * 
	 * @return <code>true</code> if the commit was successful.
	 *         <code>false</code> if the commit did not occur because the
	 *         Conditional Permission Table has been modified since the creation
	 *         of this update.
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 * @throws IllegalStateException If this update's Conditional Permissions
	 *         are not valid or inconsistent. For example, this update has two
	 *         Conditional Permissions in it with the same name.
	 */
	boolean commit();
}
