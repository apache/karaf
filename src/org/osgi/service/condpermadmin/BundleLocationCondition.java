/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/BundleLocationCondition.java,v 1.9 2005/05/25 16:22:46 twatson Exp $
 * 
 * Copyright (c) OSGi Alliance (2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.osgi.service.condpermadmin;

import java.io.FilePermission;
import org.osgi.framework.Bundle;

/**
 * 
 * Checks to see if a Bundle matches the given location pattern. Pattern matching
 * is done using FilePermission style patterns.
 * 
 * @version $Revision: 1.9 $
 */
public class BundleLocationCondition {
	private static final String CONDITION_TYPE = "org.osgi.service.condpermadmin.BundleLocationCondition";
	/**
	 * Constructs a condition that tries to match the passed Bundle's location
	 * to the location pattern.
	 * 
	 * @param bundle the Bundle being evaluated.
	 * @param info the ConditionInfo to construct the condition for.  The args of the 
	 *        ConditionInfo specify the location to match the Bundle
	 *        location to. Matching is done according to the patterns documented
	 *        in FilePermission.
	 */
	static public Condition getCondition(Bundle bundle, ConditionInfo info) {
		if (!CONDITION_TYPE.equals(info.getType()))
			throw new IllegalArgumentException("ConditionInfo must be of type \"" + CONDITION_TYPE + "\"");
		String[] args = info.getArgs();
		if (args.length != 1)
			throw new IllegalArgumentException("Illegal number of args: " + args.length);
		String location = args[0];
		FilePermission locationPat = new FilePermission(location, "read");
		FilePermission sourcePat = new FilePermission(bundle.getLocation().toString(), "read");
		return locationPat.implies(sourcePat) ? Condition.TRUE : Condition.FALSE;
	}

	private BundleLocationCondition() {
		// private constructor to prevent objects of this type
	}
}
