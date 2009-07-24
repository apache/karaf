/*
 * 
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

package org.osgi.service.deploymentadmin.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;

import org.osgi.service.deploymentadmin.DeploymentAdminPermission;

/**
 * The <code>DeploymentCustomizerPermission</code> permission gives the right to 
 * Resource Processors to access a bundle's (residing in a Deployment Package) private area.
 * The bundle and the Resource Processor (customizer) have to be in the same Deployment Package.<p>
 * 
 * The Resource Processor that has this permission is allowed to access the bundle's 
 * private area by calling the {@link DeploymentSession#getDataFile} method during the session 
 * (see {@link DeploymentSession}). After the session ends the FilePermissions are withdrawn.
 * The Resource Processor will have <code>FilePermission</code> with "read", "write" and "delete" 
 * actions for the returned {@link java.io.File} that represents the the base directory of the 
 * persistent storage area and for its subdirectories.<p>
 * 
 * The actions string is converted to lowercase before processing.
 */
public class DeploymentCustomizerPermission extends Permission {
    
    /**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	/**
     * Constant String to the "privatearea" action.
     */
    public static final String PRIVATEAREA = "privatearea";

    private static final String      delegateProperty = "org.osgi.vendor.deploymentadmin";
    private static final Constructor constructor;
    private final        Permission  delegate;
    static {
        constructor = (Constructor) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                String pckg = System.getProperty(delegateProperty);
                if (null == pckg)
                    throw new RuntimeException("Property '" + delegateProperty + "' is not set");
                try {
                    Class c = Class.forName(pckg + ".DeploymentCustomizerPermission");
                    return c.getConstructor(new Class[] {String.class, String.class});    
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }});
    }

    /**
     * Creates a new <code>DeploymentCustomizerPermission</code> object for the given 
     * <code>name</code> and <code>action</code>.<p>
     * 
     * The name parameter is a filter string. This filter has the same syntax as an OSGi filter 
     * but only the "name" attribute is allowed. The value of the attribute  
     * is a Bundle Symbolic Name that represents a bundle. The only allowed action is the 
     * "privatearea" action. E.g.
     * 
     * <pre>
     * 		Permission perm = new DeploymentCustomizerPermission("(name=com.acme.bundle)", "privatearea");
     * </pre>
     * 
     * The Resource Processor that has this permission is allowed to access the bundle's 
     * private area by calling the {@link DeploymentSession#getDataFile} method. The 
     * Resource Processor will have <code>FilePermission</code> with "read", "write" and "delete" 
     * actions for the returned {@link java.io.File} and its subdirectories during the deployment 
     * session.
     * 
     * @param name Bundle Symbolic Name of the target bundle, must not be <code>null</code>.
     * @param actions action string (only the "privatearea" or "*" action is valid; "*" means all 
     *        the possible actions), must not be <code>null</code>.
     * @throws IllegalArgumentException if the filter is invalid, the list of actions 
     *         contains unknown operations or one of the parameters is <code>null</code>
     */
    public DeploymentCustomizerPermission(String name, String actions) {
        super(name);
		try {
			try {
	            delegate = (Permission) constructor.newInstance(new Object[] {name, actions});
			}
			catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}
		catch (Error e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
    }

    /**
     * Checks two DeploymentCustomizerPermission objects for equality. 
     * Two permission objects are equal if: <p>
     * 
     * <ul>
     * 		<li>their target filters are equal (semantically and not character by 
     *   	character) and</li>
     * 		<li>their actions are the same</li> 
     * </ul>
     * 
     * @param obj the reference object with which to compare.
     * @return true if the two objects are equal.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == this)
        	return true;
        if (!(obj instanceof DeploymentCustomizerPermission))
            return false;
        DeploymentCustomizerPermission dcp = (DeploymentCustomizerPermission) obj;
        return delegate.equals(dcp.delegate);
    }

    /**
     * Returns hash code for this permission object.
     * 
     * @return Hash code for this permission object.
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * Returns the String representation of the action list.
     * 
     * @return Action list of this permission instance. It is always "privatearea".
     * @see java.security.Permission#getActions()
     */
    public String getActions() {
        return delegate.getActions();
    }

    /**
     * Checks if this DeploymentCustomizerPermission would imply the parameter permission.
     * This permission implies another DeploymentCustomizerPermission permission if:
     * 
     * <ul>
     * 		<li>both of them has the "privatearea" action (other actions are not allowed) and</li>
     * 		<li>their filters (only name attribute is allowed in the filters) match similarly to 
     * 		{@link DeploymentAdminPermission}.</li>
     * </ul>
     * 
     * The value of the name attribute means Bundle Symbolic Name and not Deployment Package 
     * Symbolic Name here!<p>
     * 
     * @param permission Permission to check.
     * @return true if this DeploymentCustomizerPermission object implies the 
     * specified permission.
     * @see java.security.Permission#implies(java.security.Permission)
     */
    public boolean implies(Permission permission) {
        if (!(permission instanceof DeploymentCustomizerPermission))
    		return false;
    	        
        DeploymentCustomizerPermission dcp = (DeploymentCustomizerPermission) permission;
        
        return delegate.implies(dcp.delegate);
    }

    /**
     * Returns a new PermissionCollection object for storing DeploymentCustomizerPermission 
     * objects.
     *  
     * @return The new PermissionCollection.
     * @see java.security.Permission#newPermissionCollection()
     */
    public PermissionCollection newPermissionCollection() {
        return delegate.newPermissionCollection();
    }

}
