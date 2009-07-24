/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
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
package org.osgi.util.mobile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

/**
 * Class representing a user prompt condition. Instances of this class hold two
 * values: a prompt string that is to be displayed to the user and the
 * permission level string according to MIDP2.0 (oneshot, session, blanket).
 *  
 */
public class UserPromptCondition implements Condition {
	
	/*
	 * NOTE: An implementor may also choose to replace this class in
	 * their distribution with a class that directly interfaces with the
	 * policy implementation. This replacement class MUST NOT alter the
	 * public/protected signature of this class.
	 */
	// this will need to be set by the implementation class
	static Method factory = null;
	Condition realUserPromptCondition;

	private final Bundle bundle;
	private final String levels;
	private final String defaultLevel;
	private final String catalogName;
	private final String message;
	
	/**
	 * Returns a UserPromptCondition object with the given prompt string and permission
	 * level. The user should be given choice as to what level of permission is
	 * given. Thus, the lifetime of the permission is controlled by the user.
	 *
	 * @param bundle the bundle to ask about.
	 * @param conditionInfo the conditionInfo containing the construction information. Its 
	 * 			{@link ConditionInfo#getArgs()} method should return a String array with 4
	 * 			strings in it:
	 * <ol start="0">
	 * <li>the possible permission levels. This is a comma-separated list that can contain
	 * 		following strings: ONESHOT SESSION BLANKET. The order is not important. This
	 * 		parameter is case-insensitive.
	 * 		</li>
	 * <li>the default permission level, one chosen from the possible permission levels. If
	 * 		it is an empty string, then there is no default. This parameter
	 * 		is case-insensitive.</li>
	 * <li>the message catalog base name. It will be loaded by a {@link java.util.ResourceBundle},
	 * 		or equivalent
	 * 		from an exporting OSGi Bundle. Thus, if the catalogName is "com.provider.messages.userprompt",
	 * 		then there should be an OSGi Bundle exporting the "com.provider.messages" package, and inside
	 * 		it files like "userprompt_en_US.properties".</li>
	 * <li>textual description of the condition, to be displayed to the user. If
	 * 		it starts with a '%' sign, then the message is looked up from the catalog specified previously.
	 * 		The key is the rest of the string after the '%' sign.</li>
	 * </ol>
	 * @return The requested UserPromptCondition.
	 * @throws IllegalArgumentException if the parameters are malformed.
	 * @throws NullPointerException if one of the parameters is <code>null</code>.
	 */
	public static Condition getCondition(Bundle bundle,ConditionInfo conditionInfo)
	{
		String[] args = conditionInfo.getArgs();
		if (args==null) throw new NullPointerException("args");
		if (args.length!=4) throw new IllegalArgumentException("args.length=="+args.length+" (should be 4)");
		if (bundle==null) throw new NullPointerException("bundle");
		String levels = args[0];
		String defaultLevel = args[1];
		String catalogName = args[2];
		String message = args[3];
		if (levels==null) throw new NullPointerException("levels");
		if (defaultLevel==null) throw new NullPointerException("defaultLevel");
		if (catalogName==null) throw new NullPointerException("catalogName");
		if (message==null) throw new NullPointerException("message");
		
		if (factory==null) {
			// the bundle implementing the UserPromptCondition has not started yet.
			// Do wrapping magick.
			return new UserPromptCondition(bundle,levels,defaultLevel,catalogName,message);
		} else {
			// there is already a factory, no need to do any wrapping magic
			try {
				return (Condition) factory.invoke(null,new Object[]{bundle,levels,defaultLevel,catalogName,message});
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				Throwable original = e.getTargetException();
				if (original instanceof NullPointerException) throw (NullPointerException) original;
				if (original instanceof IllegalArgumentException) throw (IllegalArgumentException) original;
				e.printStackTrace();
			}
			// the factory method is not working, fallback behavior:
			factory = null;
			return new UserPromptCondition(bundle,levels,defaultLevel,catalogName,message);
		}
	}

	/**
	 * Instances of the UserPromptCondition are simply store the construction parameters
	 * until a "real" UserPromptCondition is registered in setFactory(). At that point, it
	 * will delegate all calls there.
	 * @param unused this parameter is here so that ConditionalPermissionAdmin would not
	 * 		use this as the constructor instead of the getInstance
	 * @param bundle
	 * @param levels
	 * @param defaultLevel
	 * @param catalogName
	 * @param message
	 */
	private UserPromptCondition(Bundle bundle,String levels,String defaultLevel,String catalogName,String message) {
		this.bundle=bundle;
		this.levels=levels;
		this.defaultLevel=defaultLevel;
		this.catalogName=catalogName;
		this.message=message;
	}
	
	/**
	 * Check if a factory is registered, and if yes, create userprompt to delegate calls to.
	 */
	private void lookForImplementation() {
		if ((realUserPromptCondition==null)&&(factory!=null)) {
			try {
				realUserPromptCondition = (Condition) factory.invoke(null,new Object[]{bundle,levels,defaultLevel,catalogName,message});
				return;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			// only if the factory call fails with some invocation exception
			factory = null;
		}
	}
	
	/**
	 * Checks if the {@link #isSatisfied()} method needs to prompt the user, thus cannot
	 * give results instantly. 
	 * This depends on the permission level given in 
	 * {@link UserPromptCondition#getCondition(Bundle, ConditionInfo)}. 
	 * <ul>
	 * <li>ONESHOT - isPostponed always returns true. The user is prompted for question every time.</li>
	 * <li>SESSION - isPostponed returns true until the user decides either yes or no for the current session.</li>
	 * <li>BLANKET - isPostponed returns true until the user decides either always or never.</li>
	 * </ul>
	 * Regardless of the session level, the user is always given the option to reject the prompt
	 * permanently, as if BLANKET/never was chosen. In this case, the question is not postponed
	 * anymore, and {@link #isSatisfied()} returns false.<br/>
	 * If the system supports an separately accessible permission management GUI,
	 * that may reset the condition
	 * to its initial state.
	 * 
	 * @return True, if user interaction is needed.
	 */
	public boolean isPostponed() {
		lookForImplementation();
		if (realUserPromptCondition!=null) {
			return realUserPromptCondition.isPostponed();
		} else {
			return true;
		}
	}

	/**
	 * Checks whether the condition may change during the lifetime of the UserPromptCondition object.
	 * This depends on the permission level given in 
	 * {@link UserPromptCondition#getCondition(Bundle, ConditionInfo)}. 
	 * <ul>
	 * 	<li>ONESHOT - true</li>
	 *  <li>SESSION - true, if the application model's session lifetime is
	 *  		shorter than the UserPromptCondition object lifetime</li>
	 *  <li>BLANKET - false</li>
	 * </ul>
	 * If the system supports separately accessible permission management GUI,
	 * then this function may also return true for SESSION and BLANKET.
	 * 
	 * @return True, if the condition can change.
	 */
	public boolean isMutable() {
		lookForImplementation();
		if (realUserPromptCondition!=null) {
			return realUserPromptCondition.isMutable();
		} else {
			// since we don't know what the actual status is, we cannot say
			// "the condition cannot change anymore"
			return true;
		}
	}

	/**
	 * Displays the prompt string to
	 * the user and returns true if the user accepts. Depending on the
	 * amount of levels the condition is assigned to, the prompt may have
	 * multiple accept buttons and one of them can be selected by default (see
	 * default level parameter at {@link UserPromptCondition#getCondition(Bundle, ConditionInfo)}).
	 * It must always be possible for the user
	 * to stop further prompting of this question, even with ONESHOT and SESSION levels.
	 * In case of BLANKET
	 * and SESSION levels, it is possible that the user has already answered the question,
	 * in this case there will be no prompting, but immediate return with the previous answer.
	 * 
	 * @return True if the user accepts the prompt (or accepts any prompt in
	 *         case there are multiple permission levels).
	 */
	public boolean isSatisfied() {
		lookForImplementation();
		if (realUserPromptCondition!=null) {
			return realUserPromptCondition.isSatisfied();
		} else {
			// paranoid security option
			return false;
		}
	}

	/**
	 * Checks an array of UserPrompt conditions.
	 * 
	 * @param conds The array containing the UserPrompt conditions to evaluate.
	 * @param context Storage area for evaluation. The {@link org.osgi.service.condpermadmin.ConditionalPermissionAdmin}
	 * 		may evaluate a condition several times for one permission check, so this context
	 * 		will be used to store results of ONESHOT questions. This way asking the same question
	 * 		twice in a row can be avoided. If context is null, temporary results will not be stored.
	 * @return True, if all conditions are satisfied.
	 * @throws NullPointerException if conds is null.
	 */
	public boolean isSatisfied(Condition[] conds, Dictionary context) {
		lookForImplementation();
		if (realUserPromptCondition!=null) {
			return realUserPromptCondition.isSatisfied(conds,context);
		} else {
			// paranoid security option
			return false;
		}
	}
}
