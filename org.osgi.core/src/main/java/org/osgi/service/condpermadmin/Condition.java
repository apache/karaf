/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/Condition.java,v 1.13 2006/06/16 16:31:37 hargrave Exp $
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

import java.util.Dictionary;

/**
 * The interface implemented by a Condition. Conditions are bound to Permissions
 * using Conditional Permission Info. The Permissions of a ConditionalPermission
 * Info can only be used if the associated Conditions are satisfied.
 * 
 * @version $Revision: 1.13 $
 */
public interface Condition {
	/**
	 * A Condition object that will always evaluate to true and that is never
	 * postponed.
	 */
	public final static Condition	TRUE	= new BooleanCondition(true);

	/**
	 * A Condition object that will always evaluate to false and that is never
	 * postponed.
	 */
	public final static Condition	FALSE	= new BooleanCondition(false);

	/**
	 * Returns whether the evaluation must be postponed until the end of the
	 * permission check. This method returns <code>true</code> if the
	 * evaluation of the Condition must be postponed until the end of the
	 * permission check. If this method returns <code>false</code>, this
	 * Condition must be able to directly answer the {@link #isSatisfied()}
	 * method. In other words, isSatisfied() will return very quickly since no
	 * external sources, such as for example users, need to be consulted.
	 * 
	 * @return <code>true</code> to indicate the evaluation must be postponed.
	 *         Otherwise, <code>false</code> if the evaluation can be
	 *         immediately performed.
	 */
	boolean isPostponed();

	/**
	 * Returns whether the Condition is satisfied.
	 * 
	 * @return <code>true</code> to indicate the Conditions is satisfied. 
	 *         Otherwise, <code>false</code> if the Condition is not satisfied.
	 */
	boolean isSatisfied();

	/**
	 * Returns whether the Condition is mutable.
	 * 
	 * @return <code>true</code> to indicate the value returned by
	 *         {@link #isSatisfied()} can change. Otherwise, <code>false</code>
	 *         if the value returned by {@link #isSatisfied()} will not change.
	 */
	boolean isMutable();

	/**
	 * Returns whether a the set of Conditions are satisfied. Although this
	 * method is not static, it must be implemented as if it were static. All of
	 * the passed Conditions will be of the same type and will correspond to the
	 * class type of the object on which this method is invoked.
	 * 
	 * @param conditions The array of Conditions.
	 * @param context A Dictionary object that implementors can use to track
	 *        state. If this method is invoked multiple times in the same
	 *        permission evaluation, the same Dictionary will be passed multiple
	 *        times. The SecurityManager treats this Dictionary as an opaque
	 *        object and simply creates an empty dictionary and passes it to
	 *        subsequent invocations if multiple invocatios are needed.
	 * @return <code>true</code> if all the Conditions are satisfied.
	 *         Otherwise, <code>false</code> if one of the Conditions is not
	 *         satisfied.
	 */
	boolean isSatisfied(Condition conditions[], Dictionary context);

}

/**
 * Package internal class used to define the {@link Condition#FALSE} and
 * {@link Condition#TRUE} constants.
 */
final class BooleanCondition implements Condition {
	final boolean	satisfied;

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
		for (int i = 0; i < conds.length; i++) {
			if (!conds[i].isSatisfied())
				return false;
		}
		return true;
	}

}
