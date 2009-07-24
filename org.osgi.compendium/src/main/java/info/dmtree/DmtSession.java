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

import java.util.Date;

/**
 * DmtSession provides concurrent access to the DMT. All DMT manipulation
 * commands for management applications are available on the
 * <code>DmtSession</code> interface. The session is associated with a root node
 * which limits the subtree in which the operations can be executed within this
 * session.
 * <p>
 * Most of the operations take a node URI as parameter, which can be either an
 * absolute URI (starting with &quot;./&quot;) or a URI relative to the root
 * node of the session. The empty string as relative URI means the root URI the
 * session was opened with. All segments of a URI must be within the segment
 * length limit of the implementation, and the special characters '/' and '\'
 * must be escaped (preceded by a '\'). Any string can be converted to a valid
 * URI segment using the {@link Uri#mangle(String)} method.
 * <p>
 * If the URI specified does not correspond to a legitimate node in the tree an
 * exception is thrown. The only exception is the {@link #isNodeUri(String)}
 * method which returns <code>false</code> in case of an invalid URI.
 * <p>
 * Each method of <code>DmtSession</code> that accesses the tree in any way can
 * throw <code>DmtIllegalStateException</code> if the session has been closed or
 * invalidated (due to timeout, fatal exceptions, or unexpectedly unregistered
 * plugins).
 * 
 * @version $Revision: 5673 $
 */
public interface DmtSession {
    /**
     * Sessions created with <code>LOCK_TYPE_SHARED</code> lock allows
     * read-only access to the tree, but can be shared between multiple readers.
     */
    int LOCK_TYPE_SHARED = 0;

    /**
     * <code>LOCK_TYPE_EXCLUSIVE</code> lock guarantees full access to the
     * tree, but can not be shared with any other locks.
     */
    int LOCK_TYPE_EXCLUSIVE = 1;

    /**
     * <code>LOCK_TYPE_ATOMIC</code> is an exclusive lock with transactional
     * functionality. Commands of an atomic session will either fail or succeed
     * together, if a single command fails then the whole session will be rolled
     * back.
     */
    int LOCK_TYPE_ATOMIC = 2;

    /**
     * The session is open, all session operations are available.
     */
    int STATE_OPEN = 0;

    /**
     * The session is closed, DMT manipulation operations are not available,
     * they throw <code>DmtIllegalStateException</code> if tried.
     */
    int STATE_CLOSED = 1;

    /**
     * The session is invalid because a fatal error happened. Fatal errors
     * include the timeout of the session, any DmtException with the 'fatal'
     * flag set, or the case when a plugin service is unregistered while in use
     * by the session. DMT manipulation operations are not available, they throw
     * <code>DmtIllegalStateException</code> if tried.
     */
    int STATE_INVALID = 2;

    /**
     * Get the current state of this session.
     * 
     * @return the state of the session, one of {@link #STATE_OPEN},
     *         {@link #STATE_CLOSED} and {@link #STATE_INVALID}
     */
    int getState();

    /**
     * Gives the type of lock the session has.
     * 
     * @return the lock type of the session, one of {@link #LOCK_TYPE_SHARED},
     *         {@link #LOCK_TYPE_EXCLUSIVE} and {@link #LOCK_TYPE_ATOMIC}
     */
    int getLockType();

    /**
     * Gives the name of the principal on whose behalf the session was created.
     * Local sessions do not have an associated principal, in this case
     * <code>null</code> is returned.
     * 
     * @return the identifier of the remote server that initiated the session,
     *         or <code>null</code> for local sessions
     */
    String getPrincipal();

    /**
     * The unique identifier of the session. The ID is generated automatically,
     * and it is guaranteed to be unique on a machine.
     * 
     * @return the session identification number
     */
    int getSessionId();

    /**
     * Get the root URI associated with this session. Gives "<code>.</code>"
     * if the session was created without specifying a root, which means that
     * the target of this session is the whole DMT.
     * 
     * @return the root URI
     */
    String getRootUri();

    /**
     * Commits a series of DMT operations issued in the current atomic session
     * since the last transaction boundary. Transaction boundaries are the
     * creation of this object that starts the session, and all subsequent
     * {@link #commit} and {@link #rollback} calls.
     * <p>
     * This method can fail even if all operations were successful. This can
     * happen due to some multi-node semantic constraints defined by a specific
     * implementation. For example, node A can be required to always have
     * children A/B, A/C and A/D. If this condition is broken when
     * <code>commit()</code> is executed, the method will fail, and throw a
     * <code>METADATA_MISMATCH</code> exception.
     * <p>
     * An error situation can arise due to the lack of a two phase commit
     * mechanism in the underlying plugins. As an example, if plugin A has
     * committed successfully but plugin B failed, the whole session must fail,
     * but there is no way to undo the commit performed by A. To provide
     * predictable behaviour, the commit operation should continue with the
     * remaining plugins even after detecting a failure. All exceptions received
     * from failed commits are aggregated into one
     * <code>TRANSACTION_ERROR</code> exception thrown by this method.
     * <p>
     * In many cases the tree is not the only way to manage a given part of the
     * system. It may happen that while modifying some nodes in an atomic
     * session, the underlying settings are modified in parallel outside the
     * scope of the DMT. If this is detected during commit, an exception with
     * the code <code>CONCURRENT_ACCESS</code> is thrown.
     * 
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>METADATA_MISMATCH</code> if the operation failed
     *         because of meta-data restrictions
     *         <li><code>CONCURRENT_ACCESS</code> if it is detected that some
     *         modification has been made outside the scope of the DMT to the
     *         nodes affected in the session's operations
     *         <li><code>TRANSACTION_ERROR</code> if an error occurred during
     *         the commit of any of the underlying plugins
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was not opened using the
     *         <code>LOCK_TYPE_ATOMIC</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    void commit() throws DmtException;

    /**
     * Rolls back a series of DMT operations issued in the current atomic
     * session since the last transaction boundary. Transaction boundaries are
     * the creation of this object that starts the session, and all subsequent
     * {@link #commit} and {@link #rollback} calls.
     * 
     * @throws DmtException with the error code <code>ROLLBACK_FAILED</code>
     *         in case the rollback did not succeed
     * @throws DmtIllegalStateException if the session was not opened using the
     *         <code>LOCK_TYPE_ATOMIC</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    void rollback() throws DmtException;

    /**
     * Closes a session. If the session was opened with atomic lock mode, the
     * <code>DmtSession</code> must first persist the changes made to the DMT
     * by calling <code>commit()</code> on all (transactional) plugins
     * participating in the session. See the documentation of the
     * {@link #commit} method for details and possible errors during this
     * operation.
     * <p>
     * The state of the session changes to <code>DmtSession.STATE_CLOSED</code>
     * if the close operation completed successfully, otherwise it becomes
     * <code>DmtSession.STATE_INVALID</code>.
     * 
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>METADATA_MISMATCH</code> in case of atomic sessions,
     *         if the commit operation failed because of meta-data restrictions
     *         <li><code>CONCURRENT_ACCESS</code> in case of atomic sessions,
     *         if the commit operation failed because of some modification
     *         outside the scope of the DMT to the nodes affected in the session
     *         <li><code>TRANSACTION_ERROR</code> in case of atomic sessions,
     *         if an underlying plugin failed to commit
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if an underlying plugin failed
     *         to close, or if some unspecified error is encountered while
     *         attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    void close() throws DmtException;

    /**
     * Executes a node. This corresponds to the EXEC operation in OMA DM.  This
     * method cannot be called in a read-only session.
     * <p>
     * The semantics of an execute operation and the data parameter it takes
     * depends on the definition of the managed object on which the command is
     * issued.
     * 
     * @param nodeUri the node on which the execute operation is issued
     * @param data the parameter of the execute operation, can be
     *        <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if the node does not exist and
     *         the plugin does not allow executing unexisting nodes
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Execute</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if the node cannot be
     *         executed according to the meta-data (does not have
     *         <code>MetaNode.CMD_EXECUTE</code> access type)
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, if no DmtExecPlugin is associated with
     *         the node and the DmtAdmin can not execute the node, or if some
     *         unspecified error is encountered while attempting to complete the
     *         command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Exec action
     *         present
     * 
     * @see #execute(String, String, String)
     */
    void execute(String nodeUri, String data) throws DmtException;

    /**
     * Executes a node, also specifying a correlation ID for use in response
     * notifications. This operation corresponds to the EXEC command in OMA DM.
     * This method cannot be called in a read-only session.
     * <p>
     * The semantics of an execute operation and the data parameter it takes
     * depends on the definition of the managed object on which the command is
     * issued. If a correlation ID is specified, it should be used as the
     * <code>correlator</code> parameter for notifications sent in response to this
     * execute operation.
     * 
     * @param nodeUri the node on which the execute operation is issued
     * @param correlator an identifier to associate this operation with any
     *        notifications sent in response to it, can be <code>null</code> if not
     *        needed
     * @param data the parameter of the execute operation, can be
     *        <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if the node does not exist and
     *         the plugin does not allow executing unexisting nodes
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Execute</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if the node cannot be
     *         executed according to the meta-data (does not have
     *         <code>MetaNode.CMD_EXECUTE</code> access type)
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, if no DmtExecPlugin is associated with
     *         the node, or if some unspecified error is encountered while
     *         attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Exec action
     *         present
     * @see #execute(String, String)
     */
    void execute(String nodeUri, String correlator, String data)
            throws DmtException;

    /**
     * Get the Access Control List associated with a given node. The returned
     * <code>Acl</code> object does not take inheritance into account, it
     * gives the ACL specifically given to the node.
     * 
     * @param nodeUri the URI of the node
     * @return the Access Control List belonging to the node or
     *         <code>null</code> if none defined
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (the node does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException in case of local sessions, if the caller does
     *         not have <code>DmtPermission</code> for the node with the Get
     *         action present
     * @see #getEffectiveNodeAcl
     */
    Acl getNodeAcl(String nodeUri) throws DmtException;

    /**
     * Gives the Access Control List in effect for a given node. The returned
     * <code>Acl</code> takes inheritance into account, that is if there is no
     * ACL defined for the node, it will be derived from the closest ancestor
     * having an ACL defined.
     * 
     * @param nodeUri the URI of the node
     * @return the Access Control List belonging to the node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (the node does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException in case of local sessions, if the caller does
     *         not have <code>DmtPermission</code> for the node with the Get
     *         action present
     * @see #getNodeAcl
     */
    Acl getEffectiveNodeAcl(String nodeUri) throws DmtException;

    /**
     * Set the Access Control List associated with a given node. To perform this
     * operation, the caller needs to have replace rights (<code>Acl.REPLACE</code>
     * or the corresponding Java permission depending on the session type) as
     * described below:
     * <ul>
     * <li>if <code>nodeUri</code> specifies a leaf node, replace rights are
     * needed on the parent of the node
     * <li>if <code>nodeUri</code> specifies an interior node, replace rights
     * on either the node or its parent are sufficient
     * </ul>
     * <p>
     * If the given <code>acl</code> is <code>null</code> or an empty ACL
     * (not specifying any permissions for any principals), then the ACL of the 
     * node is deleted, and the node will inherit the ACL from its parent node.
     * 
     * @param nodeUri the URI of the node
     * @param acl the Access Control List to be set on the node, can be
     *        <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node or its parent
     *         (see above) does not allow the <code>Replace</code> operation
     *         for the associated principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the command attempts
     *         to set the ACL of the root node not to include Add rights for all
     *         principals
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException in case of local sessions, if the caller does
     *         not have <code>DmtPermission</code> for the node or its parent
     *         (see above) with the Replace action present
     */
    void setNodeAcl(String nodeUri, Acl acl) throws DmtException;

    /**
     * Create a copy of a node or a whole subtree. Beside the structure and
     * values of the nodes, most properties are also copied, with the exception
     * of the ACL (Access Control List), Timestamp and Version properties.
     * <p>
     * The copy method is essentially a convenience method that could be
     * substituted with a sequence of retrieval and update operations. This
     * determines the permissions required for copying. However, some
     * optimization can be possible if the source and target nodes are all
     * handled by DmtAdmin or by the same plugin. In this case, the handler
     * might be able to perform the underlying management operation more
     * efficiently: for example, a configuration table can be copied at once
     * instead of reading each node for each entry and creating it in the new
     * tree.
     * <p>
     * This method may result in any of the errors possible for the contributing
     * operations. Most of these are collected in the exception descriptions
     * below, but for the full list also consult the documentation of
     * {@link #getChildNodeNames(String)}, {@link #isLeafNode(String)},
     * {@link #getNodeValue(String)}, {@link #getNodeType(String)},
     * {@link #getNodeTitle(String)}, {@link #setNodeTitle(String, String)},
     * {@link #createLeafNode(String, DmtData, String)} and
     * {@link #createInteriorNode(String, String)}.
     * 
     * @param nodeUri the node or root of a subtree to be copied
     * @param newNodeUri the URI of the new node or root of a subtree
     * @param recursive <code>false</code> if only a single node is copied,
     *        <code>true</code> if the whole subtree is copied
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or
     *         <code>newNodeUri</code> or any segment of them is too long, or
     *         if they have too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> or
     *         <code>newNodeUri</code> is <code>null</code> or syntactically
     *         invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node, or if <code>newNodeUri</code>
     *         points to a node that cannot exist in the tree according to the
     *         meta-data (see {@link #getMetaNode(String)})
     *         <li><code>NODE_ALREADY_EXISTS</code> if
     *         <code>newNodeUri</code> points to a node that already exists
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the copied node(s)
     *         does not allow the <code>Get</code> operation, or the ACL of
     *         the parent of the target node does not allow the <code>Add</code>
     *         operation for the associated principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if <code>nodeUri</code>
     *         is an ancestor of <code>newNodeUri</code>, or if any of the
     *         implied retrieval or update operations are not allowed
     *         <li><code>METADATA_MISMATCH</code> if any of the meta-data
     *         constraints of the implied retrieval or update operations are
     *         violated
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if either URI is not within
     *         the current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the copied node(s) with the Get
     *         action present, or for the parent of the target node with the Add
     *         action
     */
    void copy(String nodeUri, String newNodeUri, boolean recursive)
            throws DmtException;

    /**
     * Create an interior node. If the parent node does not exist, it is created
     * automatically, as if this method were called for the parent URI. This way
     * all missing ancestor nodes leading to the specified node are created. Any
     * exceptions encountered while creating the ancestors are propagated to the
     * caller of this method, these are not explicitly listed in the error
     * descriptions below.
     * <p>
     * If meta-data is available for the node, several checks are made before
     * creating it. The node must have <code>MetaNode.CMD_ADD</code> access
     * type, it must be defined as a non-permanent interior node, the node name
     * must conform to the valid names, and the creation of the new node must
     * not cause the maximum occurrence number to be exceeded.
     * <p>
     * If the meta-data cannot be retrieved because the given node cannot
     * possibly exist in the tree (it is not defined in the specification), the
     * <code>NODE_NOT_FOUND</code> error code is returned (see
     * {@link #getMetaNode(String)}).
     * 
     * @param nodeUri the URI of the node to create
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a node that cannot exist in the tree (see above)
     *         <li><code>NODE_ALREADY_EXISTS</code> if <code>nodeUri</code>
     *         points to a node that already exists
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the parent node does
     *         not allow the <code>Add</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the parent node is not
     *         an interior node, or in non-atomic sessions if the underlying
     *         plugin is read-only or does not support non-atomic writing
     *         <li><code>METADATA_MISMATCH</code> if the node could not be
     *         created because of meta-data restrictions (see above)
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the parent node with the Add
     *         action present
     */
    void createInteriorNode(String nodeUri) throws DmtException;

    /**
     * Create an interior node with a given type. The type of interior node, if
     * specified, is a URI identifying a DDF document. If the parent node does
     * not exist, it is created automatically, as if
     * {@link #createInteriorNode(String)} were called for the parent URI. This
     * way all missing ancestor nodes leading to the specified node are created.
     * Any exceptions encountered while creating the ancestors are propagated to
     * the caller of this method, these are not explicitly listed in the error
     * descriptions below.
     * <p>
     * If meta-data is available for the node, several checks are made before
     * creating it. The node must have <code>MetaNode.CMD_ADD</code> access
     * type, it must be defined as a non-permanent interior node, the node name
     * must conform to the valid names, and the creation of the new node must
     * not cause the maximum occurrence number to be exceeded.
     * <p>
     * If the meta-data cannot be retrieved because the given node cannot
     * possibly exist in the tree (it is not defined in the specification), the
     * <code>NODE_NOT_FOUND</code> error code is returned (see
     * {@link #getMetaNode(String)}).
     * <p>
     * Interior node type identifiers must follow the format defined in section
     * 7.7.7.2 of the OMA Device Management Tree and Description document.
     * Checking the validity of the type string does not have to be done by the
     * DmtAdmin, this can be left to the plugin handling the node (if any), to
     * avoid unnecessary double-checks.
     * 
     * @param nodeUri the URI of the node to create
     * @param type the type URI of the interior node, can be <code>null</code>
     *        if no node type is defined
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a node that cannot exist in the tree (see above)
     *         <li><code>NODE_ALREADY_EXISTS</code> if <code>nodeUri</code>
     *         points to a node that already exists
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the parent node does
     *         not allow the <code>Add</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the parent node is not
     *         an interior node, or in non-atomic sessions if the underlying
     *         plugin is read-only or does not support non-atomic writing
     *         <li><code>METADATA_MISMATCH</code> if the node could not be
     *         created because of meta-data restrictions (see above)
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, if the type string is invalid (see
     *         above), or if some unspecified error is encountered while
     *         attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the parent node with the Add
     *         action present
     * @see #createInteriorNode(String)
     * @see <a
     *      href="http://member.openmobilealliance.org/ftp/public_documents/dm/Permanent_documents/OMA-TS-DM-TND-V1_2-20050615-C.zip">
     *      OMA Device Management Tree and Description v1.2 draft</a>
     */
    void createInteriorNode(String nodeUri, String type) throws DmtException;

    /**
     * Create a leaf node with default value and MIME type. If a node does not
     * have a default value or MIME type, this method will throw a
     * <code>DmtException</code> with error code
     * <code>METADATA_MISMATCH</code>. Note that a node might have a default
     * value or MIME type even if there is no meta-data for the node or its
     * meta-data does not specify the default.
     * <p>
     * If the parent node does not exist, it is created automatically, as if
     * {@link #createInteriorNode(String)} were called for the parent URI. This
     * way all missing ancestor nodes leading to the specified node are created.
     * Any exceptions encountered while creating the ancestors are propagated to
     * the caller of this method, these are not explicitly listed in the error
     * descriptions below.
     * <p>
     * If meta-data is available for a node, several checks are made before
     * creating it. The node must have <code>MetaNode.CMD_ADD</code> access
     * type, it must be defined as a non-permanent leaf node, the node name must
     * conform to the valid names, and the creation of the new node must not
     * cause the maximum occurrence number to be exceeded.
     * <p>
     * If the meta-data cannot be retrieved because the given node cannot
     * possibly exist in the tree (it is not defined in the specification), the
     * <code>NODE_NOT_FOUND</code> error code is returned (see
     * {@link #getMetaNode(String)}).
     * 
     * @param nodeUri the URI of the node to create
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a node that cannot exist in the tree (see above)
     *         <li><code>NODE_ALREADY_EXISTS</code> if <code>nodeUri</code>
     *         points to a node that already exists
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the parent node does
     *         not allow the <code>Add</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the parent node is not
     *         an interior node, or in non-atomic sessions if the underlying
     *         plugin is read-only or does not support non-atomic writing
     *         <li><code>METADATA_MISMATCH</code> if the node could not be
     *         created because of meta-data restrictions (see above)
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the parent node with the Add
     *         action present
     * @see #createLeafNode(String, DmtData)
     */
    void createLeafNode(String nodeUri) throws DmtException;

    /**
     * Create a leaf node with a given value and the default MIME type. If the
     * specified value is <code>null</code>, the default value is taken. If
     * the node does not have a default MIME type or value (if needed), this
     * method will throw a <code>DmtException</code> with error code
     * <code>METADATA_MISMATCH</code>. Note that a node might have a default
     * value or MIME type even if there is no meta-data for the node or its
     * meta-data does not specify the default.
     * <p>
     * If the parent node does not exist, it is created automatically, as if
     * {@link #createInteriorNode(String)} were called for the parent URI. This
     * way all missing ancestor nodes leading to the specified node are created.
     * Any exceptions encountered while creating the ancestors are propagated to
     * the caller of this method, these are not explicitly listed in the error
     * descriptions below.
     * <p>
     * If meta-data is available for a node, several checks are made before
     * creating it. The node must have <code>MetaNode.CMD_ADD</code> access
     * type, it must be defined as a non-permanent leaf node, the node name must
     * conform to the valid names, the node value must conform to the value
     * constraints, and the creation of the new node must not cause the maximum
     * occurrence number to be exceeded.
     * <p>
     * If the meta-data cannot be retrieved because the given node cannot
     * possibly exist in the tree (it is not defined in the specification), the
     * <code>NODE_NOT_FOUND</code> error code is returned (see
     * {@link #getMetaNode(String)}).
     * <p>
     * Nodes of <code>null</code> format can be created by using
     * {@link DmtData#NULL_VALUE} as second argument.
     * 
     * @param nodeUri the URI of the node to create
     * @param value the value to be given to the new node, can be
     *        <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a node that cannot exist in the tree (see above)
     *         <li><code>NODE_ALREADY_EXISTS</code> if <code>nodeUri</code>
     *         points to a node that already exists
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the parent node does
     *         not allow the <code>Add</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the parent node is not
     *         an interior node, or in non-atomic sessions if the underlying
     *         plugin is read-only or does not support non-atomic writing
     *         <li><code>METADATA_MISMATCH</code> if the node could not be
     *         created because of meta-data restrictions (see above)
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the parent node with the Add
     *         action present
     */
    void createLeafNode(String nodeUri, DmtData value) throws DmtException;

    /**
     * Create a leaf node with a given value and MIME type. If the specified
     * value or MIME type is <code>null</code>, their default values are
     * taken. If the node does not have the necessary defaults, this method will
     * throw a <code>DmtException</code> with error code
     * <code>METADATA_MISMATCH</code>. Note that a node might have a default
     * value or MIME type even if there is no meta-data for the node or its
     * meta-data does not specify the default.
     * <p>
     * If the parent node does not exist, it is created automatically, as if
     * {@link #createInteriorNode(String)} were called for the parent URI. This
     * way all missing ancestor nodes leading to the specified node are created.
     * Any exceptions encountered while creating the ancestors are propagated to
     * the caller of this method, these are not explicitly listed in the error
     * descriptions below.
     * <p>
     * If meta-data is available for a node, several checks are made before
     * creating it. The node must have <code>MetaNode.CMD_ADD</code> access
     * type, it must be defined as a non-permanent leaf node, the node name must
     * conform to the valid names, the node value must conform to the value
     * constraints, the MIME type must be among the listed types, and the
     * creation of the new node must not cause the maximum occurrence number to
     * be exceeded.
     * <p>
     * If the meta-data cannot be retrieved because the given node cannot
     * possibly exist in the tree (it is not defined in the specification), the
     * <code>NODE_NOT_FOUND</code> error code is returned (see
     * {@link #getMetaNode(String)}).
     * <p>
     * Nodes of <code>null</code> format can be created by using
     * {@link DmtData#NULL_VALUE} as second argument.
     * <p>
     * The MIME type string must conform to the definition in RFC 2045. Checking
     * its validity does not have to be done by the DmtAdmin, this can be left
     * to the plugin handling the node (if any), to avoid unnecessary
     * double-checks.
     * 
     * @param nodeUri the URI of the node to create
     * @param value the value to be given to the new node, can be
     *        <code>null</code>
     * @param mimeType the MIME type to be given to the new node, can be
     *        <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a node that cannot exist in the tree (see above)
     *         <li><code>NODE_ALREADY_EXISTS</code> if <code>nodeUri</code>
     *         points to a node that already exists
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the parent node does
     *         not allow the <code>Add</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the parent node is not
     *         an interior node, or in non-atomic sessions if the underlying
     *         plugin is read-only or does not support non-atomic writing
     *         <li><code>METADATA_MISMATCH</code> if the node could not be
     *         created because of meta-data restrictions (see above)
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, if <code>mimeType</code> is not a
     *         proper MIME type string (see above), or if some unspecified error
     *         is encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the parent node with the Add
     *         action present
     * @see #createLeafNode(String, DmtData)
     * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>
     */
    void createLeafNode(String nodeUri, DmtData value, String mimeType)
            throws DmtException;

    /**
     * Delete the given node. Deleting interior nodes is recursive, the whole
     * subtree under the given node is deleted.  It is not allowed to delete 
     * the root node of the session.
     * <p>
     * If meta-data is available for a node, several checks are made before
     * deleting it. The node must be non-permanent, it must have the
     * <code>MetaNode.CMD_DELETE</code> access type, and if zero occurrences
     * of the node are not allowed, it must not be the last one.
     * 
     * @param nodeUri the URI of the node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Delete</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the target node is the
     *         root of the session, or in non-atomic sessions if the underlying
     *         plugin is read-only or does not support non-atomic writing
     *         <li><code>METADATA_MISMATCH</code> if the node could not be
     *         deleted because of meta-data restrictions (see above)
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Delete action
     *         present
     */
    void deleteNode(String nodeUri) throws DmtException;

    /**
     * Rename a node. This operation only changes the name of the node (updating
     * the timestamp and version properties if they are supported), the value
     * and the other properties are not changed. The new name of the node must
     * be provided, the new URI is constructed from the base of the old URI and
     * the given name. It is not allowed to rename the root node of the session.
     * <p>
     * If available, the meta-data of the original and the new nodes are checked
     * before performing the rename operation. Neither node can be permanent,
     * their leaf/interior property must match, and the name change must not
     * violate any of the cardinality constraints. The original node must have
     * the <code>MetaNode.CMD_REPLACE</code> access type, and the name of the
     * new node must conform to the valid names.
     * 
     * @param nodeUri the URI of the node to rename
     * @param newName the new name property of the node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, if <code>nodeUri</code> has too many
     *         segments, or if <code>newName</code> is too long
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> or
     *         <code>newName</code> is <code>null</code> or syntactically
     *         invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node, or if the new node is not defined
     *         in the tree according to the meta-data (see
     *         {@link #getMetaNode(String)})
     *         <li><code>NODE_ALREADY_EXISTS</code> if there already exists a
     *         sibling of <code>nodeUri</code> with the name
     *         <code>newName</code>
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Replace</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the target node is the
     *         root of the session, or in non-atomic sessions if the underlying
     *         plugin is read-only or does not support non-atomic writing
     *         <li><code>METADATA_MISMATCH</code> if the node could not be
     *         renamed because of meta-data restrictions (see above)
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Replace action
     *         present
     */
    void renameNode(String nodeUri, String newName) throws DmtException;

    /**
     * Set the value of a leaf or interior node to its default.  The default
     * can be defined by the node's <code>MetaNode</code>. The method throws a 
     * <code>METADATA_MISMATCH</code> exception if the node does not have a 
     * default value.
     * 
     * @param nodeUri the URI of the node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Replace</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> in non-atomic sessions if 
     *         the underlying plugin is read-only or does not support non-atomic
     *         writing 
     *         <li><code>METADATA_MISMATCH</code> if the node is permanent or
     *         cannot be modified according to the meta-data (does not have the
     *         <code>MetaNode.CMD_REPLACE</code> access type), or if there is
     *         no default value defined for this node
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the specified node is
     *         an interior node and does not support Java object values
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Replace action
     *         present
     * @see #setNodeValue
     */
    void setDefaultNodeValue(String nodeUri) throws DmtException;

    /**
     * Set the value of a leaf or interior node. The format of the node is
     * contained in the <code>DmtData</code> object. For interior nodes, the
     * format must be <code>FORMAT_NODE</code>, while for leaf nodes this
     * format must not be used. 
     * <p>
     * If the specified value is <code>null</code>, the default value is taken. 
     * In this case, if the node does not have a default value, this method will
     * throw a <code>DmtException</code> with error code 
     * <code>METADATA_MISMATCH</code>. Nodes of <code>null</code> format can be 
     * set by using {@link DmtData#NULL_VALUE} as second argument.
     * <p>
     * An Event of type REPLACE is sent out for a leaf node. A replaced interior
     * node sends out events for each of its children in depth first order
     * and node names sorted with Arrays.sort(String[]).  When setting a value
     * on an interior node, the values of the leaf nodes under it can change,
     * but the structure of the subtree is not modified by the operation. 
     * 
     * @param nodeUri the URI of the node
     * @param data the data to be set, can be <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Replace</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the given data has
     *         <code>FORMAT_NODE</code> format but the node is a leaf node (or
     *         vice versa), or in non-atomic sessions if the underlying plugin
     *         is read-only or does not support non-atomic writing 
     *         <li><code>METADATA_MISMATCH</code> if the node is permanent or
     *         cannot be modified according to the meta-data (does not have the
     *         <code>MetaNode.CMD_REPLACE</code> access type), or if the given
     *         value does not conform to the meta-data value constraints
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the specified node is
     *         an interior node and does not support Java object values
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Replace action
     *         present
     */
    void setNodeValue(String nodeUri, DmtData data) throws DmtException;

    /**
     * Set the title property of a node. The length of the title string in UTF-8
     * encoding must not exceed 255 bytes.
     * 
     * @param nodeUri the URI of the node
     * @param title the title text of the node, can be <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Replace</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> in non-atomic sessions if
     *         the underlying plugin is read-only or does not support non-atomic
     *         writing
     *         <li><code>METADATA_MISMATCH</code> if the node cannot be
     *         modified according to the meta-data (does not have the
     *         <code>MetaNode.CMD_REPLACE</code> access type)
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Title property
     *         is not supported by the DmtAdmin implementation or the
     *         underlying plugin
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the title string is too
     *         long, if the URI is not within the current session's subtree, or
     *         if some unspecified error is encountered while attempting to
     *         complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Replace action
     *         present
     */
    void setNodeTitle(String nodeUri, String title) throws DmtException;

    /**
     * Set the type of a node. The type of leaf node is the MIME type of the
     * data it contains. The type of an interior node is a URI identifying a DDF
     * document.
     * <p>
     * For interior nodes, a <code>null</code> type string means that there is
     * no DDF document overriding the tree structure defined by the ancestors.
     * For leaf nodes, it requests that the default MIME type is used for the
     * given node. If the node does not have a default MIME type this method
     * will throw a <code>DmtException</code> with error code
     * <code>METADATA_MISMATCH</code>. Note that a node might have a default
     * MIME type even if there is no meta-data for the node or its meta-data
     * does not specify the default.
     * <p>
     * MIME types must conform to the definition in RFC 2045. Interior node type
     * identifiers must follow the format defined in section 7.7.7.2 of the OMA
     * Device Management Tree and Description document. Checking the validity of
     * the type string does not have to be done by the DmtAdmin, this can be
     * left to the plugin handling the node (if any), to avoid unnecessary
     * double-checks.
     * 
     * @param nodeUri the URI of the node
     * @param type the type of the node, can be <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Replace</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> in non-atomic sessions if
     *         the underlying plugin is read-only or does not support non-atomic
     *         writing
     *         <li><code>METADATA_MISMATCH</code> if the node is permanent or
     *         cannot be modified according to the meta-data (does not have the
     *         <code>MetaNode.CMD_REPLACE</code> access type), and in case of
     *         leaf nodes, if <code>null</code> is given and there is no
     *         default MIME type, or the given MIME type is not allowed
     *         <li><code>TRANSACTION_ERROR</code> in an atomic session if the
     *         underlying plugin is read-only or does not support atomic writing
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, if the type string is invalid (see
     *         above), or if some unspecified error is encountered while
     *         attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session was opened using the
     *         <code>LOCK_TYPE_SHARED</code> lock type, or if the session is
     *         already closed or invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Replace action
     *         present
     * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>
     * @see <a
     *      href="http://member.openmobilealliance.org/ftp/public_documents/dm/Permanent_documents/OMA-TS-DM-TND-V1_2-20050615-C.zip">
     *      OMA Device Management Tree and Description v1.2 draft</a>
     */
    void setNodeType(String nodeUri, String type) throws DmtException;

    /**
     * Get the list of children names of a node. The returned array contains the
     * names - not the URIs - of the immediate children nodes of the given node.
     * The returned child names are mangled ({@link Uri#mangle(String)}). The elements
     * are in no particular order. The returned array must not contain
     * <code>null</code> entries.
     * 
     * @param nodeUri the URI of the node
     * @return the list of child node names as a string array or an empty string
     *         array if the node has no children
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the specified node is
     *         not an interior node
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (it does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     */
    String[] getChildNodeNames(String nodeUri) throws DmtException;

    /**
     * Get the meta data which describes a given node. Meta data can only be
     * inspected, it can not be changed.
     * <p>
     * The <code>MetaNode</code> object returned to the client is the
     * combination of the meta data returned by the data plugin (if any) plus
     * the meta data returned by the DmtAdmin. If there are differences in the
     * meta data elements known by the plugin and the DmtAdmin then the plugin
     * specific elements take precedence.
     * <p>
     * Note, that a node does not have to exist for having meta-data associated
     * with it. This method may provide meta-data for any node that can possibly
     * exist in the tree (any node defined in the specification). For nodes that
     * are not defined, it may throw <code>DmtException</code> with the error
     * code <code>NODE_NOT_FOUND</code>. To allow easier implementation of
     * plugins that do not provide meta-data, it is allowed to return
     * <code>null</code> for any node, regardless of whether it is defined or
     * not.
     * 
     * @param nodeUri the URI of the node
     * @return a MetaNode which describes meta data information, can be
     *         <code>null</code> if there is no meta data available for the
     *         given node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a node that is not defined in the tree (see above)
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     */
    MetaNode getMetaNode(String nodeUri) throws DmtException;

    /**
     * Get the size of the data in a leaf node. The returned value depends on
     * the format of the data in the node, see the description of the
     * {@link DmtData#getSize()} method for the definition of node size for each
     * format.
     * 
     * @param nodeUri the URI of the leaf node
     * @return the size of the data in the node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>COMMAND_NOT_ALLOWED</code> if the specified node is
     *         not a leaf node
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (it does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Size property is
     *         not supported by the DmtAdmin implementation or the underlying
     *         plugin
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     * @see DmtData#getSize
     */
    int getNodeSize(String nodeUri) throws DmtException;

    /**
     * Get the timestamp when the node was created or last modified.
     * 
     * @param nodeUri the URI of the node
     * @return the timestamp of the last modification
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (it does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Timestamp
     *         property is not supported by the DmtAdmin implementation or the
     *         underlying plugin
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     */
    Date getNodeTimestamp(String nodeUri) throws DmtException;

    /**
     * Get the title of a node. There might be no title property set for a node.
     * 
     * @param nodeUri the URI of the node
     * @return the title of the node, or <code>null</code> if the node has no
     *         title
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (it does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Title property
     *         is not supported by the DmtAdmin implementation or the
     *         underlying plugin
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     */
    String getNodeTitle(String nodeUri) throws DmtException;

    /**
     * Get the type of a node. The type of leaf node is the MIME type of the
     * data it contains. The type of an interior node is a URI identifying a DDF
     * document; a <code>null</code> type means that there is no DDF document
     * overriding the tree structure defined by the ancestors.
     * 
     * @param nodeUri the URI of the node
     * @return the type of the node, can be <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (it does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     */
    String getNodeType(String nodeUri) throws DmtException;

    /**
     * Get the data contained in a leaf or interior node.  When retrieving the
     * value associated with an interior node, the caller must have rights to
     * read all nodes in the subtree under the given node.
     * 
     * @param nodeUri the URI of the node to retrieve
     * @return the data of the node, can not be <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node (and the ACLs
     *         of all its descendants in case of interior nodes) do not allow
     *         the <code>Get</code> operation for the associated principal
     *         <li><code>METADATA_MISMATCH</code> if the node value cannot be
     *         retrieved according to the meta-data (it does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the specified node is
     *         an interior node and does not support Java object values
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node (and all its descendants
     *         in case of interior nodes) with the Get action present
     */
    DmtData getNodeValue(String nodeUri) throws DmtException;

    /**
     * Get the version of a node. The version can not be set, it is calculated
     * automatically by the device. It is incremented modulo 0x10000 at every
     * modification of the value or any other property of the node, for both
     * leaf and interior nodes. When a node is created the initial value is 0.
     * 
     * @param nodeUri the URI of the node
     * @return the version of the node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (it does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Version property
     *         is not supported by the DmtAdmin implementation or the
     *         underlying plugin
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     */
    int getNodeVersion(String nodeUri) throws DmtException;

    /**
     * Tells whether a node is a leaf or an interior node of the DMT.
     * 
     * @param nodeUri the URI of the node
     * @return true if the given node is a leaf node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>URI_TOO_LONG</code> if <code>nodeUri</code> or a
     *         segment of it is too long, or if it has too many segments
     *         <li><code>INVALID_URI</code> if <code>nodeUri</code> is
     *         <code>null</code> or syntactically invalid
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a non-existing node
     *         <li><code>PERMISSION_DENIED</code> if the session is
     *         associated with a principal and the ACL of the node does not
     *         allow the <code>Get</code> operation for the associated
     *         principal
     *         <li><code>METADATA_MISMATCH</code> if node information cannot
     *         be retrieved according to the meta-data (it does not have
     *         <code>MetaNode.CMD_GET</code> access type)
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if the URI is not within the
     *         current session's subtree, or if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     */
    boolean isLeafNode(String nodeUri) throws DmtException;

    /**
     * Check whether the specified URI corresponds to a valid node in the DMT.
     * 
     * @param nodeUri the URI to check
     * @return true if the given node exists in the DMT
     * @throws DmtIllegalStateException if the session is already closed or
     *         invalidated
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation, or,
     *         in case of local sessions, if the caller does not have
     *         <code>DmtPermission</code> for the node with the Get action
     *         present
     */
    boolean isNodeUri(String nodeUri);
}
