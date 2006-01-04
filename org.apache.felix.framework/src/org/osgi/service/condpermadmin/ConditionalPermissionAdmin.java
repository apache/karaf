/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/ConditionalPermissionAdmin.java,v 1.6 2005/07/14 10:47:13 pkriens Exp $
 * 
 * Copyright (c) OSGi Alliance (2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.condpermadmin;

import java.security.AccessControlContext;
import java.util.Enumeration;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * This is a framework service that allows ConditionalPermissionInfos to be
 * added to, retrieved from, and removed from the framework.
 * 
 * @version $Revision: 1.6 $
 */
public interface ConditionalPermissionAdmin {
	/**
	 * Add a new Conditional Permission Info to the repository.
	 * 
	 * The Conditional Permission Info will be given a unique, never reused name.
	 * 
	 * @param conds the Conditions that need to be satisfied to enable the
	 *        corresponding Permissions.
	 * @param perms the Permissions that are enable when the corresponding
	 *        Conditions are satisfied.
	 * @return the ConditionalPermissionInfo that for the newly added Conditions
	 *         and Permissions.
	 */
	ConditionalPermissionInfo addConditionalPermissionInfo(
			ConditionInfo conds[], PermissionInfo perms[]);

	/**
	 * Set or create a Conditional Permission Info with conditions and
	 * permissions.
	 * 
	 * If the given <code>name</code> is null or not used in the repository
	 * yet, a new Conditional Permission Info must be created, otherwise the
	 * existing Conditional Permission Info must be reused.
	 * 
	 * @param name the name of this Conditional Permission Info, or
	 *        <code>null</code>.
	 * @param conds the Conditions that need to be satisfied to enable the
	 *        corresponding Permissions.
	 * @param perms the Permissions that are enable when the corresponding
	 *        Conditions are satisfied.
	 * @return the ConditionalPermissionInfo that for the newly added Conditions
	 *         and Permissions.
	 */
	ConditionalPermissionInfo setConditionalPermissionInfo(String name,
			ConditionInfo conds[], PermissionInfo perms[]);

	/**
	 * Returns the ConditionalPermissionInfos that are currently managed by
	 * ConditionalPermissionAdmin. The Enumeration is made up of
	 * ConditionalPermissionInfos. Calling ConditionalPermissionInfo.delete()
	 * will remove the ConditionalPermissionInfo from
	 * ConditionalPermissionAdmin.
	 * 
	 * @return the ConditionalPermissionInfos that are currently managed by
	 *         ConditionalPermissionAdmin. The Enumeration is made up of
	 *         ConditionalPermissionInfos.
	 */
	Enumeration getConditionalPermissionInfos();

	/**
	 * Return the the Conditional Permission Info with the given name.
	 * 
	 * @param name the name of the Conditional Permission Info that must be
	 *        returned
	 */
	ConditionalPermissionInfo getConditionalPermissionInfo(String name);

	/**
	 * Returns the AccessControlContext that corresponds to the given signers.
	 * 
	 * @param signers the signers that will be checked agains
	 *        BundleSignerCondition.
	 * @return an AccessControlContext that has the Permissions associated with
	 *         the signer.
	 */
	AccessControlContext getAccessControlContext(String signers[]);
}
