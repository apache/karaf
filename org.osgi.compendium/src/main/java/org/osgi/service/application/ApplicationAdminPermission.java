/*
 * Copyright (c) OSGi Alliance (2004, 2009). All Rights Reserved.
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

package org.osgi.service.application;

import java.security.Permission;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * This class implements permissions for manipulating applications and their
 * instances.
 * <P>
 * ApplicationAdminPermission can be targeted to applications that matches the
 * specified filter.
 * <P>
 * ApplicationAdminPermission may be granted for different actions:
 * <code>lifecycle</code>, <code>schedule</code> and <code>lock</code>. The
 * permission <code>schedule</code> implies the permission
 * <code>lifecycle</code>.
 * 
 * @version $Revision: 6860 $
 */
public class ApplicationAdminPermission extends Permission {
	private static final long serialVersionUID = 1L;
  
	/**
	 * Allows the lifecycle management of the target applications.
	 */
	public static final String LIFECYCLE_ACTION = "lifecycle";

	/**
	 * Allows scheduling of the target applications. The permission to
	 * schedule an application implies that the scheduler can also 
	 * manage the lifecycle of that application i.e. <code>schedule</code>
	 * implies <code>lifecycle</code>
	 */
	public static final String SCHEDULE_ACTION = "schedule";

	/**
	 * Allows setting/unsetting the locking state of the target applications.
	 */
	public static final String LOCK_ACTION = "lock";

	private ApplicationDescriptor	applicationDescriptor;

	/**
	 * Constructs an ApplicationAdminPermission. The <code>filter</code>
	 * specifies the target application. The <code>filter</code> is an
	 * LDAP-style filter, the recognized properties are <code>signer</code>
	 * and <code>pid</code>. The pattern specified in the <code>signer</code>
	 * is matched with the Distinguished Name chain used to sign the application. 
	 * Wildcards in a DN are not matched according to the filter string rules, 
	 * but according to the rules defined for a DN chain. The attribute 
	 * <code>pid</code> is matched with the PID of the application according to
	 * the filter string rules. 
	 * <p>
	 * If the <code>filter</code> is <code>null</code> then it matches 
	 * <code>"*"</code>. If
	 * <code>actions</code> is <code>"*"</code> then it identifies all the
	 * possible actions.
	 * 
	 * @param filter
	 *            filter to identify application. The value <code>null</code>
	 *            is equivalent to <code>"*"</code> and it indicates "all application".
	 * @param actions
	 *            comma-separated list of the desired actions granted on the
	 *            applications or "*" means all the actions. It must not be
	 *            <code>null</code>. The order of the actions in the list is
	 *            not significant.
	 * @throws InvalidSyntaxException 
	 *            is thrown if the specified <code>filter</code> is not syntactically
	 *            correct.
	 * 
	 * @exception NullPointerException
	 *                is thrown if the actions parameter is <code>null</code>
	 * 
	 * @see ApplicationDescriptor
	 * @see org.osgi.framework.AdminPermission
	 */
	public ApplicationAdminPermission(String filter, String actions) throws InvalidSyntaxException {
		super(filter == null ? "*" : filter);
		
		if( filter == null )
			filter = "*";
		
		if( actions == null )
			throw new NullPointerException( "Action string cannot be null!" );
		
		this.applicationDescriptor = null;
		this.filter = (filter == null ? "*" : filter);
		this.actions = actions;

		if( !filter.equals( "*" ) && !filter.equals( "<<SELF>>" ) )
			FrameworkUtil.createFilter( this.filter ); // check if the filter is valid
		init();
	}
	
	/**
	 * This contructor should be used when creating <code>ApplicationAdminPermission</code>
	 * instance for <code>checkPermission</code> call. 
	 * @param application the tareget of the operation, it must not be <code>null</code>
	 * @param actions the required operation. it must not be <code>null</code>
	 * @throws NullPointerException if any of the arguments is null. 
	 */
	public ApplicationAdminPermission(ApplicationDescriptor application, String actions) {
		super(application.getApplicationId());
				
		if( application == null || actions == null )
			throw new NullPointerException( "ApplicationDescriptor and action string cannot be null!" );
		
		this.filter = application.getApplicationId();
		this.applicationDescriptor = application;
		this.actions = actions;
		
		init();
	}
	
	/**
	 * This method can be used in the {@link java.security.ProtectionDomain}
	 * implementation in the <code>implies</code> method to insert the
	 * application ID of the current application into the permission being
	 * checked. This enables the evaluation of the 
	 * <code>&lt;&lt;SELF&gt;&gt;</code> pseudo targets.
	 * @param applicationId the ID of the current application.
	 * @return the permission updated with the ID of the current application
	 */
	public ApplicationAdminPermission setCurrentApplicationId(String applicationId) {
		ApplicationAdminPermission newPerm = null;
		
		if( this.applicationDescriptor == null ) {
			try {
				newPerm = new ApplicationAdminPermission( this.filter, this.actions );
			}catch( InvalidSyntaxException e ) {
				throw new RuntimeException(e); /* this can never happen */
			}
		}
		else	
		    newPerm = new ApplicationAdminPermission( this.applicationDescriptor, this.actions );
		
		newPerm.applicationID = applicationId;
		
		return newPerm;
	}

	/**
	 * Checks if the specified <code>permission</code> is implied by this permission.
	 * The method returns true under the following conditions:
	 * <UL>
	 * <LI> This permission was created by specifying a filter (see {@link #ApplicationAdminPermission(String, String)})
	 * <LI> The implied <code>otherPermission</code> was created for a particular {@link ApplicationDescriptor}
	 *      (see {@link #ApplicationAdminPermission(ApplicationDescriptor, String)})
	 * <LI> The <code>filter</code> of this permission mathes the <code>ApplicationDescriptor</code> specified
	 *      in the <code>otherPermission</code>. If the filter in this permission is the 
	 *      <code>&lt;&lt;SELF&gt;&gt;</code> pseudo target, then the currentApplicationId set in the 
	 *      <code>otherPermission</code> is compared to the application Id of the target 
	 *      <code>ApplicationDescriptor</code>.
	 * <LI> The list of permitted actions in this permission contains all actions required in the 
	 *      <code>otherPermission</code>  
	 * </UL> 
	 * Otherwise the method returns false.
	 * @param otherPermission the implied permission
	 * @return true if this permission implies the <code>otherPermission</code>, false otherwise.
	 */
  public boolean implies(Permission otherPermission) {
  	  if( otherPermission == null )
  	  	return false;
  	  	
      if(!(otherPermission instanceof ApplicationAdminPermission))
          return false;

      ApplicationAdminPermission other = (ApplicationAdminPermission) otherPermission;

      if( !filter.equals("*") ) {
       	if( other.applicationDescriptor == null )
       		return false;
       	
      	if( filter.equals( "<<SELF>>") ) {
            if( other.applicationID == null )
          		return false; /* it cannot be, this might be a bug */
            
      		if( !other.applicationID.equals( other.applicationDescriptor.getApplicationId() ) )
      			return false;
      	}
      	else {
      		Hashtable props = new Hashtable();
      		props.put( "pid", other.applicationDescriptor.getApplicationId() );
      		props.put( "signer", new SignerWrapper( other.applicationDescriptor ) );
      		      		
      		Filter flt = getFilter();
      		if( flt == null )
      			return false;
      		
      		if( !flt.match( props ) )
      			return false;
      	}
      }
      
      if( !actionsVector.containsAll( other.actionsVector ) )
      	return false;
      
      return true;
  }

  public boolean equals(Object with) {
  	if( with == null || !(with instanceof ApplicationAdminPermission) )
  		return false;
  	
  	ApplicationAdminPermission other = (ApplicationAdminPermission)with;  	
  	
  	// Compare actions:
  	if( other.actionsVector.size() != actionsVector.size() )
  		return false;
  	
  	for( int i=0; i != actionsVector.size(); i++ )
  		if( !other.actionsVector.contains( actionsVector.get( i ) ) )
  			return false;
  	
  	
  	return equal(this.filter, other.filter ) && equal(this.applicationDescriptor, other.applicationDescriptor)
  			&& equal(this.applicationID, other.applicationID);
  }
  
  /**
   * Compares parameters for equality. If both object are null, they are considered
   * equal.
   * @param a object to compare
   * @param b other object to compare
   * @return true if both objects are equal or both are null
   */
  private static boolean equal(Object a, Object b) {
	  // This equation is true if both references are null or both point
	  // to the same object. In both cases they are considered as equal.
	  if( a == b ) {
		  return true;
	  }
	  
	  return a.equals(b);
  }

  public int hashCode() {
	  int hc = 0;
	  for( int i=0; i != actionsVector.size(); i++ )
		  hc ^= ((String)actionsVector.get( i )).hashCode();
	  hc ^= (null == this.filter )? 0 : this.filter.hashCode();
	  hc ^= (null == this.applicationDescriptor) ? 0 : this.applicationDescriptor.hashCode();
	  hc ^= (null == this.applicationID) ? 0 : this.applicationID.hashCode();
	  return hc;
  }

  /**
   * Returns the actions of this permission.
   * @return the actions specified when this permission was created
   */
  public String getActions() {
  	return actions;
  }

  private String applicationID;

  private static final Vector ACTIONS = new Vector();
  private              Vector actionsVector;
  private final        String filter;
  private final        String actions;
  private              Filter appliedFilter = null; 
  
  static {
      ACTIONS.add(LIFECYCLE_ACTION);
      ACTIONS.add(SCHEDULE_ACTION);
      ACTIONS.add(LOCK_ACTION);
  }

  private static Vector actionsVector(String actions) {
      Vector v = new Vector();
      StringTokenizer t = new StringTokenizer(actions.toUpperCase(), ",");
      while (t.hasMoreTokens()) {
          String action = t.nextToken().trim();
          v.add(action.toLowerCase());
      }
      
      if( v.contains( SCHEDULE_ACTION ) && !v.contains( LIFECYCLE_ACTION ) )
    	  v.add( LIFECYCLE_ACTION );
      
      return v;
  }
  

  private static class SignerWrapper extends Object {
  	private String pattern;
  	private ApplicationDescriptor appDesc;
  	
  	/**
  	 * @param pattern
  	 */
  	public SignerWrapper(String pattern) {
  		this.pattern = pattern;    			
  	}
  	
  	SignerWrapper(ApplicationDescriptor appDesc) {
  		this.appDesc = appDesc;
  	}
  	
  	public boolean equals(Object o) {
  		if (!(o instanceof SignerWrapper))
  			return false;
  		SignerWrapper other = (SignerWrapper) o;
  		ApplicationDescriptor matchAppDesc = (ApplicationDescriptor) (appDesc != null ? appDesc : other.appDesc);
  		String matchPattern = appDesc != null ? other.pattern : pattern;
  		return matchAppDesc.matchDNChain(matchPattern);
  	}
  }
  
  private void init() {
		actionsVector = actionsVector( actions );

		if ( actions.equals("*") )
			actionsVector = actionsVector( LIFECYCLE_ACTION + "," + SCHEDULE_ACTION + "," + LOCK_ACTION );
		else if (!ACTIONS.containsAll(actionsVector))
      throw new IllegalArgumentException("Illegal action!");
		
		applicationID = null;
  }
  
  private Filter getFilter() {
  	if (appliedFilter == null) {
  		try {
  			appliedFilter = FrameworkUtil.createFilter(filter);
		} catch (InvalidSyntaxException e) {
			//we will return null
		}
  	}     		
  	return appliedFilter;
  }
}
