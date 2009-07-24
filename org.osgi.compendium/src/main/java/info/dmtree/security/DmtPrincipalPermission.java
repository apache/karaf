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

package info.dmtree.security;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Indicates the callers authority to create DMT sessions on behalf of a remote
 * management server. Only protocol adapters communicating with management
 * servers should be granted this permission.
 * <p>
 * <code>DmtPrincipalPermission</code> has a target string which controls the
 * name of the principal on whose behalf the protocol adapter can act. A
 * wildcard is allowed at the end of the target string, to allow using any
 * principal name with the given prefix. The &quot;*&quot; target means the
 * adapter can create a session in the name of any principal.
 * 
 * @version $Revision: 5673 $
 */
public class DmtPrincipalPermission extends Permission {
    private static final long serialVersionUID = 6388752177325038332L;

    // specifies whether the target string had a wildcard at the end
    private final boolean isPrefix;

    // the target string without the wildcard (if there was one)
    private final String principal;

    /**
     * Creates a new <code>DmtPrincipalPermission</code> object with its name
     * set to the target string. Name must be non-null and non-empty.
     * 
     * @param target the name of the principal, can end with <code>*</code> to
     *        match any principal with the given prefix
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>name</code> is empty
     */
    public DmtPrincipalPermission(String target) {
        super(target);

        if (target == null)
            throw new NullPointerException(
                    "'target' parameter must not be null.");

        if (target.equals(""))
            throw new IllegalArgumentException(
                    "'target' parameter must not be empty.");

        isPrefix = target.endsWith("*");
        if (isPrefix)
            principal = target.substring(0, target.length() - 1);
        else
            principal = target;
    }

    /**
     * Creates a new <code>DmtPrincipalPermission</code> object using the
     * 'canonical' two argument constructor. In this version this class does not
     * define any actions, the second argument of this constructor must be "*"
     * so that this class can later be extended in a backward compatible way.
     * 
     * @param target the name of the principal, can end with <code>*</code> to
     *        match any principal with the given prefix
     * @param actions no actions defined, must be "*" for forward compatibility
     * @throws NullPointerException if <code>name</code> or
     *         <code>actions</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>name</code> is empty or
     *         <code>actions</code> is not "*"
     */
    public DmtPrincipalPermission(String target, String actions) {
        this(target);

        if (actions == null)
            throw new NullPointerException(
                    "'actions' parameter must not be null.");

        if (!actions.equals("*"))
            throw new IllegalArgumentException(
                    "'actions' parameter must be \"*\".");
    }

    /**
     * Checks whether the given object is equal to this DmtPrincipalPermission
     * instance. Two DmtPrincipalPermission instances are equal if they have the
     * same target string.
     * 
     * @param obj the object to compare to this DmtPrincipalPermission instance
     * @return <code>true</code> if the parameter represents the same
     *         permissions as this instance
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof DmtPrincipalPermission))
            return false;

        DmtPrincipalPermission other = (DmtPrincipalPermission) obj;

        return isPrefix == other.isPrefix && principal.equals(other.principal);
    }

    /**
     * Returns the action list (always <code>*</code> in the current version).
     * 
     * @return the action string &quot;*&quot;
     */
    public String getActions() {
        return "*";
    }

    /**
     * Returns the hash code for this permission object. If two
     * DmtPrincipalPermission objects are equal according to the {@link #equals}
     * method, then calling this method on each of the two
     * DmtPrincipalPermission objects must produce the same integer result.
     * 
     * @return hash code for this permission object
     */
    public int hashCode() {
        return new Boolean(isPrefix).hashCode() ^ principal.hashCode();
    }

    /**
     * Checks if this DmtPrincipalPermission object implies the specified
     * permission. Another DmtPrincipalPermission instance is implied by this
     * permission either if the target strings are identical, or if this target
     * can be made identical to the other target by replacing a trailing
     * &quot;*&quot; with any string.
     * 
     * @param p the permission to check for implication
     * @return true if this DmtPrincipalPermission instance implies the
     *         specified permission
     */
    public boolean implies(Permission p) {
        if (!(p instanceof DmtPrincipalPermission))
            return false;

        DmtPrincipalPermission other = (DmtPrincipalPermission) p;

        return impliesPrincipal(other);
    }

    /**
     * Returns a new PermissionCollection object for storing
     * DmtPrincipalPermission objects.
     * 
     * @return the new PermissionCollection
     */
    public PermissionCollection newPermissionCollection() {
        return new DmtPrincipalPermissionCollection();
    }

    /*
     * Returns true if the principal parameter of the given
     * DmtPrincipalPermission is implied by the principal of this permission,
     * i.e. this principal is a prefix of the other principal but ends with a *,
     * or the two principal strings are equal.
     */
    boolean impliesPrincipal(DmtPrincipalPermission p) {
        return isPrefix ? p.principal.startsWith(principal) : !p.isPrefix
                && p.principal.equals(principal);
    }
}

/**
 * Represents a homogeneous collection of DmtPrincipalPermission objects.
 */
final class DmtPrincipalPermissionCollection extends PermissionCollection {
    private static final long serialVersionUID = -6692103535775802684L;

    private ArrayList perms;

    /**
     * Create an empty DmtPrincipalPermissionCollection object.
     */
    public DmtPrincipalPermissionCollection() {
        perms = new ArrayList();
    }

    /**
     * Adds a permission to the DmtPrincipalPermissionCollection.
     * 
     * @param permission the Permission object to add
     * @exception IllegalArgumentException if the permission is not a
     *            DmtPrincipalPermission
     * @exception SecurityException if this DmtPrincipalPermissionCollection
     *            object has been marked readonly
     */
    public void add(Permission permission) {
        if (!(permission instanceof DmtPrincipalPermission))
            throw new IllegalArgumentException(
                    "Cannot add permission, invalid permission type: "
                            + permission);
        if (isReadOnly())
            throw new SecurityException(
                    "Cannot add permission, collection is marked read-only.");

        // only add new permission if it is not already implied by the
        // permissions in the collection
        if (!implies(permission)) {
            // remove all permissions that are implied by the new one
            Iterator i = perms.iterator();
            while (i.hasNext())
                if (permission.implies((DmtPrincipalPermission) i.next()))
                    i.remove();

            // no need to synchronize because all adds are done sequentially
            // before any implies() calls
            perms.add(permission);

        }
    }

    /**
     * Check whether this set of permissions implies the permission specified in
     * the parameter.
     * 
     * @param permission the Permission object to compare
     * @return true if the parameter permission is a proper subset of the
     *         permissions in the collection, false otherwise
     */
    public boolean implies(Permission permission) {
        if (!(permission instanceof DmtPrincipalPermission))
            return false;

        DmtPrincipalPermission other = (DmtPrincipalPermission) permission;

        Iterator i = perms.iterator();
        while (i.hasNext())
            if (((DmtPrincipalPermission) i.next()).impliesPrincipal(other))
                return true;

        return false;
    }

    /**
     * Returns an enumeration of all the DmtPrincipalPermission objects in the
     * container. The returned value cannot be <code>null</code>.
     * 
     * @return an enumeration of all the DmtPrincipalPermission objects
     */
    public Enumeration elements() {
        // Convert Iterator into Enumeration
        return Collections.enumeration(perms);
    }
}
