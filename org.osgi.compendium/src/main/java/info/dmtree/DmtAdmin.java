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
package info.dmtree;

/**
 * An interface providing methods to open sessions and register listeners. The
 * implementation of <code>DmtAdmin</code> should register itself in the OSGi
 * service registry as a service. <code>DmtAdmin</code> is the entry point for
 * applications to use the DMT API.
 * <p>
 * The <code>getSession</code> methods are used to open a session on a specified
 * subtree of the DMT. A typical way of usage:
 * 
 * <pre>
 * serviceRef = context.getServiceReference(DmtAdmin.class.getName());
 * DmtAdmin admin = (DmtAdmin) context.getService(serviceRef);
 * DmtSession session = admin.getSession(&quot;./OSGi/Configuration&quot;);
 * session.createInteriorNode(&quot;./OSGi/Configuration/my.table&quot;);
 * </pre>
 * <p>
 * The methods for opening a session take a node URI (the session root) as a
 * parameter. All segments of the given URI must be within the segment length
 * limit of the implementation, and the special characters '/' and '\' must be
 * escaped (preceded by a '\'). Any string can be converted to a valid URI
 * segment using the {@link Uri#mangle(String)} method.
 * <p>
 * It is possible to specify a lock mode when opening the session (see lock type
 * constants in {@link DmtSession}). This determines whether the session can run
 * in parallel with other sessions, and the kinds of operations that can be
 * performed in the session. All Management Objects constituting the device
 * management tree must support read operations on their nodes, while support
 * for write operations depends on the Management Object. Management Objects
 * supporting write access may support transactional write, non-transactional
 * write or both. Users of <code>DmtAdmin</code> should consult the Management
 * Object specification and implementation for the supported update modes. If
 * Management Object definition permits, implementations are encouraged to
 * support both update modes.
 * <p>
 * This interface also contains methods for manipulating the set of
 * <code>DmtEventListener</code> objects that are called when the structure or
 * content of the tree is changed. These methods are not needed in an OSGi
 * environment, clients should register listeners through the Event Admin
 * service.
 * 
 * @version $Revision: 5673 $
 */
public interface DmtAdmin {
    /**
     * Opens a <code>DmtSession</code> for local usage on a given subtree of
     * the DMT with non transactional write lock. This call is equivalent to the
     * following:
     * <code>getSession(null, subtreeUri, DmtSession.LOCK_TYPE_EXCLUSIVE)</code>
     * <p>
     * The <code>subtreeUri</code> parameter must contain an absolute URI.  It
     * can also be <code>null</code>, in this case the session is opened with 
     * the default session root, &quot;.&quot;, that gives access to the whole 
     * tree.
     * <p>
     * To perform this operation the caller must have <code>DmtPermission</code>
     * for the <code>subtreeUri</code> node with the Get action present.
     * 
     * @param subtreeUri the subtree on which DMT manipulations can be performed
     *        within the returned session
     * @return a <code>DmtSession</code> object for the requested subtree
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>subtreeUri</code> or
     *         a segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>subtreeUri</code> is
     *         syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>subtreeUri</code>
     *         specifies a non-existing node
     *         <li><code>SESSION_CREATION_TIMEOUT</code> if the operation
     *         timed out because of another ongoing session
     *         <li><code>COMMAND_FAILED</code> if <code>subtreeUri</code>
     *         specifies a relative URI, or some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have 
     *         <code>DmtPermission</code> for the given root node with the Get 
     *         action present 
     */
    DmtSession getSession(String subtreeUri) throws DmtException;

    /**
     * Opens a <code>DmtSession</code> for local usage on a specific DMT
     * subtree with a given lock mode. This call is equivalent to the
     * following: <code>getSession(null, subtreeUri, lockMode)</code>
     * <p>
     * The <code>subtreeUri</code> parameter must contain an absolute URI.  It
     * can also be <code>null</code>, in this case the session is opened with 
     * the default session root, &quot;.&quot;, that gives access to the whole 
     * tree.
     * <p>
     * To perform this operation the caller must have <code>DmtPermission</code>
     * for the <code>subtreeUri</code> node with the Get action present.
     * 
     * @param subtreeUri the subtree on which DMT manipulations can be performed
     *        within the returned session
     * @param lockMode one of the lock modes specified in
     *        <code>DmtSession</code>
     * @return a <code>DmtSession</code> object for the requested subtree
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>subtreeUri</code> or
     *         a segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>subtreeUri</code> is
     *         syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>subtreeUri</code>
     *         specifies a non-existing node
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if atomic sessions are
     *         not supported by the implementation and <code>lockMode</code> 
     *         requests an atomic session
     *         <li><code>SESSION_CREATION_TIMEOUT</code> if the operation 
     *         timed out because of  another ongoing session
     *         <li><code>COMMAND_FAILED</code> if <code>subtreeUri</code>
     *         specifies a relative URI, if <code>lockMode</code> is unknown,
     *         or some unspecified error is encountered while attempting to 
     *         complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have 
     *         <code>DmtPermission</code> for the given root node with the Get 
     *         action present 
     */
    DmtSession getSession(String subtreeUri, int lockMode) throws DmtException;

    /**
     * Opens a <code>DmtSession</code> on a specific DMT subtree using a
     * specific lock mode on behalf of a remote principal. If local management
     * applications are using this method then they should provide
     * <code>null</code> as the first parameter. Alternatively they can use
     * other forms of this method without providing a principal string. 
     * <p>
     * The <code>subtreeUri</code> parameter must contain an absolute URI.  It
     * can also be <code>null</code>, in this case the session is opened with 
     * the default session root, &quot;.&quot;, that gives access to the whole 
     * tree.  
     * <p>
     * This method is guarded by <code>DmtPrincipalPermission</code> in case of
     * remote sessions.  In addition, the caller must have Get access rights 
     * (ACL in case of remote sessions, <code>DmtPermission</code> in case of
     * local sessions) on the <code>subtreeUri</code> node to perform this
     * operation. 
     * 
     * @param principal the identifier of the remote server on whose behalf the
     *        data manipulation is performed, or <code>null</code> for local
     *        sessions
     * @param subtreeUri the subtree on which DMT manipulations can be performed
     *        within the returned session
     * @param lockMode one of the lock modes specified in
     *        <code>DmtSession</code>
     * @return a <code>DmtSession</code> object for the requested subtree
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>subtreeUri</code> or
     *         a segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>subtreeUri</code> is
     *         syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>subtreeUri</code>
     *         specifies a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if <code>principal</code> is
     *         not <code>null</code> and the ACL of the node does not allow the
     *         <code>Get</code> operation for the principal on the given root 
     *         node 
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if atomic sessions are
     *         not supported by the implementation and <code>lockMode</code> 
     *         requests an atomic session
     *         <li><code>SESSION_CREATION_TIMEOUT</code> if the operation
     *         timed out because of  another ongoing session
     *         <li><code>COMMAND_FAILED</code> if <code>subtreeUri</code>
     *         specifies a relative URI, if <code>lockMode</code> is unknown,
     *         or some unspecified error is encountered while attempting to 
     *         complete the command
     *         </ul>
     * @throws SecurityException in case of remote sessions, if the caller does 
     *         not have the required <code>DmtPrincipalPermission</code> with a 
     *         target matching the <code>principal</code> parameter, or in case
     *         of local sessions, if the caller does not have 
     *         <code>DmtPermission</code> for the given root node with the Get 
     *         action present 
     */
    DmtSession getSession(String principal, String subtreeUri, int lockMode)
            throws DmtException;

    /**
     * Registers an event listener on behalf of a local application. The given
     * listener will receive notification on all changes affecting the specified
     * subtree. The subtree is specified by its root node URI. An event is
     * delivered to the registered listener if at least one affected node is
     * within this subtree. The events can also be filtered by specifying a
     * bitmask of relevant event types (e.g.
     * <code>DmtEvent.ADDED | DmtEvent.REPLACED | DmtEvent.SESSION_CLOSED</code>).
     * Only event types included in the bitmask will be delivered to the
     * listener.
     * <p>
     * The listener will only receive the change notifications of nodes for
     * which the registering application has the appropriate GET
     * {@link info.dmtree.security.DmtPermission}.
     * <p>
     * If the specified <code>listener</code> was already registered, calling
     * this method will update the registration.
     * 
     * @param type a bitmask of event types the caller is interested in
     * @param uri the URI of the root node of a subtree, must not be
     *        <code>null</code>
     * @param listener the listener to be registered, must not be
     *        <code>null</code>
     * @throws SecurityException if the caller doesn't have the necessary GET
     *         <code>DmtPermission</code> for the given URI
     * @throws NullPointerException if the <code>uri</code> or
     *         <code>listener</code> parameter is <code>null</code>
     * @throws IllegalArgumentException if the <code>type</code> parameter
     *         contains invalid bits (not corresponding to any event type
     *         defined in <code>DmtEvent</code>), or if the <code>uri</code>
     *         parameter is invalid (is not an absolute URI or is syntactically
     *         incorrect)
     */
    void addEventListener(int type, String uri, DmtEventListener listener);

    /**
     * Registers an event listener on behalf of a remote principal. The given
     * listener will receive notification on all changes affecting the specified
     * subtree. The subtree is specified by its root node URI. An event is
     * delivered to the registered listener if at least one affected node is
     * within this subtree. The events can also be filtered by specifying a
     * bitmask of relevant event types (e.g.
     * <code>DmtEvent.ADDED | DmtEvent.REPLACED | DmtEvent.SESSION_CLOSED</code>).
     * Only event types included in the bitmask will be delivered to the
     * listener.
     * <p>
     * The listener will only receive the change notifications of nodes for
     * which the node ACL grants GET access to the specified principal.
     * <p>
     * If the specified <code>listener</code> was already registered, calling
     * this method will update the registration.
     * 
     * @param principal the management server identity the caller is acting on
     *        behalf of, must not be <code>null</code>
     * @param type a bitmask of event types the caller is interested in
     * @param uri the URI of the root node of a subtree, must not be
     *        <code>null</code>
     * @param listener the listener to be registered, must not be
     *        <code>null</code>
     * @throws SecurityException if the caller doesn't have the necessary
     *         <code>DmtPrincipalPermission</code> to use the specified
     *         principal
     * @throws NullPointerException if the <code>principal</code>,
     *         <code>uri</code> or <code>listener</code> parameter is 
     *         <code>null</code>
     * @throws IllegalArgumentException if the <code>type</code> parameter
     *         contains invalid bits (not corresponding to any event type
     *         defined in <code>DmtEvent</code>), or if the <code>uri</code>
     *         parameter is invalid (is not an absolute URI or is syntactically
     *         incorrect)
     */
    void addEventListener(String principal, int type, String uri,
            DmtEventListener listener);

    /**
     * Remove a previously registered listener. After this call, the listener
     * will not receive change notifications.
     * 
     * @param listener the listener to be unregistered, must not be
     *        <code>null</code>
     * @throws NullPointerException if the <code>listener</code> parameter is
     *         <code>null</code>
     */
    void removeEventListener(DmtEventListener listener);
}
