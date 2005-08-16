/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/ConditionalPermissionInfo.java,v 1.7 2005/07/14 10:47:13 pkriens Exp $
 *
 * Copyright (c) OSGi Alliance (2004, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
 
package org.osgi.service.condpermadmin;

import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * This interface describes a binding of a set of Conditions to a set of
 * Permissions. Instances of this interface are obtained from the
 * ConditionalPermissionAdmin service. This interface is also used to remove
 * ConditionalPermissionCollections from ConditionPermissionAdmin.
 */
public interface ConditionalPermissionInfo {
	/**
	 * Returns the ConditionInfos for the Conditions that must be satisfied to
	 * enable this ConditionalPermissionCollection.
	 */
	ConditionInfo[] getConditionInfos();

	/**
	 * Returns the PermissionInfos for the Permission in this
	 * ConditionalPermissionCollection.
	 */
	PermissionInfo[] getPermissionInfos();

	/**
	 * Removes the ConditionalPermissionCollection from the
	 * ConditionalPermissionAdmin.
	 */
	void delete();
	
	/**
	 * Return the name of this Conditional Permission Info object.
	 * 
	 */
	String getName();
}
