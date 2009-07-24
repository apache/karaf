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
package info.dmtree.spi;

import info.dmtree.DmtData;
import info.dmtree.DmtException;
import info.dmtree.MetaNode;

import java.util.Date;

/**
 * Provides read-only access to the part of the tree handled by the plugin that
 * created this session.
 * <p>
 * Since the {@link ReadWriteDataSession} and {@link TransactionalDataSession}
 * interfaces inherit from this interface, some of the method descriptions do
 * not apply for an instance that is only a <code>ReadableDataSession</code>.
 * For example, the {@link #close} method description also contains information
 * about its behaviour when invoked as part of a transactional session.
 * <p>
 * The <code>nodePath</code> parameters appearing in this interface always
 * contain an array of path segments identifying a node in the subtree of this
 * plugin. This parameter contains an absolute path, so the first segment is
 * always &quot;.&quot;. Special characters appear escaped in the segments.
 * <p>
 * <strong>Error handling</strong>
 * <p>
 * When a tree access command is called on the DmtAdmin service, it must perform
 * an extensive set of checks on the parameters and the authority of the caller
 * before delegating the call to a plugin. Therefore plugins can take certain
 * circumstances for granted: that the path is valid and is within the subtree
 * of the plugin and the session, the command can be applied to the given node
 * (e.g. the target of <code>getChildNodeNames</code> is an interior node), etc.
 * All errors described by the error codes {@link DmtException#INVALID_URI},
 * {@link DmtException#URI_TOO_LONG}, {@link DmtException#PERMISSION_DENIED},
 * {@link DmtException#COMMAND_NOT_ALLOWED} and
 * {@link DmtException#TRANSACTION_ERROR} are fully filtered out before control
 * reaches the plugin.
 * <p>
 * If the plugin provides meta-data for a node, the DmtAdmin service must also
 * check the constraints specified by it, as described in {@link MetaNode}. If
 * the plugin does not provide meta-data, it must perform the necessary checks
 * for itself and use the {@link DmtException#METADATA_MISMATCH} error code to
 * indicate such discrepancies.
 * <p>
 * The DmtAdmin also ensures that the targeted nodes exist before calling the
 * plugin (except, of course, before the <code>isNodeUri</code> call). However,
 * some small amount of time elapses between the check and the call, so in case
 * of plugins where the node structure can change independantly from the DMT,
 * the target node might disappear in that time. For example, a whole subtree
 * can disappear when a Monitorable application is unregistered, which might
 * happen in the middle of a DMT session accessing it. Plugins managing such
 * nodes always need to check whether they still exist and throw
 * {@link DmtException#NODE_NOT_FOUND} as necessary, but for more static
 * subtrees there is no need for the plugin to use this error code.
 * <p>
 * The plugin can use the remaining error codes as needed. If an error does not
 * fit into any other category, the {@link DmtException#COMMAND_FAILED} code
 * should be used.
 * 
 * @version $Revision: 5673 $
 */
public interface ReadableDataSession {
    /**
     * Notifies the plugin that the given node has changed outside the scope of
     * the plugin, therefore the Version and Timestamp properties must be
     * updated (if supported). This method is needed because the ACL property of
     * a node is managed by the DmtAdmin instead of the plugin. The DmtAdmin
     * must call this method whenever the ACL property of a node changes.
     * 
     * @param nodePath the absolute path of the node that has changed
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     */
    void nodeChanged(String[] nodePath) throws DmtException;

    /**
     * Closes a session. This method is always called when the session ends for
     * any reason: if the session is closed, if a fatal error occurs in any
     * method, or if any error occurs during commit or rollback. In case the
     * session was invalidated due to an exception during commit or rollback, it
     * is guaranteed that no methods are called on the plugin until it is
     * closed. In case the session was invalidated due to a fatal exception in
     * one of the tree manipulation methods, only the rollback method is called
     * before this (and only in atomic sessions).
     * <p>
     * This method should not perform any data manipulation, only cleanup
     * operations. In non-atomic read-write sessions the data manipulation
     * should be done instantly during each tree operation, while in atomic
     * sessions the <code>DmtAdmin</code> always calls
     * {@link TransactionalDataSession#commit} automatically before the session
     * is actually closed.
     * 
     * @throws DmtException with the error code <code>COMMAND_FAILED</code> if
     *         the plugin failed to close for any reason
     */
    void close() throws DmtException;

    /**
     * Get the list of children names of a node. The returned array contains the
     * names - not the URIs - of the immediate children nodes of the given node.
     * The returned child names must be mangled ({@link info.dmtree.Uri#mangle(String)}).
     * The returned array may contain <code>null</code> entries, but these are
     * removed by the DmtAdmin before returning it to the client.
     * 
     * @param nodePath the absolute path of the node
     * @return the list of child node names as a string array or an empty string
     *         array if the node has no children
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>METADATA_MISMATCH</code> if the information could
     *         not be retrieved because of meta-data restrictions
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    String[] getChildNodeNames(String[] nodePath) throws DmtException;

    /**
     * Get the meta data which describes a given node. Meta data can be only
     * inspected, it can not be changed.
     * <p>
     * Meta data support by plugins is an optional feature. It can be used, for
     * example, when a data plugin is implemented on top of a data store or
     * another API that has their own metadata, such as a relational database,
     * in order to avoid metadata duplication and inconsistency. The meta data
     * specific to the plugin returned by this method is complemented by meta
     * data from the DmtAdmin before returning it to the client. If there are
     * differences in the meta data elements known by the plugin and the
     * <code>DmtAdmin</code> then the plugin specific elements take
     * precedence.
     * <p>
     * Note, that a node does not have to exist for having meta-data associated
     * with it. This method may provide meta-data for any node that can possibly
     * exist in the tree (any node defined by the Management Object provided by
     * the plugin). For nodes that are not defined, a <code>DmtException</code>
     * may be thrown with the <code>NODE_NOT_FOUND</code> error code. To allow
     * easier implementation of plugins that do not provide meta-data, it is
     * allowed to return <code>null</code> for any node, regardless of whether
     * it is defined or not.
     * 
     * @param nodePath the absolute path of the node
     * @return a MetaNode which describes meta data information, can be
     *         <code>null</code> if there is no meta data available for the
     *         given node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodeUri</code>
     *         points to a node that is not defined in the tree (see above)
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    MetaNode getMetaNode(String[] nodePath) throws DmtException;

    /**
     * Get the size of the data in a leaf node. The value to return depends on
     * the format of the data in the node, see the description of the
     * {@link DmtData#getSize()} method for the definition of node size for each
     * format.
     * 
     * @param nodePath the absolute path of the leaf node
     * @return the size of the data in the node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>METADATA_MISMATCH</code> if the information could
     *         not be retrieved because of meta-data restrictions
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Size property is
     *         not supported by the plugin
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     * @see DmtData#getSize
     */
    int getNodeSize(String[] nodePath) throws DmtException;

    /**
     * Get the timestamp when the node was last modified.
     * 
     * @param nodePath the absolute path of the node
     * @return the timestamp of the last modification
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>METADATA_MISMATCH</code> if the information could
     *         not be retrieved because of meta-data restrictions
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Timestamp
     *         property is not supported by the plugin
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    Date getNodeTimestamp(String[] nodePath) throws DmtException;

    /**
     * Get the title of a node. There might be no title property set for a node.
     * 
     * @param nodePath the absolute path of the node
     * @return the title of the node, or <code>null</code> if the node has no
     *         title
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>METADATA_MISMATCH</code> if the information could
     *         not be retrieved because of meta-data restrictions
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Title property
     *         is not supported by the plugin
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    String getNodeTitle(String[] nodePath) throws DmtException;

    /**
     * Get the type of a node. The type of leaf node is the MIME type of the
     * data it contains. The type of an interior node is a URI identifying a DDF
     * document; a <code>null</code> type means that there is no DDF document
     * overriding the tree structure defined by the ancestors.
     * 
     * @param nodePath the absolute path of the node
     * @return the type of the node, can be <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>METADATA_MISMATCH</code> if the information could
     *         not be retrieved because of meta-data restrictions
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    String getNodeType(String[] nodePath) throws DmtException;

    /**
     * Check whether the specified path corresponds to a valid node in the DMT.
     * 
     * @param nodePath the absolute path to check
     * @return true if the given node exists in the DMT
     */
    boolean isNodeUri(String[] nodePath);

    /**
     * Tells whether a node is a leaf or an interior node of the DMT.
     * 
     * @param nodePath the absolute path of the node
     * @return true if the given node is a leaf node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>METADATA_MISMATCH</code> if the information could
     *         not be retrieved because of meta-data restrictions
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    boolean isLeafNode(String[] nodePath) throws DmtException;

    /**
     * Get the data contained in a leaf or interior node.
     * 
     * @param nodePath the absolute path of the node to retrieve
     * @return the data of the leaf node, must not be <code>null</code>
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>METADATA_MISMATCH</code> if the information could
     *         not be retrieved because of meta-data restrictions
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the specified node is
     *         an interior node and does not support Java object values
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    DmtData getNodeValue(String[] nodePath) throws DmtException;

    /**
     * Get the version of a node. The version can not be set, it is calculated
     * automatically by the device. It is incremented modulo 0x10000 at every
     * modification of the value or any other property of the node, for both
     * leaf and interior nodes. When a node is created the initial value is 0.
     * 
     * @param nodePath the absolute path of the node
     * @return the version of the node
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>nodePath</code>
     *         points to a non-existing node
     *         <li><code>METADATA_MISMATCH</code> if the information could
     *         not be retrieved because of meta-data restrictions
     *         <li><code>FEATURE_NOT_SUPPORTED</code> if the Version property
     *         is not supported by the plugin
     *         <li><code>DATA_STORE_FAILURE</code> if an error occurred while
     *         accessing the data store
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if the caller does not have the necessary
     *         permissions to execute the underlying management operation
     */
    int getNodeVersion(String[] nodePath) throws DmtException;
}
