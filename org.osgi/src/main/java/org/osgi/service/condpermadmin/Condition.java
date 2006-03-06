/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/Condition.java,v 1.9 2005/05/25 16:22:46 twatson Exp $
 *
 * Copyright (c) OSGi Alliance (2004, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.condpermadmin;

import java.util.Dictionary;

/**
 * This interface is used to implement Conditions that are bound to Permissions
 * using ConditionalPermissionCollection. The Permissions of the
 * ConditionalPermissionCollection can only be used if the associated Condition
 * is satisfied.
 */
public interface Condition {
	/**
	 * A condition object that will always evaluate to true and that is never postponed.
	 */
	public final static Condition TRUE = new BooleanCondition(true);

	/**
	 * A condition object that will always evaluate to false and that is never postponed.
	 */
	public final static Condition FALSE = new BooleanCondition(false);

	/**
	 * This method returns true if the evaluation of the Condition must be postponed
	 * until the end of the permission check. If it returns false, it must be able
	 * to directly answer the isSatisfied method. In other
	 * words, isSatisfied() will return very quickly since no external sources,
	 * such as for example users, need to be consulted.
	 * 
	 * @return false if evaluation is immediate, otherwise true to indicate the evaluation must be postponed.
	 */
	boolean isPostponed();

	/**
	 * This method returns true if the Condition is satisfied.
	 */
	boolean isSatisfied();

	/**
	 * This method returns true if the satisfiability may change.
	 */
	boolean isMutable();

	/**
	 * This method returns true if the set of Conditions are satisfied. Although
	 * this method is not static, it should be implemented as if it were static.
	 * All of the passed Conditions will have the same type and will correspond
	 * to the class type of the object on which this method is invoked.
	 *
	 * @param conds the array of Conditions that must be satisfied
	 * @param context a Dictionary object that implementors can use to track 
	 * state. If this method is invoked multiple times in the same permission 
	 * evaluation, the same Dictionary will be passed multiple times. The
	 * SecurityManager treats this Dictionary as an opaque object simply
	 * creates an empty dictionary and passes it to subsequent invocations
	 * if multiple invocatios are needed.
	 * @return true if all the Conditions are satisfied.
	 */
	boolean isSatisfied(Condition conds[], Dictionary context);

	/**
	 * Package internal class used to define the {@link Condition#FALSE} and 
	 * {@link Condition#TRUE} constants.
	 */
	final static class BooleanCondition implements Condition {
		boolean satisfied;
		BooleanCondition(boolean satisfied) {
			this.satisfied = satisfied;
		}
		public boolean isPostponed() {
			return false;
		}
		public boolean isSatisfied() {
			return satisfied;
		}
		public boolean isMutable() {
			return false;
		}
		public boolean isSatisfied(Condition[] conds, Dictionary context) {
			for(int i = 0; i < conds.length; i++) {
				if (!conds[i].isSatisfied())
					return false;
			}
			return true;
		}
		
	}
}
