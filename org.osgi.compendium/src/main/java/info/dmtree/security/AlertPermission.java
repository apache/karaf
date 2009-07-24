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
 * Indicates the callers authority to send alerts to management servers,
 * identified by their principal names.
 * <p>
 * <code>AlertPermission</code> has a target string which controls the principal
 * names where alerts can be sent. A wildcard is allowed at the end of the
 * target string, to allow sending alerts to any principal with a name matching
 * the given prefix. The &quot;*&quot; target means that alerts can be sent to
 * any destination.
 * 
 * @version $Revision: 5673 $
 */
public class AlertPermission extends Permission {
    private static final long serialVersionUID = -3206463101788245739L;

    // specifies whether the target string had a wildcard at the end
    private final boolean isPrefix;

    // the target string without the wildcard (if there was one)
    private final String serverId;

    /**
     * Creates a new <code>AlertPermission</code> object with its name set to
     * the target string. Name must be non-null and non-empty.
     * 
     * @param target the name of a principal, can end with <code>*</code> to
     *        match any principal identifier with the given prefix
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>name</code> is empty
     */
    public AlertPermission(String target) {
        super(target);

        if (target == null)
            throw new NullPointerException(
                    "'target' parameter must not be null.");

        if (target.equals(""))
            throw new IllegalArgumentException(
                    "'target' parameter must not be empty.");

        isPrefix = target.endsWith("*");
        if (isPrefix)
            serverId = target.substring(0, target.length() - 1);
        else
            serverId = target;
    }

    /**
     * Creates a new <code>AlertPermission</code> object using the 'canonical'
     * two argument constructor. In this version this class does not define any
     * actions, the second argument of this constructor must be "*" so that this
     * class can later be extended in a backward compatible way.
     * 
     * @param target the name of the server, can end with <code>*</code> to
     *        match any server identifier with the given prefix
     * @param actions no actions defined, must be "*" for forward compatibility
     * @throws NullPointerException if <code>name</code> or
     *         <code>actions</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>name</code> is empty or
     *         <code>actions</code> is not "*"
     */
    public AlertPermission(String target, String actions) {
        this(target);

        if (actions == null)
            throw new NullPointerException(
                    "'actions' parameter must not be null.");

        if (!actions.equals("*"))
            throw new IllegalArgumentException(
                    "'actions' parameter must be \"*\".");
    }

    /**
     * Checks whether the given object is equal to this AlertPermission
     * instance. Two AlertPermission instances are equal if they have the same
     * target string.
     * 
     * @param obj the object to compare to this AlertPermission instance
     * @return <code>true</code> if the parameter represents the same
     *         permissions as this instance
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof AlertPermission))
            return false;

        AlertPermission other = (AlertPermission) obj;

        return isPrefix == other.isPrefix && serverId.equals(other.serverId);
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
     * Returns the hash code for this permission object. If two AlertPermission
     * objects are equal according to the {@link #equals} method, then calling
     * this method on each of the two AlertPermission objects must produce the
     * same integer result.
     * 
     * @return hash code for this permission object
     */
    public int hashCode() {
        return new Boolean(isPrefix).hashCode() ^ serverId.hashCode();
    }

    /**
     * Checks if this AlertPermission object implies the specified permission.
     * Another AlertPermission instance is implied by this permission either if
     * the target strings are identical, or if this target can be made identical
     * to the other target by replacing a trailing &quot;*&quot; with any
     * string.
     * 
     * @param p the permission to check for implication
     * @return true if this AlertPermission instance implies the specified
     *         permission
     */
    public boolean implies(Permission p) {
        if (!(p instanceof AlertPermission))
            return false;

        AlertPermission other = (AlertPermission) p;

        return impliesServer(other);
    }

    /**
     * Returns a new PermissionCollection object for storing AlertPermission
     * objects.
     * 
     * @return the new PermissionCollection
     */
    public PermissionCollection newPermissionCollection() {
        return new DmtAlertPermissionCollection();
    }

    /*
     * Returns true if the server name parameter of the given AlertPermission is
     * implied by the server name of this permission, i.e. this server name is a
     * prefix of the other one but ends with a *, or the two server names are
     * equal.
     */
    boolean impliesServer(AlertPermission p) {
        return isPrefix ? p.serverId.startsWith(serverId) : !p.isPrefix
                && p.serverId.equals(serverId);
    }
}

/**
 * Represents a homogeneous collection of AlertPermission objects.
 */
final class DmtAlertPermissionCollection extends PermissionCollection {
    private static final long serialVersionUID = 2288462124510043429L;

    private ArrayList perms;

    /**
     * Create an empty DmtAlertPermissionCollection object.
     */
    public DmtAlertPermissionCollection() {
        perms = new ArrayList();
    }

    /**
     * Adds a permission to the DmtAlertPermissionCollection.
     * 
     * @param permission the Permission object to add
     * @exception IllegalArgumentException if the permission is not a
     *            AlertPermission
     * @exception SecurityException if this DmtAlertPermissionCollection object
     *            has been marked readonly
     */
    public void add(Permission permission) {
        if (!(permission instanceof AlertPermission))
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
                if (permission.implies((AlertPermission) i.next()))
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
        if (!(permission instanceof AlertPermission))
            return false;

        AlertPermission other = (AlertPermission) permission;

        Iterator i = perms.iterator();
        while (i.hasNext())
            if (((AlertPermission) i.next()).impliesServer(other))
                return true;

        return false;
    }

    /**
     * Returns an enumeration of all the AlertPermission objects in the
     * container. The returned value cannot be <code>null</code>.
     * 
     * @return an enumeration of all the AlertPermission objects
     */
    public Enumeration elements() {
        // Convert Iterator into Enumeration
        return Collections.enumeration(perms);
    }
}
