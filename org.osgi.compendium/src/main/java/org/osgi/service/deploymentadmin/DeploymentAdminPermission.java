/*
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

package org.osgi.service.deploymentadmin;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;

/**
 * DeploymentAdminPermission controls access to the Deployment Admin service.<p>
 * 
 * The permission uses a filter string formatted similarly to the {@link org.osgi.framework.Filter}. 
 * The filter determines the target of the permission. The <code>DeploymentAdminPermission</code> uses the 
 * <code>name</code> and the <code>signer</code> filter attributes only. The value of the <code>signer</code> 
 * attribute is matched against the signer chain (represented with its semicolon separated Distinguished Name chain) 
 * of the Deployment Package, and the value of the <code>name</code> attribute is matched against the value of the 
 * "DeploymentPackage-Name" manifest header of the Deployment Package. Example: 
 * 
 * <ul>
 * 		<li>(signer=cn = Bugs Bunny, o = ACME, c = US)</li>
 * 		<li>(name=org.osgi.ExampleApp)</li>
 * </ul>
 * 
 * Wildcards also can be used:<p>
 * 
 * <pre>
 * (signer=cn=*,o=ACME,c=*)  
 * </pre>
 * "cn" and "c" may have an arbitrary value
 * 
 * <pre>
 * (signer=*, o=ACME, c=US)  
 * </pre>
 * Only the value of "o" and "c" are significant
 * 
 * <pre>
 * (signer=* ; ou=S &amp; V, o=Tweety Inc., c=US)
 * </pre>
 * The first element of the certificate chain is 
 * not important, only the second (the 
 * Distingushed Name of the root certificate)
 * 
 * <pre>
 * (signer=- ; *, o=Tweety Inc., c=US)
 * </pre>
 * The same as the previous but '-' represents 
 * zero or more certificates, whereas the asterisk 
 * only represents a single certificate
 * 
 * <pre>
 * (name=*)                  
 * </pre>
 * The name of the Deployment Package doesn't matter
 * 
 * <pre>
 * (name=org.osgi.*)         
 * </pre>
 * The name has to begin with "org.osgi."
 * 
 * <p>The following actions are allowed:<p>
 * 
 * <b>list</b>
 * <p>
 * A holder of this permission can access the inventory information of the deployment
 * packages selected by the &lt;filter&gt; string. The filter selects the deployment packages
 * on which the holder of the permission can acquire detailed inventory information.
 * See {@link DeploymentAdmin#getDeploymentPackage(Bundle)}, 
 * {@link DeploymentAdmin#getDeploymentPackage(String)} and
 * {@link DeploymentAdmin#listDeploymentPackages}.<p>
 * 
 * <b>install</b><p>
 * 
 * A holder of this permission can install/update deployment packages if the deployment
 * package satisfies the &lt;filter&gt; string. See {@link DeploymentAdmin#installDeploymentPackage}.<p>
 * 
 * <b>uninstall</b><p>
 * 
 * A holder of this permission can uninstall deployment packages if the deployment
 * package satisfies the &lt;filter&gt; string. See {@link DeploymentPackage#uninstall}.<p>
 * 
 * <b>uninstall_forced</b><p>
 * 
 * A holder of this permission can forcefully uninstall deployment packages if the deployment
 * package satisfies the &lt;filter&gt; string. See {@link DeploymentPackage#uninstallForced}.<p>
 * 
 * <b>cancel</b><p>
 * 
 * A holder of this permission can cancel an active deployment action. This action being
 * cancelled could correspond to the install, update or uninstall of a deployment package
 * that satisfies the &lt;filter&gt; string. See {@link DeploymentAdmin#cancel}<p>
 * 
 * <b>metadata</b><p>
 * 
 * A holder of this permission is able to retrieve metadata information about a Deployment 
 * Package (e.g. is able to ask its manifest hedares). 
 * See {@link org.osgi.service.deploymentadmin.DeploymentPackage#getBundle(String)},
 * {@link org.osgi.service.deploymentadmin.DeploymentPackage#getBundleInfos()},
 * {@link org.osgi.service.deploymentadmin.DeploymentPackage#getHeader(String)}, 
 * {@link org.osgi.service.deploymentadmin.DeploymentPackage#getResourceHeader(String, String)},
 * {@link org.osgi.service.deploymentadmin.DeploymentPackage#getResourceProcessor(String)}, 
 * {@link org.osgi.service.deploymentadmin.DeploymentPackage#getResources()}<p>
 *
 * The actions string is converted to lowercase before processing.
 */
public final class DeploymentAdminPermission extends Permission {
    
    /**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	/**
     * Constant String to the "install" action.<p>
     * 
     * @see DeploymentAdmin#installDeploymentPackage(InputStream)
     */
    public static final String INSTALL            = "install";

    /**
     * Constant String to the "list" action.<p>
     * 
     * @see DeploymentAdmin#listDeploymentPackages()
     * @see DeploymentAdmin#getDeploymentPackage(String)
     * @see DeploymentAdmin#getDeploymentPackage(Bundle) 
     */
    public static final String LIST               = "list";
    
    /**
     * Constant String to the "uninstall" action.<p>
     * 
     * @see DeploymentPackage#uninstall()
     */
    public static final String UNINSTALL          = "uninstall";

    /**
     * Constant String to the "uninstall_forced" action.<p>
     * 
     * @see DeploymentPackage#uninstallForced()
     */
    public static final String UNINSTALL_FORCED   = "uninstall_forced";
    
    /**
     * Constant String to the "cancel" action.<p>
     * 
     * @see DeploymentAdmin#cancel
     */
    public static final String CANCEL             = "cancel";
    
    /**
     * Constant String to the "metadata" action.<p>
     * 
     * @see org.osgi.service.deploymentadmin.DeploymentPackage#getBundle(String)
     * @see org.osgi.service.deploymentadmin.DeploymentPackage#getBundleInfos()
     * @see org.osgi.service.deploymentadmin.DeploymentPackage#getHeader(String)
     * @see org.osgi.service.deploymentadmin.DeploymentPackage#getResourceHeader(String, String)
     * @see org.osgi.service.deploymentadmin.DeploymentPackage#getResourceProcessor(String)
     * @see org.osgi.service.deploymentadmin.DeploymentPackage#getResources()
     */
    public static final String METADATA           = "metadata";
    
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
                    Class c = Class.forName(pckg + ".DeploymentAdminPermission");
                    return c.getConstructor(new Class[] {String.class, String.class});    
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }});
    }
    
    /**
     * Creates a new <code>DeploymentAdminPermission</code> object for the given <code>name</code> and 
     * <code>action</code>.<p>
     * The <code>name</code> parameter identifies the target depolyment package the permission 
     * relates to. The <code>actions</code> parameter contains the comma separated list of allowed actions. 
     * 
     * @param name filter string, must not be null.
     * @param actions action string, must not be null. "*" means all the possible actions.
     * @throws IllegalArgumentException if the filter is invalid, the list of actions 
     *         contains unknown operations or one of the parameters is null
     */
    public DeploymentAdminPermission(String name, String actions) {
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
     * Checks two DeploymentAdminPermission objects for equality. 
     * Two permission objects are equal if: <p>
     * 
     * <ul>
     * 		<li>their target filters are semantically equal and</li>
     * 		<li>their actions are the same</li> 
     * </ul>
     * 
     * @param obj The reference object with which to compare.
     * @return true if the two objects are equal.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == this)
        	return true;
        if (!(obj instanceof DeploymentAdminPermission))
            return false;
        DeploymentAdminPermission dap = (DeploymentAdminPermission) obj;
        return delegate.equals(dap.delegate);
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
     * Returns the String representation of the action list.<p>
     * The method always gives back the actions in the following (alphabetical) order: 
     * <code>cancel, install, list, metadata, uninstall, uninstall_forced</code>
     * 
     * @return Action list of this permission instance. This is a comma-separated 
     *         list that reflects the action parameter of the constructor.
     * @see java.security.Permission#getActions()
     */
    public String getActions() {
        return delegate.getActions();
    }

    /**
     * Checks if this DeploymentAdminPermission would imply the parameter permission.<p>
     * Precondition of the implication is that the action set of this permission is the superset 
     * of the action set of the other permission. Further rules of implication are determined 
     * by the {@link org.osgi.framework.Filter} rules and the "OSGi Service Platform, Core 
     * Specification Release 4, Chapter Certificate Matching".<p>
     * 
     * The allowed attributes are: <code>name</code> (the symbolic name of the deployment 
     * package) and <code>signer</code> (the signer of the deployment package). In both cases 
     * wildcards can be used.<p>
     * 
     * Examples:
     * 
     * <pre>
     * 		1. DeploymentAdminPermission("(name=org.osgi.ExampleApp)", "list")
     * 		2. DeploymentAdminPermission("(name=org.osgi.ExampleApp)", "list, install")
     * 		3. DeploymentAdminPermission("(name=org.osgi.*)", "list")
     * 		4. DeploymentAdminPermission("(signer=*, o=ACME, c=US)", "list")
     * 		5. DeploymentAdminPermission("(signer=cn = Bugs Bunny, o = ACME, c = US)", "list")
     * </pre><p>
     * 
     * <pre>  
     * 		1. implies 1.
     * 		2. implies 1.
     * 		1. doesn't implies 2.
     * 		3. implies 1.
     * 		4. implies 5.
     * </pre>
     * 
     * @param permission Permission to check.
     * @return true if this DeploymentAdminPermission object implies the 
     * specified permission.
     * @see java.security.Permission#implies(java.security.Permission)
     * @see org.osgi.framework.Filter
     */
    public boolean implies(Permission permission) {
        if (!(permission instanceof DeploymentAdminPermission))
    		return false;
    	        
        DeploymentAdminPermission dap = (DeploymentAdminPermission) permission;
        
        return delegate.implies(dap.delegate);
    }

    /**
     * Returns a new PermissionCollection object for storing DeploymentAdminPermission 
     * objects. 
     * 
     * @return The new PermissionCollection.
     * @see java.security.Permission#newPermissionCollection()
     */
    public PermissionCollection newPermissionCollection() {
        return delegate.newPermissionCollection();
    }

}
